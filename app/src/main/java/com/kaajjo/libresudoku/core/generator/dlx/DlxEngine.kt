package com.kaajjo.libresudoku.core.generator.dlx

import kotlin.random.Random

/**
 * A high-performance exact-cover solver based on Knuth's Dancing Links (Algorithm X).
 *
 * The constraint matrix is held in flat [IntArray] buffers rather than linked node objects, so no
 * per-node allocation occurs during the search. It is built once at construction and then reused:
 * every [solve] leaves it in exactly the state it had after construction, allowing a single
 * instance to serve both the initial full-solution pass and every uniqueness check during
 * generation.
 *
 * @property boardSize the side length of the grid (e.g. 9 or 12)
 * @property gridSizeRow the height of a box in cells (sectionHeight)
 * @property gridSizeCol the width of a box in cells (sectionWidth)
 * @property random the randomness source used to shuffle row-insertion order, which randomizes each
 *   full solution and removes the need for explicit board seeding
 */
class DlxEngine(
    private val boardSize: Int,
    private val gridSizeRow: Int,
    private val gridSizeCol: Int,
    private val random: Random = Random.Default
) {
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

    // pre allocated buffer for addRow to avoid IntArray allocations during matrix assembly
    private val givenRows = IntArray(boardSize * boardSize)

    /**
     * The number of solutions found by the most recent [solve] call.
     */
    var solutionCount = 0
        private set

    /**
     * True once enough solutions have been found to stop early (a second solution under `limitToTwo`).
     */
    var isSolved = false
        private set


    /**
     * The first solution found by the most recent [solve], as a row-major grid of values 1..N.
     */
    val solvedBoard = IntArray(boardSize * boardSize)

    init {
        initializeEmptyMatrix()
        buildSudokuMatrix()
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
     */
    private fun buildSudokuMatrix() {
        val order = IntArray(numChoices) { it }
        order.shuffle(random)

        val boxesPerRow = boardSize / gridSizeCol

        for (rowId in order) {
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
     * post-construction state and the instance can be reused. When a single solution is found it is
     * written to [solvedBoard].
     *
     * @param initialBoard the starting grid, row-major, with 0 for empty cells and 1..N for clues
     * @param limitToTwo if true, the search stops as soon as a second solution is found (enough to
     *   decide uniqueness); if false, all solutions are counted
     * @return the number of solutions found (0, 1, or 2 when [limitToTwo] is set)
     */
    fun solve(initialBoard: IntArray, limitToTwo: Boolean = true): Int {
        // state reset is mandatory for safe engine reuse
        solutionCount = 0
        isSolved = false

        var depth = 0

        // apply given clues by forcing their corresponding rows
        for (i in initialBoard.indices) {
            val value = initialBoard[i]
            if (value != 0) {
                val r = i / boardSize
                val c = i % boardSize
                val v = value - 1

                val rowId = (r * boardSize * boardSize) + (c * boardSize) + v
                val firstNode = firstNodeOfRowId[rowId]

                cover(columnOfNode[firstNode])
                var j = right[firstNode]
                while (j != firstNode) {
                    cover(columnOfNode[j])
                    j = right[j]
                }

                givenRows[depth] = firstNode
                currentSolution[depth] = rowId
                depth++
            }
        }

        search(depth, limitToTwo)

        // backtrack clues in strict reverse order to leave the matrix perfectly clean
        // cover/uncover don't alter left/right data node links, making row traversal safe here
        for (d in depth - 1 downTo 0) {
            val firstNode = givenRows[d]
            var j = left[firstNode]
            while (j != firstNode) {
                uncover(columnOfNode[j])
                j = left[j]
            }
            uncover(columnOfNode[firstNode])
        }

        return solutionCount
    }

    /**
     * Recursively searches for an exact cover (Algorithm X).
     *
     * Selects the unsatisfied column with the fewest candidate rows (Knuth's minimum-size column
     * heuristic), covers it, and tries each candidate row in turn. Every cover is matched by an
     * uncover even on early termination, so the matrix stays consistent for later reuse.
     *
     * @param depth the number of rows currently committed to the partial solution
     * @param limitToTwo if true, aborts the search once two solutions have been counted
     */
    private fun search(depth: Int, limitToTwo: Boolean) {
        if (isSolved && limitToTwo) return // safe to exit early, no columns are covered in this frame

        if (right[head] == head) {
            solutionCount++
            if (solutionCount == 1) saveSolution(depth)
            if (solutionCount >= 2 && limitToTwo) isSolved = true
            return
        }

        // S heuristic: choose the column with the fewest candidates
        var minSize = Int.MAX_VALUE
        var bestCol = head
        var c = right[head]
        while (c != head) {
            if (columnSize[c] < minSize) {
                minSize = columnSize[c]
                bestCol = c
            }
            c = right[c]
        }

        if (minSize == 0) return

        cover(bestCol)

        var r = down[bestCol]
        while (r != bestCol) {
            currentSolution[depth] = rowOfNode[r]

            var j = right[r]
            while (j != r) {
                cover(columnOfNode[j])
                j = right[j]
            }

            search(depth + 1, limitToTwo)

            // always uncover the row, even before an early exit
            // otherwise, the matrix remains corrupted and the engine cannot be reused
            j = left[r]
            while (j != r) {
                uncover(columnOfNode[j])
                j = left[j]
            }

            if (isSolved && limitToTwo) break // stop searching, but ensure the matrix is left consistent

            r = down[r]
        }

        uncover(bestCol)
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
     * Decodes the committed row ids into [solvedBoard] as a row-major grid of values 1..N.
     *
     * @param depth the number of committed rows in the partial solution to decode
     */
    private fun saveSolution(depth: Int) {
        for (i in 0 until depth) {
            val rowId = currentSolution[i]
            val r = rowId / (boardSize * boardSize)
            val c = (rowId / boardSize) % boardSize
            val v = (rowId % boardSize) + 1
            solvedBoard[r * boardSize + c] = v
        }
    }
}
