package se.nefalas;

import java.util.Arrays;

class Sudoku {
    private int[] values;

    Sudoku(int[] numbers) {
        this.values = numbers;
    }

    int getValue(int row, int column) {
        return this.values[getIndexFromRowAndColumn(row, column)];
    }

    void setValue(int row, int column, int value) {
        this.values[getIndexFromRowAndColumn(row, column)] = value;
    }

    boolean canHaveValue(int row, int column, int value) {
        if (this.getValue(row, column) != 0) {
            return false;
        }

        int[] rowArray = this.getRow(row);
        int[] columnArray = this.getColumn(column);
        int[] blockArray = this.getBlock(row, column);

        return Utils.intArrayDoesNotContain(rowArray, value)
                && Utils.intArrayDoesNotContain(columnArray, value)
                && Utils.intArrayDoesNotContain(blockArray, value);
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

    void print() {
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

    private static int getIndexFromRowAndColumn(int row, int column) {
        return 9 * row + column;
    }

}