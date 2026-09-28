package com.kaajjo.libresudoku.core.generator.dlx

import kotlin.random.Random

/**
 * A high-performance exact-cover solver based on Knuth's Dancing Links (Algorithm X).
 *
 * The constraint matrix is held in flat [IntArray] buffers rather than linked node objects, so no
 * per-node allocation occurs during the search. It is built once at construction and then reused:
 * every [solve], including a cooperative cancellation, restores its construction state, allowing a single
 * instance to serve both the initial full-solution pass and every uniqueness check during
 * generation.
 *
 * A single instance is deterministic: [random] is consumed once, while the matrix is built, so
 * solving the same board twice always yields the same first solution. Create a fresh engine for
 * every puzzle that must be randomized — caching one instance across generations would produce
 * identical grids.
 *
 * The engine is mutable and must not be shared across threads. [solvedBoard] returns a snapshot so
 * callers cannot mutate its internal solution buffer.
 *
 * @property boardSize the side length of the grid (e.g. 9 or 12)
 * @property gridSizeRow the height of a box in cells (sectionHeight)
 * @property gridSizeCol the width of a box in cells (sectionWidth)
 * @property random the randomness source used to shuffle row-insertion order, which randomizes each
 *   full solution and removes the need for explicit board seeding
 * @param buildCheckpoint Cooperative cancellation callback invoked while constructing the constraint matrix.
 */
