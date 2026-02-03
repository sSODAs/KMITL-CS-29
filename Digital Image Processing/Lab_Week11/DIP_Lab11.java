package Lab_Week11;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class DIP_Lab11 { // 66050170
    public static void main(String[] args) {
        ImageManager im = new ImageManager();
        im.read("Lab_Week11/images/4/motion01.512.bmp");

        // Quest 015
        String[] sequences = new String[10];
        for (int i = 0; i < 10; i++) {
            String num = String.format("%02d", i + 1);
            sequences[i] = "Lab_Week11/images/4/motion" + num + ".512.bmp";
        }

        im.ADINegative(sequences, 25, 50);
        im.write("Lab_Week11/images/4/motion_negative.bmp");

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

    // Accumulative Difference Image
    public void ADIAbsolute(String[] sequences, int threshold, int step) {
        if (img == null) {
            return;
        }

        BufferedImage tempBuf = new BufferedImage(width, height, img.getType());

        for (int n = 0; n < sequences.length; n++) {
            BufferedImage otherImage = null;
            try {
                otherImage = ImageIO.read(new File(sequences[n]));
            }

            catch (IOException e) {
                System.out.println(e);
                return;
            }
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int color1 = img.getRGB(x, y);
                    int r1 = (color1 >> 16) & 0xff;
                    int g1 = (color1 >> 8) & 0xff;
                    int b1 = color1 & 0xff;

                    int color2 = otherImage.getRGB(x, y);
                    int r2 = (color2 >> 16) & 0xff;
                    int g2 = (color2 >> 8) & 0xff;
                    int b2 = color2 & 0xff;

                    int dr = r1 - r2;
                    int dg = g1 - g2;
                    int db = b1 - b2;

                    int dGray = (int) Math.round(0.2126 * dr + 0.7152 * dg + 0.0722 * db);

                    if (Math.abs(dGray) > threshold) {
                        int currentColor = tempBuf.getRGB(x, y) & 0xff;
                        currentColor += step;

                        currentColor = currentColor > 255 ? 255 : currentColor;
                        currentColor = currentColor < 0 ? 0 : currentColor;

                        int newColor = (currentColor << 16) | (currentColor << 8) | currentColor;
                        tempBuf.setRGB(x, y, newColor);
                    }
                }
            }
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                img.setRGB(x, y, tempBuf.getRGB(x, y));
            }
        }
    }

    // negative ADI
    public void ADINegative(String[] sequences, int threshold, int step) {
        if (img == null) {
            return;
        }
        BufferedImage tempBuf = new BufferedImage(width, height, img.getType());

        for (int n = 0; n < sequences.length; n++) {
            BufferedImage otherImage = null;
            try {
                otherImage = ImageIO.read(new File(sequences[n]));
            } catch (IOException e) {
                System.out.println(e);
                return;
            }
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int color1 = img.getRGB(x, y);
                    int r1 = (color1 >> 16) & 0xff;
                    int g1 = (color1 >> 8) & 0xff;
                    int b1 = color1 & 0xff;

                    int color2 = otherImage.getRGB(x, y);
                    int r2 = (color2 >> 16) & 0xff;
                    int g2 = (color2 >> 8) & 0xff;
                    int b2 = color2 & 0xff;

                    int dr = r1 - r2;
                    int dg = g1 - g2;
                    int db = b1 - b2;

                    int dGray = (int) Math.round(0.2126 * dr + 0.7152 * dg + 0.0722 * db);

                    if (dGray < -threshold) {
                        int currentColor = tempBuf.getRGB(x, y) & 0xff;
                        currentColor += step;
                        currentColor = Math.min(255, Math.max(0, currentColor));
                        int newColor = (currentColor << 16) | (currentColor << 8) | currentColor;
                        tempBuf.setRGB(x, y, newColor);
                    }
                }
            }
        }

        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                img.setRGB(x, y, tempBuf.getRGB(x, y));
    }
}