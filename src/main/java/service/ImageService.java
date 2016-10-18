package service;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ImageService {

    public void compareTwoImages() {
        JFrame frame = new JFrame();
        frame.setSize(300, 300);
        File firstImage = getImage("Choose first image, with which we will compare changed image");

        File secondImage = getImage("Choose second image, for this one differences will be shown");


        if (firstImage != null && secondImage != null) {
            BufferedImage bufferedImage1 = readImage(firstImage);
            BufferedImage bufferedImage2 = readImage(secondImage);

            if (bufferedImage1 != null && bufferedImage2 != null) {
                validateSize(bufferedImage1, bufferedImage2);

                RedPixel[][] redPixels = getDifference(bufferedImage1, bufferedImage2);
                Map<Integer, Coordinate> rectanglesCoordinates = getRectanglesCoordinatesWithoutOverlapping(redPixels);

                int[] imagePixelsArr = createPixelArray(bufferedImage1.getWidth(), bufferedImage1.getHeight(), bufferedImage2);
                BufferedImage resultImage = createImage(imagePixelsArr, bufferedImage2.getWidth(), bufferedImage2.getHeight());

                addRectangles(resultImage, rectanglesCoordinates);

                File folder = getSaveFolder();
                writeImage(resultImage, folder.getAbsolutePath());
            }
        }
    }

    private File getImage(String titleText) {
        final JFileChooser fileChooser = new JFileChooser();
        FileFilter imageFilter = new FileNameExtensionFilter("Image files", ImageIO.getReaderFileSuffixes());
        fileChooser.setFileFilter(imageFilter);
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setDialogTitle(titleText);

        int fileState = fileChooser.showOpenDialog(null);
        return getChosenImage(fileChooser, fileState);
    }


    private File getSaveFolder() {
        JFileChooser folderChooser = new JFileChooser();
        folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        folderChooser.setDialogTitle("Chose folder for comparison image");
        folderChooser.resetChoosableFileFilters();
        int fileState = folderChooser.showSaveDialog(null);
        if (fileState == JFileChooser.APPROVE_OPTION) {
            return folderChooser.getSelectedFile();
        } else {
            System.exit(0);
        }
        return null;
    }


    private File getChosenImage(JFileChooser fileChooser, int chosenState) {
        if (chosenState == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFile();
        } else if (chosenState == JFileChooser.CANCEL_OPTION) {
            System.exit(0);
        }
        return null;
    }


    private BufferedImage readImage(File file) {
        try {
            return ImageIO.read(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Creates BufferedImage from array of pixels
     */
    private BufferedImage createImage(int[] pixels, int width, int height) {
        int[] bitMasks = new int[]{0xFF0000, 0xFF00, 0xFF, 0xFF000000};
        SinglePixelPackedSampleModel sm = new SinglePixelPackedSampleModel(
                DataBuffer.TYPE_INT, width, height, bitMasks);
        DataBufferInt db = new DataBufferInt(pixels, pixels.length);
        WritableRaster wr = Raster.createWritableRaster(sm, db, new Point());
        return new BufferedImage(ColorModel.getRGBdefault(), wr, false, null);
    }

    /**
     * Add red rectangles to an image
     */
    private void addRectangles(BufferedImage resultImage, Map<Integer, Coordinate> rectanglesCoordinates) {
        for (Map.Entry<Integer, Coordinate> entry : rectanglesCoordinates.entrySet()) {
            Coordinate coordinate = entry.getValue();
            addRectangleToImage(resultImage, new Rectangle(coordinate.bottomX, coordinate.bottomY, coordinate.topX - coordinate.bottomX,
                    coordinate.topY - coordinate.bottomY));
        }

    }

    private void addRectangleToImage(BufferedImage bufferedImage, Rectangle rectangle) {
        Graphics2D g2 = bufferedImage.createGraphics();
        g2.drawImage(bufferedImage, null, null);
        g2.setColor(Color.RED);
        g2.draw(rectangle);
    }


    private void writeImage(BufferedImage bufferedImage, String path) {

        File imageFile = new File(path + "/" + UUID.randomUUID() + ".png");
        try {
            ImageIO.write(bufferedImage, "png", imageFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




    private RedPixel[][] getDifference(BufferedImage bufferedImage1, BufferedImage bufferedImage2) {
        if (bufferedImage1 != null && bufferedImage2 != null) {
            int width = bufferedImage1.getWidth();
            int height = bufferedImage1.getHeight();
            RedPixel[][] redPixels = new RedPixel[width][height];
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    int pixel1 = bufferedImage1.getRGB(x, y);
                    int pixel2 = bufferedImage2.getRGB(x, y);
                    float comparisonResult = comparePixels(pixel1, pixel2);
                    if (comparisonResult >= 1.10) {
                        redPixels[x][y] = new RedPixel(x, y);
                    }
                }
            }
            int groupNumber = 0;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (redPixels[x][y] != null && redPixels[x][y].groupNumber == null) {
                        groupNumber++;
                    }
                    assignGroupNumber(redPixels, groupNumber, x, y);
                }
            }
            return redPixels;
        }

        return null;
    }

    /**
     * Here we can add alert messages
     */
    private void validateSize(BufferedImage bufferedImage1, BufferedImage bufferedImage2) {
        if (!isSizesMatch(bufferedImage1, bufferedImage2)) {
            System.exit(0);
        }
    }

    private Map<Integer, Coordinate> getRectanglesCoordinates(RedPixel[][] redPixels) {
        int width = redPixels.length;
        int height = redPixels[0].length;
        Map<Integer, Coordinate> coordinates = new HashMap<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                RedPixel redPixel = redPixels[x][y];
                if (redPixel != null) {
                    Coordinate coordinate = coordinates.get(redPixel.groupNumber);
                    if (coordinate != null) {
                        if (coordinate.topX < x) {
                            coordinate.topX = x;
                        }
                        if (coordinate.topY < y) {
                            coordinate.topY = y;
                        }
                        if (coordinate.bottomX > x) {
                            coordinate.bottomX = x;
                        }
                        if (coordinate.bottomY > y) {
                            coordinate.bottomY = y;
                        }
                    } else {
                        coordinate = new Coordinate(x, y, x, y);
                    }
                    coordinates.put(redPixel.groupNumber, coordinate);
                }
            }
        }
        return coordinates;
    }

    private Map<Integer, Coordinate> getRectanglesCoordinatesWithoutOverlapping(RedPixel[][] redPixels) {
        Map<Integer, Coordinate> coordinateMap = getRectanglesCoordinates(redPixels);
        Map<Integer, Coordinate> result = new HashMap<>();
        for (Map.Entry<Integer, Coordinate> entry : coordinateMap.entrySet()) {
            Coordinate coordinate = entry.getValue();
            boolean isOverlapped = false;
            for (Map.Entry<Integer, Coordinate> entry2 : coordinateMap.entrySet()) {
                if (!entry.equals(entry2)) {
                    Coordinate coordinate2 = entry2.getValue();
                    if (coordinate2.topX >= coordinate.topX &&
                            coordinate2.topY >= coordinate.topY &&
                            coordinate2.bottomX <= coordinate.bottomX &&
                            coordinate2.bottomY <= coordinate.bottomY) {
                        isOverlapped = true;
                    }
                }
            }
            if (!isOverlapped) {
                result.put(entry.getKey(), coordinate);
            }
        }
        return result;
    }

    private void assignGroupNumber(RedPixel[][] redPixels, int groupNumber, int x, int y) {
        int width = redPixels.length;
        int height = redPixels[0].length;
        if (x < width && y < height && x > 0 && y > 0 && redPixels[x][y] != null && redPixels[x][y].groupNumber == null) {
            redPixels[x][y].groupNumber = groupNumber;
            assignGroupNumber(redPixels, groupNumber, x - 1, y - 1);
            assignGroupNumber(redPixels, groupNumber, x, y - 1);
            assignGroupNumber(redPixels, groupNumber, x + 1, y - 1);
            assignGroupNumber(redPixels, groupNumber, x + 1, y);
            assignGroupNumber(redPixels, groupNumber, x + 1, y + 1);
            assignGroupNumber(redPixels, groupNumber, x, y + 1);
            assignGroupNumber(redPixels, groupNumber, x - 1, y + 1);
            assignGroupNumber(redPixels, groupNumber, x - 1, y);
        }
    }


    private int[] createPixelArray(int width, int height, BufferedImage bufferedImage2) {
        int[] imagePixelsArr = new int[width * height];
        int counter = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                imagePixelsArr[counter++] = bufferedImage2.getRGB(x, y);
            }
        }
        return imagePixelsArr;
    }

    /**
     * I assume that images size must be equal
     */
    private boolean isSizesMatch(BufferedImage bufferedImage1, BufferedImage bufferedImage2) {
        return bufferedImage1.getWidth() == bufferedImage2.getWidth() && bufferedImage1.getHeight() == bufferedImage2.getHeight();
    }

    private float comparePixels(int rgb1, int rgb2) {
        return (float) Math.max(Math.abs(rgb1), Math.abs(rgb2)) / Math.min(Math.abs(rgb1), Math.abs(rgb2));
    }

    private class RedPixel {
        private int x;
        private int y;
        private Integer groupNumber;

        private RedPixel(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    private class Coordinate {
        private int topX;
        private int topY;
        private int bottomX;
        private int bottomY;

        public Coordinate(int topX, int topY, int bottomX, int bottomY) {
            this.topX = topX;
            this.topY = topY;
            this.bottomX = bottomX;
            this.bottomY = bottomY;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Coordinate that = (Coordinate) o;

            if (topX != that.topX) return false;
            if (topY != that.topY) return false;
            if (bottomX != that.bottomX) return false;
            return bottomY == that.bottomY;

        }

        @Override
        public int hashCode() {
            int result = topX;
            result = 31 * result + topY;
            result = 31 * result + bottomX;
            result = 31 * result + bottomY;
            return result;
        }
    }

}
