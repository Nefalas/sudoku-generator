package se.nefalas;

public class Computer {

    static boolean solve(Sudoku sudoku) {
        int row = 0, column = 0;

        for (int i = 0; i < 81; i++) {
            row = i / 9;
            column = i % 9;

            if (sudoku.getValue(row, column) != 0) {
                continue;
            }

            for (int value = 1; value <= 9; value++) {
                if (!sudoku.canHaveValue(row, column, value)) {
                    continue;
                }

                sudoku.setValue(row, column, value);
                sudoku.print();

                if (sudoku.isFull()) {
                    return true;
                } else if (solve(sudoku)) {
                    return true;
                } else {
                    sudoku.setValue(row, column, 0);
                }
            }

            break;
        }

        System.out.println("Backtrack");
        return false;
    }
}
