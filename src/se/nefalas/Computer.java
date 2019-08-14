package se.nefalas;

class Computer {

    static boolean solve(Sudoku sudoku, boolean print) {
        fillPossibleValues(sudoku, print);

        return solveRecursive(sudoku, print);
    }

    private static void fillPossibleValues(Sudoku sudoku, boolean print) {
        int row, column;
        for (int i = 0; i < 81; i++) {
            row = i / 9;
            column = i % 9;

            int currentValue = sudoku.getValue(row, column);

            if (currentValue != 0) {
                sudoku.removeAllPossibleValues(row, column);
                sudoku.removePossibleValueFromNeighbours(row, column, currentValue);
            } else {
                for (int val = 1; val <= 9; val++) {
                    if (sudoku.cannotHaveValue(row, column, val)) {
                        sudoku.removePossibleValue(row, column, val);
                    }
                }

                int[] remainingValues = sudoku.getPossibleValues(row, column);
                if (remainingValues.length == 1) {
                    sudoku.setValue(row, column, remainingValues[0]);
                    sudoku.removePossibleValueFromNeighbours(row, column, remainingValues[0]);
                    sudoku.removeAllPossibleValues(row, column);

                    if (print) sudoku.print();
                }
            }
        }
    }

    private static boolean solveRecursive(Sudoku sudoku, boolean print) {
        if (print) sudoku.print();

        int row, column;
        for (int i = 0; i < 81; i++) {
            row = i / 9;
            column = i % 9;

            if (sudoku.getValue(row, column) != 0) {
                continue;
            }

            int[] possibleValues = sudoku.getPossibleValues(row, column);
            for (int value : possibleValues) {
                if (sudoku.cannotHaveValue(row, column, value)) {
                    continue;
                }

                sudoku.setValue(row, column, value);
                if (print) sudoku.print();

                if (sudoku.isFull()) {
                    return true;
                } else if (solveRecursive(sudoku, print)) {
                    return true;
                } else {
                    sudoku.setValue(row, column, 0);
                }
            }

            break;
        }

        return false;
    }
}
