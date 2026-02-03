package Lab_Week12;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

public class DIP_Lab12 { // 66050170
    public static void main(String[] args) {
        ImageManager im = new ImageManager();
        im.read("Lab_Week12/images/mandril.bmp");

        // Quest 016
        im.detectHarrisFeatures(1000);
        im.write("Lab_Week12/images/mandril_harris.bmp");
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

    public ArrayList<Point> detectHarrisFeatures(int strongest) {
        // convert to gray scale first
        convertToGrayscale();

        double[][] Ix = new double[height][width];
        double[][] Iy = new double[height][width];

        // Initialize matrices to store products of gradients
        double[][] Ix2 = new double[height][width];
        double[][] Iy2 = new double[height][width];
        double[][] Ixy = new double[height][width];

        // Compute gradients Ix and Iy, drop the border
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int color = img.getRGB(x, y);
                int gray = color & 0xff;
                Ix[y][x] = ((img.getRGB(x + 1, y) & 0xff) - (img.getRGB(x - 1, y) & 0xff)) /
                        2.0;
                Iy[y][x] = ((img.getRGB(x, y + 1) & 0xff) - (img.getRGB(x, y - 1) & 0xff)) /
                        2.0;
                Ix2[y][x] = Ix[y][x] * Ix[y][x];
                Iy2[y][x] = Iy[y][x] * Iy[y][x];
                Ixy[y][x] = Ix[y][x] * Iy[y][x];
            }
        }
        // apply 3 x 3 gaussian smoothing for each matrices
        double[][] Sx2 = new double[height][width];
        double[][] Sy2 = new double[height][width];
        double[][] Sxy = new double[height][width];

        double[] gaussian = {
                1.0 / 16.0, 2.0 / 16.0, 1.0 / 16.0,
                2.0 / 16.0, 4.0 / 16.0, 1.0 / 16.0,
                1.0 / 16.0, 2.0 / 16.0, 1.0 / 16.0
        };

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {

                Sx2[y][x] = 0;
                Sy2[y][x] = 0;
                Sxy[y][x] = 0;

                for (int i = y - 1; i <= y + 1; i++) {
                    for (int j = x - 1; j <= x + 1; j++) {
                        Sx2[y][x] += Ix2[i][j] * gaussian[(i - (y - 1)) * 3 + (j - (x - 1))];
                        Sy2[y][x] += Iy2[i][j] * gaussian[(i - (y - 1)) * 3 + (j - (x - 1))];
                        Sxy[y][x] += Ixy[i][j] * gaussian[(i - (y - 1)) * 3 + (j - (x - 1))];
                    }
                }
            }
        }

        double[][] corners = new double[height][width];

        // Compute the corner response function R
        // High R = Corner, Low R = Flat, Negative R = Edge
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double det = Sx2[y][x] * Sy2[y][x] - Sxy[y][x] * Sxy[y][x];
                double trace = Sx2[y][x] + Sy2[y][x];
                corners[y][x] = det - 0.04 * trace * trace;
            }
        }

        ArrayList<Point> cornerPoints = new ArrayList<>();
        ArrayList<Double> cornerValues = new ArrayList<>();

        // Maxima Suspression (see if it is the maximum value to the neighbours)
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                // if negative, not a corner
                if (corners[y][x] < 0)
                    continue;
                // see if local maxima
                double peak = corners[y][x];
                boolean isMaxima = true;

                // Check 3x3 neighborhood
                for (int k = -1; k <= 1 && isMaxima; k++) {
                    for (int l = -1; l <= 1; l++) {
                        if (k == 0 && l == 0)
                            continue; // Skip the center pixel

                        int testX = x + k;
                        int testY = y + l;

                        if (corners[testY][testX] > peak) {
                            isMaxima = false;
                            break; // Early exit if a larger neighbor is found
                        }
                    }
                }
                if (isMaxima) {
                    // Point is a local maxima, find the correct position to insert
                    int insertPos = 0;
                    while (insertPos < cornerValues.size() && cornerValues.get(insertPos) > peak) {
                        insertPos++;
                    }

                    // Insert corner in the correct position
                    cornerPoints.add(insertPos, new Point(x, y));
                    cornerValues.add(insertPos, peak);

                    // If we have more points than needed, remove the weakest ones
                    if (cornerPoints.size() > strongest) {
                        cornerPoints.remove(strongest);
                        cornerValues.remove(strongest);
                    }
                }
            }
        }
        restoreToOriginal();
        convertToGrayscale();

        // Draw red X
        for (Point p : cornerPoints) {
            int redColor = (255 << 16) | (0 << 8) | 0;
            img.setRGB(p.x, p.y, redColor);
            img.setRGB(p.x + 1, p.y + 1, redColor);
            img.setRGB(p.x + 1, p.y - 1, redColor);
            img.setRGB(p.x - 1, p.y + 1, redColor);
            img.setRGB(p.x - 1, p.y - 1, redColor);
        }

        return cornerPoints;

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
}