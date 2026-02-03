package Lab_Week7;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import javax.imageio.ImageIO;

public class DIP_Lab7 { // 66050170

    public static void main(String[] args) {
        ImageManager im = new ImageManager();
        im.read("Lab_Week7\\images\\mandril.bmp");

        // Quest 011
        im.resizeBilinear(3.5, 3.5);
        im.write("Lab_Week7\\images\\mandril_up35.bmp");
        im.restoreToOriginal();

        im.resizeBilinear(0.35, 0.35);
        im.write("Lab_Week7\\images\\mandril_down035.bmp");
        im.restoreToOriginal();
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

    // Scaling Algorithms
    public void restoreToOriginal() {
        width = original.getWidth();
        height = original.getHeight();
        img = new BufferedImage(width, height, img.getType());

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                img.setRGB(x, y, original.getRGB(x, y));
            }
        }
    }

    // Nearest-Neighbour interpolation
    public void resizeNearestNeighbour(double scaleX, double scaleY) {
        if (img == null) {
            return;
        }

        int newWidth = (int) Math.round(width * scaleX);
        int newHeight = (int) Math.round(height * scaleY);
        BufferedImage tempBuf = new BufferedImage(newWidth, newHeight, img.getType());

        for (int y = 0; y < newHeight; y++) {
            for (int x = 0; x < newWidth; x++) {
                int xNearest = (int) Math.round(x / scaleX);
                int yNearest = (int) Math.round(y / scaleY);
                xNearest = xNearest >= width ? width - 1 : xNearest;
                xNearest = xNearest < 0 ? 0 : xNearest;
                yNearest = yNearest >= height ? height - 1 : yNearest;
                yNearest = yNearest < 0 ? 0 : yNearest;
                tempBuf.setRGB(x, y, img.getRGB(xNearest, yNearest));
            }
        }

        img = new BufferedImage(newWidth, newHeight, img.getType());
        width = newWidth;
        height = newHeight;

        for (int y = 0; y < newHeight; y++) {
            for (int x = 0; x < newWidth; x++) {
                img.setRGB(x, y, tempBuf.getRGB(x, y));
            }
        }
    }

    // Bilinear interpolation
    public void resizeBilinear(double scaleX, double scaleY) {
        if (img == null) {
            return;
        }
        int newWidth = (int) Math.round(width * scaleX);
        int newHeight = (int) Math.round(height * scaleY);
        BufferedImage tempBuf = new BufferedImage(newWidth, newHeight, img.getType());
        for (int y = 0; y < newHeight; y++) {
            for (int x = 0; x < newWidth; x++) {
                double oldX = x / scaleX;
                double oldY = y / scaleY;
                // get 4 coordinates
                int x1 = Math.min((int) Math.floor(oldX), width - 1);
                int y1 = Math.min((int) Math.floor(oldY), height - 1);
                int x2 = Math.min((int) Math.ceil(oldX), width - 1);
                int y2 = Math.min((int) Math.ceil(oldY), height - 1);
                // get colours
                int color11 = img.getRGB(x1, y1);
                int r11 = (color11 >> 16) & 0xff;
                int g11 = (color11 >> 8) & 0xff;
                int b11 = color11 & 0xff;
                int color12 = img.getRGB(x1, y2);
                int r12 = (color12 >> 16) & 0xff;
                int g12 = (color12 >> 8) & 0xff;
                int b12 = color12 & 0xff;
                int color21 = img.getRGB(x2, y1);
                int r21 = (color21 >> 16) & 0xff;
                int g21 = (color21 >> 8) & 0xff;
                int b21 = color21 & 0xff;
                int color22 = img.getRGB(x2, y2);
                int r22 = (color22 >> 16) & 0xff;
                int b22 = color22 & 0xff;
                int g22 = (color22 >> 8) & 0xff;

                // interpolate x
                double P1r = (x2 - oldX) * r11 + (oldX - x1) * r21;
                double P1g = (x2 - oldX) * g11 + (oldX - x1) * g21;
                double P1b = (x2 - oldX) * b11 + (oldX - x1) * b21;
                double P2r = (x2 - oldX) * r12 + (oldX - x1) * r22;
                double P2g = (x2 - oldX) * g12 + (oldX - x1) * g22;
                double P2b = (x2 - oldX) * b12 + (oldX - x1) * b22;

                if (x1 == x2) {
                    P1r = r11;
                    P1g = g11;
                    P1b = b11;
                    P2r = r22;
                    P2g = g22;
                    P2b = b22;
                }
                // interpolate y
                double Pr = (y2 - oldY) * P1r + (oldY - y1) * P2r;
                double Pg = (y2 - oldY) * P1g + (oldY - y1) * P2g;
                double Pb = (y2 - oldY) * P1b + (oldY - y1) * P2b;
                if (y1 == y2) {
                    Pr = P1r;
                    Pg = P1g;
                    Pb = P1b;
                }
                int r = (int) Math.round(Pr);
                int g = (int) Math.round(Pg);
                int b = (int) Math.round(Pb);
                r = r > 255 ? 255 : r;
                r = r < 0 ? 0 : r;
                g = g > 255 ? 255 : g;
                g = g < 0 ? 0 : g;
                b = b > 255 ? 255 : b;
                b = b < 0 ? 0 : b;
                int newColor = (r << 16) | (g << 8) | b;
                tempBuf.setRGB(x, y, newColor);
            }
        }
        img = new BufferedImage(newWidth, newHeight, img.getType());
        width = newWidth;
        height = newHeight;
        for (int y = 0; y < newHeight; y++) {
            for (int x = 0; x < newWidth; x++) {
                img.setRGB(x, y, tempBuf.getRGB(x, y));
            }
        }
    }
}
