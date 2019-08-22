package se.nefalas;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

import org.apache.commons.cli.*;

import javax.imageio.ImageIO;

public class Main {

    private static Options options;
    private static GUI gui;

    public static void main(String[] args) {
        CommandLine cmd = parseArgs(args);
        if (cmd == null) {
            return;
        }

        String imgPath = null;
        BufferedImage bufferedImage = null;

        if (cmd.hasOption("i")) {
            imgPath = cmd.getOptionValue("i");
        } else if (cmd.hasOption("e")) {
            String filename;

            switch (cmd.getOptionValue("e")) {
                case "1":
                default:
                    filename = "sudoku.jpg";
                    break;
                case "2":
                    filename = "hard_sudoku.PNG";
                    break;
                case "3":
                    filename = "hard_sudoku2.PNG";
            }

            try {
                bufferedImage = ImageIO.read(Objects.requireNonNull(Main.class.getClassLoader().getResourceAsStream(filename)));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        } else {
            printHelp("You need to select either an image or an example");

            return;
        }

        boolean isDebug = cmd.hasOption("db");

        gui = new GUI();
        gui.init();
        gui.start();
        gui.setStep(GUI.STEP.READ);

        SudokuReader sudokuReader = new SudokuReader(Main::onReadUpdate);
        sudokuReader.setDebug(isDebug);

        Sudoku sudoku = imgPath == null
                ? sudokuReader.readSudoku(bufferedImage)
                : sudokuReader.readSudoku(new File(imgPath).getAbsolutePath());
        sudoku.print(true);

        gui.setStep(GUI.STEP.SOLVE);

        Computer computer = new Computer(gui::setSudoku, Main::onSolve);
        computer.solve(sudoku);
    }

    private static CommandLine parseArgs(String[] args) {
        options = new Options();

        Option imagePath = new Option("i", "image", true, "input image path");
        imagePath.setRequired(false);
        options.addOption(imagePath);

//        Option directoryPath = new Option("d", "directory", true, "input images directory path");
//        directoryPath.setRequired(false);
//        options.addOption(directoryPath);

        Option debug = new Option("db", "debug", false, "debug mode");
        debug.setRequired(false);
        options.addOption(debug);

        Option example = new Option("e", "example", true, "run example (1, 2 or 3)");
        example.setRequired(false);
        options.addOption(example);

        Option help = new Option("h", "help", false, "print help");
        help.setRequired(false);
        options.addOption(help);

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);

            if (cmd.hasOption("h")) {
                printHelp("Help");
            }

            return cmd;
        } catch (ParseException e) {
            printHelp(e.getMessage());

            return null;
        }
    }

    private static void printHelp(String message) {
        HelpFormatter formatter = new HelpFormatter();

        System.out.println(message);
        formatter.printHelp("<program name>", options);

        System.exit(1);
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
