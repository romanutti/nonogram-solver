import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Solves a nonogram incredibly fast.
 */
public class NonogramSolver {
    /** Unknown field on nonogram */
    public static final char FIELD_UNKNOWN = ' ';
    /** Black field on nonogram */
    public static final char FIELD_BLACK = '#';
    /** White field on nonogram */
    public static final char FIELD_WHITE = '.';

    // Constraints read from input file
    private static final NonogramConstraints constraints = new NonogramConstraints();

    // Information to know in which
    // block in column constraints
    // processing currently is
    private static int[][] columnIndex = new int[constraints.getN()][constraints.getM()];
    private static int[][] columnCounter = new int[constraints.getN()][constraints.getM()];

    // Calculated solutions
    private static List<NonogrammSolution> solutions = new ArrayList<>();
    private static int numberOfSolutions;


    /**
     * Starts solving the nonogram.
     *
     */
    public static void main(String[] args) throws FileNotFoundException {
        // Initialize result variables
        PrintWriter out = new PrintWriter("nonogramm.out");
        NonogrammSolution solution = new NonogrammSolution(constraints.getN(), constraints.getM());

        // Start measuring execution time
        long startTime = System.currentTimeMillis();
        System.out.println("Going down that rabbit hole...");
        numberOfSolutions = solve(solution, 0);

        // Check number of solutions found
        if (numberOfSolutions > 0) {
            System.out.println("Hooray! " + numberOfSolutions + " solution(s) found");
            // Write solutions to file
            for (NonogrammSolution sol : solutions) {
                out.println(sol.toString());
            }
        } else {
            System.out.println("No solution found :/");
        }

        // Display execution time
        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        System.out.println("Execution time was: " + elapsedTime + " ms");

        out.close();
    }

    /**
     * Solves a nonogram using backtracking algorithm.
     *
     * @param solution Current solution
     * @param rowIndex Row index
     * @return Number of solutions found
     */
    public static int solve(NonogrammSolution solution, int rowIndex) {
        if (rowIndex == constraints.getN()) {
            // All fields set, so
            // add solution to result set
            solutions.add(solution.copy());
            return 1;
        } else {
            // Still fields to be set
            int numberOfSolutions = 0;

            // Get all possible permutations for row
            List<StringBuilder> permutations = constraints.getRowPermutations(rowIndex);

            // Calculate expected row
            // if already one row is set
            StringBuilder expectedRow = rowIndex > 0 ? getExpectedRow(solution, rowIndex): new StringBuilder();

            // Try to find a solution for every permutation possible
            outerloop:
            for (int i = 0; i < permutations.size(); i++) {
                // Get current permutation
                StringBuilder row = permutations.get(i);

                // If in first row just take
                // the current permutation as
                // starting point
                if (rowIndex == 0){
                    expectedRow = constraints.getRowPermutations(rowIndex).get(i);
                }

                // Check whether the expected line matches permutation
                for (int j = 0; j < expectedRow.length(); j++) {
                    if (expectedRow.charAt(j) != FIELD_UNKNOWN && expectedRow.charAt(j) != row.charAt(j)) {
                        // Try next permutation otherwise
                        continue outerloop;
                    }
                }

                // Backup old and set new row
                char[] oldValues = solution.setRow(rowIndex, row);
                BlockInformationBackup blockInformationBackup = updateBlockInformation(solution, rowIndex);

                // Solve next row
                numberOfSolutions += solve(solution, rowIndex + 1);

                // Reset row if not successful
                resetBlockInformation(blockInformationBackup);
                solution.resetRow(rowIndex, oldValues);
            }
            return numberOfSolutions;
        }
    }


