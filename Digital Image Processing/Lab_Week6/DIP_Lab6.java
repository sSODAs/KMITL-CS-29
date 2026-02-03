package Lab_Week6;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import javax.imageio.ImageIO;

public class DIP_Lab6 { // 66050170
    public static void main(String[] args) {
        ImageManager im = new ImageManager();
        im.read("Lab_Week6\\images\\mandril.bmp");

        // Quest 009
        im.addSaltAndPepperNoise(0.10); // 10% salt + 10% pepper
        im.write("Lab_Week6\\images\\mandril_salt_pepper.bmp");
        im.restoreToOriginal();

        // Quest 010
        im.addPepperNoise(0.10);
        im.write("Lab_Week6\\images\\mandril_pepper.bmp");

        im.read("Lab_Week6\\images\\mandril_pepper.bmp");
        im.contraharmonicFilter(3, -1.5);
        im.write("Lab_Week6\\images\\mandril_filter_Q-15.bmp");
        
        
        im.read("Lab_Week6\\images\\mandril_pepper.bmp");
        im.contraharmonicFilter(3, 1.5);
        im.write("Lab_Week6\\images\\mandril_filter_Q15.bmp");
        

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

    // Noises (Salt noises)
    public void addSaltNoise(double percent) {
        if (img == null) {
            return;
        }
        double noOfPX = height * width;
        int noiseAdded = (int) (percent * noOfPX);
        Random rnd = new Random();
        int whiteColor = 255 << 16 | 255 << 8 | 255;
        for (int i = 1; i <= noiseAdded; i++) {
            int x = rnd.nextInt(width);
            int y = rnd.nextInt(height);
            img.setRGB(x, y, whiteColor);
        }
    }

    // Pepper noises
    public void addPepperNoise(double percent) {
        if (img == null) {
            return;
        }

        double noOfPX = height * width;
        int noiseAdded = (int) (percent * noOfPX);
        Random rnd = new Random();
        int blackColor = 0;

        for (int i = 1; i <= noiseAdded; i++) {
            int x = rnd.nextInt(width);
            int y = rnd.nextInt(height);
            img.setRGB(x, y, blackColor);
        }
    }

    public void addSaltAndPepperNoise(double percentEach) {
        addSaltNoise(percentEach);
        addPepperNoise(percentEach);
    }

    // Uniform noises
    public void addUniformNoise(double percent, int distribution) {
        if (img == null) {
            return;
        }

        double noOfPX = height * width;
        int noiseAdded = (int) (percent * noOfPX);
        Random rnd = new Random();

        for (int i = 1; i <= noiseAdded; i++) {
            int x = rnd.nextInt(width);
            int y = rnd.nextInt(height);
            int color = img.getRGB(x, y);
            int gray = color & 0xff;
            gray += (rnd.nextInt(distribution * 2) - distribution);
            gray = gray > 255 ? 255 : gray;
            gray = gray < 0 ? 0 : gray;
            int newColor = gray << 16 | gray << 8 | gray;
            img.setRGB(x, y, newColor);
        }
    }

    // Contraharmonic filter
    public void contraharmonicFilter(int size, double Q) {
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
                double sumRedAbove = 0, sumGreenAbove = 0, sumBlueAbove = 0;
                double sumRedBelow = 0, sumGreenBelow = 0, sumBlueBelow = 0;
                for (int i = y - size / 2; i <= y + size / 2; i++) {
                    for (int j = x - size / 2; j <= x + size / 2; j++) {
                        if (i >= 0 && i < height && j >= 0 && j < width) {
                            int color = img.getRGB(j, i);
                            int r = (color >> 16) & 0xff;
                            int g = (color >> 8) & 0xff;
                            int b = color & 0xff;
                            sumRedAbove += Math.pow(r, Q + 1);
                            sumGreenAbove += Math.pow(g, Q + 1);
                            sumBlueAbove += Math.pow(b, Q + 1);
                            sumRedBelow += Math.pow(r, Q);
                            sumGreenBelow += Math.pow(g, Q);
                            sumBlueBelow += Math.pow(b, Q);
                        }
                    }
                }
                sumRedAbove /= sumRedBelow;
                sumRedAbove = sumRedAbove > 255 ? 255 : sumRedAbove;
                sumRedAbove = sumRedAbove < 0 ? 0 : sumRedAbove;
                sumGreenAbove /= sumGreenBelow;
                sumGreenAbove = sumGreenAbove > 255 ? 255 : sumGreenAbove;
                sumGreenAbove = sumGreenAbove < 0 ? 0 : sumGreenAbove;
                sumBlueAbove /= sumBlueBelow;
                sumBlueAbove = sumBlueAbove > 255 ? 255 : sumBlueAbove;
                sumBlueAbove = sumBlueAbove < 0 ? 0 : sumBlueAbove;

                int newColor = ((int) sumRedAbove << 16) | ((int) sumGreenAbove << 8) | (int) sumBlueAbove;

                tempBuf.setRGB(x, y, newColor);
            }
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                img.setRGB(x, y, tempBuf.getRGB(x, y));
            }
        }
    }

    // Alpha-trimmed mean filter
    public void alphaTrimmedFilter(int size, int d) {
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
                int[] kernelRed = new int[size * size];
                int[] kernelGreen = new int[size * size];
                int[] kernelBlue = new int[size * size];
                for (int i = y - size / 2; i <= y + size / 2; i++) {
                    for (int j = x - size / 2; j <= x + size / 2; j++) {
                        int r, g, b, k;
                        if (i >= 0 && i < height && j >= 0 && j < width) {
                            int color = img.getRGB(j, i);
                            r = (color >> 16) & 0xff;
                            g = (color >> 8) & 0xff;
                            b = color & 0xff;
                            kernelRed[(i - (y - size / 2)) * size + (j - (x - size / 2))] = r;
                            kernelGreen[(i - (y - size / 2)) * size + (j - (x - size / 2))] = g;
                            kernelBlue[(i - (y - size / 2)) * size + (j - (x - size / 2))] = b;
                        }
                    }
                }

                for (int j = 0; j < size * size - 1; j++) {
                    {
                        int temp;
                        if (kernelRed[j] > kernelRed[j + 1]) {
                            temp = kernelRed[j];
                            kernelRed[j] = kernelRed[j + 1];
                            kernelRed[j + 1] = temp;
                        }
                        if (kernelGreen[j] > kernelGreen[j + 1]) {
                            temp = kernelGreen[j];
                            kernelGreen[j] = kernelGreen[j + 1];
                            kernelGreen[j + 1] = temp;
                        }
                        if (kernelBlue[j] > kernelBlue[j + 1]) {
                            temp = kernelBlue[j];
                            kernelBlue[j] = kernelBlue[j + 1];
                            kernelBlue[j + 1] = temp;
                        }
                    }
                }

                int remainingPixel = size * size - d;
                int red = 0, green = 0, blue = 0;

                for (int i = 0; i < remainingPixel; i++) {
                    red += kernelRed[(d / 2) + i];
                    green += kernelGreen[(d / 2) + i];
                    blue += kernelBlue[(d / 2) + i];
                }

                red /= remainingPixel;
                red = red > 255 ? 255 : red;
                red = red < 0 ? 0 : red;
                green /= remainingPixel;
                green = green > 255 ? 255 : green;
                green = green < 0 ? 0 : green;
                blue /= remainingPixel;
                blue = blue > 255 ? 255 : blue;
                blue = blue < 0 ? 0 : blue;
                int newColor = (red << 16) | (green << 8) | blue;
                tempBuf.setRGB(x, y, newColor);
            }
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                img.setRGB(x, y, tempBuf.getRGB(x, y));
            }
        }
    }
}
