package com.kaajjo.libresudoku.core.qqwing;

// @formatter:off
/*
 * qqwing - Sudoku solver and generator
 * Copyright (C) 2006-2014 Stephen Ostermiller http://ostermiller.org/
 * Copyright (C) 2007 Jacques Bensimon (jacques@ipm.com)
 * Copyright (C) 2007 Joel Yarde (joel.yarde - gmail.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
// @formatter:on

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The board containing all the memory structures and methods for solving or
 * generating sudoku puzzles.
 */
public class QQWing {

    public static final String QQWING_VERSION = "1.3.4";

    private static final String NL = System.getProperties().getProperty("line.separator");

    //public static final int GRID_SIZE = 3;

    public static int GRID_SIZE_ROW = 3;

    public static int GRID_SIZE_COL = 3;

    public static int ROW_COL_SEC_SIZE = (GRID_SIZE_ROW * GRID_SIZE_COL);

    public static int SEC_GROUP_SIZE = (ROW_COL_SEC_SIZE * GRID_SIZE_ROW);

    public static  int BOARD_SIZE = (ROW_COL_SEC_SIZE * ROW_COL_SEC_SIZE);

    public static int POSSIBILITY_SIZE = (BOARD_SIZE * ROW_COL_SEC_SIZE);

    private static Random random = new Random();

    /**
     * The last round of solving
     */
    private int lastSolveRound;

    /**
     * The 81 integers that make up a sudoku puzzle. Givens are 1-9, unknowns
     * are 0. Once initialized, this puzzle remains as is. The answer is worked
     * out in "solution".
     */
    private int[] puzzle = new int[BOARD_SIZE];

    /**
     * The 81 integers that make up a sudoku puzzle. The solution is built here,
     * after completion all will be 1-9.
     */
    private int[] solution = new int[BOARD_SIZE];

    /**
     * Recursion depth at which each of the numbers in the solution were placed.
     * Useful for backing out solve branches that don't lead to a solution.
     */
    private int[] solutionRound = new int[BOARD_SIZE];

    /**
     * The 729 integers that make up a the possible values for a Sudoku puzzle.
     * (9 possibilities for each of 81 squares). If possibilities[i] is zero,
     * then the possibility could still be filled in according to the Sudoku
     * rules. When a possibility is eliminated, possibilities[i] is assigned the
     * round (recursion level) at which it was determined that it could not be a
     * possibility.
     */
    private int[] possibilities = new int[POSSIBILITY_SIZE];

    /**
     * An array the size of the board (81) containing each of the numbers 0-n
     * exactly once. This array may be shuffled so that operations that need to
     * look at each cell can do so in a random order.
     */
    private int[] randomBoardArray = fillIncrementing(new int[BOARD_SIZE]);

    /**
     * An array with one element for each position (9), in some random order to
     * be used when trying each position in turn during guesses.
     */
    private int[] randomPossibilityArray = fillIncrementing(new int[ROW_COL_SEC_SIZE]);

    /**
     * Whether or not to record history
     */
    private boolean recordHistory = false;

    /**
     * Whether or not to print history as it happens
     */
    private boolean logHistory = false;

    /**
     * A list of moves used to solve the puzzle. This list contains all moves,
     * even on solve branches that did not lead to a solution.
     */
    private final ArrayList<LogItem> solveHistory = new ArrayList<LogItem>();

    /**
     * A list of moves used to solve the puzzle. This list contains only the
     * moves needed to solve the puzzle, but doesn't contain information about
     * bad guesses.
     */
    private final ArrayList<LogItem> solveInstructions = new ArrayList<LogItem>();

    /**
     * The style with which to print puzzles and solutions
     */
    private PrintStyle printStyle = PrintStyle.READABLE;

    private GameType gameType = GameType.Unspecified;
    private GameDifficulty difficulty = GameDifficulty.Unspecified;

    /**
     * Create a new Sudoku board
     */
    public QQWing(GameType type, GameDifficulty difficulty) {
        gameType = type;
        this.difficulty = difficulty;

        GRID_SIZE_ROW = type.getSectionHeight();    // 3    // 2

        GRID_SIZE_COL = type.getSectionWidth();     // 3    // 3

        ROW_COL_SEC_SIZE = (GRID_SIZE_ROW * GRID_SIZE_COL); //  3*3 = 9     // 6

        SEC_GROUP_SIZE = (ROW_COL_SEC_SIZE * GRID_SIZE_ROW);    // 9 * 3 = 27 ? // 12

        BOARD_SIZE = (ROW_COL_SEC_SIZE * ROW_COL_SEC_SIZE);     // 9 * 9 = 81   // 36

        POSSIBILITY_SIZE = (BOARD_SIZE * ROW_COL_SEC_SIZE);     // 81 * 9

        puzzle = new int[BOARD_SIZE];

        solution = new int[BOARD_SIZE];

        solutionRound = new int[BOARD_SIZE];

        possibilities = new int[POSSIBILITY_SIZE];

        randomBoardArray = fillIncrementing(new int[BOARD_SIZE]);

        randomPossibilityArray = fillIncrementing(new int[ROW_COL_SEC_SIZE]);
    }

    private static int[] fillIncrementing(int[] arr){
        for (int i = 0; i < arr.length; i++) {
            arr[i] = i;
        }
        return arr;
    }

    /**
     * Get the number of cells that are set in the puzzle (as opposed to figured
     * out in the solution
     */
    public int getGivenCount() {
        int count = 0;
        for (int i = 0; i < BOARD_SIZE; i++) {
            if (puzzle[i] != 0) count++;
        }
        return count;
    }

    /**
     * Set the board to the given puzzle. The given puzzle must be an array of
     * 81 integers.
     */
    public boolean setPuzzle(int[] initPuzzle) {
        for (int i = 0; i < BOARD_SIZE; i++) {
            puzzle[i] = (initPuzzle == null) ? 0 : initPuzzle[i];
        }
        return reset();
    }

    public void setRandom(int seed) {
        random = new Random(seed);
    }

    /**
     * Reset the board to its initial state with only the givens. This method
     * clears any solution, resets statistics, and clears any history messages.
     */
    private boolean reset() {
        Arrays.fill(solution, 0);
        Arrays.fill(solutionRound, 0);
        Arrays.fill(possibilities, 0);
        solveHistory.clear();
        solveInstructions.clear();

        int round = 1;
        for (int position = 0; position < BOARD_SIZE; position++) {
            if (puzzle[position] > 0) {
                int valIndex = puzzle[position] - 1;
                int valPos = getPossibilityIndex(valIndex, position);
                int value = puzzle[position];
                if (possibilities[valPos] != 0) return false;
                mark(position, round, value);
                if (logHistory || recordHistory) addHistoryItem(new LogItem(round, LogType.GIVEN, value, position));
            }
        }

        return true;
    }

