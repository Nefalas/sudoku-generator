package se.nefalas;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;

import static java.util.stream.Collectors.toMap;
import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;

class SudokuReader {

    static{ System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    static Sudoku readSudoku(String imgPath) {
        List<Rect> rects = getContours(imgPath);

        for (Rect rect : rects) {
            System.out.println(rect.toString());
        }

        return null;
    }

    private static List<Rect> getContours(String imgPath) {
        Mat original = Imgcodecs.imread(imgPath);
        Mat image = new Mat();

        Imgproc.cvtColor(original, image, Imgproc.COLOR_BGR2GRAY);
        Imgproc.threshold(image, image, 180, 255, 3);

        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(image, contours, new Mat(), Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);

        showMat(image, 2, "Transformed image");

        Mat contourImage = original.clone();
        Mat contoursOnly = new Mat(original.size(), original.type());

        int index = 0;
        List<Rect> rects = new ArrayList<>();
        for (MatOfPoint mat : contours) {
            rects.add(Imgproc.boundingRect(mat));
            Imgproc.drawContours(contourImage, contours, index, new Scalar(0, 255, 0), 1);
            Imgproc.drawContours(contoursOnly, contours, index++, new Scalar(0, 255, 0), 1);
        }

        if (rects.size() > 0) {
            rects = pruneContours(rects);
        }

        Mat prunedContours = new Mat(original.size(), original.type());
        for (Rect contour : rects) {
            Imgproc.rectangle(prunedContours, contour, new Scalar(0, 255, 0));
        }

        showMat(original, 2, "Original");
        showMat(contourImage, 2, "Contours on image");
        showMat(contoursOnly, 2,"Contours");
        showMat(prunedContours, 2, "Pruned contours");

        return rects;
    }

    private static List<Rect> pruneContours(List<Rect> contours) {
        int size = getMostCommonSize(contours);
        int threshold = (int)Math.round(size * 0.8);

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
            for (int size : new int[] { contour.height, contour.width }) {
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

    private static void showMat(Mat img, double ratio, String name) {
        Imgproc.resize(img, img, new Size(img.width() * ratio, img.height() * ratio));
        MatOfByte matOfByte = new MatOfByte();
        Imgcodecs.imencode(".jpg", img, matOfByte);

        byte[] byteArray = matOfByte.toArray();
        BufferedImage bufImage;

        try {
            InputStream in = new ByteArrayInputStream(byteArray);
            bufImage = ImageIO.read(in);
            JFrame frame = new JFrame();

            frame.setTitle(name);
            frame.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            frame.getContentPane().add(new JLabel(new ImageIcon(bufImage)));
            frame.pack();
            frame.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
