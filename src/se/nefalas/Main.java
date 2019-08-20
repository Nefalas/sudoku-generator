package se.nefalas;

import java.util.Objects;

public class Main {

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

        GUI gui = new GUI();
        gui.init();
        gui.start();

        Computer computer = new Computer(gui);

        class OnDone implements Callback {
            @Override
            public void run() {
                gui.stop();
            }
        }

        computer.solve(sudoku, new OnDone());

        String imgPath = Objects.requireNonNull(Main.class.getClassLoader().getResource("sudoku.jpg")).getFile();
        System.out.println(imgPath);

        SudokuReader.readSudoku("D:/Projects/SudokuSolver/out/production/SudokuSolver/sudoku.jpg");
    }
}
