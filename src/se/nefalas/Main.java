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
        Sudoku sudoku = new Sudoku(values);
        sudoku.print();

        if (Computer.solve(sudoku)) {
            System.out.println("Yes");
        } else {
            System.out.println("No");
        }
    }
}
