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

    SudokuReader() {
        this.tesseract = new Tesseract();

        String path = Objects.requireNonNull(this.getClass().getClassLoader().getResource("tessdata")).getFile();
        this.tesseract.setDatapath(path);
        this.tesseract.setTessVariable("tessedit_char_whitelist", "123456789");
    }

    Sudoku readSudoku(String imgPath) {
        Mat image = Imgcodecs.imread(imgPath);
        Mat preparedImage = prepareImage(image);

        showMat(image, 2, "Original");
        showMat(preparedImage, 2, "Transformed");

        List<Rect> bBoxes = getBBoxes(image, preparedImage);
        List<Point> corners = getCornersFromRects(bBoxes);

        Mat cornersOnly = new Mat(image.size(), CvType.CV_8UC3, new Scalar(0, 0, 0));
        Mat gridOnly = cornersOnly.clone();
        Mat gridOnImage = image.clone();

        for (Point corner : corners) {
            Imgproc.circle(cornersOnly, corner, 1, new Scalar(0, 255, 0), 1);
        }

        showMat(cornersOnly, 2, "Corners");

        List<Double> rows = extractGridValues(corners, GRID_VALUES.ROWS);
        List<Double> cols = extractGridValues(corners, GRID_VALUES.COLUMNS);

        if (rows.size() != 10 && cols.size() != 10) {
            System.out.println("Could not read sudoku");

            return null;
        }

        drawGrid(gridOnly, rows, cols);
        drawGrid(gridOnImage, rows, cols);

        showMat(gridOnly, 2, "Grid");
        showMat(gridOnImage, 2, "Grid on image");

        Rectangle[] rois = getROIs(rows, cols);
        BufferedImage ocrImage = matToImage(preparedImage);

        try {
            int[] values = readROIs(ocrImage, rois);

            return new Sudoku(values);
        } catch (TesseractException e) {
            e.printStackTrace();

            return null;
        }
    }

    private static Mat prepareImage(Mat image) {
        Mat result = new Mat();

        Imgproc.cvtColor(image, result, Imgproc.COLOR_BGR2GRAY);
        Imgproc.threshold(result, result, 180, 255, Imgproc.THRESH_TOZERO);

        return result;
    }

    private static List<Rect> getBBoxes(Mat original, Mat image) {
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(image, contours, new Mat(), Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);

        Mat contourImage = original.clone();
        Mat contoursOnly = new Mat(original.size(), CvType.CV_8UC3, new Scalar(0, 0, 0));
        Mat bboxesOnly = contoursOnly.clone();
        Mat prunedBBoxes = contoursOnly.clone();

        int index = 0;
        List<Rect> bboxes = new ArrayList<>();
        for (MatOfPoint mat : contours) {
            Rect bbox = Imgproc.boundingRect(mat);
            bboxes.add(bbox);

            Imgproc.drawContours(contourImage, contours, index, new Scalar(0, 255, 0), 1);
            Imgproc.drawContours(contoursOnly, contours, index++, new Scalar(0, 255, 0), 1);
            Imgproc.rectangle(bboxesOnly, bbox, new Scalar(0, 255, 0));
        }

        if (bboxes.size() > 0) {
            bboxes = pruneContours(bboxes);
        }

        for (Rect contour : bboxes) {
            Imgproc.rectangle(prunedBBoxes, contour, new Scalar(0, 255, 0));
        }

        showMat(contourImage, 2, "Contours on image");
        showMat(contoursOnly, 2, "Contours");
        showMat(bboxesOnly, 2, "Bounding boxes");
        showMat(prunedBBoxes, 2, "Pruned bounding boxes");

        return bboxes;
    }

    private static List<Rect> pruneContours(List<Rect> contours) {
        int size = getMostCommonSize(contours);
        int threshold = (int) Math.round(size * 0.8);

        List<Rect> pruned = new ArrayList<>();
        for (Rect contour : contours) {
            if (contour.width >= threshold || contour.height >= threshold) {
                pruned.add(contour);
            }
        }

        return pruned;
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

    private static Rectangle[] getROIs(List<Double> rows, List<Double> cols) {
        Rectangle[] boxes = new Rectangle[81];

        int index = 0;
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                int x = (int) Math.round(cols.get(col));
                int y = (int) Math.round(rows.get(row));
                int width = (int) Math.round(cols.get(col + 1) - x);
                int height = (int) Math.round(rows.get(row + 1) - y);

                boxes[index++] = new Rectangle(x, y, width, height);
            }
        }

        return boxes;
    }

    private int readROI(BufferedImage image, Rectangle rectangle) throws TesseractException {
        try {
            String result = this.tesseract.doOCR(image, rectangle).trim();
            System.out.print(result + " --- ");

            result = result.replaceAll("[^0-9]", "");
            System.out.println(result);

            if (result.equals("")) {
                return 0;
            }

            return Integer.parseInt(result);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private int[] readROIs(BufferedImage image, Rectangle[] rois) throws TesseractException {
        int[] values = new int[rois.length];

        for (int i = 0; i < rois.length; i++) {
            values[i] = readROI(image, rois[i]);
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
        byte[] data = new byte[mat.width() * mat.height() * (int)mat.elemSize()];

        mat.get(0, 0, data);
        int type = mat.channels() == 1 ? BufferedImage.TYPE_BYTE_GRAY : BufferedImage.TYPE_3BYTE_BGR;

        BufferedImage out = new BufferedImage(mat.width(), mat.height(), type);
        out.getRaster().setDataElements(0, 0, mat.width(), mat.height(), data);

        return out;
    }

    private static void showMat(Mat img, double ratio, String name) {
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
}