    private static StringBuilder getExpectedRow(NonogrammSolution solution, int rowIndex) {
        // Reset expected rot
        StringBuilder expectedRow = new StringBuilder();
        for (int i = 0; i < constraints.getM(); i++) {
            int curColumnIndex = columnIndex[rowIndex - 1][i];
            int curColumnCounter = columnCounter[rowIndex - 1][i];
            if (curColumnIndex == constraints.getColumnConstraints()[i].size()){ // && curColumnCounter == constraints.getColumnConstraints()[i].get(curColumnIndex)) {
                // Field has to be empty as all black cells
                // already seg
                expectedRow.append(FIELD_WHITE);
            } else {
                //int blockSize = constraints.getColumnConstraints()[i].get(curColumnIndex);
                if (columnCounter[rowIndex - 1][i] > 0) {
                    // Cell in previous row was set
                    int blockSize = constraints.getColumnConstraints()[i].get(columnIndex[rowIndex - 1][i]);
                    //int curColumnCounter = columnCounter[rowIndex - 1][i];

                    if (blockSize > curColumnCounter) {
                        // Still cells in block left
                        expectedRow.append(FIELD_BLACK);
                    } else
                        // All cells of available blocks already set
                        expectedRow.append(FIELD_WHITE);

                } else {
                    int minCellsNeeded = getMinCellsNeeded(rowIndex, i);

                    if (constraints.getN() - rowIndex < minCellsNeeded) {
                        // Cell has to be set to be able
                        // to fullfill column constraint until last row
                        expectedRow.append(FIELD_BLACK);
                    } else {
                        // Cell in previous row was not set,
                        // Could be set now or left empty
                        expectedRow.append(FIELD_UNKNOWN);
                    }
                }
            }
        }

        return expectedRow;
    }

    /**
     * Calculates how many cells are needed to fulfil column constraints.
     *
     * @param rowIndex Current row
     * @param colIndex Current column
     * @return Number of cells needed
     */
    private static int getMinCellsNeeded(int rowIndex, int colIndex) {
        // Get last column index information
        // if already a row is set
        int curColumnIndex = rowIndex == 0 ? 0 : columnIndex[rowIndex - 1][colIndex];
        int curColumnCounter = rowIndex == 0 ? 0 : columnCounter[rowIndex - 1][colIndex];

        // Sum values for all remaning
        // column block sizes
        int remainingCells = 0;
        for (int i = 0; i < constraints.getColumnConstraints()[colIndex].size() - curColumnIndex; i++) {
            int blockSize = rowIndex == 0 ?
                    0 :
                    constraints.getColumnConstraints()[colIndex].get(curColumnIndex + i);

            // Sum remaining block sizes
            if (curColumnCounter < blockSize) {
                // Block is already in use
                remainingCells += blockSize - curColumnCounter;
            } else {
                // Future block
                remainingCells += constraints.getColumnConstraints()[colIndex].get(curColumnIndex + i);
            }

            if (i != constraints.getColumnConstraints()[colIndex].size() - curColumnIndex) {
                // Add at least one space,
                // except on last row
                remainingCells++;
            }
        }

        return remainingCells;
    }

    /**
     * Updates the information for block size index and counter of all colums.
     *
     * @param solution Current solution
     * @param rowIndex Current row
     * @return Block information before update
     */
    private static BlockInformationBackup updateBlockInformation(NonogrammSolution solution, int rowIndex) {
        // Save information for reset
        BlockInformationBackup blockInformationBackup = new BlockInformationBackup(columnIndex, columnCounter);

        // Update values
        for (int i = 0; i < constraints.getM(); i++) {
            // Copy state from previous row
            columnCounter[rowIndex][i] = rowIndex == 0 ? 0 : columnCounter[rowIndex - 1][i];
            columnIndex[rowIndex][i] = rowIndex == 0 ? 0 : columnIndex[rowIndex - 1][i];

            if (solution.getRow(rowIndex).charAt(i) == FIELD_WHITE) {
                if (rowIndex > 0 && columnCounter[rowIndex - 1][i] != 0) {
                    // Bit was set in previous row, but not in actual,
                    // move index to next block
                    columnCounter[rowIndex][i] = 0;
                    columnIndex[rowIndex][i]++;
                }
            } else
                // Bit has to be set,
                // increase block counter
                columnCounter[rowIndex][i]++;
        }

        return blockInformationBackup;
    }

