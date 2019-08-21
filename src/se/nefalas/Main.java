package se.nefalas;

import java.io.File;
import java.util.Objects;

public class Main {

    private static GUI gui;

    public static void main(String[] args) {
        gui = new GUI();
        gui.init();
        gui.start();
        gui.setStep(GUI.STEP.READ);

        //String filename = "sudoku.jpg";
        String filename = "hard_sudoku2.PNG";
        //String filename = "hard_sudoku.PNG";
        String imgPath = Objects.requireNonNull(Main.class.getClassLoader().getResource(filename)).getFile();

        SudokuReader sudokuReader = new SudokuReader(Main::onReadUpdate);
        sudokuReader.setDebug(true);

        Sudoku sudoku = sudokuReader.readSudoku(new File(imgPath).getAbsolutePath());
        sudoku.print(true);

        gui.setStep(GUI.STEP.SOLVE);

        Computer computer = new Computer(gui::setSudoku, Main::onSolve);
        computer.solve(sudoku);
    }

    private static void onReadUpdate(int[] values) {
        Sudoku sudoku = new Sudoku(values);

        gui.setSudoku(sudoku, 0);
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
