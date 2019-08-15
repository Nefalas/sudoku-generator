package se.nefalas;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferStrategy;

public class GUI extends Canvas {

    private Sudoku sudoku;

    private Graphics2D g;
    private BufferStrategy strategy;

    private final int WINDOW_WIDTH = 1280;
    private final int WINDOW_HEIGHT = 720;

    private final Color BACKGROUND_COLOR = Color.WHITE;

    private final Font NUMBER_FONT = new Font("Lucida Blackletter", Font.PLAIN, 30);

    GUI(Sudoku sudoku) {
        this.sudoku = sudoku;

        this.setupJFrame();
    }

    private void setupJFrame() {
        JFrame container = new JFrame("Radix Sort Visual");
        container.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = (JPanel) container.getContentPane();
        panel.setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        panel.setLayout(null);

        setBounds(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
        panel.add(this);

        setIgnoreRepaint(true);

        container.setResizable(false);
        container.setVisible(true);
        container.pack();

        requestFocus();

        createBufferStrategy(2);
        this.strategy = getBufferStrategy();

        this.init();
    }

    private void init() {
        g = (Graphics2D) strategy.getDrawGraphics();
        g.setColor(BACKGROUND_COLOR);
        g.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
    }

}
