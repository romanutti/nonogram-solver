import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Loads input data and handles row- and column constraints.
 */
public final class NonogramConstraints {
    // Variables used to read input file
    private static final BufferedReader inp;
    private static int inpPos = 0;
    private static int nextPos = 0;
    private static int inpLinePos = 1;
    private static String inpLine;

    // Dimensions
    private final int m;
    private final int n;

    // Constraints
    private final List<Integer>[] rowConstraints;
    private final List<Integer>[] columnConstraints;

    // Permutations
    private final List<StringBuilder>[] rowPermutations;

    static {
        try {
            inp = new BufferedReader(new FileReader("nonogramm.in"));
            inpLine = inp.readLine();
        } catch (IOException _e) {
            throw new RuntimeException(_e);
        }
    }

    public NonogramConstraints() {
        // Read data from input file and
        // get dimensions
        m = Integer.parseInt(next());
        n = Integer.parseInt(next());

        // Create row and column
        // block lengths
        rowConstraints = buildConstraintVector(n, 1);
        columnConstraints = buildConstraintVector(m, n + 1);

        // Calculate row permutations
        // According to constraints
        this.rowPermutations = new ArrayList[getN()];
        buildPermutations();
    }

    /**
     * Gets number of columns.
     *
     * @return Number of columns
     */
    public int getM() {
        return m;
    }

    /**
     * Gets number of rows.
     *
     * @return Number of rows
     */
    public int getN() {
        return n;
    }

    /**
     * Gets column constraints.
     *
     * @return Block lengths for columns
     */
    public List<Integer>[] getColumnConstraints() {
        return columnConstraints;
    }

    /**
     * Gets row permutations by index.
     *
     * @param rowIndex Row index
     * @return Row permutation for specified row
     */
    public List<StringBuilder> getRowPermutations(int rowIndex) {
        return rowPermutations[rowIndex];
    }

    /**
     * Calculates possible permutations for respective row.
     *
     * @param rowIndex Row index
     * @return List of possible permutations according to row constraints.
     */
    public List<StringBuilder> calculateRowPermutations(int rowIndex) {
        List<Integer> rowConstraint = rowConstraints[rowIndex];

        return calculatePermutations(rowConstraint, getM());
    }

    /**
     * Reads next number from input file.
     *
     * @return Read number
     */
    private String next() {
        nextPos = inpLine.indexOf(' ', inpPos + 1);
        String token = inpLine.substring(inpPos, nextPos == -1 ? inpLine.length() : nextPos);
        if (nextPos == -1) {
            inpLinePos++;
            try {
                inpLine = inp.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        inpPos = nextPos + 1;
        return token;
    }

    /**
     * Builds a constraint containing vectors of possible blocks per entry.
     *
     * @param size   Number of entries
     * @param offset Offset used when reading data from input file
     * @return Vector containing constraints
     */
    private List<Integer>[] buildConstraintVector(int size, int offset) {
        List<Integer>[] vector = new ArrayList[size];

        // Read from offset to end
        int i = 0;
        while (inpLinePos <= size + offset) {
            int curNumber = Integer.parseInt(next());

            if (vector[i] == null) {
                // First entry in vector
                vector[i] = new ArrayList<>();
            }
            vector[i].add(curNumber);
            if (nextPos == -1) {
                // Input number is on a new line
                i++;
            }
        }
        return vector;
    }

    /**
     * Calculate possible permutations for respective constraint.
     *
     * @param constraints Row/column consraint
     * @param length      Length of row/column
     * @return List of possible permutations
     */
    private List<StringBuilder> calculatePermutations(List<Integer> constraints, int length) {
        // Check how many cells are used
        // at least
        int sumOfUsedCells = constraints.stream().mapToInt(Integer::intValue).sum();

        // Constraints do not fit in row
        if (sumOfUsedCells > length)
            return Collections.EMPTY_LIST;

        List<StringBuilder> result = new ArrayList<>();
        if (constraints.isEmpty()) {
            // No constraints available
            result.add(duplicateChar(NonogramSolver.FIELD_WHITE, length));
        } else {
            // Place the block at the beginning of the row
            List<StringBuilder> prefix = calculatePermutations(constraints.subList(1, constraints.size()),
                    length - constraints.get(0));

            // Iterate through permutations
            for (StringBuilder rest : prefix) {
                // Duplicate char to row length
                StringBuilder duplicatedChar = duplicateChar(NonogramSolver.FIELD_BLACK, constraints.get(0)).append(rest);
                if (duplicatedChar.length() == length) {
                    if (countBlackGroups(duplicatedChar) == constraints.size()) {
                        result.add(duplicatedChar);
                    }
                }
            }

            // Place the block NOT at the beginning of the row
            List<StringBuilder> midfix = calculatePermutations(constraints, length - 1);
            for (StringBuilder rest : midfix) {
                result.add(new StringBuilder().append(NonogramSolver.FIELD_WHITE).append(rest));
            }
        }
        return result;
    }

    /**
     * Calculates possible row permutations.
     */
    private void buildPermutations() {
        // Set permutations
        for (int row = 0; row < getN(); row++) {
            List<StringBuilder> currentRowPermutations = calculateRowPermutations(row);
            this.rowPermutations[row] = currentRowPermutations;
        }
    }

    /**
     * Counts number of black fields in string
     *
     * @param line Line in which groups are counted
     * @return Number of groups of black fields
     */
    private int countBlackGroups(StringBuilder line) {
        char currentChar = ' ';
        int counter = 0;
        for (int i = 0; i < line.length(); i++) {
            if (currentChar != NonogramSolver.FIELD_BLACK && line.charAt(i) == NonogramSolver.FIELD_BLACK) {
                // Increase if first of group
                counter++;
            }
            currentChar = line.charAt(i);
        }
        return counter;
    }

    /**
     * Duplicates a char n-times.
     *
     * @param c                 Char to duplicate
     * @param duplicationFactor Factor times which the char will be duplicated
     * @return Duplicated char
     */
    private StringBuilder duplicateChar(char c, int duplicationFactor) {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < duplicationFactor; i++) {
            result.append(c);
        }

        return result;
    }
}
