package se.nefalas;

public class Main {

    public static void main(String[] args) {
        int[] values = {
                0,0,0, 0,9,0, 6,0,8,
                0,2,4, 0,0,0, 0,0,0,
                0,0,8, 0,0,3, 7,0,0,

                3,0,0, 0,0,0, 9,0,0,
                0,0,0, 2,5,0, 0,3,0,
                0,0,0, 4,0,0, 0,0,5,

                8,4,0, 5,0,0, 0,0,6,
                0,3,0, 0,0,4, 0,2,0,
                0,1,0, 0,0,7, 0,0,0
        };
//        int[] values = {0, 0, 0, 0, 0, 0, 0, 1, 4, 0, 0, 0, 0, 2, 0, 0, 0, 0, 5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 8, 0, 4, 0, 0, 0, 7, 0, 0, 0, 0, 0, 5, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5, 0, 7, 3, 0, 0, 0, 4, 2, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 0, 6, 0, 0};

        Sudoku sudoku = new Sudoku(values);

        if (Computer.solve(sudoku, true)) {
            System.out.println("Solved");
        } else {
            System.out.println("Could not solve");
        }

        sudoku.print(true);
    }
}
