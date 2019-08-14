package se.nefalas;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class Sudoku {
    private int[] values;
    private int[] possibleValues;
    private long lastPrint;

    Sudoku(int[] numbers) {
        this.values = numbers;
        this.possibleValues = new int[81];
        Arrays.fill(this.possibleValues, ~0);
        this.lastPrint = 0;
    }

    int getValue(int row, int column) {
        return this.values[getIndexFromRowAndColumn(row, column)];
    }

    void setValue(int row, int column, int value) {
        this.values[getIndexFromRowAndColumn(row, column)] = value;
    }

    int[] getPossibleValues(int row, int column) {
        int bits = this.possibleValues[getIndexFromRowAndColumn(row, column)];

        return getPossibleValuesFromBits(bits);
    }

    void addPossibleValue(int row, int column, int value) {
        int index = getIndexFromRowAndColumn(row, column);

        int bits = this.possibleValues[index];
        bits |= 1 << (value - 1);

        this.possibleValues[index] = bits;
    }

    void removePossibleValue(int row, int column, int value) {
        int index = getIndexFromRowAndColumn(row, column);

        int bits = this.possibleValues[index];
        bits &= ~(1 << (value - 1));

        this.possibleValues[index] = bits;
    }

    void removeAllPossibleValues(int row, int column) {
        this.possibleValues[getIndexFromRowAndColumn(row, column)] = 0;
    }

    void removePossibleValueFromNeighbours(int row, int column, int value) {
        this.removePossibleValueFromRow(row, value);
        this.removePossibleValueFromColumn(column, value);
        this.removePossibleValueFromBlock(row, column, value);
    }

    private void removePossibleValueFromRow(int row, int value) {
        for (int i = 0; i < 9; i++) {
            this.removePossibleValue(row, i, value);
        }
    }

    private void removePossibleValueFromColumn(int column, int value) {
        for (int i = 0; i < 9; i++) {
            this.removePossibleValue(i, column, value);
        }
    }

    private void removePossibleValueFromBlock(int row, int column, int value) {
        int startRowIndex = (row / 3) * 3;
        int startColumnIndex = (column / 3) * 3;

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                this.removePossibleValue(startRowIndex + i, startColumnIndex + j, value);
            }
        }
    }

    boolean cannotHaveValue(int row, int column, int value) {
        if (this.getValue(row, column) != 0) {
            return true;
        }

        int[] rowArray = this.getRow(row);
        int[] columnArray = this.getColumn(column);
        int[] blockArray = this.getBlock(row, column);

        return Utils.intArrayContains(rowArray, value)
                || Utils.intArrayContains(columnArray, value)
                || Utils.intArrayContains(blockArray, value);
    }

    private int[] getRow(int rowIndex) {
        int startIndex = rowIndex * 9;
        int endIndex = startIndex + 9;

        return Arrays.copyOfRange(this.values, startIndex, endIndex);
    }

    private int[] getColumn(int columnIndex) {
        int[] array = new int[9];
        for (int i = 0; i < 9; i++) {
            array[i] = this.values[i * 9 + columnIndex];
        }

        return array;
    }

    private int[] getBlock(int row, int column) {
        int startRowIndex = (row / 3) * 3;
        int startColumnIndex = (column / 3) * 3;
        int[] array = new int[9];

        int index = 0;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                array[index] = this.getValue(startRowIndex + i, startColumnIndex + j);
                index++;
            }
        }

        return array;
    }

    boolean isFull() {
        return Utils.intArrayDoesNotContain(this.values, 0);
    }

    void print(boolean force) {
        long now = System.currentTimeMillis();
        long elapsed = now - this.lastPrint;

        if (!force && this.lastPrint != -1 && elapsed < 1000 / 60) {
            return;
        }

        this.lastPrint = now;
        Utils.clearScreen();

        for (int i = 0; i < 81; i++) {
            int value = this.values[i];

            if (value == 0) {
                System.out.print(" ");
            } else {
                System.out.print(value);
            }
            System.out.print(" ");

            if ((i + 1) % 3 == 0 && i % 9 != 8) {
                System.out.print("| ");
            }

            if ((i+1) % 9 == 0) {
                System.out.println();
            }

            if ((i + 1) % 27 == 0 && i != 80) {
                System.out.println("---------------------");
            }
        }

        System.out.println();
        System.out.println();
    }

    void print() {
        this.print(false);
    }

    private static int getIndexFromRowAndColumn(int row, int column) {
        return 9 * row + column;
    }

    private static int[] getPossibleValuesFromBits(int bits) {
        List<Integer> values = new ArrayList<>();

        for (int i = 0; i < 9; i++) {
            if ((bits >> i & 1) == 1) {
                values.add(i + 1);
            }
        }

        return values.stream().mapToInt(i -> i).toArray();
    }
}