package Lab_Week2;

import java.awt.image.BufferedImage; // ใช้จัดการภาพ
import java.io.File; // ใช้ระบุ path ของไฟล์
import java.io.IOException;

import javax.imageio.ImageIO; // ใช้อ่าน/เขียนไฟล์ภาพ

public class DIP_Lab2 {
    public static void main(String[] args) {
        ImageManager im = new ImageManager();
        im.read("Lab1\\images\\mandril.bmp"); // อ่านไฟล์ต้นฉบับ
        im.convertToRed();
        im.write("Lab1\\images\\mandril_red.bmp"); // เขียนเป็นไฟล์ใหม่
        im.restoreToOriginal();

        // Quest 002
        im.convertToGreen();
        im.write("Lab1\\images\\mandril_green.bmp");
        im.restoreToOriginal();

        im.convertToBlue();
        im.write("Lab1\\images\\mandril_blue.bmp");
        im.restoreToOriginal();

        // Quest 003
        im.convertToGray();
        im.write("Lab1\\images\\mandril_gray.bmp");
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

    /*
     * ARGB (Alpha, Red, Green, Blue)
     * [8 บิต][8 บิต][8 บิต][8 บิต]
     * 
     * color = (r << 16) | (0 << 8) | 0; (แดง ไปอยู่บิตที่ 16–23)
     * color = (0 << 16) | (g << 8) | 0; (เขียว อยู่ที่ 8–15)
     * color = (0 << 16) | (0 << 8) | b; (ฟ้า อยู่ที่ 0–7)
     */

    // RED
    public void convertToRed() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int color = img.getRGB(x, y);
                int r = (color >> 16) & 0xff; // ดัน R มาด้านขวา 16 บิต แล้วกรองให้เหลือ 8 บิต
                color = (r << 16) | (0 << 8) | 0;
                img.setRGB(x, y, color);
            }
        }
    }

    // BLUE
    public void convertToBlue() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int color = img.getRGB(x, y);
                int b = color & 0xff; // ไม่ต้อง shift เพราะ B อยู่ขวาสุด
                color = (0 << 16) | (0 << 8) | b;
                img.setRGB(x, y, color);
            }
        }
    }

    // GREEN
    public void convertToGreen() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int color = img.getRGB(x, y);
                int g = (color >> 8) & 0xff; // ดัน G มา 8 บิต แล้วกรอง
                color = (0 << 16) | (g << 8) | 0;
                img.setRGB(x, y, color);
            }
        }
    }

    // GRAY
    public void convertToGray() {
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

    public void restoreToOriginal() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                img.setRGB(x, y, original.getRGB(x, y));
            }
        }
    }
}