    /**
     * Get the gameDifficulty rating.
     */
    public GameDifficulty getDifficulty() {
        if (getGuessCount() > 0) return GameDifficulty.Challenge;
        if (getBoxLineReductionCount() > 0) return GameDifficulty.Hard;
        if (getPointingPairTripleCount() > 0) return GameDifficulty.Hard;
        if (getHiddenPairCount() > 0) return GameDifficulty.Moderate;
        if (getNakedPairCount() > 0) return GameDifficulty.Moderate;

        switch (gameType) {
            case Default6x6:
                if (getHiddenSingleCount() > 0)
                    return GameDifficulty.Moderate;
                break;
            case Default9x9:
                if (getHiddenSingleCount() > 10) {
                    return GameDifficulty.Moderate;
                }
                break;
            case Default12x12:
                if (getHiddenSingleCount() > 20)
                    return GameDifficulty.Moderate;
                break;
            default:
                if(getHiddenSingleCount() > 10)
                    return GameDifficulty.Moderate;
        }
        switch (gameType) {
            case Default6x6:
                if (getSingleCount() > 10)
                    return GameDifficulty.Easy;
                break;
            case Default9x9:
                if(getSingleCount() > 35)
                    return GameDifficulty.Easy;
                break;
            default:
                if (getSingleCount() > 20)
                    return GameDifficulty.Easy;
        }

        return GameDifficulty.Unspecified;
    }

    /**
     * Get the gameDifficulty rating.
     */
    public String getDifficultyAsString() {
        return getDifficulty().name();
    }

    /**
     * Get the number of cells for which the solution was determined because
     * there was only one possible value for that cell.
     */
    public int getSingleCount() {
        return getLogCount(solveInstructions, LogType.SINGLE);
    }

    /**
     * Get the number of cells for which the solution was determined because
     * that cell had the only possibility for some value in the row, column, or
     * section.
     */
    public int getHiddenSingleCount() {
        return (getLogCount(solveInstructions, LogType.HIDDEN_SINGLE_ROW) +
                getLogCount(solveInstructions, LogType.HIDDEN_SINGLE_COLUMN) + getLogCount(solveInstructions, LogType.HIDDEN_SINGLE_SECTION));
    }

    /**
     * Get the number of naked pair reductions that were performed in solving
     * this puzzle.
     */
    public int getNakedPairCount() {
        return (getLogCount(solveInstructions, LogType.NAKED_PAIR_ROW) +
                getLogCount(solveInstructions, LogType.NAKED_PAIR_COLUMN) + getLogCount(solveInstructions, LogType.NAKED_PAIR_SECTION));
    }

    /**
     * Get the number of hidden pair reductions that were performed in solving
     * this puzzle.
     */
    public int getHiddenPairCount() {
        return (getLogCount(solveInstructions, LogType.HIDDEN_PAIR_ROW) +
                getLogCount(solveInstructions, LogType.HIDDEN_PAIR_COLUMN) + getLogCount(solveInstructions, LogType.HIDDEN_PAIR_SECTION));
    }

    /**
     * Get the number of pointing pair/triple reductions that were performed in
     * solving this puzzle.
     */
    public int getPointingPairTripleCount() {
        return (getLogCount(solveInstructions, LogType.POINTING_PAIR_TRIPLE_ROW) + getLogCount(solveInstructions, LogType.POINTING_PAIR_TRIPLE_COLUMN));
    }

    /**
     * Get the number of box/line reductions that were performed in solving this
     * puzzle.
     */
    public int getBoxLineReductionCount() {
        return (getLogCount(solveInstructions, LogType.ROW_BOX) + getLogCount(solveInstructions, LogType.COLUMN_BOX));
    }

    /**
     * Get the number lucky guesses in solving this puzzle.
     */
    public int getGuessCount() {
        return getLogCount(solveInstructions, LogType.GUESS);
    }

    /**
     * Get the number of backtracks (unlucky guesses) required when solving this
     * puzzle.
     */
    public int getBacktrackCount() {
        return getLogCount(solveHistory, LogType.ROLLBACK);
    }

    private void shuffleRandomArrays() {
        shuffleArray(randomBoardArray, BOARD_SIZE);
        shuffleArray(randomPossibilityArray, ROW_COL_SEC_SIZE);
    }

    private void clearPuzzle() {
        // Clear any existing puzzle
        for (int i = 0; i < BOARD_SIZE; i++) {
            puzzle[i] = 0;
        }
        reset();
    }

    public boolean generatePuzzle() {
        return generatePuzzleSymmetry(Symmetry.NONE);
    }

    public boolean generatePuzzleSymmetry(Symmetry symmetry) {

        if (symmetry == Symmetry.RANDOM) symmetry = getRandomSymmetry();

        // Don't record history while generating.
        boolean recHistory = recordHistory;
        setRecordHistory(false);
        boolean lHistory = logHistory;
        setLogHistory(false);

        clearPuzzle();

        // Start by getting the randomness in order so that
        // each puzzle will be different from the last.
        shuffleRandomArrays();

        // Now solve the puzzle the whole way. The solve
        // uses random algorithms, so we should have a
        // really randomly totally filled sudoku
        // Even when starting from an empty grid
        solve();

        if (symmetry == Symmetry.NONE) {
            // Rollback any square for which it is obvious that
            // the square doesn't contribute to a unique solution
            // (ie, squares that were filled by logic rather
            // than by guess)
            rollbackNonGuesses();
        }

        // Record all marked squares as the puzzle so
        // that we can call countSolutions without losing it.
        for (int i = 0; i < BOARD_SIZE; i++) {
            puzzle[i] = solution[i];
        }

        // Rerandomize everything so that we test squares
        // in a different order than they were added.
        shuffleRandomArrays();

        // Remove one value at a time and see if
        // the puzzle still has only one solution.
        // If it does, leave it out the point because
        // it is not needed.
        for (int i = 0; i < BOARD_SIZE; i++) {
            // check all the positions, but in shuffled order
            int position = randomBoardArray[i];
            if (puzzle[position] > 0) {
                int positionsym1 = -1;
                int positionsym2 = -1;
                int positionsym3 = -1;
                switch (symmetry) {
                    case ROTATE90:
                        positionsym2 = rowColumnToCell(ROW_COL_SEC_SIZE - 1 - cellToColumn(position), cellToRow(position));
                        positionsym3 = rowColumnToCell(cellToColumn(position), ROW_COL_SEC_SIZE - 1 - cellToRow(position));
                    case ROTATE180:
                        positionsym1 = rowColumnToCell(ROW_COL_SEC_SIZE - 1 - cellToRow(position), ROW_COL_SEC_SIZE - 1 - cellToColumn(position));
                        break;
                    case MIRROR:
                        positionsym1 = rowColumnToCell(cellToRow(position), ROW_COL_SEC_SIZE - 1 - cellToColumn(position));
                        break;
                    case FLIP:
                        positionsym1 = rowColumnToCell(ROW_COL_SEC_SIZE - 1 - cellToRow(position), cellToColumn(position));
                        break;
                    default:
                        break;
                }
                // try backing out the value and
                // counting solutions to the puzzle
                int savedValue = puzzle[position];
                puzzle[position] = 0;
                int savedSym1 = 0;
                if (positionsym1 >= 0) {
                    savedSym1 = puzzle[positionsym1];
                    puzzle[positionsym1] = 0;
                }
                int savedSym2 = 0;
                if (positionsym2 >= 0) {
                    savedSym2 = puzzle[positionsym2];
                    puzzle[positionsym2] = 0;
                }
                int savedSym3 = 0;
                if (positionsym3 >= 0) {
                    savedSym3 = puzzle[positionsym3];
                    puzzle[positionsym3] = 0;
                }
                reset();
                if (countSolutions(2, true) > 1) {
                    // Put it back in, it is needed
                    puzzle[position] = savedValue;
                    if (positionsym1 >= 0 && savedSym1 != 0) puzzle[positionsym1] = savedSym1;
                    if (positionsym2 >= 0 && savedSym2 != 0) puzzle[positionsym2] = savedSym2;
                    if (positionsym3 >= 0 && savedSym3 != 0) puzzle[positionsym3] = savedSym3;
                }
            }
        }

        // Clear all solution info, leaving just the puzzle.
        reset();

        // Restore recording history.
        setRecordHistory(recHistory);
        setLogHistory(lHistory);

        return true;
    }

