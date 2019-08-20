package se.nefalas;

class Computer {

    private GUI gui;

    private boolean isSolved;

    Computer(GUI gui) {
        this.gui = gui;
    }

    void solve(Sudoku sudoku, Callback callback) {
        long start = System.currentTimeMillis();

        fillPossibleValues(sudoku);

        if (sudoku.isFull()) {
            sudoku.print(true);

            long elapsed = System.currentTimeMillis() - start;
            double elapsedSeconds = (double) elapsed / 1000.0;
            String message = String.format("Took %.4f seconds to solve", elapsedSeconds);
            System.out.println(message);

            return;
        }

        int firstIndex = sudoku.getFirstEmptyIndex();
        int[] possibleValues = sudoku.getPossibleValues(firstIndex);

        for (int value : possibleValues) {
            Sudoku copy = sudoku.copy();
            copy.setValue(firstIndex, value);

            String name = "Solver " + value + " at " + firstIndex;

            RecursiveThreadSolver solver = new RecursiveThreadSolver(copy, name, start, value - 1, callback);
            solver.start();
        }
    }

    private void fillPossibleValues(Sudoku sudoku) {
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

    private Sudoku solveRecursive(Sudoku sudoku, int index) {
        if (isSolved) return null;
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

                this.gui.setSudoku(copy, index);

                fillPossibleValues(copy);

                if (copy.isFull()) {
                    return copy;
                } else {
                    Sudoku solved = solveRecursive(copy, index);

                    if (solved != null) {
                        return solved;
                    }
                }
            }

            break;
        }

        return null;
    }

    class RecursiveThreadSolver implements Runnable {
        private Thread thread;
        private String name;
        private long start;
        private int index;

        private Callback callback;

        private Sudoku sudoku;

        RecursiveThreadSolver(Sudoku sudoku, String name, long start, int index, Callback callback) {
            this.sudoku = sudoku;
            this.name = name;
            this.start = start;
            this.index = index;
            this.callback = callback;
        }

        @Override
        public void run() {
            Sudoku result = solveRecursive(this.sudoku, this.index);

            if (result != null) {
                isSolved = true;

                result.print(true);

                long elapsed = System.currentTimeMillis() - start;
                double elapsedSeconds = (double) elapsed / 1000.0;
                String message = String.format("Took %.4f seconds to solve by %s", elapsedSeconds, this.name);
                System.out.println(message);

                callback.run();
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

interface Callback {
    void run();
}