    /**
     * Set block information to an earlier stage.
     *
     * @param blockInformationBackup Earlier block information
     */
    private static void resetBlockInformation(BlockInformationBackup blockInformationBackup) {
        // Deep copy index
        int[][] newColumnIndex = new int[blockInformationBackup.columnIndex.length][];
        for (int i = 0; i < blockInformationBackup.columnIndex.length; i++) {
            newColumnIndex[i] = blockInformationBackup.columnIndex[i].clone();
        }

        // Deep copy counter
        int[][] newColumnCounter = new int[blockInformationBackup.columnCounter.length][];
        for (int i = 0; i < blockInformationBackup.columnCounter.length; i++) {
            newColumnCounter[i] = blockInformationBackup.columnCounter[i].clone();
        }

        columnIndex = newColumnIndex;
        columnCounter = newColumnCounter;
    }

    /**
     * Solution of a nonogram.
     */
    static class NonogrammSolution {
        // n x m matrix
        private char nonogramm[][];

        public NonogrammSolution(int n, int m) {
            nonogramm = new char[n][m];

            // Initialize matrix
            for (int i = 0; i < nonogramm.length; i++) {
                for (int j = 0; j < nonogramm[i].length; j++) {
                    nonogramm[i][j] = FIELD_UNKNOWN;
                }
            }
        }

        // Getter & Setter
        public char get(int rowIndex, int colIndex) {
            return nonogramm[rowIndex][colIndex];
        }

        public void set(int rowIndex, int colIndex, char value) {
            nonogramm[rowIndex][colIndex] = value;
        }

        /**
         * Clears specific row.
         *
         * @param rowIndex Row index
         */
        public void resetRow(int rowIndex, char[] oldValues) {
            // Reset row
            for (int i = 0; i < constraints.getM(); i++) {
                nonogramm[rowIndex][i] = oldValues[i];
            }
        }

        /**
         * Gets filled in fields of row by index.
         *
         * @param rowIndex Row index
         * @return Known fields of row
         */
        public String getRow(int rowIndex) {
            char[] row = nonogramm[rowIndex];

            StringBuilder sb = new StringBuilder();
            for (char cell : row) {
                if (cell != FIELD_UNKNOWN)
                    sb.append(cell);
            }

            return sb.toString();
        }

        /**
         * Sets values for specified row and return old row values.
         *
         * @param rowIndex Row index
         * @param row Row to set
         * @return Old fields of row
         */
        public char[] setRow(int rowIndex, StringBuilder row) {
            // Initialize old value array
            char[] oldValues = new char[constraints.getM()];

            // Set row
            for (int i = 0; i < constraints.getM(); i++) {
                oldValues[i] = nonogramm[rowIndex][i];
                nonogramm[rowIndex][i] = row.charAt(i);
            }

            return oldValues;
        }

        /**
         * Creates a copy of the current solution.
         *
         * @return Copied instance of solution
         */
        public NonogrammSolution copy() {
            NonogrammSolution copy = new NonogrammSolution(constraints.getN(), constraints.getM());

            for (int row = 0; row < constraints.getN(); row++) {
                for (int col = 0; col < constraints.getM(); col++) {
                    copy.set(row, col, this.get(row, col));
                }
            }

            return copy;
        }

        /**
         * Converts matrix to output string.
         *
         * @return Matrix as output line
         */
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < nonogramm.length; i++) {
                for (int j = 0; j < nonogramm[i].length ; j++) {
                    sb.append(nonogramm[i][j]);
                }
            }
            return sb.toString();
        }

    }

    /**
     * Backup of block information for all columns.
     *
     */
    static class BlockInformationBackup {
        private int[][] columnIndex;
        private int[][] columnCounter;

        public BlockInformationBackup(int[][] columnIndex, int[][] columnCounter) {
            // Deep copy index
            int[][] newColumnIndex = new int[columnIndex.length][];
            for (int i = 0; i < columnIndex.length; i++) {
                newColumnIndex[i] = columnIndex[i].clone();
            }

            // Deep copy counter
            int[][] newColumnCounter = new int[columnCounter.length][];
            for (int i = 0; i < columnCounter.length; i++) {
                newColumnCounter[i] = columnCounter[i].clone();
            }

            this.columnIndex = newColumnIndex;
            this.columnCounter = newColumnCounter;
        }
    }
}
