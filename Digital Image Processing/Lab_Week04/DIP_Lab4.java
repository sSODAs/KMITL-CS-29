package Lab_Week4;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import javax.imageio.ImageIO;

public class DIP_Lab4 { //66050170
    public static void main(String[] args) {
        ImageManager im = new ImageManager();
        im.read("Lab_Week4\\images\\mandril.bmp");

        // Quest 006
        im.medianFilter(3);
        im.write("Lab_Week4\\\\images\\\\mandril_medianFilter3x3.bmp");
        im.restoreToOriginal();

        im.medianFilter(7);
        im.write("Lab_Week4\\\\images\\\\mandril_medianFilter7x7.bmp");
        im.restoreToOriginal();

        im.medianFilter(15);
        im.write("Lab_Week4\\\\images\\\\mandril_medianFilter15x15.bmp");
        im.restoreToOriginal();

        // Quest 007
        im.unsharpMasking(3, 1);
        im.write("Lab_Week4\\\\images\\\\mandril_unsharpMasking.bmp");
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

    public void restoreToOriginal() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                img.setRGB(x, y, original.getRGB(x, y));
            }
        }
    }

    // Averaging Filter (หรือ Mean Filter) เป็น filter เชิงเส้นแบบง่ายที่สุด
    // ใช้สำหรับ ทำให้ภาพเรียบขึ้น (blur) และ ลด noise ในภาพ
    public void averagingFilter(int size) {
        if (img == null) {
            return;
        }

        if (size % 2 == 0) {
            System.out.println("Size Invalid: must be odd number!");
            return;
        }

        BufferedImage tempBuf = new BufferedImage(width, height, img.getType());
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int sumRed = 0, sumGreen = 0, sumBlue = 0;
                for (int i = y - size / 2; i <= y + size / 2; i++) {
                    for (int j = x - size / 2; j <= x + size / 2; j++) {
                        if (i >= 0 && i < height && j >= 0 && j < width) {
                            int color = img.getRGB(j, i);
                            int r = (color >> 16) & 0xff;
                            int g = (color >> 8) & 0xff;
                            int b = color & 0xff;
                            sumRed += r;
                            sumGreen += g;
                            sumBlue += b;
                        }
                    }
                }

                sumRed /= (size * size);
                sumRed = sumRed > 255 ? 255 : sumRed;
                sumRed = sumRed < 0 ? 0 : sumRed;
                sumGreen /= (size * size);
                sumGreen = sumGreen > 255 ? 255 : sumGreen;
                sumGreen = sumGreen < 0 ? 0 : sumGreen;
                sumBlue /= (size * size);
                sumBlue = sumBlue > 255 ? 255 : sumBlue;
                sumBlue = sumBlue < 0 ? 0 : sumBlue;
                int newColor = (sumRed << 16) | (sumGreen << 8) | sumBlue;
                tempBuf.setRGB(x, y, newColor);
            }
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                img.setRGB(x, y, tempBuf.getRGB(x, y));
            }
        }
    }

    // Median Filter (Nonlinear Filtering)
    public void medianFilter(int size) {
        if (img == null) {
            return;
        }

        if (size % 2 == 0) {
            System.out.println("Size Invalid: must be odd number!");
            return;
        }

        BufferedImage tempBuf = new BufferedImage(width, height, img.getType());
        int windowSize = size * size;
        int[] rValues = new int[windowSize];
        int[] gValues = new int[windowSize];
        int[] bValues = new int[windowSize];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int count = 0;

                for (int i = y - size / 2; i <= y + size / 2; i++) {
                    for (int j = x - size / 2; j <= x + size / 2; j++) {
                        if (i >= 0 && i < height && j >= 0 && j < width) {
                            int color = img.getRGB(j, i);
                            rValues[count] = (color >> 16) & 0xff;
                            gValues[count] = (color >> 8) & 0xff;
                            bValues[count] = color & 0xff;
                            count++;
                        }
                    }
                }

                java.util.Arrays.sort(rValues, 0, count);
                java.util.Arrays.sort(gValues, 0, count);
                java.util.Arrays.sort(bValues, 0, count);

                int medianRed = rValues[count / 2];
                int medianGreen = gValues[count / 2];
                int medianBlue = bValues[count / 2];

                int newColor = (medianRed << 16) | (medianGreen << 8) | medianBlue;
                tempBuf.setRGB(x, y, newColor);
            }
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                img.setRGB(x, y, tempBuf.getRGB(x, y));
            }
        }
    }

    public void unsharpMasking(int size, int k) {
        if (img == null)
            return;
        if (size % 2 == 0) {
            System.out.println("Size Invalid: must be odd number!");
            return;
        }

        BufferedImage original = new BufferedImage(width, height, img.getType());
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                original.setRGB(x, y, img.getRGB(x, y));
            }
        }

        averagingFilter(size);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rf, gf, bf;
                int color_f = original.getRGB(x, y);
                rf = (color_f >> 16) & 0xff;
                gf = (color_f >> 8) & 0xff;
                bf = color_f & 0xff;

                int rb, gb, bb;
                int color_b = img.getRGB(x, y);
                rb = (color_b >> 16) & 0xff;
                gb = (color_b >> 8) & 0xff;
                bb = color_b & 0xff;

                int r_final = rf + k * (rf - rb);
                int g_final = gf + k * (gf - gb);
                int b_final = bf + k * (bf - bb);

                r_final = r_final > 255 ? 255 : (r_final < 0 ? 0 : r_final);
                g_final = g_final > 255 ? 255 : (g_final < 0 ? 0 : g_final);
                b_final = b_final > 255 ? 255 : (b_final < 0 ? 0 : b_final);

                int newColor = (r_final << 16) | (g_final << 8) | b_final;

                img.setRGB(x, y, newColor);
            }
        }
    }
}
