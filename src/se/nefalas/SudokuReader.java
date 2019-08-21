package se.nefalas;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

class SudokuReader {

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    private enum GRID_VALUES {ROWS, COLUMNS}

    private Tesseract tesseract;
    private boolean debug;

    private OnUpdate onUpdate;

    SudokuReader(OnUpdate onUpdate) {
        this.onUpdate = onUpdate;

        this.tesseract = new Tesseract();

        String path = Objects.requireNonNull(this.getClass().getClassLoader().getResource("tessdata")).getFile();
        this.tesseract.setDatapath(new File(path).getAbsolutePath());
        this.tesseract.setTessVariable("debug_file", "/dev/null");
        this.tesseract.setTessVariable("load_system_dawg", "F");
        this.tesseract.setTessVariable("load_freq_dawg", "F");
        this.tesseract.setTessVariable("tessedit_char_whitelist", "123456789");
        this.tesseract.setPageSegMode(10);

        this.debug = false;
    }

    Sudoku readSudoku(String imgPath) {
        long start = System.currentTimeMillis();

        Mat image = Imgcodecs.imread(imgPath);
        Mat gridDetectionImage = prepareImageForGridDetection(image);
        Mat OCRImage = prepareImageForOCR(image);

        if (this.debug) {
            showMat(image, "Original", 2, 5, 0);
            showMat(gridDetectionImage, "Grid detection", 2, 5, 1);
            showMat(OCRImage,"OCR", 2, 5, 2);
        }

        PrunedBBoxes bboxes = this.getBBoxes(image, gridDetectionImage);

        List<Point> corners = getCornersFromRects(bboxes.getPruned());

        if (this.debug) {
            Mat cornersOnly = new Mat(image.size(), CvType.CV_8UC3, new Scalar(0, 0, 0));

            for (Point corner : corners) {
                Imgproc.circle(cornersOnly, corner, 1, new Scalar(0, 255, 0), 1);
            }

            showMat(cornersOnly, "Corners", 2, 5, 7);
        }

        List<Double> rows = extractGridValues(corners, GRID_VALUES.ROWS);
        List<Double> cols = extractGridValues(corners, GRID_VALUES.COLUMNS);

        if (rows.size() != 10 && cols.size() != 10) {
            System.out.println("Could not read sudoku");

            return null;
        }

        if (this.debug) {
            Mat gridOnly = new Mat(image.size(), CvType.CV_8UC3, new Scalar(0, 0, 0));
            Mat gridOnImage = image.clone();

            drawGrid(gridOnly, rows, cols);
            drawGrid(gridOnImage, rows, cols);

            showMat(gridOnly,"Grid", 2, 5, 8);
            showMat(gridOnImage,"Grid on image", 2, 5, 9);
        }

        Rectangle[] boxes = getBoxes(rows, cols);

        boolean[] usedBoxes = this.getUsedBoxes(boxes, bboxes.getRemaining());

        BufferedImage ocrImage = matToImage(OCRImage);

        int[] values = readBoxes(ocrImage, boxes, usedBoxes);

        long elapsed = System.currentTimeMillis() - start;
        double elapsedSeconds = (double) elapsed / 1000.0;
        String message = String.format("Took %.4f seconds to read", elapsedSeconds);
        System.out.println(message);

        return new Sudoku(values);
    }

    void setDebug(boolean active) {
        this.debug = active;
    }

    private static Mat prepareImageForGridDetection(Mat image) {
        Mat result = new Mat();

        Imgproc.cvtColor(image, result, Imgproc.COLOR_BGR2GRAY);
        Imgproc.threshold(result, result, 225, 255, Imgproc.THRESH_BINARY);

        return result;
    }

    private static Mat prepareImageForOCR(Mat image) {
        Mat result = new Mat();

        Imgproc.cvtColor(image, result, Imgproc.COLOR_BGR2GRAY);
        Imgproc.threshold(result, result, 180, 255, Imgproc.THRESH_BINARY);

        return result;
    }

    private PrunedBBoxes getBBoxes(Mat original, Mat image) {
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(image, contours, new Mat(), Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);

        Mat contourImage = original.clone();
        Mat contoursOnly = new Mat(original.size(), CvType.CV_8UC3, new Scalar(0, 0, 0));
        Mat bboxesOnly = contoursOnly.clone();
        Mat prunedBBoxesOnly = contoursOnly.clone();

        int index = 0;
        List<Rect> bboxes = new ArrayList<>();
        for (MatOfPoint mat : contours) {
            Rect bbox = Imgproc.boundingRect(mat);
            bboxes.add(bbox);

            if (this.debug) {
                Imgproc.drawContours(contourImage, contours, index, new Scalar(0, 255, 0), 1);
                Imgproc.drawContours(contoursOnly, contours, index++, new Scalar(0, 255, 0), 1);
                Imgproc.rectangle(bboxesOnly, bbox, new Scalar(0, 255, 0));
            }
        }
        PrunedBBoxes prunedBBoxes = this.pruneBBoxes(bboxes);

        for (Rect bbox : prunedBBoxes.getPruned()) {
            Imgproc.rectangle(prunedBBoxesOnly, bbox, new Scalar(0, 255, 0));
        }

        if (this.debug) {
            showMat(contourImage,"Contours on image", 2, 5, 3);
            showMat(contoursOnly,"Contours", 2, 5, 4);
            showMat(bboxesOnly, "Bounding boxes", 2, 5, 5);
            showMat(prunedBBoxesOnly,"Pruned bounding boxes", 2, 5, 6);
        }

        return prunedBBoxes;
    }

