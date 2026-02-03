package Lab_Week9;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class DIP_Lab9 { // 66050170
    public static void main(String[] args) {

        ImageManager im = new ImageManager();
        im.read("Lab_Week9/images/Mandril.bmp");
        
        // Quest 013
        im.cannyEdgeDetector(100, 180);
        im.write("Lab_Week9/images/Mandril_canny.bmp");
    }

}

class ImageManager {
    public int width, height, bitDepth;
    private BufferedImage img, original;

    public boolean read(String fileName) {
        try {
            img = ImageIO.read(new File(fileName));

            width = img.getWidth();
            height = img.getHeight();
            bitDepth = img.getColorModel().getPixelSize();
            original = new BufferedImage(width, height, img.getType());
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    original.setRGB(x, y, img.getRGB(x, y));
                }
            }
            System.out.println("Image " + fileName + " with " + width + " x " + height + " pixels (" + bitDepth
                    + " bitsper pixel) has been read!");
            return true;
        } catch (IOException e) {
            System.out.println(e);
            return false;
        }
    }

    public boolean write(String fileName) {
        try {
            ImageIO.write(img, "bmp", new File(fileName));
            System.out.println("Image : " + fileName + " has been written!");
            return true;
        } catch (IOException | NullPointerException e) {
            System.out.println(e);
            return false;
        }
    }

    public void restoreToOriginal() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                img.setRGB(x, y, original.getRGB(x, y));
            }
        }
    }

    // Thresholding
    public void thresholding(int threshold) {
        if (img == null)
            return;

        convertToGrayscale();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int color = img.getRGB(x, y);
                int gray = color & 0xff;

                gray = gray < threshold ? 0 : 255;

                color = (gray << 16) | (gray << 8) | gray;

                img.setRGB(x, y, color);
            }
        }
    }

    public void otsuThreshold() {
        if (img == null)
            return;

        convertToGrayscale();

        int[] histogram = new int[256];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int color = img.getRGB(x, y);
                int gray = color & 0xff;

                histogram[gray]++;
            }
        }

        float[] histogramNorm = new float[histogram.length];
        float pixelNum = width * height;

        for (int i = 0; i < histogramNorm.length; i++) {
            histogramNorm[i] = histogram[i] / pixelNum;
        }

        float[] histogramCS = new float[256];
        float[] histogramMean = new float[256];

        for (int i = 0; i < 256; i++) {
            if (i == 0) {
                histogramCS[i] = histogramNorm[i];
                histogramMean[i] = 0;
            } else {
                histogramCS[i] = histogramCS[i - 1] + histogramNorm[i];
                histogramMean[i] = histogramMean[i - 1] + histogramNorm[i] * i;
            }
        }

        float globalMean = histogramMean[255];
        float max = Float.MIN_VALUE;
        float maxVariance = Float.MIN_VALUE;
        int countMax = 0;

        for (int i = 0; i < 256; i++) {
            float variance = (float) Math.pow(globalMean * histogramCS[i] - histogramMean[i], 2)
                    / (histogramCS[i] * (1 - histogramCS[i]));

            if (variance > maxVariance) {
                maxVariance = variance;
                max = i;
                countMax = 1;
            } else if (variance == maxVariance) {
                countMax++;
                max = ((max * (countMax - 1)) + i) / countMax;
            }
        }

        thresholding((int) Math.round(max));
    }

    public void convertToGrayscale() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int color = img.getRGB(x, y);
                int r = (color >> 16) & 0xff;
                int g = (color >> 8) & 0xff;
                int b = color & 0xff;
                int gray = (r + g + b) / 3;
                color = (gray << 16) | (gray << 8) | gray;
                img.setRGB(x, y, color);
            }
        }
    }

    // Linear Spatial Filter
    public void linearSpatialFilter(double[] kernel, int size) {
        if (img == null)
            return;

        if (size % 2 == 0) {
            System.out.println("Size Invalid: must be odd number!");
            return;
        }

        BufferedImage tempBuf = new BufferedImage(width, height, img.getType());

        for (int y = 0; y < height; y++) {

            for (int x = 0; x < width; x++) {
                double sumRed = 0, sumGreen = 0, sumBlue = 0;

                for (int i = y - size / 2; i <= y + size / 2; i++) {
                    for (int j = x - size / 2; j <= x + size / 2; j++) {
                        if (i >= 0 && i < height && j >= 0 && j < width) {
                            int color = img.getRGB(j, i);
                            int r = (color >> 16) & 0xff;
                            int g = (color >> 8) & 0xff;
                            int b = color & 0xff;

                            sumRed += r * kernel[(i - (y - size / 2)) * size + (j - (x - size / 2))];
                            sumGreen += g * kernel[(i - (y - size / 2)) * size + (j - (x - size / 2))];
                            sumBlue += b * kernel[(i - (y - size / 2)) * size + (j - (x - size / 2))];
                        }
                    }
                }

                sumRed = sumRed > 255 ? 255 : sumRed;
                sumRed = sumRed < 0 ? 0 : sumRed;

                sumGreen = sumGreen > 255 ? 255 : sumGreen;
                sumGreen = sumGreen < 0 ? 0 : sumGreen;

                sumBlue = sumBlue > 255 ? 255 : sumBlue;
                sumBlue = sumBlue < 0 ? 0 : sumBlue;

                int newColor = ((int) sumRed << 16) | ((int) sumGreen << 8) | (int) sumBlue;

                tempBuf.setRGB(x, y, newColor);
            }
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                img.setRGB(x, y, tempBuf.getRGB(x, y));
            }
        }
    }

    // Canny Edge Detector
    public void cannyEdgeDetector(int lower, int upper) {

        // Step 1 - Apply 5 x 5 Gaussian filter
        double[] gaussian = { 2.0 / 159.0, 4.0 / 159.0, 5.0 / 159.0, 4.0 / 159.0, 2.0 / 159.0,
                4.0 / 159.0, 9.0 / 159.0, 12.0 / 159.0, 9.0 / 159.0, 4.0 / 159.0,
                5.0 / 159.0, 12.0 / 159.0, 15.0 / 159.0, 12.0 / 159.0, 5.0 / 159.0,
                4.0 / 159.0, 9.0 / 159.0, 12.0 / 159.0, 9.0 / 159.0, 4.0 / 159.0,
                2.0 / 159.0, 4.0 / 159.0, 5.0 / 159.0, 4.0 / 159.0, 2.0 / 159.0 };
        linearSpatialFilter(gaussian, 5);

        convertToGrayscale();

        // Step 2 - Find intensity gradient
        double[] sobelX = { 1, 0, -1, 2, 0, -2, 1, 0, -1 };
        double[] sobelY = { 1, 2, 1, 0, 0, 0, -1, -2, -1 };

        double[][] magnitude = new double[height][width];
        double[][] direction = new double[height][width];

        for (int y = 3; y < height - 3; y++) {
            for (int x = 3; x < width - 3; x++) {
                double gx = 0, gy = 0;
                for (int i = y - 1; i <= y + 1; i++) {
                    for (int j = x - 1; j <= x + 1; j++) {
                        if (i >= 0 && i < height && j >= 0 && j < width) {
                            int color = img.getRGB(j, i);
                            int gray = color & 0xff;

                            gx += gray * sobelX[(i - (y - 1)) * 3 + (j - (x - 1))];
                            gy += gray * sobelY[(i - (y - 1)) * 3 + (j - (x - 1))];
                        }
                    }
                }
                magnitude[y][x] = Math.sqrt(gx * gx + gy * gy);
                direction[y][x] = Math.atan2(gy, gx) * 180 / Math.PI;
            }
        }

        // Step 3 - Nonmaxima Suppression
        double[][] gn = new double[height][width];
        for (int y = 3; y < height - 3; y++) {
            for (int x = 3; x < width - 3; x++) {
                int targetX = 0, targetY = 0;
                // find closest direction
                if (direction[y][x] <= -157.5) {
                    targetX = 1;
                    targetY = 0;
                } else if (direction[y][x] <= -112.5) {
                    targetX = 1;
                    targetY = -1;
                } else if (direction[y][x] <= -67.5) {
                    targetX = 0;
                    targetY = 1;
                } else if (direction[y][x] <= -22.5) {
                    targetX = 1;
                    targetY = 1;
                } else if (direction[y][x] <= 22.5) {
                    targetX = 1;
                    targetY = 0;
                } else if (direction[y][x] <= 67.5) {
                    targetX = 1;
                    targetY = -1;
                } else if (direction[y][x] <= 112.5) {
                    targetX = 0;
                    targetY = 1;
                } else if (direction[y][x] <= 157.5) {
                    targetX = 1;
                    targetY = 1;
                } else {
                    targetX = 1;
                    targetY = 0;
                }

                if (y + targetY >= 0 && y + targetY < height &&
                        x + targetX >= 0 && x + targetX < width &&
                        magnitude[y][x] < magnitude[y + targetY][x + targetX]) {
                    gn[y][x] = 0;
                } else if (y - targetY >= 0 && y - targetY < height &&
                        x - targetX >= 0 && x - targetX < width &&
                        magnitude[y][x] < magnitude[y - targetY][x - targetX]) {
                    gn[y][x] = 0;
                } else {
                    gn[y][x] = magnitude[y][x];
                }
            }
        }

        // Step 4 - Hysteresis Thresholding

        // set back first
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int newGray = (int) gn[y][x];
                newGray = newGray > 255 ? 255 : newGray;
                newGray = newGray < 0 ? 0 : newGray;

                int newColor = (newGray << 16) | (newGray << 8) | newGray;
                img.setRGB(x, y, newColor);
            }
        }

        // upper threshold checking with recursive
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int checking = img.getRGB(x, y) & 0xff;
                if (checking >= upper) {
                    checking = 255;
                    int newColor = (checking << 16) | (checking << 8) | checking;
                    img.setRGB(x, y, newColor);
                    hystConnect(x, y, lower);
                }
            }
        }

        // clear unwanted values
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int checking = img.getRGB(x, y) & 0xff;

                if (checking != 255) {
                    int newColor = (0 << 16) | (0 << 8) | 0;
                    img.setRGB(x, y, newColor);
                }
            }
        }
    }

    private void hystConnect(int x, int y, int threshold) {
        int value = 0;
        for (int i = y - 1; i <= y + 1; i++) {
            for (int j = x - 1; j <= x + 1; j++) {
                if ((j < width) && (i < height) &&
                        (j >= 0) && (i >= 0) &&
                        (j != x) && (i != y)) {
                    value = img.getRGB(j, i) & 0xff;
                    if (value != 255) {
                        if (value >= threshold) {
                            int newColor = (255 << 16) | (255 << 8) | 255;
                            img.setRGB(j, i, newColor);
                            hystConnect(j, i, threshold);
                        } else {
                            int newColor = (0 << 16) | (0 << 8) | 0;
                            img.setRGB(j, i, newColor);
                        }
                    }
                }
            }
        }
    }
}