    private void rollbackNonGuesses() {
        // Guesses are odd rounds
        // Non-guesses are even rounds
        for (int i = 2; i <= lastSolveRound; i += 2) {
            rollbackRound(i);

            // Some hack to make easy levels on 12x12 .. because the generator wasn't able to create some
            //if(difficulty == GameDifficulty.Easy && gameType == GameType.Default_12x12) {
            //    i += 2; // skip every 2nd round to find "easy" levels more frequent. Still takes about 20 Seconds.
            //}
        }
    }

    public void setPrintStyle(PrintStyle ps) {
        printStyle = ps;
    }

    public void setRecordHistory(boolean recHistory) {
        recordHistory = recHistory;
    }

    public void setLogHistory(boolean logHist) {
        logHistory = logHist;
    }

    private void addHistoryItem(LogItem l) {
        if (logHistory) {
            l.print();
            System.out.println();
        }
        if (recordHistory) {
            solveHistory.add(l); // ->push_back(l);
            solveInstructions.add(l); // ->push_back(l);
        } else {
            l = null;
        }
    }

    private void printHistory(ArrayList<LogItem> v) {
        System.out.print(historyToString(v));
    }

    private String historyToString(ArrayList<LogItem> v) {
        StringBuilder sb = new StringBuilder();
        if (!recordHistory) {
            sb.append("History was not recorded.").append(NL);
            if (printStyle == PrintStyle.CSV) {
                sb.append(" -- ").append(NL);
            } else {
                sb.append(NL);
            }
        }
        for (int i = 0; i < v.size(); i++) {
            sb.append(i + 1 + ". ").append(NL);
            (v.get(i)).print();
            if (printStyle == PrintStyle.CSV) {
                sb.append(" -- ").append(NL);
            } else {
                sb.append(NL);
            }
        }
        if (printStyle == PrintStyle.CSV) {
            sb.append(",").append(NL);
        } else {
            sb.append(NL);
        }
        return sb.toString();
    }

    public void printSolveInstructions() {
        System.out.print(getSolveInstructionsString());
    }

    public String getSolveInstructionsString() {
        if (isSolved()) {
            return historyToString(solveInstructions);
        } else {
            return "No solve instructions - Puzzle is not possible to solve.";
        }
    }

    public List<LogItem> getSolveInstructions() {
        if (isSolved()) {
            return Collections.unmodifiableList(solveInstructions);
        } else {
            return Collections.emptyList();
        }
    }

    public void printSolveHistory() {
        printHistory(solveHistory);
    }

    public String getSolveHistoryString() {
        return historyToString(solveHistory);
    }

    public List<LogItem> getSolveHistory() {
        return Collections.unmodifiableList(solveHistory);
    }

    public boolean solve() {
        reset();
        shuffleRandomArrays();
        return solve(2);
    }

    private boolean solve(int round) {
        lastSolveRound = round;
        while (singleSolveMove(round)) {
            if (isSolved()) return true;
            if (isImpossible()) return false;
        }

        int nextGuessRound = round + 1;
        int nextRound = round + 2;
        for (int guessNumber = 0; guess(nextGuessRound, guessNumber); guessNumber++) {
            if (isImpossible() || !solve(nextRound)) {
                rollbackRound(nextRound);
                rollbackRound(nextGuessRound);
            } else {
                return true;
            }
        }
        return false;
    }

    /**
     * return true if the puzzle has a solution
     * and only a single solution
     */
    public boolean hasUniqueSolution(){
        return countSolutionsLimited() == 1;
    }

    /**
     * Count the number of solutions to the puzzle
     */
    public int countSolutions() {
        return countSolutions(false);
    }

    /**
     * Count the number of solutions to the puzzle
     * but return two any time there are two or
     * more solutions.  This method will run much
     * falter than countSolutions() when there
     * are many possible solutions and can be used
     * when you are interested in knowing if the
     * puzzle has zero, one, or multiple solutions.
     */
    public int countSolutionsLimited(){
        return countSolutions(true);
    }

    private int countSolutions(boolean limitToTwo) {
        // Don't record history while generating.
        boolean recHistory = recordHistory;
        setRecordHistory(false);
        boolean lHistory = logHistory;
        setLogHistory(false);

        reset();
        int solutionCount = countSolutions(2, limitToTwo);

        // Restore recording history.
        setRecordHistory(recHistory);
        setLogHistory(lHistory);

        return solutionCount;
    }

    private int countSolutions(int round, boolean limitToTwo) {
        while (singleSolveMove(round)) {
            if (isSolved()) {
                rollbackRound(round);
                return 1;
            }
            if (isImpossible()) {
                rollbackRound(round);
                return 0;
            }
        }

        int solutions = 0;
        int nextRound = round + 1;
        for (int guessNumber = 0; guess(nextRound, guessNumber); guessNumber++) {
            solutions += countSolutions(nextRound, limitToTwo);
            if (limitToTwo && solutions >= 2) {
                rollbackRound(round);
                return solutions;
            }
        }
        rollbackRound(round);
        return solutions;
    }

