package se.nefalas;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferStrategy;
import java.util.Timer;
import java.util.TimerTask;

class GUI extends Canvas {

    private Graphics2D g;
    private BufferStrategy strategy;
    private JPanel panel;

    private int windowWidth = 1280;
    private int windowHeight = 720;

    private final Color BACKGROUND_COLOR = Color.WHITE;
    private final Color TEXT_COLOR = Color.GRAY;

    private Font NUMBER_FONT = new Font("Lucida Blackletter", Font.PLAIN, 30);

    private boolean isRunning = false;

    private Sudoku[] sudokus;

    GUI() {
        this.sudokus = new Sudoku[9];

        this.setupJFrame();
    }

    private void setupJFrame() {
        JFrame container = new JFrame("Sudoku Solver");
        container.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        panel = (JPanel) container.getContentPane();
        panel.setPreferredSize(new Dimension(windowWidth, windowHeight));
        panel.setLayout(null);

        setBounds(0, 0, windowWidth, windowHeight);
        panel.add(this);

        setIgnoreRepaint(true);

        container.setResizable(true);
        container.setVisible(true);
        container.pack();

        requestFocus();

        createBufferStrategy(2);
        this.strategy = getBufferStrategy();
    }

    void init() {
        panel.addComponentListener(new PanelAdapter());
    }

    void start() {
        this.isRunning = true;
        this.run();
    }

    void stop() {
        this.isRunning = false;
        System.out.println("GUI stopped");
    }

    private void run() {
        this.run(false);
    }

    private void run(boolean force) {
        if (!this.isRunning && !force) {
            return;
        }

        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                long start = System.currentTimeMillis();
                final int FPS = 60;

                GUI.this.setup();
                GUI.this.draw();
                GUI.this.display();

                long elapsed = System.currentTimeMillis() - start;
                int remaining = (int) ((1000 / FPS) - elapsed);

                if (remaining < 0) {
                    remaining = 0;
                }

                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        GUI.this.run();
                    }
                }, remaining);
            }
        });
    }

    void setSudoku(Sudoku sudoku, int index) {
        this.sudokus[index] = sudoku;
    }

    private void setup() {
        g = (Graphics2D) strategy.getDrawGraphics();
        g.setColor(BACKGROUND_COLOR);
        g.fillRect(0, 0, windowWidth, windowHeight);
    }

    private void draw() {
        this.drawAllGrids();
    }

    private void display() {
        g.dispose();
        strategy.show();
    }

    private void drawAllGrids() {
        final double margin = 50.0;
        final double spacerRatio = .02;
        final double totalWidth = windowWidth - 2.0 * margin;
        final double totalHeight = windowHeight - 2.0 * margin;

        final double maxGridWidth = (totalWidth * (1 - spacerRatio)) / 3.0;
        final double maxGridHeight = (totalHeight * (1 - spacerRatio)) / 3.0;

        final double gridSize = Math.min(maxGridWidth, maxGridHeight);

        final double spacer = spacerRatio * Math.min(totalWidth, totalHeight);

        final double left = margin + (totalWidth - 3 * gridSize - 2 * spacer) / 2;
        final double top = margin + (totalHeight - 3 * gridSize - 2 * spacer) / 2;

        final int fontSize = (int) Math.round(gridSize * 0.08);

        NUMBER_FONT = new Font("Lucida Blackletter", Font.PLAIN, fontSize);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int x = (int) Math.round(left + col * (gridSize + spacer));
                int y = (int) Math.round(top + row * (gridSize + spacer));

                int index = row * 3 + col;

                this.drawGrid(x, y, (int) Math.round(gridSize), sudokus[index]);
            }
        }
    }

    private void drawGrid(int left, int top, int size, Sudoku sudoku) {
        BasicStroke thick = new BasicStroke(3);
        BasicStroke medium = new BasicStroke(2.5F);
        BasicStroke thin = new BasicStroke(1);

        double unit = size / 9.0;
        int right = left + size;
        int bottom = top + size;

        for (int i = 0; i < 10; i++) {
            g.setColor(Color.BLACK);

            if (i == 0 || i == 9) {
                g.setStroke(thick);
            } else if (i % 3 == 0) {
                g.setStroke(medium);
            } else {
                g.setStroke(thin);
            }

            int width = left + (int) Math.round(i * unit);
            int height = top + (int) Math.round(i * unit);

            g.drawLine(left, height, right, height);
            g.drawLine(width, top, width, bottom);

            if (sudoku != null && i < 9) {
                int[] numbers = sudoku.getRow(i);
                int y = top + (int) Math.round(i * unit);

                g.setColor(TEXT_COLOR);
                g.setFont(NUMBER_FONT);
                for (int col = 0; col < 9; col++) {
                    int number = numbers[col];

                    if (number == 0) {
                        continue;
                    }

                    int x = left + (int) Math.round(col * unit);
                    int unitInt = (int) Math.round(unit);

                    this.drawCenteredString(x, y, unitInt, unitInt, Integer.toString(number));
                }
            }
        }
    }

    private void drawCenteredString(int rectX, int rectY, int rectW, int rectH, String text) {
        FontMetrics metrics = g.getFontMetrics(NUMBER_FONT);
        int x = rectX + (rectW - metrics.stringWidth(text)) / 2;
        int y = rectY + ((rectH - metrics.getHeight()) / 2) + metrics.getAscent();

        g.drawString(text, x, y);
    }

    private class PanelAdapter extends ComponentAdapter {
        @Override
        public void componentResized(ComponentEvent e) {
            super.componentResized(e);
            windowWidth = e.getComponent().getWidth();
            windowHeight = e.getComponent().getHeight();
            setBounds(0, 0, windowWidth, windowHeight);

            if (!isRunning) {
                run(true);
            }
        }
    }
}