internal class DlxEngine(
    private val boardSize: Int,
    private val gridSizeRow: Int,
    private val gridSizeCol: Int,
    private val random: Random = Random.Default,
    buildCheckpoint: () -> Unit = {}
) {
    init {
        require(boardSize > 0) { "boardSize must be positive" }
        require(boardSize <= 25) { "boardSize=$boardSize exceeds the supported maximum of 25" }
        require(gridSizeRow > 0 && gridSizeCol > 0) {
            "Box dimensions must be positive"
        }
        require(gridSizeRow.toLong() * gridSizeCol == boardSize.toLong()) {
            "Box dimensions ${gridSizeRow}x$gridSizeCol must contain exactly $boardSize cells"
        }

        val choices = boardSize.toLong() * boardSize * boardSize
        val constraints = boardSize.toLong() * boardSize * 4
        require(1L + constraints + choices * 4 <= Int.MAX_VALUE) {
            "boardSize=$boardSize is too large for the DLX matrix"
        }
    }

    private val numChoices = boardSize * boardSize * boardSize
    private val numConstraints = boardSize * boardSize * 4
    private val maxNodes = 1 + numConstraints + (numChoices * 4)

    // flat doubly-linked lists representing the sparse matrix
    private val left = IntArray(maxNodes)
    private val right = IntArray(maxNodes)
    private val up = IntArray(maxNodes)
    private val down = IntArray(maxNodes)

    private val columnOfNode = IntArray(maxNodes)
    private val rowOfNode = IntArray(maxNodes)
    private val columnSize = IntArray(numConstraints + 1)

    // fast access to a row's first node to quickly force clues during solve
    private val firstNodeOfRowId = IntArray(numChoices)

    private val head = 0
    private var nodeCounter = 0

    // pre allocated buffer for addRow to avoid IntArray allocations during matrix assembly
    private val colBuf = IntArray(4)

    private val currentSolution = IntArray(boardSize * boardSize)

    // first nodes of the rows forced as givens by the current solve, saved for rollback
    private val givenRows = IntArray(boardSize * boardSize)

    /**
     * The number of solutions found by the most recent [solve] call.
     */
    var solutionCount = 0
        private set

    private var limitReached = false


    private val solvedBoardBuffer = IntArray(boardSize * boardSize)

    /**
     * A snapshot of the first solution found by the most recent [solve], as a row-major grid of
     * values 1..N. It contains only zeros when no solution was found or capture was disabled, and after an aborted search.
     */
    val solvedBoard: IntArray
        get() = solvedBoardBuffer.clone()

    init {
        buildCheckpoint()
        initializeEmptyMatrix()
        buildSudokuMatrix(buildCheckpoint)
        buildCheckpoint()
    }

    /**
     * Initializes the column-header ring.
     *
     * Index 0 is the sentinel [head]; indices `1..numConstraints` are the constraint-column headers,
     * linked into a circular doubly linked list. Each header starts pointing to itself vertically
     * (an empty column) with a size of zero.
     */
    private fun initializeEmptyMatrix() {
        nodeCounter = numConstraints
        for (i in 0..numConstraints) {
            right[i] = if (i == numConstraints) head else i + 1
            left[i] = if (i == head) numConstraints else i - 1
            up[i] = i
            down[i] = i
            columnOfNode[i] = i
            columnSize[i] = 0
        }
    }

    /**
     * Populates the matrix with every candidate placement (row, column, value) for an empty board.
     *
     * Each placement contributes one matrix row satisfying four constraints: the cell, row, column,
     * and box "exactly once" rules. Insertion order is shuffled with [random], which randomizes every
     * column's down-links and therefore the order in which [search] explores candidates; solving an
     * empty board then produces a randomized full solution with no manual seeding. The logical row id
     * is derived directly from (r, c, v), so decoding in [solve] and [saveSolution] stays correct
     * regardless of insertion order.
     *
     * @param checkpoint Cooperative cancellation or deadline callback; exceptions propagate to the caller.
     */
    private fun buildSudokuMatrix(checkpoint: () -> Unit) {
        val order = IntArray(numChoices) { it }
        order.shuffle(random)

        val boxesPerRow = boardSize / gridSizeCol

        for ((orderIndex, rowId) in order.withIndex()) {
            if (orderIndex and 127 == 0) checkpoint()
            val r = rowId / (boardSize * boardSize)
            val c = (rowId / boardSize) % boardSize
            val v = rowId % boardSize

            val boxIndex = (r / gridSizeRow) * boxesPerRow + (c / gridSizeCol)

            val col1 = 1 + (r * boardSize + c)
            val col2 = 1 + boardSize * boardSize + (r * boardSize + v)
            val col3 = 1 + boardSize * boardSize * 2 + (c * boardSize + v)
            val col4 = 1 + boardSize * boardSize * 3 + (boxIndex * boardSize + v)

            addRow(rowId, col1, col2, col3, col4)
        }
    }

    /**
     * Links a single candidate placement into the matrix as one row spanning four constraint columns.
     *
     * Each new node is appended to the bottom of its column and inserted into the row's circular
     * horizontal list; the row's first node is recorded in [firstNodeOfRowId] for fast lookup when
     * forcing givens.
     *
     * @param rowId the logical identifier of this placement, encoding (r, c, v)
     * @param c1 the cell-constraint column index
     * @param c2 the row-constraint column index
     * @param c3 the column-constraint column index
     * @param c4 the box-constraint column index
     */
    private fun addRow(rowId: Int, c1: Int, c2: Int, c3: Int, c4: Int) {
        colBuf[0] = c1; colBuf[1] = c2; colBuf[2] = c3; colBuf[3] = c4
        var firstNodeInRow = -1

        for (col in colBuf) {
            nodeCounter++
            val newNode = nodeCounter

            rowOfNode[newNode] = rowId
            columnOfNode[newNode] = col

            // vertical linking: append to the bottom of the column
            val lastInColumn = up[col]
            down[lastInColumn] = newNode
            up[newNode] = lastInColumn
            down[newNode] = col
            up[col] = newNode

            columnSize[col]++

            // horizontal linking: insert into the row's circular list
            if (firstNodeInRow == -1) {
                firstNodeInRow = newNode
                left[newNode] = newNode
                right[newNode] = newNode
                firstNodeOfRowId[rowId] = firstNodeInRow
            } else {
                val lastInRow = left[firstNodeInRow]
                right[lastInRow] = newNode
                left[newNode] = lastInRow
                right[newNode] = firstNodeInRow
                left[firstNodeInRow] = newNode
            }
        }
    }

    /**
     * Solves the given board and counts its solutions.
     *
     * The method leaves the matrix unchanged: the state counters are reset on entry, the supplied
     * givens are covered and then uncovered in reverse on exit, so the matrix is restored to its
     * post-construction state and the instance can be reused. When at least one solution is found,
     * the first one is written to [solvedBoard] when saveFirstSolution is enabled.
     *
     * The board must not contain conflicting clues (the same value twice in a row, column or box)
     * or values outside 1..N; such boards are rejected with [IllegalArgumentException] and the
     * matrix is rolled back, so the engine stays usable. Covering a conflicting clue would corrupt
     * the shared matrix beyond repair.
     *
     * @param initialBoard the starting grid, row-major, with 0 for empty cells and 1..N for clues;
     *   its length must be exactly N*N
     * @param maxSolutions the number of solutions after which the search stops; 2 (the default) is
     *   enough to decide uniqueness, 1 finds a single solution as fast as possible. Anything much
     *   larger is unsafe on sparse boards, whose solution counts are astronomically big.
     * @param checkpoint Cooperative cancellation or deadline callback; exceptions propagate to the caller.
     * @param maxSearchNodes Positive node budget for this search; exhaustion throws [DlxSearchBudgetExceeded].
     * @param saveFirstSolution Whether to retain the first solution in [solvedBoard]; false performs counting only.
     * @return the number of solutions found (at most [maxSolutions])
     * @throws IllegalArgumentException if the board has the wrong length or contains an out-of-range
     *   or conflicting clue
     */
    fun solve(
        initialBoard: IntArray,
        maxSolutions: Int = 2,
        checkpoint: () -> Unit = {},
        maxSearchNodes: Long = 2_000_000L,
        saveFirstSolution: Boolean = true
    ): Int = execute(initialBoard, maxSolutions, -1, checkpoint, maxSearchNodes, saveFirstSolution)

    /**
     * PRECONDITION: restoring removedValue at removedCell makes the board uniquely solvable.
     * Under that condition, a second solution exists iff there is a solution with a DIFFERENT
     * value at that cell. Only for single-clue removal; NOT for removing a symmetry pair.
     * Search/cancellation limits throw; they never return false ("no alternative").
     *
     * @param board Row-major grid after removing exactly one clue from a known unique puzzle.
     * @param removedCell Zero-based position of that removed clue; its board entry must be zero.
     * @param removedValue Original one-based value of the removed clue.
     * @param checkpoint Cooperative cancellation or deadline callback; exceptions propagate to the caller.
     * @param maxSearchNodes Positive node budget for this search; exhaustion throws [DlxSearchBudgetExceeded].
     */
    fun hasAlternativeAfterRemoval(
        board: IntArray, removedCell: Int, removedValue: Int,
        checkpoint: () -> Unit = {}, maxSearchNodes: Long = 2_000_000L
    ): Boolean {
        require(board.size == boardSize * boardSize)
        require(removedCell in board.indices && board[removedCell] == 0)
        require(removedValue in 1..boardSize)
        val rowId = removedCell * boardSize + removedValue - 1
        return execute(board, 1, rowId, checkpoint, maxSearchNodes, false) != 0
    }

    var searchNodes: Long = 0L
        private set
    private var checkpointAction: () -> Unit = {}
    private var searchNodeLimit = Long.MAX_VALUE
    private var captureSolution = true
    private var busy = false

    private fun execute(
        initialBoard: IntArray, maxSolutions: Int, excludedRowId: Int,
        checkpoint: () -> Unit, maxSearchNodes: Long, saveFirstSolution: Boolean
    ): Int {
        require(maxSolutions >= 1)
        require(maxSearchNodes > 0L)
        require(initialBoard.size == boardSize * boardSize)
        check(!busy) { "DLX is not concurrent or reentrant; own one instance per request" }
        busy = true
        solutionCount = 0; limitReached = false; searchNodes = 0L
        solvedBoardBuffer.fill(0)
        checkpointAction = checkpoint; searchNodeLimit = maxSearchNodes; captureSolution = saveFirstSolution
        var depth = 0
        var excluded = -1
        try {
            checkpointAction()
            if (excludedRowId >= 0) {
                excluded = firstNodeOfRowId[excludedRowId]
                var node = excluded
                do {
                    down[up[node]] = down[node]; up[down[node]] = up[node]
                    columnSize[columnOfNode[node]]--
                    node = right[node]
                } while (node != excluded)
            }
            for (cell in initialBoard.indices) {
                checkpointAction()
                val value = initialBoard[cell]
                if (value == 0) continue
                require(value in 1..boardSize) { "Value $value at cell $cell outside 1..$boardSize" }
                val rowId = cell * boardSize + value - 1
                val firstNode = firstNodeOfRowId[rowId]
                require(down[up[firstNode]] == firstNode) { "Conflicting clue $value at cell $cell" }
                // No throwing checkpoints inside this indivisible cover group.
                cover(columnOfNode[firstNode])
                var j = right[firstNode]
                while (j != firstNode) { cover(columnOfNode[j]); j = right[j] }
                givenRows[depth] = firstNode; currentSolution[depth] = rowId; depth++
            }
            search(depth, maxSolutions)
            checkpointAction()
            return solutionCount
        } catch (failure: Throwable) {
            solutionCount = 0; solvedBoardBuffer.fill(0)
            throw failure
        } finally {
            uncoverGivens(depth)
            if (excluded >= 0) {
                var node = left[excluded]
                while (true) {
                    columnSize[columnOfNode[node]]++
                    down[up[node]] = node; up[down[node]] = node
                    if (node == excluded) break
                    node = left[node]
                }
            }
            checkpointAction = {}; busy = false
        }
    }

    /**
     * Backtracks the first [depth] forced givens in strict reverse order, leaving the matrix
     * perfectly clean. Cover/uncover don't alter left/right data node links, making row traversal
     * safe here.
     *
     * @param depth Number of forced given rows to restore in reverse order.
     */
    private fun uncoverGivens(depth: Int) {
        for (d in depth - 1 downTo 0) {
            val firstNode = givenRows[d]
            var j = left[firstNode]
            while (j != firstNode) {
                uncover(columnOfNode[j])
                j = left[j]
            }
            uncover(columnOfNode[firstNode])
        }
    }

    /**
     * Recursively searches for an exact cover (Algorithm X).
     *
     * Selects the unsatisfied column with the fewest candidate rows (Knuth's minimum-size column
     * heuristic), covers it, and tries each candidate row in turn. Every cover is matched by an
     * uncover even on early termination, so the matrix stays consistent for later reuse.
     *
     * @param depth the number of rows currently committed to the partial solution
     * @param maxSolutions the number of solutions after which the search stops
     */
    private fun search(depth: Int, maxSolutions: Int) {
        if (limitReached) return
        if (searchNodes >= searchNodeLimit) throw DlxSearchBudgetExceeded(searchNodes)
        searchNodes++
        if (searchNodes and 127L == 1L) checkpointAction()

        if (right[head] == head) {
            solutionCount++
            if (solutionCount == 1 && captureSolution) saveSolution(depth)
            if (solutionCount >= maxSolutions) limitReached = true
            return
        }

        // S heuristic: choose the column with the fewest candidates
        // size 0 is a dead end and size 1 is a forced move, so stop scanning at either
        var minSize = Int.MAX_VALUE
        var bestCol = head
        var c = right[head]
        while (c != head) {
            val size = columnSize[c]
            if (size < minSize) {
                minSize = size
                bestCol = c
                if (size <= 1) break
            }
            c = right[c]
        }

        if (minSize == 0) return

        cover(bestCol)
        try {
            var r = down[bestCol]
            while (r != bestCol) {
                currentSolution[depth] = rowOfNode[r]
                var j = right[r]
                while (j != r) { cover(columnOfNode[j]); j = right[j] }
                try {
                    search(depth + 1, maxSolutions)
                } finally {
                    j = left[r]
                    while (j != r) { uncover(columnOfNode[j]); j = left[j] }
                }
                if (limitReached) break
                r = down[r]
            }
        } finally {
            uncover(bestCol)
        }
    }

    /**
     * Removes a column from the matrix and unlinks every row that intersects it.
     *
     * Standard Dancing Links cover: the header is detached from the header ring and, for each row in
     * the column, that row's other nodes are spliced out vertically and their column sizes decremented.
     *
     * @param colHeader the index of the column header to cover
     */
    private fun cover(colHeader: Int) {
        right[left[colHeader]] = right[colHeader]
        left[right[colHeader]] = left[colHeader]

        var i = down[colHeader]
        while (i != colHeader) {
            var j = right[i]
            while (j != i) {
                down[up[j]] = down[j]
                up[down[j]] = up[j]
                columnSize[columnOfNode[j]]--
                j = right[j]
            }
            i = down[i]
        }
    }

    /**
     * Reverses [cover], restoring a column and every row that intersects it.
     *
     * The links are reattached in the exact reverse order of [cover], returning the matrix to its
     * prior state.
     *
     * @param colHeader the index of the column header to uncover
     */
    private fun uncover(colHeader: Int) {
        var i = up[colHeader]
        while (i != colHeader) {
            var j = left[i]
            while (j != i) {
                columnSize[columnOfNode[j]]++
                down[up[j]] = j
                up[down[j]] = j
                j = left[j]
            }
            i = up[i]
        }

        right[left[colHeader]] = colHeader
        left[right[colHeader]] = colHeader
    }

    /**
     * Decodes the committed row ids into [solvedBoardBuffer] as a row-major grid of values 1..N.
     *
     * @param depth the number of committed rows in the partial solution to decode
     */
    private fun saveSolution(depth: Int) {
        for (i in 0 until depth) {
            val rowId = currentSolution[i]
            val cell = rowId / boardSize
            solvedBoardBuffer[cell] = rowId % boardSize + 1
        }
    }
}

/**
 * Incomplete search, never evidence of uniqueness.
 *
 * @param nodes Number of visited search nodes when the limit was reached.
 */
class DlxSearchBudgetExceeded(val nodes: Long) : RuntimeException("DLX search node budget exhausted at $nodes")