    private void rollbackRound(int round) {
        if (logHistory || recordHistory) addHistoryItem(new LogItem(round, LogType.ROLLBACK));
        for (int i = 0; i < BOARD_SIZE; i++) {
            if (solutionRound[i] == round) {
                solutionRound[i] = 0;
                solution[i] = 0;
            }
        }
        for (int i = 0; i < POSSIBILITY_SIZE; i++) {
            if (possibilities[i] == round) {
                possibilities[i] = 0;
            }
        }
        while (solveInstructions.size() > 0 && (solveInstructions.get(solveInstructions.size() - 1)).getRound() == round) {
            int i = solveInstructions.size() - 1;
            solveInstructions.remove(i);
        }
    }

    public boolean isSolved() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            if (solution[i] == 0) {
                return false;
            }
        }
        return true;
    }

    private boolean isImpossible() {
        for (int position = 0; position < BOARD_SIZE; position++) {
            if (solution[position] == 0) {
                int count = 0;
                for (int valIndex = 0; valIndex < ROW_COL_SEC_SIZE; valIndex++) {
                    int valPos = getPossibilityIndex(valIndex, position);
                    if (possibilities[valPos] == 0) count++;
                }
                if (count == 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private int findPositionWithFewestPossibilities() {
        int minPossibilities = ROW_COL_SEC_SIZE+1;
        int bestPosition = 0;
        for (int i = 0; i < BOARD_SIZE; i++) {
            int position = randomBoardArray[i];
            if (solution[position] == 0) {
                int count = 0;
                for (int valIndex = 0; valIndex < ROW_COL_SEC_SIZE; valIndex++) {
                    int valPos = getPossibilityIndex(valIndex, position);
                    if (possibilities[valPos] == 0) count++;
                }
                if (count < minPossibilities) {
                    minPossibilities = count;
                    bestPosition = position;
                }
            }
        }
        return bestPosition;
    }

    private boolean guess(int round, int guessNumber) {
        int localGuessCount = 0;
        int position = findPositionWithFewestPossibilities();
        for (int i = 0; i < ROW_COL_SEC_SIZE; i++) {
            int valIndex = randomPossibilityArray[i];
            int valPos = getPossibilityIndex(valIndex, position);
            if (possibilities[valPos] == 0) {
                if (localGuessCount == guessNumber) {
                    int value = valIndex + 1;
                    if (logHistory || recordHistory) addHistoryItem(new LogItem(round, LogType.GUESS, value, position));
                    mark(position, round, value);
                    return true;
                }
                localGuessCount++;
            }
        }
        return false;
    }

    private boolean singleSolveMove(int round) {
        if (onlyPossibilityForCell(round)) return true;
        if (onlyValueInSection(round)) return true;
        if (onlyValueInRow(round)) return true;
        if (onlyValueInColumn(round)) return true;
        if (handleNakedPairs(round)) return true;
        if (pointingRowReduction(round)) return true;
        if (pointingColumnReduction(round)) return true;
        if (rowBoxReduction(round)) return true;
        if (colBoxReduction(round)) return true;
        if (hiddenPairInRow(round)) return true;
        if (hiddenPairInColumn(round)) return true;
        return hiddenPairInSection(round);
    }

    private boolean colBoxReduction(int round) {
        for (int valIndex = 0; valIndex < ROW_COL_SEC_SIZE; valIndex++) {
            for (int col = 0; col < ROW_COL_SEC_SIZE; col++) {
                int colStart = columnToFirstCell(col);
                boolean inOneBox = true;
                int colBox = -1;
                // this part is checked!
                for (int i = 0; i < GRID_SIZE_COL; i++) {
                    for (int j = 0; j < GRID_SIZE_ROW; j++) {
                        int row = i * GRID_SIZE_ROW + j;
                        int position = rowColumnToCell(row, col);
                        int valPos = getPossibilityIndex(valIndex, position);
                        if (possibilities[valPos] == 0) {
                            if (colBox == -1 || colBox == i) {
                                colBox = i;
                            } else {
                                inOneBox = false;
                            }
                        }
                    }
                }
                if (inOneBox && colBox != -1) {
                    boolean doneSomething = false;
                    int row = GRID_SIZE_ROW * colBox;
                    int secStart = cellToSectionStartCell(rowColumnToCell(row, col));
                    int secStartRow = cellToRow(secStart);
                    int secStartCol = cellToColumn(secStart);
                    for (int i = 0; i < GRID_SIZE_COL; i++) {
                        for (int j = 0; j < GRID_SIZE_ROW; j++) {
                            int row2 = secStartRow + j;
                            int col2 = secStartCol + i;
                            int position = rowColumnToCell(row2, col2);
                            int valPos = getPossibilityIndex(valIndex, position);
                            if (col != col2 && possibilities[valPos] == 0) {
                                possibilities[valPos] = round;
                                doneSomething = true;
                            }
                        }
                    }
                    if (doneSomething) {
                        if (logHistory || recordHistory) addHistoryItem(new LogItem(round, LogType.COLUMN_BOX, valIndex + 1, colStart));
                        return true;
                    }
                }
            }
        }
        return false;
    }


    private boolean rowBoxReduction(int round) {
        for (int valIndex = 0; valIndex < ROW_COL_SEC_SIZE; valIndex++) {
            for (int row = 0; row < ROW_COL_SEC_SIZE; row++) {
                int rowStart = rowToFirstCell(row);
                boolean inOneBox = true;
                int rowBox = -1;
                for (int i = 0; i < GRID_SIZE_ROW; i++) {
                    for (int j = 0; j < GRID_SIZE_COL; j++) {
                        int column = i * GRID_SIZE_COL + j;
                        int position = rowColumnToCell(row, column);
                        int valPos = getPossibilityIndex(valIndex, position);
                        if (possibilities[valPos] == 0) {
                            if (rowBox == -1 || rowBox == i) {
                                rowBox = i;
                            } else {
                                inOneBox = false;
                            }
                        }
                    }
                }
                if (inOneBox && rowBox != -1) {
                    boolean doneSomething = false;
                    int column = GRID_SIZE_COL * rowBox;
                    int secStart = cellToSectionStartCell(rowColumnToCell(row, column));
                    int secStartRow = cellToRow(secStart);
                    int secStartCol = cellToColumn(secStart);
                    for (int i = 0; i < GRID_SIZE_ROW; i++) {
                        for (int j = 0; j < GRID_SIZE_COL; j++) {
                            int row2 = secStartRow + i;
                            int col2 = secStartCol + j;
                            int position = rowColumnToCell(row2, col2);
                            int valPos = getPossibilityIndex(valIndex, position);
                            if (row != row2 && possibilities[valPos] == 0) {
                                possibilities[valPos] = round;
                                doneSomething = true;
                            }
                        }
                    }
                    if (doneSomething) {
                        if (logHistory || recordHistory) addHistoryItem(new LogItem(round, LogType.ROW_BOX, valIndex + 1, rowStart));
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // CHECKED!
    private boolean pointingRowReduction(int round) {
        for (int valIndex = 0; valIndex < ROW_COL_SEC_SIZE; valIndex++) {
            for (int section = 0; section < ROW_COL_SEC_SIZE; section++) {
                int secStart = sectionToFirstCell(section);
                boolean inOneRow = true;
                int boxRow = -1;
                for (int j = 0; j < GRID_SIZE_ROW; j++) {
                    for (int i = 0; i < GRID_SIZE_COL; i++) {
                        int secVal = secStart + i + (ROW_COL_SEC_SIZE * j);
                        int valPos = getPossibilityIndex(valIndex, secVal);
                        if (possibilities[valPos] == 0) {
                            if (boxRow == -1 || boxRow == j) {
                                boxRow = j;
                            } else {
                                inOneRow = false;
                            }
                        }
                    }
                }
                if (inOneRow && boxRow != -1) {
                    boolean doneSomething = false;
                    int row = cellToRow(secStart) + boxRow;
                    int rowStart = rowToFirstCell(row);

                    for (int i = 0; i < ROW_COL_SEC_SIZE; i++) {
                        int position = rowStart + i;
                        int section2 = cellToSection(position);
                        int valPos = getPossibilityIndex(valIndex, position);
                        if (section != section2 && possibilities[valPos] == 0) {
                            possibilities[valPos] = round;
                            doneSomething = true;
                        }
                    }
                    if (doneSomething) {
                        if (logHistory || recordHistory) addHistoryItem(new LogItem(round, LogType.POINTING_PAIR_TRIPLE_ROW, valIndex + 1, rowStart));
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // CHECKED! .. pretty sure this is correct now
    private boolean pointingColumnReduction(int round) {
        for (int valIndex = 0; valIndex < ROW_COL_SEC_SIZE; valIndex++) {
            for (int section = 0; section < ROW_COL_SEC_SIZE; section++) {
                int secStart = sectionToFirstCell(section);
                boolean inOneCol = true;
                int boxCol = -1;
                for (int i = 0; i < GRID_SIZE_COL; i++) {
                    for (int j = 0; j < GRID_SIZE_ROW; j++) {
                        int secVal = secStart + i + (ROW_COL_SEC_SIZE * j);
                        int valPos = getPossibilityIndex(valIndex, secVal);
                        if (possibilities[valPos] == 0) {
                            if (boxCol == -1 || boxCol == i) {
                                boxCol = i;
                            } else {
                                inOneCol = false;
                            }
                        }
                    }
                }
                if (inOneCol && boxCol != -1) {
                    boolean doneSomething = false;
                    int col = cellToColumn(secStart) + boxCol;
                    int colStart = columnToFirstCell(col);

                    for (int i = 0; i < ROW_COL_SEC_SIZE; i++) {
                        int position = colStart + (ROW_COL_SEC_SIZE * i);
                        int section2 = cellToSection(position);
                        int valPos = getPossibilityIndex(valIndex, position);
                        if (section != section2 && possibilities[valPos] == 0) {
                            possibilities[valPos] = round;
                            doneSomething = true;
                        }
                    }
                    if (doneSomething) {
                        if (logHistory || recordHistory) addHistoryItem(new LogItem(round, LogType.POINTING_PAIR_TRIPLE_COLUMN, valIndex + 1, colStart));
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // CHECKED!
    private int countPossibilities(int position) {
        int count = 0;
        for (int valIndex = 0; valIndex < ROW_COL_SEC_SIZE; valIndex++) {
            int valPos = getPossibilityIndex(valIndex, position);
            if (possibilities[valPos] == 0) count++;
        }
        return count;
    }

    // CHECKED!
    private boolean arePossibilitiesSame(int position1, int position2) {
        for (int valIndex = 0; valIndex < ROW_COL_SEC_SIZE; valIndex++) {
            int valPos1 = getPossibilityIndex(valIndex, position1);
            int valPos2 = getPossibilityIndex(valIndex, position2);
            if ((possibilities[valPos1] == 0 || possibilities[valPos2] == 0) && (possibilities[valPos1] != 0 || possibilities[valPos2] != 0)) {
                return false;
            }
        }
        return true;
    }

    // CHECKED!
    private boolean removePossibilitiesInOneFromTwo(int position1, int position2, int round) {
        boolean doneSomething = false;
        for (int valIndex = 0; valIndex < ROW_COL_SEC_SIZE; valIndex++) {
            int valPos1 = getPossibilityIndex(valIndex, position1);
            int valPos2 = getPossibilityIndex(valIndex, position2);
            if (possibilities[valPos1] == 0 && possibilities[valPos2] == 0) {
                possibilities[valPos2] = round;
                doneSomething = true;
            }
        }
        return doneSomething;
    }

    // CHECKED!
    private boolean hiddenPairInColumn(int round) {
        for (int column = 0; column < ROW_COL_SEC_SIZE; column++) {
            for (int valIndex = 0; valIndex < ROW_COL_SEC_SIZE; valIndex++) {
                int r1 = -1;
                int r2 = -1;
                int valCount = 0;
                for (int row = 0; row < ROW_COL_SEC_SIZE; row++) {
                    int position = rowColumnToCell(row, column);
                    int valPos = getPossibilityIndex(valIndex, position);
                    if (possibilities[valPos] == 0) {
                        if (r1 == -1 || r1 == row) {
                            r1 = row;
                        } else if (r2 == -1 || r2 == row) {
                            r2 = row;
                        }
                        valCount++;
                    }
                }
                if (valCount == 2) {
                    for (int valIndex2 = valIndex + 1; valIndex2 < ROW_COL_SEC_SIZE; valIndex2++) {
                        int r3 = -1;
                        int r4 = -1;
                        int valCount2 = 0;
                        for (int row = 0; row < ROW_COL_SEC_SIZE; row++) {
                            int position = rowColumnToCell(row, column);
                            int valPos = getPossibilityIndex(valIndex2, position);
                            if (possibilities[valPos] == 0) {
                                if (r3 == -1 || r3 == row) {
                                    r3 = row;
                                } else if (r4 == -1 || r4 == row) {
                                    r4 = row;
                                }
                                valCount2++;
                            }
                        }
                        if (valCount2 == 2 && r1 == r3 && r2 == r4) {
                            boolean doneSomething = false;
                            for (int valIndex3 = 0; valIndex3 < ROW_COL_SEC_SIZE; valIndex3++) {
                                if (valIndex3 != valIndex && valIndex3 != valIndex2) {
                                    int position1 = rowColumnToCell(r1, column);
                                    int position2 = rowColumnToCell(r2, column);
                                    int valPos1 = getPossibilityIndex(valIndex3, position1);
                                    int valPos2 = getPossibilityIndex(valIndex3, position2);
                                    if (possibilities[valPos1] == 0) {
                                        possibilities[valPos1] = round;
                                        doneSomething = true;
                                    }
                                    if (possibilities[valPos2] == 0) {
                                        possibilities[valPos2] = round;
                                        doneSomething = true;
                                    }
                                }
                            }
                            if (doneSomething) {
                                if (logHistory || recordHistory) addHistoryItem(new LogItem(round, LogType.HIDDEN_PAIR_COLUMN, valIndex + 1, rowColumnToCell(r1, column)));
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    // CHECKED!
    private boolean hiddenPairInSection(int round) {
        for (int section = 0; section < ROW_COL_SEC_SIZE; section++) {
            for (int valIndex = 0; valIndex < ROW_COL_SEC_SIZE; valIndex++) {
                int si1 = -1;
                int si2 = -1;
                int valCount = 0;
                for (int secInd = 0; secInd < ROW_COL_SEC_SIZE; secInd++) {
                    int position = sectionToCell(section, secInd);
                    int valPos = getPossibilityIndex(valIndex, position);
                    if (possibilities[valPos] == 0) {
                        if (si1 == -1 || si1 == secInd) {
                            si1 = secInd;
                        } else if (si2 == -1 || si2 == secInd) {
                            si2 = secInd;
                        }
                        valCount++;
                    }
                }
                if (valCount == 2) {
                    for (int valIndex2 = valIndex + 1; valIndex2 < ROW_COL_SEC_SIZE; valIndex2++) {
                        int si3 = -1;
                        int si4 = -1;
                        int valCount2 = 0;
                        for (int secInd = 0; secInd < ROW_COL_SEC_SIZE; secInd++) {
                            int position = sectionToCell(section, secInd);
                            int valPos = getPossibilityIndex(valIndex2, position);
                            if (possibilities[valPos] == 0) {
                                if (si3 == -1 || si3 == secInd) {
                                    si3 = secInd;
                                } else if (si4 == -1 || si4 == secInd) {
                                    si4 = secInd;
                                }
                                valCount2++;
                            }
                        }
                        if (valCount2 == 2 && si1 == si3 && si2 == si4) {
                            boolean doneSomething = false;
                            for (int valIndex3 = 0; valIndex3 < ROW_COL_SEC_SIZE; valIndex3++) {
                                if (valIndex3 != valIndex && valIndex3 != valIndex2) {
                                    int position1 = sectionToCell(section, si1);
                                    int position2 = sectionToCell(section, si2);
                                    int valPos1 = getPossibilityIndex(valIndex3, position1);
                                    int valPos2 = getPossibilityIndex(valIndex3, position2);
                                    if (possibilities[valPos1] == 0) {
                                        possibilities[valPos1] = round;
                                        doneSomething = true;
                                    }
                                    if (possibilities[valPos2] == 0) {
                                        possibilities[valPos2] = round;
                                        doneSomething = true;
                                    }
                                }
                            }
                            if (doneSomething) {
                                if (logHistory || recordHistory) addHistoryItem(new LogItem(round, LogType.HIDDEN_PAIR_SECTION, valIndex + 1, sectionToCell(section, si1)));
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    // CHECKED!
    private boolean hiddenPairInRow(int round) {
        for (int row = 0; row < ROW_COL_SEC_SIZE; row++) {
            for (int valIndex = 0; valIndex < ROW_COL_SEC_SIZE; valIndex++) {
                int c1 = -1;
                int c2 = -1;
                int valCount = 0;
                for (int column = 0; column < ROW_COL_SEC_SIZE; column++) {
                    int position = rowColumnToCell(row, column);
                    int valPos = getPossibilityIndex(valIndex, position);
                    if (possibilities[valPos] == 0) {
                        if (c1 == -1 || c1 == column) {
                            c1 = column;
                        } else if (c2 == -1 || c2 == column) {
                            c2 = column;
                        }
                        valCount++;
                    }
                }
                if (valCount == 2) {
                    for (int valIndex2 = valIndex + 1; valIndex2 < ROW_COL_SEC_SIZE; valIndex2++) {
                        int c3 = -1;
                        int c4 = -1;
                        int valCount2 = 0;
                        for (int column = 0; column < ROW_COL_SEC_SIZE; column++) {
                            int position = rowColumnToCell(row, column);
                            int valPos = getPossibilityIndex(valIndex2, position);
                            if (possibilities[valPos] == 0) {
                                if (c3 == -1 || c3 == column) {
                                    c3 = column;
                                } else if (c4 == -1 || c4 == column) {
                                    c4 = column;
                                }
                                valCount2++;
                            }
                        }
                        if (valCount2 == 2 && c1 == c3 && c2 == c4) {
                            boolean doneSomething = false;
                            for (int valIndex3 = 0; valIndex3 < ROW_COL_SEC_SIZE; valIndex3++) {
                                if (valIndex3 != valIndex && valIndex3 != valIndex2) {
                                    int position1 = rowColumnToCell(row, c1);
                                    int position2 = rowColumnToCell(row, c2);
                                    int valPos1 = getPossibilityIndex(valIndex3, position1);
                                    int valPos2 = getPossibilityIndex(valIndex3, position2);
                                    if (possibilities[valPos1] == 0) {
                                        possibilities[valPos1] = round;
                                        doneSomething = true;
                                    }
                                    if (possibilities[valPos2] == 0) {
                                        possibilities[valPos2] = round;
                                        doneSomething = true;
                                    }
                                }
                            }
                            if (doneSomething) {
                                if (logHistory || recordHistory) addHistoryItem(new LogItem(round, LogType.HIDDEN_PAIR_ROW, valIndex + 1, rowColumnToCell(row, c1)));
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    // CHECKED!
    private boolean handleNakedPairs(int round) {
        for (int position = 0; position < BOARD_SIZE; position++) {
            int possibilities = countPossibilities(position);
            if (possibilities == 2) {
                int row = cellToRow(position);
                int column = cellToColumn(position);
                int section = cellToSectionStartCell(position);
                for (int position2 = position; position2 < BOARD_SIZE; position2++) {
                    if (position != position2) {
                        int possibilities2 = countPossibilities(position2);
                        if (possibilities2 == 2 && arePossibilitiesSame(position, position2)) {
                            if (row == cellToRow(position2)) {
                                boolean doneSomething = false;
                                for (int column2 = 0; column2 < ROW_COL_SEC_SIZE; column2++) {
                                    int position3 = rowColumnToCell(row, column2);
                                    if (position3 != position && position3 != position2 && removePossibilitiesInOneFromTwo(position, position3, round)) {
                                        doneSomething = true;
                                    }
                                }
                                if (doneSomething) {
                                    if (logHistory || recordHistory) addHistoryItem(new LogItem(round, LogType.NAKED_PAIR_ROW, 0, position));
                                    return true;
                                }
                            }
                            if (column == cellToColumn(position2)) {
                                boolean doneSomething = false;
                                for (int row2 = 0; row2 < ROW_COL_SEC_SIZE; row2++) {
                                    int position3 = rowColumnToCell(row2, column);
                                    if (position3 != position && position3 != position2 && removePossibilitiesInOneFromTwo(position, position3, round)) {
                                        doneSomething = true;
                                    }
                                }
                                if (doneSomething) {
                                    if (logHistory || recordHistory) addHistoryItem(new LogItem(round, LogType.NAKED_PAIR_COLUMN, 0, position));
                                    return true;
                                }
                            }
                            if (section == cellToSectionStartCell(position2)) {
                                boolean doneSomething = false;
                                int secStart = cellToSectionStartCell(position);
                                for (int i = 0; i < GRID_SIZE_COL; i++) {
                                    for (int j = 0; j < GRID_SIZE_ROW; j++) {
                                        int position3 = secStart + i + (ROW_COL_SEC_SIZE * j);
                                        if (position3 != position && position3 != position2 && removePossibilitiesInOneFromTwo(position, position3, round)) {
                                            doneSomething = true;
                                        }
                                    }
                                }
                                if (doneSomething) {
                                    if (logHistory || recordHistory) addHistoryItem(new LogItem(round, LogType.NAKED_PAIR_SECTION, 0, position));
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Mark exactly one cell which is the only possible value for some row, if
     * such a cell exists. This method will look in a row for a possibility that
     * is only listed for one cell. This type of cell is often called a
     * "hidden single"
     * CHECKED!
     */
    private boolean onlyValueInRow(int round) {
        for (int row = 0; row < ROW_COL_SEC_SIZE; row++) {
            for (int valIndex = 0; valIndex < ROW_COL_SEC_SIZE; valIndex++) {
                int count = 0;
                int lastPosition = 0;
                for (int col = 0; col < ROW_COL_SEC_SIZE; col++) {
                    int position = (row * ROW_COL_SEC_SIZE) + col;
                    int valPos = getPossibilityIndex(valIndex, position);
                    if (possibilities[valPos] == 0) {
                        count++;
                        lastPosition = position;
                    }
                }
                if (count == 1) {
                    int value = valIndex + 1;
                    if (logHistory || recordHistory) addHistoryItem(new LogItem(round, LogType.HIDDEN_SINGLE_ROW, value, lastPosition));
                    mark(lastPosition, round, value);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Mark exactly one cell which is the only possible value for some column,
     * if such a cell exists. This method will look in a column for a
     * possibility that is only listed for one cell. This type of cell is often
     * called a "hidden single"
     * CHECKED!
     */
    private boolean onlyValueInColumn(int round) {
        for (int col = 0; col < ROW_COL_SEC_SIZE; col++) {
            for (int valIndex = 0; valIndex < ROW_COL_SEC_SIZE; valIndex++) {
                int count = 0;
                int lastPosition = 0;
                for (int row = 0; row < ROW_COL_SEC_SIZE; row++) {
                    int position = rowColumnToCell(row, col);
                    int valPos = getPossibilityIndex(valIndex, position);
                    if (possibilities[valPos] == 0) {
                        count++;
                        lastPosition = position;
                    }
                }
                if (count == 1) {
                    int value = valIndex + 1;
                    if (logHistory || recordHistory) addHistoryItem(new LogItem(round, LogType.HIDDEN_SINGLE_COLUMN, value, lastPosition));
                    mark(lastPosition, round, value);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Mark exactly one cell which is the only possible value for some section,
     * if such a cell exists. This method will look in a section for a
     * possibility that is only listed for one cell. This type of cell is often
     * called a "hidden single"
     * Checked!
     */
    private boolean onlyValueInSection(int round) {
        for (int sec = 0; sec < ROW_COL_SEC_SIZE; sec++) {
            int secPos = sectionToFirstCell(sec);
            for (int valIndex = 0; valIndex < ROW_COL_SEC_SIZE; valIndex++) {
                int count = 0;
                int lastPosition = 0;
                for (int i = 0; i < GRID_SIZE_COL; i++) {
                    for (int j = 0; j < GRID_SIZE_ROW; j++) {
                        int position = secPos + i + ROW_COL_SEC_SIZE * j;
                        int valPos = getPossibilityIndex(valIndex, position);
                        if (possibilities[valPos] == 0) {
                            count++;
                            lastPosition = position;
                        }
                    }
                }
                if (count == 1) {
                    int value = valIndex + 1;
                    if (logHistory || recordHistory) addHistoryItem(new LogItem(round, LogType.HIDDEN_SINGLE_SECTION, value, lastPosition));
                    mark(lastPosition, round, value);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Mark exactly one cell that has a single possibility, if such a cell
     * exists. This method will look for a cell that has only one possibility.
     * This type of cell is often called a "single"
     * Checked!
     */
    private boolean onlyPossibilityForCell(int round) {
        for (int position = 0; position < BOARD_SIZE; position++) {
            if (solution[position] == 0) {
                int count = 0;
                int lastValue = 0;
                for (int valIndex = 0; valIndex < ROW_COL_SEC_SIZE; valIndex++) {
                    int valPos = getPossibilityIndex(valIndex, position);
                    if (possibilities[valPos] == 0) {
                        count++;
                        lastValue = valIndex + 1;
                    }
                }
                if (count == 1) {
                    mark(position, round, lastValue);
                    if (logHistory || recordHistory) addHistoryItem(new LogItem(round, LogType.SINGLE, lastValue, position));
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Mark the given value at the given position. Go through the row, column,
     * and section for the position and remove the value from the possibilities.
     *
     * @param position Position into the board (0-80)
     * @param round Round to mark for rollback purposes
     * @param value The value to go in the square at the given position
     * Checked!
     */
    private void mark(int position, int round, int value) {
        if (solution[position] != 0) throw new IllegalArgumentException("Marking position that already has been marked.");
        if (solutionRound[position] != 0) throw new IllegalArgumentException("Marking position that was marked another round.");
        int valIndex = value - 1;
        solution[position] = value;

        int possInd = getPossibilityIndex(valIndex, position);
        if (possibilities[possInd] != 0) throw new IllegalArgumentException("Marking impossible position.");

        // Take this value out of the possibilities for everything in the row
        solutionRound[position] = round;
        int rowStart = cellToRow(position) * ROW_COL_SEC_SIZE;
        for (int col = 0; col < ROW_COL_SEC_SIZE; col++) {
            int rowVal = rowStart + col;
            int valPos = getPossibilityIndex(valIndex, rowVal);
            // System.out.println("Row Start: "+rowStart+" Row Value: "+rowVal+" Value Position: "+valPos);
            if (possibilities[valPos] == 0) {
                possibilities[valPos] = round;
            }
        }

        // Take this value out of the possibilities for everything in the column
        int colStart = cellToColumn(position);
        for (int i = 0; i < ROW_COL_SEC_SIZE; i++) {
            int colVal = colStart + (ROW_COL_SEC_SIZE * i);
            int valPos = getPossibilityIndex(valIndex, colVal);
            // System.out.println("Col Start: "+colStart+" Col Value: "+colVal+" Value Position: "+valPos);
            if (possibilities[valPos] == 0) {
                possibilities[valPos] = round;
            }
        }

        // Take this value out of the possibilities for everything in section
        int secStart = cellToSectionStartCell(position);
        for (int i = 0; i < GRID_SIZE_COL; i++) {
            for (int j = 0; j < GRID_SIZE_ROW; j++) {
                int secVal = secStart + i + (ROW_COL_SEC_SIZE * j);
                int valPos = getPossibilityIndex(valIndex, secVal);
                // System.out.println("Sec Start: "+secStart+" Sec Value: "+secVal+" Value Position: "+valPos);
                if (possibilities[valPos] == 0) {
                    possibilities[valPos] = round;
                }
            }
        }

        // This position itself is determined, it should have possibilities.
        for (valIndex = 0; valIndex < ROW_COL_SEC_SIZE; valIndex++) {
            int valPos = getPossibilityIndex(valIndex, position);
            if (possibilities[valPos] == 0) {
                possibilities[valPos] = round;
            }
        }
    }

    /**
     * print the given BOARD_SIZEd array of ints as a sudoku puzzle. Use print
     * options from member variables.
     */
    private void print(int[] sudoku) {
        System.out.print(puzzleToString(sudoku));
    }

    private String puzzleToString(int[] sudoku) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < BOARD_SIZE; i++) {
            if (printStyle == PrintStyle.READABLE) {
                sb.append(" ");
            }
            if (sudoku[i] == 0) {
                sb.append('.');
            } else {
                sb.append(sudoku[i]);
            }
            if (i == BOARD_SIZE - 1) {
                if (printStyle == PrintStyle.CSV) {
                    sb.append(",");
                } else {
                    sb.append(NL);
                }
                if (printStyle == PrintStyle.READABLE || printStyle == PrintStyle.COMPACT) {
                    sb.append(NL);
                }
            } else if (i % ROW_COL_SEC_SIZE == ROW_COL_SEC_SIZE - 1) {
                if (printStyle == PrintStyle.READABLE || printStyle == PrintStyle.COMPACT) {
                    sb.append(NL);
                }
                if (i % SEC_GROUP_SIZE == SEC_GROUP_SIZE - 1) {
                    if (printStyle == PrintStyle.READABLE) {
                        sb.append("-------|-------|-------").append(NL);
                    }
                }
            } else if (i % GRID_SIZE_ROW == GRID_SIZE_ROW - 1) {
                if (printStyle == PrintStyle.READABLE) {
                    sb.append(" |");
                }
            }
        }
        return sb.toString();
    }

    /**
     * Print the sudoku puzzle.
     */
    public void printPuzzle() {
        print(puzzle);
    }

    public String getPuzzleString() {
        return puzzleToString(puzzle);
    }

    public int[] getPuzzle() {
        return puzzle.clone();
    }

    /**
     * Print the sudoku solution.
     */
    public void printSolution() {
        print(solution);
    }

    public String getSolutionString() {
        return puzzleToString(solution);
    }

    public int[] getSolution() {
        return solution.clone();
    }

    /**
     * Given a vector of LogItems, determine how many log items in the vector
     * are of the specified type.
     */
    private int getLogCount(ArrayList<LogItem> v, LogType type) {
        int count = 0;
        for (int i = 0; i < v.size(); i++) {
            if ((v.get(i)).getType() == type) count++;
        }
        return count;
    }

    /**
     * Shuffle the values in an array of integers.
     */
    private static void shuffleArray(int[] array, int size) {
        for (int i = 0; i < size; i++) {
            int tailSize = size - i;
            int randTailPos = Math.abs(random.nextInt()) % tailSize + i;
            int temp = array[i];
            array[i] = array[randTailPos];
            array[randTailPos] = temp;
        }
    }

    private static Symmetry getRandomSymmetry() {
        Symmetry[] values = Symmetry.values();
        // not the first and last value which are NONE and RANDOM
        return values[(Math.abs(random.nextInt()) % (values.length - 1)) + 1];
    }

    /**
     * Given the index of a cell (0-80) calculate the column (0-8) in which that
     * cell resides.
     * Checked!
     */
    static int cellToColumn(int cell) {
        return cell % ROW_COL_SEC_SIZE;
    }

    /**
     * Given the index of a cell (0-80) calculate the row (0-8) in which it
     * resides.
     * Checked!
     */
    static int cellToRow(int cell) {
        return cell / ROW_COL_SEC_SIZE;
    }

    /**
     * Given the index of a cell (0-80) calculate the section (0-8) in which it
     * resides.
     * Checked!
     */
    static int cellToSection(int cell) {
        return ((cell / SEC_GROUP_SIZE * GRID_SIZE_ROW)
                + (cellToColumn(cell) / GRID_SIZE_COL));
    }

    /**
     * Given the index of a cell (0-80) calculate the cell (0-80) that is the
     * upper left start cell of that section.
     * Checked!
     */
    static int cellToSectionStartCell(int cell) {
        return ((cell / SEC_GROUP_SIZE * SEC_GROUP_SIZE)
                + (cellToColumn(cell) / GRID_SIZE_COL * GRID_SIZE_COL));
    }

    /**
     * Given a row (0-8) calculate the first cell (0-80) of that row.
     * Checked!
     */
    static int rowToFirstCell(int row) {
        return ROW_COL_SEC_SIZE * row;
    }

    /**
     * Given a column (0-8) calculate the first cell (0-80) of that column.
     * Checked!
     */
    static int columnToFirstCell(int column) {
        return column;
    }

    /**
     * Given a section (0-8) calculate the first cell (0-80) of that section.
     * Checked!
     */
    static int sectionToFirstCell(int section) {
        return ((section % GRID_SIZE_ROW * GRID_SIZE_COL)
                + (section / GRID_SIZE_ROW * SEC_GROUP_SIZE));
    }

    /**
     * Given a value for a cell (0-8) and a cell number (0-80) calculate the
     * offset into the possibility array (0-728).
     * Checked!
     */
    static int getPossibilityIndex(int valueIndex, int cell) {
        return valueIndex + (ROW_COL_SEC_SIZE * cell);
    }

    /**
     * Given a row (0-8) and a column (0-8) calculate the cell (0-80).
     * Checked!
     */
    static int rowColumnToCell(int row, int column) {
        return (row * ROW_COL_SEC_SIZE) + column;
    }

    /**
     * Given a section (0-8) and an offset into that section (0-8) calculate the
     * cell (0-80)
     * Checked!
     */
    static int sectionToCell(int section, int offset) {
        return (sectionToFirstCell(section)
                + ((offset / GRID_SIZE_COL) * ROW_COL_SEC_SIZE)
                + (offset % GRID_SIZE_COL));
    }
}
