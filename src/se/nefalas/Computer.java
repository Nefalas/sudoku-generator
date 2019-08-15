package se.nefalas;

class Computer {

    static void solve(Sudoku sudoku) {
        long start = System.currentTimeMillis();

        fillPossibleValues(sudoku);

        if (sudoku.isFull()) {
            return;
        }

        int firstIndex = sudoku.getFirstEmptyIndex();
        int[] possibleValues = sudoku.getPossibleValues(firstIndex);

        for (int value : possibleValues) {
            Sudoku copy = sudoku.copy();
            copy.setValue(firstIndex, value);

            String name = "Solver " + value + " at " + firstIndex;

            RecursiveThreadSolver solver = new RecursiveThreadSolver(copy, name);
            solver.start();
        }

//        long elapsed = System.currentTimeMillis() - start;
//        double elapsedSeconds = (double)elapsed / 1000.0;
//        String message = String.format("Took %.4f seconds to solve", elapsedSeconds);
//        System.out.println(message);
    }

    private static void fillPossibleValues(Sudoku sudoku) {
        int row, column;
        boolean didChange;

        do {
            didChange = false;
            for (int i = 0; i < 81; i++) {
                if (sudoku.isEmptyAtIndex(i)) {
                    continue;
                }

                row = i / 9;
                column = i % 9;

                int currentValue = sudoku.getValue(row, column);

                if (currentValue != 0) {
                    sudoku.emptyPossibleValues(row, column, currentValue);

                    didChange = true;
                } else {
                    int[] remainingValues = sudoku.getPossibleValues(row, column);
                    if (remainingValues.length == 1) {
                        sudoku.setValue(row, column, remainingValues[0]);
                        sudoku.emptyPossibleValues(row, column, remainingValues[0]);

                        didChange = true;
                    }
                }
            }
        } while (didChange);
    }

    private static Sudoku solveRecursive(Sudoku sudoku) {
        if (sudoku.isFull()) return sudoku;

        int row, column;
        for (int i = 0; i < 81; i++) {
            row = i / 9;
            column = i % 9;

            if (sudoku.getValue(row, column) != 0) {
                continue;
            }

            int[] possibleValues = sudoku.getPossibleValues(row, column);
            for (int value : possibleValues) {
                Sudoku copy = sudoku.copy();
                copy.setValue(row, column, value);
                fillPossibleValues(copy);

                if (copy.isFull()) {
                    return copy;
                } else {
                    Sudoku solved = solveRecursive(copy);

                    if (solved != null) {
                        return solved;
                    }
                }
            }

            break;
        }

        return null;
    }

    static class RecursiveThreadSolver implements Runnable {
        private Thread thread;
        private String name;

        private Sudoku sudoku;

        RecursiveThreadSolver(Sudoku sudoku, String name) {
            this.sudoku = sudoku;
            this.name = name;
        }

        @Override
        public void run() {
            Sudoku result = Computer.solveRecursive(this.sudoku);

            if (result == null) {
                System.out.println("Could not solve by " + name);
            } else {
                System.out.println("Solved by " + name);
                result.print(true);
            }
        }

        void start() {
            if (this.thread == null) {
                this.thread = new Thread(this, this.name);
                this.thread.start();
            }
        }
    }
}