    private PrunedBBoxes pruneBBoxes(List<Rect> bboxes) {
        int size = getMostCommonSize(bboxes);
        int threshold = (int) Math.round(size * 0.8);
        int maxSize = size * 4;

        List<Rect> pruned = new ArrayList<>();
        List<Rect> remaining = new ArrayList<>();
        for (Rect bbox : bboxes) {
            if (bbox.width < maxSize && bbox.height < maxSize) {
                if (bbox.width >= threshold || bbox.height >= threshold) {
                    pruned.add(bbox);
                } else {
                    remaining.add(bbox);
                }
            }
        }

        return new PrunedBBoxes(pruned, remaining);
    }

    private boolean[] getUsedBoxes(Rectangle[] boxes, List<Rect> remainingBBoxes) {
        boolean[] usedBoxes = new boolean[boxes.length];
        Arrays.fill(usedBoxes, false);

        for (Rect bbox : remainingBBoxes) {
            for (int i = 0; i < boxes.length; i++) {
                if (isRectInRectangle(bbox, boxes[i])) {
                    usedBoxes[i] = true;
                    break;
                }
            }
        }

        return usedBoxes;
    }

    private boolean isRectInRectangle(Rect bbox, Rectangle rectangle) {
        int centerX = bbox.x + bbox.width / 2;
        int centerY = bbox.y + bbox.height / 2;

        return centerX >= rectangle.x
                && centerX <= (rectangle.x + rectangle.width)
                && centerY >= rectangle.y
                && centerY <= (rectangle.y + rectangle.height);
    }

