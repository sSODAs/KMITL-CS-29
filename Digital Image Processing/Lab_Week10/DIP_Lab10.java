package Lab_Week10;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class DIP_Lab10 { // 66050170
    public static void main(String[] args) {
        ImageManager im = new ImageManager();
        im.read("Lab_Week10/images/mandril.bmp");

        // Quest 014
        im.houghTransform(0.5);
        im.write("Lab_Week10/images/mandril_hough.bmp");
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

    public void houghTransform(double percent) {
        // The image should be converted to edge map first

        // Work out how the hough space is quantized
        int numOfTheta = 720;
        double thetaStep = Math.PI / numOfTheta;
        int highestR = (int) (Math.max(width, height) * Math.sqrt(2));
        int centreX = width / 2;
        int centreY = height / 2;
        System.out.println("Hough array w: " + numOfTheta + " height: " + 2 * highestR);

        // Create the hough array and initialize to zero
        int[][] houghArray = new int[numOfTheta][2 * highestR];
        for (int i = 0; i < numOfTheta; i++) {
            for (int j = 0; j < 2 * highestR; j++) {
                houghArray[i][j] = 0;
            }
        }

        // Step 1 - find each edge pixel
        // Find edge points and vote in array
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pointColor = img.getRGB(x, y) & 0xff;
                if (pointColor != 0) {
                    // Edge pixel found
                    for (int i = 0; i < numOfTheta; i++) {
                        // Step 2 - Apply the line equation and update hough array
                        // Work out the r values for each theta step
                        int r = (int) ((x - centreX) * Math.cos(i * thetaStep) +
                                (y - centreY) * Math.sin(i * thetaStep));
                        // Move all values into positive range for display purposes
                        r = r + highestR;
                        if (r < 0 || r >= 2 * highestR)
                            continue;

                        // Increment hough array
                        houghArray[i][r]++;
                    }
                }
            }
        }
        // Step 3 - Apply threshold to hough array to find line
        int maxHough = 0;

        for (int i = 0; i < numOfTheta; i++) {
            for (int j = 0; j < 2 * highestR; j++) {
                // Find the max hough value for the thresholding operation
                if (houghArray[i][j] > maxHough) {
                    maxHough = houghArray[i][j];
                }
            }
        }
        // Set the threshold limit
        int threshold = (int) (percent * maxHough);

        // Step 4 - Draw lines
        // Search for local peaks above threshold to draw

        for (int i = 0; i < numOfTheta; i++) {
            for (int j = 0; j < 2 * highestR; j++) {
                // only consider points above threshold
                if (houghArray[i][j] >= threshold) {
                    // see if local maxima
                    boolean draw = true;
                    int peak = houghArray[i][j];
                    for (int k = -1; k <= 1; k++) {
                        for (int l = -1; l <= 1; l++) {
                            // not seeing itself
                            if (k == 0 && l == 0)
                                continue;
                            int testTheta = i + k;
                            int testOffset = j + l;

                            if (testOffset < 0 || testOffset >= 2 * highestR)
                                continue;
                            if (testTheta < 0)
                                testTheta = testTheta + numOfTheta;
                            if (testTheta >= numOfTheta)
                                testTheta = testTheta - numOfTheta;

                            if (houghArray[testTheta][testOffset] > peak) {
                                // found bigger point
                                draw = false;
                                break;
                            }
                        }
                    }

                    // point found is not local maxima
                    if (!draw) {
                        continue;
                    }

                    // if local maxima, draw red back
                    double tsin = Math.sin(i * thetaStep);
                    double tcos = Math.cos(i * thetaStep);
                    if (i <= numOfTheta / 4 || i >= (3 * numOfTheta) / 4) {
                        for (int y = 0; y < height; y++) {
                            // vertical line
                            int x = (int) (((j - highestR) - ((y - centreY) * tsin)) / tcos) + centreX;
                            if (x < width && x >= 0) {
                                int redColor = (255 << 16) | (0 << 8) | 0;
                                img.setRGB(x, y, redColor);
                            }
                        }
                    } else {
                        for (int x = 0; x < width; x++) {
                            // horizontal line
                            int y = (int) (((j - highestR) - ((x - centreX) * tcos)) / tsin) + centreY;
                            if (y < height && y >= 0) {
                                int redColor = (255 << 16) | (0 << 8) | 0;
                                img.setRGB(x, y, redColor);
                            }
                        }
                    }
                }
            }
        }
    }
}