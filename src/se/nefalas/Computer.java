package se.nefalas;

class Computer {

    private OnUpdate onUpdate;
    private OnSolve onSolve;

    private boolean isSolved;

    Computer(OnUpdate onUpdate, OnSolve onSolve) {
        this.onUpdate = onUpdate;
        this.onSolve = onSolve;
    }

    void solve(Sudoku sudoku) {
        long start = System.currentTimeMillis();

        fillPossibleValues(sudoku);

        if (sudoku.isFull()) {
            this.onSolve.run(sudoku, 4, start);

            return;
        }

        int firstIndex = sudoku.getFirstEmptyIndex();
        int[] possibleValues = sudoku.getPossibleValues(firstIndex);

        for (int value : possibleValues) {
            Sudoku copy = sudoku.copy();
            copy.setValue(firstIndex, value);

            String name = "Solver " + value + " at " + firstIndex;

            RecursiveThreadSolver solver = new RecursiveThreadSolver(copy, name, start, value - 1);
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

                this.onUpdate.run(copy, index);

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

        private Sudoku sudoku;

        RecursiveThreadSolver(Sudoku sudoku, String name, long start, int index) {
            this.sudoku = sudoku;
            this.name = name;
            this.start = start;
            this.index = index;
        }

        @Override
        public void run() {
            Sudoku result = solveRecursive(this.sudoku, this.index);

            if (result != null) {
                isSolved = true;

                Computer.this.onSolve.run(result, this.index, start);
            }
        }

        void start() {
            if (this.thread == null) {
                this.thread = new Thread(this, this.name);
                this.thread.start();
            }
        }
    }

    interface OnUpdate {
        void run(Sudoku sudoku, int sudokuIndex);
    }

    interface OnSolve {
        void run(Sudoku sudoku, int sudokuIndex, long start);
    }

}