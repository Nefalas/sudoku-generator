package se.nefalas;

import java.util.Objects;

public class Main {

    private static GUI gui;

    public static void main(String[] args) {
//        int[] values = {
//                0,0,0, 0,9,0, 6,0,8,
//                0,2,4, 0,0,0, 0,0,0,
//                0,0,8, 0,0,3, 7,0,0,
//
//                3,0,0, 0,0,0, 9,0,0,
//                0,0,0, 2,5,0, 0,3,0,
//                0,0,0, 4,0,0, 0,0,5,
//
//                8,4,0, 5,0,0, 0,0,6,
//                0,3,0, 0,0,4, 0,2,0,
//                0,1,0, 0,0,7, 0,0,0
//        };
//        int[] values = {
//                0,9,6, 1,5,7, 0,3,0,
//                0,1,8, 0,0,6, 7,0,0,
//                0,0,3, 2,0,0, 1,0,0,
//
//                5,3,1, 6,0,0, 0,0,4,
//                6,0,0, 8,0,0, 0,5,0,
//                0,0,0, 5,0,9, 0,0,3,
//
//                9,0,0, 0,1,0, 3,0,8,
//                0,8,5, 7,6,0, 0,2,0,
//                0,7,0, 9,0,8, 5,6,0
//        };
        int[] values = {0, 0, 0, 0, 0, 0, 0, 1, 4, 0, 0, 0, 0, 2, 0, 0, 0, 0, 5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 8, 0, 4, 0, 0, 0, 7, 0, 0, 0, 0, 0, 5, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5, 0, 7, 3, 0, 0, 0, 4, 2, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 0, 6, 0, 0};

        Sudoku sudoku = new Sudoku(values);

        gui = new GUI();
        gui.init();
        gui.start();

        Computer computer = new Computer(gui::setSudoku, Main::onSolve);
        computer.solve(sudoku);

        String imgPath = Objects.requireNonNull(Main.class.getClassLoader().getResource("sudoku.jpg")).getFile();
        SudokuReader sudokuReader = new SudokuReader();

        Sudoku imageSudoku = sudokuReader.readSudoku(imgPath);
        imageSudoku.print(true);
    }

    private static void onSolve(Sudoku sudoku, int sudokuIndex, long start) {
        long elapsed = System.currentTimeMillis() - start;
        double elapsedSeconds = (double) elapsed / 1000.0;
        String message = String.format("Took %.4f seconds to solve", elapsedSeconds);
        System.out.println(message);

        gui.stop();

        gui.setSudoku(sudoku, sudokuIndex);
        gui.run(true);
    }
}
