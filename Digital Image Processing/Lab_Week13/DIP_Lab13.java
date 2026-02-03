package Lab_Week13;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class DIP_Lab13 { // 66050170
    public static void main(String[] args) {
        ImageManager im = new ImageManager();
        im.read("Lab_Week13/images/qrcode.bmp");

        double[][] srcPoints = {
                { 256, 133 }, // top-left
                { 419, 146 }, // top-right
                { 403, 348 }, // bottom-right
                { 244, 320 } // bottom-left
        };

        double[][] dstPoints = {
                { 0, 0 }, // top-left
                { 512, 0 }, // top-right
                { 512, 512 }, // bottom-right
                { 0, 512 } // bottom-left
        };

        // Quest 017: คำนวณ Homography
        double[] H = im.calculateHomography(srcPoints, dstPoints);
        System.out.println("Homography Matrix (3x3):");
        for (int i = 0; i < 9; i++) {
            System.out.print(H[i] + ((i % 3 == 2) ? "\n" : "\t"));
        }

        // Quest 018
        im.applyHomography(H);
        im.write("Lab_Week13/images/qrcode_warped.bmp");

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

    // ───────────────────────────────
    // Quest 017: คำนวณ Homography
    public double[] calculateHomography(double[][] srcPoints, double[][] dstPoints) {
        double[][] A = new double[8][8];
        double[] b = new double[8];
        for (int i = 0; i < 4; i++) {
            double xSrc = srcPoints[i][0];
            double ySrc = srcPoints[i][1];
            double xDst = dstPoints[i][0];
            double yDst = dstPoints[i][1];

            A[2 * i][0] = xSrc;
            A[2 * i][1] = ySrc;
            A[2 * i][2] = 1;
            A[2 * i][3] = 0;
            A[2 * i][4] = 0;
            A[2 * i][5] = 0;
            A[2 * i][6] = -xSrc * xDst;
            A[2 * i][7] = -ySrc * xDst;

            A[2 * i + 1][0] = 0;
            A[2 * i + 1][1] = 0;
            A[2 * i + 1][2] = 0;
            A[2 * i + 1][3] = xSrc;
            A[2 * i + 1][4] = ySrc;
            A[2 * i + 1][5] = 1;
            A[2 * i + 1][6] = -xSrc * yDst;
            A[2 * i + 1][7] = -ySrc * yDst;

            b[2 * i] = xDst;
            b[2 * i + 1] = yDst;
        }
        // Solve using Gaussian elimination
        // This function will solve the system A * x = b
        // You can use Gaussian elimination, LU decomposition, or any other method
        return gaussianElimination(A, b);
    }

    public double[] gaussianElimination(double[][] A, double[] b) {
        int n = b.length;
        for (int i = 0; i < n; i++) {
            // Pivoting
            int max = i;
            for (int j = i + 1; j < n; j++) {
                if (Math.abs(A[j][i]) > Math.abs(A[max][i])) {
                    max = j;
                }
            }

            // Swap rows in A
            double[] temp = A[i];
            A[i] = A[max];
            A[max] = temp;

            // Swap entries in b
            double t = b[i];
            b[i] = b[max];
            b[max] = t;

            // Normalize the row
            for (int k = i + 1; k < n; k++) {
                double factor = A[k][i] / A[i][i];
                b[k] -= factor * b[i];
                for (int j = i; j < n; j++) {
                    A[k][j] -= factor * A[i][j];
                }
            }
        }

        // Back substitution
        double[] x = new double[n];
        for (int i = n - 1; i >= 0; i--) {
            double sum = 0.0;
            for (int j = i + 1; j < n; j++) {
                sum += A[i][j] * x[j];
            }
            x[i] = (b[i] - sum) / A[i][i];
        }

        // The last element of the homography matrix (h33) is 1
        double[] homography = new double[9];
        System.arraycopy(x, 0, homography, 0, 8);
        homography[8] = 1;

        return homography;
    }

    // Image warpping
    public static double[] invertHomography(double[] H) {
        double[] invH = new double[9];

        double det = H[0] * (H[4] * H[8] - H[5] * H[7])
                - H[1] * (H[3] * H[8] - H[5] * H[6])
                + H[2] * (H[3] * H[7] - H[4] * H[6]);

        if (det == 0)
            throw new IllegalArgumentException("Matrix is not invertible");

        double invDet = 1.0 / det;
        invH[0] = invDet * (H[4] * H[8] - H[5] * H[7]);
        invH[1] = invDet * (H[2] * H[7] - H[1] * H[8]);
        invH[2] = invDet * (H[1] * H[5] - H[2] * H[4]);

        invH[3] = invDet * (H[5] * H[6] - H[3] * H[8]);
        invH[4] = invDet * (H[0] * H[8] - H[2] * H[6]);
        invH[5] = invDet * (H[2] * H[3] - H[0] * H[5]);

        invH[6] = invDet * (H[3] * H[7] - H[4] * H[6]);
        invH[7] = invDet * (H[1] * H[6] - H[0] * H[7]);
        invH[8] = invDet * (H[0] * H[4] - H[1] * H[3]);

        return invH;
    }

    public double[] applyHomographyToPoint(double[] H, double x, double y) {
        // Homogeneous coordinates calculation after transformation
        double xh = H[0] * x + H[1] * y + H[2];
        double yh = H[3] * x + H[4] * y + H[5];
        double w = H[6] * x + H[7] * y + H[8];

        // Normalize by w to get the Cartesian coordinates in the destination image
        double xPrime = xh / w;
        double yPrime = yh / w;

        return new double[] { xPrime, yPrime };
    }

    // ───────────────────────────────
    // Quest 018: Apply Homography
    public void applyHomography(double[] H) {
        BufferedImage output = new BufferedImage(width, height, img.getType());

        double[] invH = invertHomography(H);

        // Iterate over every pixel in the destination image
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Apply the inverse of the homography to find the corresponding source pixel
                double[] sourcePoint = applyHomographyToPoint(invH, x, y);

                int srcX = (int) Math.round(sourcePoint[0]);
                int srcY = (int) Math.round(sourcePoint[1]);

                // Check if the calculated source coordinates are within the source image bounds
                if (srcX >= 0 && srcX < width && srcY >= 0 && srcY < height) {
                    // Copy the pixel from the source image to the destination image
                    Color color = new Color(img.getRGB(srcX, srcY));
                    output.setRGB(x, y, color.getRGB());
                } else {
                    // If out of bounds, set the destination pixel to a default color
                    output.setRGB(x, y, Color.BLACK.getRGB());
                }
            }
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                img.setRGB(x, y, output.getRGB(x, y));
            }
        }
    }
}