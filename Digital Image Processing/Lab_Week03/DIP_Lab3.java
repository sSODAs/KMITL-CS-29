package Lab_Week3;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import javax.imageio.ImageIO;

public class DIP_Lab3 { //66050170
    public static void main(String[] args) {
        ImageManager im = new ImageManager();
        im.read("Lab_Week3\\images\\mandril.bmp");

        // Quest 004
        im.Gamma_transformation(2.2);
        im.write("Lab_Week3\\images\\mandril_gamma_22.bmp");
        im.restoreToOriginal();

        im.Gamma_transformation(0.4);
        im.write("Lab_Week3\\images\\mandril_gamma_04.bmp");
        im.restoreToOriginal();

        // Quest 005
        im.adjustContrast(-100);
        im.write("Lab_Week3\\images\\mandril_contrast_minus.bmp");
        im.restoreToOriginal();

        im.adjustContrast(100);
        im.write("Lab_Week3\\images\\mandril_contrast_plus.bmp");
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

    // Brightness
    public void adjustBrightness(int brightness) {

        // ภาพแต่ละ pixel มีค่า R, G, B (Red, Green, Blue) ที่อยู่ในช่วง 0 - 255
        // ความสว่างเพิ่มขึ้น = เพิ่มค่า RGB ทุกตัวเท่า ๆ กัน
        // ความสว่างลดลง = ลบค่า RGB ทุกตัว

        /*
         * สูตร:
         * r = r + brightness;
         * r = r > 255 ? 255 : r;
         * r = r < 0 ? 0 : r;
         * 
         * clamp ค่าให้อยู่ในช่วง 0-255 หากไม่จำกัดค่าไว้ในช่วง [0, 255] จะเกิด
         * overflow หรือ underflow ทำให้ภาพผิดเพี้ยน
         */

        if (img == null)
            return;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int color = img.getRGB(x, y);
                int r = (color >> 16) & 0xff;
                int g = (color >> 8) & 0xff;
                int b = color & 0xff;

                r = r + brightness;
                r = r > 255 ? 255 : r;
                r = r < 0 ? 0 : r;

                g = g + brightness;
                g = g > 255 ? 255 : g;
                g = g < 0 ? 0 : g;

                b = b + brightness;
                b = b > 255 ? 255 : b;
                b = b < 0 ? 0 : b;

                color = (r << 16) | (g << 8) | b;

                img.setRGB(x, y, color);
            }
        }
    }

    // Negative image (ภาพเชิงลบ)
    public void invert() {

        // การสร้างภาพเชิงลบคือการ “กลับด้าน” ความเข้มของสี
        // ใช้สูตร 𝑠 = 𝐿 − 𝑟 (L = 255 ใน 8-bit image)

        /*
         * กลับด้านตัวความเข้มของสี
         * r = 255 - r;
         * g = 255 - g;
         * b = 255 - b;
         * 
         * ผลลัพธ์:
         * พื้นหลังสีขาวกลายเป็นดำ/วัตถุสีเข้มจะกลายเป็นสว่างขึ้น
         */

        if (img == null) {
            return;
        }
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int color = img.getRGB(x, y);
                int r = (color >> 16) & 0xff;
                int g = (color >> 8) & 0xff;
                int b = color & 0xff;

                r = 255 - r;
                g = 255 - g;
                b = 255 - b;

                color = (r << 16) | (g << 8) | b;

                img.setRGB(x, y, color);
            }
        }
    }

    // Histogram (ฮิสโตแกรมของภาพขาวดำ)
    public int[] getGrayscaleHistogram() {

        // Histogram คือการนับจำนวน pixel ที่มีความเข้มระดับต่าง ๆ
        // ก่อนจะสร้าง histogram จากภาพสี ต้อง แปลงภาพเป็น grayscale ก่อน
        // เพื่อให้ histogram แสดงความเข้มเพียง 1 channel (ง่ายต่อการวิเคราะห์)

        /*
         * int gray = color & 0xff; ดึงค่าความเข้มของ grayscale
         * histogram[gray]++; เพิ่มจำนวนของค่านั้นใน array
         */

        if (img == null) {
            return null;
        }

        convertToGray();
        int[] histogram = new int[256];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int color = img.getRGB(x, y);
                int gray = color & 0xff;
                histogram[gray]++;
            }
        }
        restoreToOriginal();
        return histogram;
    }

    public void writeHistogramToCSV(int[] histogram, String fileName) {
        try {
            FileWriter fw = new FileWriter(fileName);
            for (int i = 0; i < histogram.length; i++) {
                fw.write(histogram[i] + ",");
            }
            fw.close();
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    // Contrast (การคำนวณ contrast)
    public float getContrast() {

        /*
         * คำนวณค่าเฉลี่ยความเข้ม
         * วนลูปผ่านทุก pixel แล้วหาค่าเบี่ยงเบนจากค่าเฉลี่ย
         * ยกกำลังสอง + ถัวเฉลี่ย + ถอดราก
         */

        if (img == null) {
            return 0;
        }

        float contrast = 0;
        int[] histogram = getGrayscaleHistogram();
        float avgIntensity = 0;
        float pixelNum = width * height;

        for (int i = 0; i < histogram.length; i++) {
            avgIntensity += histogram[i] * i;
        }
        avgIntensity /= pixelNum;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int color = img.getRGB(x, y);
                int value = color & 0xff;
                contrast += Math.pow((value) - avgIntensity, 2);
            }
        }

        contrast = (float) Math.sqrt(contrast / pixelNum);
        return contrast;
    }

    public void adjustContrast(int contrast) { // การปรับ contrast

        /*
         * หลักการ:
         * การเพิ่ม contrast = ขยายช่วง histogram (ดึงค่าห่างออกจากค่าเฉลี่ย)
         * การลด contrast = บีบ histogram เข้าหาค่าเฉลี่ย
         * 
         * ขั้นตอน:
         * คำนวณ contrast ปัจจุบัน
         * กำหนด min/max ใหม่ โดยใช้ค่า contrast ที่ต้องการ
         * คำนวณ contrastFactor (อัตราการขยาย histogram)
         * ปรับ RGB แต่ละ channel โดยใช้สูตร: new=(r−min)×factor+newMin
         */

        if (img == null) {
            return;
        }

        float currentContrast = getContrast();
        int[] histogram = getGrayscaleHistogram();
        float avgIntensity = 0;
        float pixelNum = width * height;

        for (int i = 0; i < histogram.length; i++) {
            avgIntensity += histogram[i] * i;
        }

        avgIntensity /= pixelNum;
        float min = avgIntensity - currentContrast;
        float max = avgIntensity + currentContrast;
        float newMin = avgIntensity - currentContrast - contrast / 2;
        float newMax = avgIntensity + currentContrast + contrast / 2;

        newMin = newMin < 0 ? 0 : newMin;
        newMax = newMax < 0 ? 0 : newMax;
        newMin = newMin > 255 ? 255 : newMin;
        newMax = newMax > 255 ? 255 : newMax;

        if (newMin > newMax) {
            float temp = newMax;
            newMax = newMin;
            newMin = temp;
        }

        float contrastFactor = (newMax - newMin) / (max - min);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                int color = img.getRGB(x, y);
                int r = (color >> 16) & 0xff;
                int g = (color >> 8) & 0xff;
                int b = color & 0xff;

                r = (int) ((r - min) * contrastFactor + newMin);
                r = r > 255 ? 255 : r;
                r = r < 0 ? 0 : r;

                g = (int) ((g - min) * contrastFactor + newMin);
                g = g > 255 ? 255 : g;
                g = g < 0 ? 0 : g;

                b = (int) ((b - min) * contrastFactor + newMin);
                b = b > 255 ? 255 : b;
                b = b < 0 ? 0 : b;

                color = (r << 16) | (g << 8) | b;
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

    public void Gamma_transformation(double gamma) {
        if (img == null) {
            return;
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int color = img.getRGB(x, y);
                int r = (color >> 16) & 0xff;
                int g = (color >> 8) & 0xff;
                int b = color & 0xff;

                r = (int) (255 * Math.pow(r / 255.0, gamma));
                g = (int) (255 * Math.pow(g / 255.0, gamma));
                b = (int) (255 * Math.pow(b / 255.0, gamma));

                r = Math.max(0, Math.min(255, r));
                g = Math.max(0, Math.min(255, g));
                b = Math.max(0, Math.min(255, b));

                int newColor = (r << 16) | (g << 8) | b;
                img.setRGB(x, y, newColor);
            }
        }

    }
}
