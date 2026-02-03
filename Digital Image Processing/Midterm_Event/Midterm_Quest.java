package Midterm_Event;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Midterm_Quest { // 66050170  อยากได้รางวัลคือ เลือกหดบอร์ดเกมทั้งบอร์ด 1 แถว ด้านซ้าย ʕ⁠っ⁠•⁠ᴥ⁠•⁠ʔ⁠っ

    public static void main(String[] args) {
        ImageManager im = new ImageManager();
        im.read("Midterm_Event\\raw\\gamemaster_noise_2025.bmp");

        // ลบ noise ด้วย Median Filter + Gaussian Blur ทำ 5 รอบ โดย Median Filter ลด salt-and-pepper noise และใช้ Gaussian Blur ให้ภาพเบลอขึ้น
        for (int i = 0; i < 5; i++) {
            im.medianFilter(5);
            im.gaussianBlur(5, 1);
            im.write("Midterm_Event\\editing\\de_noise.bmp");
        }

        // เกมมาสเตอร์ บอกไม่อยากให้มันมีสีเขียว และเพื่อความง่ายในการจัดการสี เลยเขียน func removeGreenPixels ลบสีเขียวออกจากภาพโดย ตั้งค่าช่องสีเขียว (G) ให้เป็น 0 ทั้งหมด
        im.removeGreenPixels();
        im.write("Midterm_Event\\editing\\removeGreen.bmp");

        // ทำให้ภาพเป็น block ขนาด 16x16 pixel เฉลี่ยสีในแต่ละ blockเพื่อให้ง่ายในการแก้ไขสีในภาพ
        im.pixelate(16);
        im.write("Midterm_Event\\editing\\pixelate.bmp");

        // ตอนนี้สีข้องภาพ ตัวละคอนและขอบมีสีที่ใกล้เคียงกัน ทำให้มันจะยากถ้าจะต้องนั่งแก้สีของตัวละครเลยให้พื้นหลังเป็นสีขาวไปก่อน
        im.replaceBackgroundWithWhite();
        im.write("Midterm_Event\\editing\\replaceBackground.bmp");


        // Phase 2 ใส่สัตัวละคร เราใช้วิธีจูนสี หาสีที่จะเปลี่ยน สีที่เปลี่ยน และพื้นที่ที่เราจะเปลี่ยนโดย จูนหาค่าเฉลี่ยนเฉดสีแบบ RGB และตำแหน่งแกน x และ y 
        // โดยสีบางสวนเราจูนหาที่ใกล้เคยงตามเควสของเกมมาสเตอร์

        /* ARGB (Alpha, Red, Green, Blue)
        * [8 บิต][8 บิต][8 บิต][8 บิต]
        * color = (r << 16) | (0 << 8) | 0; (แดง ไปอยู่บิตที่ 16–23)
        * color = (0 << 16) | (g << 8) | 0; (เขียว อยู่ที่ 8–15)
        * color = (0 << 16) | (0 << 8) | b; (ฟ้า อยู่ที่ 0–7)
        */

        int skinColor = (210 << 16) | (180 << 8) | 140; // ผิวของเกมมาสเตอร์สีน้ำตาลอ่อน
        int hairColor = (106 << 16) | (0 << 8) | 255; // ผมสีม่วง
        int mustacheColor = (128 << 16) | (128 << 8) | 128; // หนวดสีเทา
        int shirtColor = (0 << 16) | (0 << 8) | 255; // เสื้อสีน้ำเงิน
        int background = (148 << 16) | (6 << 8) | 7; // พื้นหลังสีเดียวกับสีที่ทุกคนเลือกใช้มากที่สุดในเกมครั้งนี้ (ไม่รู้อะสุ่มเอามีแดงกะน้ำเงิน)
        int reflection = (255 << 16) | (255 << 8) | 255;
        int glasses = (0 << 16) | (0 << 8) | 0;

        // การเปลี่ยนสีของเรา ตำแหน่งที่จะเปลี่ยน แกน x, y + ค่าเฉลี่ยของสี RGB + ตัวค่าสีที่จะเปลี่ยนตามเควส + ใช้ tolerance เพื่อยอมให้สีใกล้เคียงถูกเปลี่ยนด้วย 
        im.replaceColorInArea(0, 272, im.width - 1, 399, 80, 130, 0, 20, 40, 120, mustacheColor, 40);

        im.replaceColorInArea(0, 0, im.width - 1, 176, 20, 110, 0, 15, 20, 80, hairColor, 20);

        im.replaceColorInArea(0, 432, im.width - 1, im.height - 1, 30, 100, 0, 50, 40, 150, shirtColor, 40);

        im.replaceColorInArea(0, 0, im.width - 1, im.height - 1, 255, 255, 255, 255, 255, 255, background, 40);

        im.replaceColorInArea(0, 176, im.width - 1, 399, 134, 146, 0, 10, 130, 140, reflection, 20);

        im.replaceColorInArea(0, 176, im.width - 1, 399, 41, 61, 0, 10, 45, 76, glasses, 20);

        im.replaceColorInArea(0, 0, im.width - 1, im.height - 1, 102, 158, 0, 0, 144, 203, skinColor, 40);
        im.write("Midterm_Event\\editing\\coloring.bmp");


        // pixelate อีกครั้งเพราะมันยังมีจุดขาวเล็กๆตามรูป (ไม่รู้เกิดมาได้ไงเหมือนกัน)
        im.pixelate(16);
        im.write("Midterm_Event\\editing\\gamemaster_noise_2025_Final.bmp");

        // รูปสุดท้าย จะมีสีไม่เกิน 7 สีรวมขาวและดำ (เช็คจาก histogram)

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
        width = original.getWidth();
        height = original.getHeight();
        img = new BufferedImage(width, height, img.getType());

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                img.setRGB(x, y, original.getRGB(x, y));
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

    // Gaussian Blur ทำให้ภาพนุ่มขึ้น ลบรายละเอียดจุกจิก โดยสร้าง Gaussian kernel ขนาด size โดยใช้ค่า sigma ควบคุมความเบลอ และคูณ kernel กับค่าพิกเซลรอบๆ แล้วบวกผลรวมเพื่อให้ได้สีใหม่
    public void gaussianBlur(int size, double sigma) {
        if (img == null)
            return;
        if (size % 2 == 0) {
            System.out.println("Size Invalid: must be odd number!");
            return;
        }

        double[][] kernel = generateGaussianKernel(size, sigma);
        BufferedImage tempBuf = new BufferedImage(width, height, img.getType());

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double sumRed = 0, sumGreen = 0, sumBlue = 0;

                for (int i = 0; i < size; i++) {
                    for (int j = 0; j < size; j++) {
                        int pixelY = y + i - size / 2;
                        int pixelX = x + j - size / 2;

                        if (pixelY >= 0 && pixelY < height && pixelX >= 0 && pixelX < width) {
                            int color = img.getRGB(pixelX, pixelY);
                            int r = (color >> 16) & 0xff;
                            int g = (color >> 8) & 0xff;
                            int b = color & 0xff;

                            sumRed += r * kernel[i][j];
                            sumGreen += g * kernel[i][j];
                            sumBlue += b * kernel[i][j];
                        }
                    }
                }

                int newRed = (int) Math.round(sumRed);
                int newGreen = (int) Math.round(sumGreen);
                int newBlue = (int) Math.round(sumBlue);

                newRed = newRed > 255 ? 255 : (newRed < 0 ? 0 : newRed);
                newGreen = newGreen > 255 ? 255 : (newGreen < 0 ? 0 : newGreen);
                newBlue = newBlue > 255 ? 255 : (newBlue < 0 ? 0 : newBlue);

                int newColor = (newRed << 16) | (newGreen << 8) | newBlue;
                tempBuf.setRGB(x, y, newColor);
            }
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                img.setRGB(x, y, tempBuf.getRGB(x, y));
            }
        }
    }

    private double[][] generateGaussianKernel(int size, double sigma) {
        double[][] kernel = new double[size][size];
        double sum = 0.0;
        int center = size / 2;

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                int x = i - center;
                int y = j - center;
                kernel[i][j] = Math.exp(-(x * x + y * y) / (2 * sigma * sigma)) / (2 * Math.PI * sigma * sigma);
                sum += kernel[i][j];
            }
        }

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                kernel[i][j] /= sum;
            }
        }

        return kernel;
    }

    // Remove Green Color ลบข้อมูลช่องสีเขียวทั้งหมด
    public void removeGreenPixels() {
        if (img == null)
            return;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int color = img.getRGB(x, y);
                int r = (color >> 16) & 0xff;
                int b = color & 0xff;

                color = (r << 16) | (0 << 8) | b;
                img.setRGB(x, y, color);
            }
        }
    }


    // Averaging Filter ใช้สำหรับ ทำให้ภาพเรียบขึ้น (blur) และ ลด noise ในภาพ
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

    // pixelate ทำให้ภาพเป็นบล็อกๆ เหมือนภาพพิกเซลต่ำ โดยแบ่งภาพเป็นบล็อกสี่เหลี่ยมขนาด blockSize x blockSize คำนวณสีเฉลี่ยของบล็อกนั้น และใส่สีเฉลี่ยกลับทุกพิกเซลในบล็อก
    public void pixelate(int blockSize) {
        if (img == null || blockSize <= 1)
            return;

        for (int y = 0; y < height; y += blockSize) {
            for (int x = 0; x < width; x += blockSize) {
                int sumR = 0, sumG = 0, sumB = 0, count = 0;

                for (int dy = 0; dy < blockSize && y + dy < height; dy++) {
                    for (int dx = 0; dx < blockSize && x + dx < width; dx++) {
                        int color = img.getRGB(x + dx, y + dy);
                        sumR += (color >> 16) & 0xff;
                        sumG += (color >> 8) & 0xff;
                        sumB += color & 0xff;
                        count++;
                    }
                }

                int avgR = sumR / count;
                int avgG = sumG / count;
                int avgB = sumB / count;
                int avgColor = (avgR << 16) | (avgG << 8) | avgB;

                for (int dy = 0; dy < blockSize && y + dy < height; dy++) {
                    for (int dx = 0; dx < blockSize && x + dx < width; dx++) {
                        img.setRGB(x + dx, y + dy, avgColor);
                    }
                }
            }
        }
    }

    private boolean isPink(int r, int g, int b) {
        return (r >= 190 && r <= 210) &&
                (g >= 70 && g <= 90) &&
                (b >= 170 && b <= 190);
    }

    // replaceBackgroundWithWhite ลบพื้นหลังออกแล้วแทนด้วยสีขาว โดยเราจะหาสีขอบภาพ ใช้ Flood Fill เพื่อตามหาพื้นหลังที่สีใกล้เคียง และเปลี่ยนพิกเซลเหล่านั้นเป็นสีขาว ยกเว้นสีที่อยู่ใน ignore list
    // เราใช้วิธีการแบ่ง ภาพเป็น 4 ส่วน เนื่อจากแต่ละส่วนใช้ค่า tolerance1 และ minLum1 ไม่เท่ากัน เลยแบ่งเป็นสีส่วนเพื่อความง่ายในการลบ 
    public void replaceBackgroundWithWhite() {
        if (img == null)
            return;

        int whiteColor = (255 << 16) | (255 << 8) | 255;
        boolean[][] visited = new boolean[height][width];
        java.util.Queue<int[]> queue = new java.util.LinkedList<>();

        int sampleR = 0, sampleG = 0, sampleB = 0, sampleCount = 0;
        for (int x = 0; x < width; x++) {
            int[] top = getRGB(img.getRGB(x, 0));
            int[] bottom = getRGB(img.getRGB(x, height - 1));
            sampleR += top[0] + bottom[0];
            sampleG += top[1] + bottom[1];
            sampleB += top[2] + bottom[2];
            sampleCount += 2;
        }
        for (int y = 0; y < height; y++) {
            int[] left = getRGB(img.getRGB(0, y));
            int[] right = getRGB(img.getRGB(width - 1, y));
            sampleR += left[0] + right[0];
            sampleG += left[1] + right[1];
            sampleB += left[2] + right[2];
            sampleCount += 2;
        }
        sampleR /= sampleCount;
        sampleG /= sampleCount;
        sampleB /= sampleCount;

        // tolerance ยอมให้สีที่ต่างจากเงื่อนไขได้มากแค่ไหน ถ้า tolerance สูง → สีใกล้เคียงก็โดนเปลี่ยนหมด
        // minLum ค่า ความสว่างขั้นต่ำ ของพิกเซลที่จะถูกเลือก ถ้าความสว่างของพิกเซลต่ำกว่า 20 → ถือว่ามืดเกินไป ไม่เลือกมาเปลี่ยนสี เพื่อกันไม่ให้เลือกพิกเซลที่เป็นเงามืดหรือดำสนิท เพราะมันอาจไม่ใช่ส่วนที่เราต้องการ
        int tolerance1 = 70, minLum1 = 20;
        int tolerance2 = 70, minLum2 = 20;
        int tolerance3 = 71, minLum3 = 48;
        int tolerance4 = 99, minLum4 = 20;

        for (int x = 0; x < width; x++) {
            queue.add(new int[] { x, 0 });
            queue.add(new int[] { x, height - 1 });
        }
        for (int y = 0; y < height; y++) {
            queue.add(new int[] { 0, y });
            queue.add(new int[] { width - 1, y });
        }

        while (!queue.isEmpty()) {
            int[] p = queue.poll();
            int x = p[0], y = p[1];
            if (x < 0 || y < 0 || x >= width || y >= height)
                continue;
            if (visited[y][x])
                continue;
            visited[y][x] = true;

            int[] rgb = getRGB(img.getRGB(x, y));
            int dist = colorDistance(rgb[0], rgb[1], rgb[2], sampleR, sampleG, sampleB);
            int lum = luminance(rgb[0], rgb[1], rgb[2]);

            boolean replace = false;

            if (y >= 0 && y <= 176) {
                if (dist <= tolerance1 && lum >= minLum1) {
                    replace = true;
                }
            } else if (y >= 177 && y <= 384) {
                if (dist <= tolerance2 && lum >= minLum2) {
                    replace = true;
                }
            } else if (y >= 385 && y <= 431) {
                if (dist <= tolerance3 && lum >= minLum3) {
                    replace = true;
                }
            } else if (y >= 432 && y < height) {
                if (dist <= tolerance4 && lum >= minLum4) {
                    replace = true;
                }
            }

            if (replace && !isPink(rgb[0], rgb[1], rgb[2])) {
                img.setRGB(x, y, whiteColor);
                queue.add(new int[] { x + 1, y });
                queue.add(new int[] { x - 1, y });
                queue.add(new int[] { x, y + 1 });
                queue.add(new int[] { x, y - 1 });
            }
        }
    }

    private int[] getRGB(int color) {
        return new int[] {
                (color >> 16) & 0xff,
                (color >> 8) & 0xff,
                color & 0xff
        };
    }

    private int colorDistance(int r1, int g1, int b1, int r2, int g2, int b2) {
        return (int) Math.sqrt(
                (r1 - r2) * (r1 - r2) +
                        (g1 - g2) * (g1 - g2) +
                        (b1 - b2) * (b1 - b2));
    }

    private int luminance(int r, int g, int b) {
        return (int) (0.299 * r + 0.587 * g + 0.114 * b);
    }

    // replaceColorInArea เปลี่ยนสีในพื้นที่ที่เลือกและมีค่าสีตามเงื่อนไข โดยการทำงานคือ เริ่มจากพิกัด (x, y) ตรวจสอบค่าสีว่าต้องอยู่ในช่วง rMin~rMax, gMin~gMax, bMin~bMax ภายใน tolerance และใช้ Flood Fill เปลี่ยนพิกเซลเหล่านั้นเป็นสีใหม่ (newR, newG, newB)
    public void replaceColorInArea(
            int x1, int y1, int x2, int y2,
            int rMin, int rMax,
            int gMin, int gMax,
            int bMin, int bMax,
            int newColor,
            int tolerance) {
        for (int y = y1; y <= y2 && y < height; y++) {
            for (int x = x1; x <= x2 && x < width; x++) {
                int color = img.getRGB(x, y);
                int r = (color >> 16) & 0xFF;
                int g = (color >> 8) & 0xFF;
                int b = color & 0xFF;

                if (isWithinRange(r, rMin, rMax, tolerance) &&
                        isWithinRange(g, gMin, gMax, tolerance) &&
                        isWithinRange(b, bMin, bMax, tolerance)) {

                    if (hasNeighborWithinRange(x, y, rMin, rMax, gMin, gMax, bMin, bMax, tolerance)) {
                        img.setRGB(x, y, newColor);
                    }
                }
            }
        }
    }

    private boolean hasNeighborWithinRange(int x, int y,
            int rMin, int rMax,
            int gMin, int gMax,
            int bMin, int bMax,
            int tolerance) {

        int[][] neighbors = { { x + 1, y }, { x - 1, y }, { x, y + 1 }, { x, y - 1 } };
        for (int[] n : neighbors) {
            int nx = n[0], ny = n[1];
            if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                int c = img.getRGB(nx, ny);
                int nr = (c >> 16) & 0xFF;
                int ng = (c >> 8) & 0xFF;
                int nb = c & 0xFF;
                if (isWithinRange(nr, rMin, rMax, tolerance) &&
                        isWithinRange(ng, gMin, gMax, tolerance) &&
                        isWithinRange(nb, bMin, bMax, tolerance)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isWithinRange(int val, int min, int max, int tol) {
        return val >= (min - tol) && val <= (max + tol);
    }

}