    private static int getMostCommonSize(List<Rect> contours) {
        Map<Integer, Integer> sizes = new HashMap<>();

        for (Rect contour : contours) {
            for (int size : new int[]{contour.height, contour.width}) {
                if (sizes.containsKey(size)) {
                    sizes.put(size, sizes.get(size) + 1);
                } else {
                    sizes.put(size, 1);
                }
            }
        }

        Map<Integer, Integer> sorted = sizes
                .entrySet()
                .stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));

        return sorted.entrySet().iterator().next().getKey();
    }

    private static List<Point> getCornersFromRects(List<Rect> rects) {
        List<Point> corners = new ArrayList<>();
        for (Rect rect : rects) {
            corners.addAll(getCornersFromRect(rect));
        }

        return groupClosePoints(corners);
    }

    private static List<Point> getCornersFromRect(Rect rect) {
        List<Point> corners = new ArrayList<>();
        corners.add(new Point(rect.x, rect.y));
        corners.add(new Point(rect.x + rect.width, rect.y));
        corners.add(new Point(rect.x, rect.y + rect.height));
        corners.add(new Point(rect.x + rect.width, rect.y + rect.height));

        return corners;
    }

    private static List<Point> groupClosePoints(List<Point> points) {
        final int threshold = 5;

        List<Point> grouped = new ArrayList<>();
        for (Point point : points) {
            if (grouped.size() == 0) {
                grouped.add(point);
            } else {
                boolean didChange = false;

                for (int i = 0; i < grouped.size(); i++) {
                    Point currentPoint = grouped.get(i);
                    double distance = getDistanceBetweenPoints(point, currentPoint);

                    if (distance <= threshold) {
                        grouped.set(i, getMiddlePoint(point, currentPoint));
                        didChange = true;

                        break;
                    }
                }

                if (!didChange) {
                    grouped.add(point);
                }
            }
        }

        return grouped;
    }

    private static List<Double> groupValues(List<Double> values) {
        final int threshold = 5;

        List<Double> grouped = new ArrayList<>();
        for (double value : values) {
            if (grouped.size() == 0) {
                grouped.add(value);
            } else {
                boolean didChange = false;

                for (int i = 0; i < grouped.size(); i++) {
                    double currentValue = grouped.get(i);
                    double difference = Math.abs(value - currentValue);

                    if (difference <= threshold) {
                        grouped.set(i, (value + currentValue) / 2.0);
                        didChange = true;

                        break;
                    }
                }

                if (!didChange) {
                    grouped.add(value);
                }
            }
        }

        return grouped;
    }

    private static List<Double> extractGridValues(List<Point> points, GRID_VALUES gridValues) {
        List<Double> values = points
                .stream()
                .map(point -> gridValues == GRID_VALUES.COLUMNS ? point.x : point.y)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        return groupValues(values);
    }

    private static double getDistanceBetweenPoints(Point firstPoint, Point secondPoint) {
        return Math.sqrt(Math.pow(secondPoint.x - firstPoint.x, 2) + Math.pow(secondPoint.y - firstPoint.y, 2));
    }

    private static Point getMiddlePoint(Point firstPoint, Point secondPoint) {
        double x = Math.round((firstPoint.x + secondPoint.x) / 2.0);
        double y = Math.round((firstPoint.y + secondPoint.y) / 2.0);

        return new Point(x, y);
    }

    private static Rectangle[] getBoxes(List<Double> rows, List<Double> cols) {
        Rectangle[] boxes = new Rectangle[81];
        final double margin = 0.1;

        int index = 0;
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                int x = (int) Math.round(cols.get(col));
                int y = (int) Math.round(rows.get(row));
                int width = (int) Math.round(cols.get(col + 1) - x);
                int height = (int) Math.round(rows.get(row + 1) - y);

                boxes[index++] = new Rectangle(
                        (int) Math.round(x + margin * width),
                        (int) Math.round(y + margin * height),
                        (int) Math.round(width - 2 * margin * width),
                        (int) Math.round(height - 2 * margin * height)
                );
            }
        }

        return boxes;
    }

    private int readROI(BufferedImage image, Rectangle rectangle) {
        try {
            String result = this.tesseract.doOCR(image, rectangle).trim().replaceAll("[^0-9]", "");

            if (result.equals("")) {
                return 0;
            }

            return Integer.parseInt(result);
        } catch (TesseractException | NumberFormatException e) {
            return 0;
        }
    }

    private int[] readBoxes(BufferedImage image, Rectangle[] boxes, boolean[] usedBoxes) {
        int[] values = new int[boxes.length];

        for (int i = 0; i < boxes.length; i++) {
            boolean isUsed = usedBoxes[i];

            if (isUsed) {
                values[i] = readROI(image, boxes[i]);
            } else {
                values[i] = 0;
            }

            this.onUpdate.run(values);
        }

        return values;
    }

    private static void drawGrid(Mat img, List<Double> rows, List<Double> cols) {
        double xMin = cols.get(0), xMax = cols.get(cols.size() - 1);
        double yMin = rows.get(0), yMax = rows.get(rows.size() - 1);
        double maxSize = Math.max(rows.size(), cols.size());

        for (int i = 0; i < maxSize; i++) {
            if (cols.size() > i) {
                double x = cols.get(i);
                drawLine(img, x, yMin, x, yMax);
            }

            if (rows.size() > i) {
                double y = rows.get(i);
                drawLine(img, xMin, y, xMax, y);
            }
        }
    }

    private static void drawLine(Mat img, double x1, double y1, double x2, double y2) {
        Point start = new Point(x1, y1);
        Point end = new Point(x2, y2);
        Imgproc.line(img, start, end, new Scalar(0, 255, 0), 2);
    }

    private static BufferedImage matToImage(Mat mat) {
        byte[] data = new byte[mat.width() * mat.height() * (int) mat.elemSize()];

        mat.get(0, 0, data);
        int type = mat.channels() == 1 ? BufferedImage.TYPE_BYTE_GRAY : BufferedImage.TYPE_3BYTE_BGR;

        BufferedImage out = new BufferedImage(mat.width(), mat.height(), type);
        out.getRaster().setDataElements(0, 0, mat.width(), mat.height(), data);

        return out;
    }

    private static void showMat(Mat img, String name, int rows, int cols, int index) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        double maxWidth = screenSize.getWidth() / (double) cols;
        double maxHeight = screenSize.getHeight() / (double) rows;

        double size = Math.min(maxWidth, maxHeight) - 15; // account for borders
        double ratio = size / Math.max(img.width(), img.height());

        int row = index / cols;
        int col = index % cols;

        int x = (int) Math.round(col * maxWidth);
        int y = (int) Math.round(row * (img.height() * ratio + 40)); // account for top bar

        Mat resized = new Mat();
        Imgproc.resize(img, resized, new Size(img.width() * ratio, img.height() * ratio));
        MatOfByte matOfByte = new MatOfByte();
        Imgcodecs.imencode(".jpg", resized, matOfByte);

        byte[] byteArray = matOfByte.toArray();
        BufferedImage bufImage;

        try {
            InputStream in = new ByteArrayInputStream(byteArray);
            bufImage = ImageIO.read(in);
            JFrame frame = new JFrame();

            frame.setLocation(x, y);
            frame.setTitle(name);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // DISPOSE_ON_CLOSE
            frame.setResizable(false);
            frame.getContentPane().add(new JLabel(new ImageIcon(bufImage)));
            frame.pack();
            frame.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class PrunedBBoxes {
        private final List<Rect> pruned;
        private final List<Rect> remaining;

        PrunedBBoxes(List<Rect> pruned, List<Rect> remaining) {
            this.pruned = pruned;
            this.remaining = remaining;
        }

        List<Rect> getPruned() {
            return pruned;
        }

        List<Rect> getRemaining() {
            return remaining;
        }
    }

    interface OnUpdate {
        void run(int[] values);
    }
}