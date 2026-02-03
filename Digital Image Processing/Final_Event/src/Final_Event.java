
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import javax.imageio.ImageIO;

public class Final_Event {

    public static void main(String[] args) {
        ImageManager im = new ImageManager();

        // --- อ่านภาพหลักจากไฟล์ ---
        // การอ่านภาพ BMP เข้ามาเป็น BufferedImage เพื่อใช้ประมวลผล
        im.read("./raw/FinalDIP67.bmp");

        // --- กำหนดจุด 4 จุดในภาพต้นฉบับและจุดเป้าหมายสำหรับ Homography ---

        double[][] srcPoints = { // srcPoints: 4 มุมในภาพต้นฉบับ
                { 50, 135 }, { 545, 65 }, { 755, 315 }, { 70, 515 }
        };

        double[][] dstPoints = { // dstPoints: 4 มุมในภาพเป้าหมาย (rectangular)
                { 0, 0 }, { 800, 0 }, { 800, 600 }, { 0, 600 }
        };

        // --- คำนวณ Homography matrix จาก 4 จุดที่กำหนด ---
        // เราต้องการแปลง perspective ของภาพให้เป็นรูปสี่เหลี่ยมมุมฉาก
        double[] H = im.calculateHomography(srcPoints, dstPoints);

        // --- นำ Homography ไป Warp ภาพ ---
        // Warp image ปรับ perspective ให้ตรงตามที่ต้องการ
        im.applyHomography(H);
        im.write("./output/step1_warped.bmp");

        // --- Preprocess: แปลงภาพเป็น Grayscale ---
        // ลดข้อมูล RGB เป็น grayscale เพื่อเตรียม thresholding
        im.toGrayscale();
        im.write("./output/step2_toGrayscale.bmp");

        // --- Denoise: ลด noise ในภาพ ---
        im.gaussianBlur(3, 3); // Gaussian Blur: ลด noise แบบ smoothing โดย size 3x3 และ sigma=3
        im.medianFilter(3); // Median Filter: ลด noise แบบ impulse noise โดย size 3x3
        im.write("./output/step3_denoise.bmp");

        // --- Thresholding: แปลงเป็น Binary image ---
        // หา threshold อัตโนมัติ โดยวิเคราะห์ histogram
        im.otsuThreshold();
        im.write("./output/step4_otsuThreshold.bmp");

        // --- Morphological operations ---
        im.morphologicalOpening(1); // Opening (Erosion->Dilation): ลบจุดรบกวนเล็ก ๆ
        im.morphologicalClosing(2); // Closing (Dilation->Erosion): เติมช่องว่างเล็ก ๆ
        im.write("./output/step5_morphological.bmp");

        // --- Crop ขอบภาพเล็กน้อย เพื่อไม่ให้มี artifacts ขอบ ---
        for (int y = 0; y < im.height; y++) {
            for (int x = 0; x < im.width; x++) {
                if (x < 5 || x > im.width - 6 || y < 5 || y > im.height - 6) {
                    im.img.setRGB(x, y, 0xFFFFFF);
                }
            }
        }

        // --- Segment digits: แยกตัวเลขแต่ละตัวออกมาเป็นไฟล์ PNG ---
        String digitDir = "./output/digits_output";
        im.segmentDigits(digitDir);

        // --- Load template digits ---
        // template เป็นภาพของตัวเลข 0-9 โดยเราจะเปรียบเทียบ Hamming distance กับ digit
        // ในภาพ
        Map<Integer, int[][]> templates = im.loadTemplates("template_digits", 128);

        // --- Match digits ทั้งหมดในภาพ ---
        String finalOutput = im.matchAllDigits("./output/digits_output", templates, 128);
        System.out.println("Final output: " + finalOutput);

        /*
         * --- การเตรียม template picture ---
         *
         * อ่านภาพ template_pic.bmp แล้วทำ preprocessing เหมือนกับภาพหลัก
         * เพื่อให้ได้ภาพ binary ที่ชัดเจน จากนั้นแยกตัวเลขออกมาเก็บเป็นไฟล์ PNG
         * ในโฟลเดอร์ template_digits โดย เราแปลงภาพเป็น
         * -> grayscale
         * -> thresholding
         * -> morphological
         * แต่ภพาพสีมันพื้นหลังดำตัวหนังสือขาวซึ่งเช็คกับตัวหลักยากเลย -> invert colors
         * -> segment digits ตัดเลขออกมา
         */

    }
}

class ImageManager {
    public int width, height, bitDepth; // ขนาดและ bit depth ของภาพ
    BufferedImage img; // ภาพปัจจุบัน
    private BufferedImage original; // ภาพต้นฉบับ (สำรอง)

    // --- อ่านภาพจากไฟล์ ---
    public boolean read(String fileName) {
        try {
            img = ImageIO.read(new File(fileName));

            width = img.getWidth();
            height = img.getHeight();
            bitDepth = img.getColorModel().getPixelSize();

            // ทำสำเนาภาพต้นฉบับเพื่อ backup
            original = new BufferedImage(width, height, img.getType());
            for (int y = 0; y < height; y++)
                for (int x = 0; x < width; x++)
                    original.setRGB(x, y, img.getRGB(x, y));

            System.out.println("Image " + fileName + " read: " + width + "x" + height + " (" + bitDepth + " bpp)");
            return true;
        } catch (IOException e) {
            System.out.println(e);
            return false;
        }
    }

    // --- เขียนภาพลงไฟล์ ---
    public boolean write(String fileName) {
        try {
            ImageIO.write(img, "bmp", new File(fileName));
            System.out.println("Image " + fileName + " written!");
            return true;
        } catch (IOException | NullPointerException e) {
            System.out.println(e);
            return false;
        }
    }

    // --- แปลงภาพเป็น grayscale ---
    // หลักการ: แปลงแต่ละ pixel จาก RGB เป็น grayscale เพื่อให้ง่ายต่อการประมวลผล
    // ใช้สูตรมาตรฐาน: Gray = 0.3*R + 0.59*G + 0.11*B
    public void toGrayscale() {
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int gray = (int) (0.3 * r + 0.59 * g + 0.11 * b);
                int newPixel = (gray << 16) | (gray << 8) | gray;
                img.setRGB(x, y, newPixel);
            }
    }

    // --- Gaussian blur ---
    // หลักการ: ทำให้ภาพเบลอ ลด noise แบบ smooth
    // ใช้ kernel Gaussian ขนาด (size x size) กับ sigma เป็นตัวกำหนด spread
    public void gaussianBlur(int size, double sigma) {
        if (img == null)
            return;
        if (size % 2 == 0) {
            System.out.println("Size Invalid: must be odd number!");
            return;
        }

        // สร้าง kernel Gaussian
        double[][] kernel = generateGaussianKernel(size, sigma);
        BufferedImage tempBuf = new BufferedImage(width, height, img.getType());

        // convolution: สำหรับแต่ละ pixel, คูณค่า kernel รอบ pixel
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

    // --- generate Gaussian kernel ---
    // หลักการ: สร้าง kernel แบบ normalized เพื่อใช้ใน Gaussian blur
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

        // normalize kernel (รวมค่า = 1)
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                kernel[i][j] /= sum;
            }
        }

        return kernel;
    }

    // --- Median filter ลด noise แต่คงขอบคม ---
    // หลักการ: สำหรับแต่ละ pixel, นำค่ารอบๆ ใน window มาหา median
    // ข้อดี: ลด noise แบบ salt & pepper ได้ดี โดยไม่ทำให้ edges เบลอมาก
    public void medianFilter(int size) {
        if (img == null || size % 2 == 0)
            return;

        BufferedImage temp = new BufferedImage(width, height, img.getType());
        int radius = size / 2;
        int[] window = new int[size * size];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int count = 0;
                // เก็บค่ารอบ pixel ใน window
                for (int dy = -radius; dy <= radius; dy++) {
                    for (int dx = -radius; dx <= radius; dx++) {
                        int nx = x + dx, ny = y + dy;
                        if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                            int gray = img.getRGB(nx, ny) & 0xFF;
                            window[count++] = gray;
                        }
                    }
                }
                Arrays.sort(window, 0, count); // sort หา median
                int median = window[count / 2];
                int val = (median << 16) | (median << 8) | median;
                temp.setRGB(x, y, val);
            }
        }

        // คัดลอกกลับไปยัง img
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                img.setRGB(x, y, temp.getRGB(x, y));
    }

    // --- ปรับ contrast ---
    public void contrastStretch(int low, int high) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int gray = img.getRGB(x, y) & 0xFF;
                int val = (gray - low) * 255 / (high - low);
                val = Math.max(0, Math.min(255, val));
                int newPixel = (val << 16) | (val << 8) | val;
                img.setRGB(x, y, newPixel);
            }
        }
    }

    // --- Otsu thresholding แยก foreground/background ---
    public void otsuThreshold() {
        int[] hist = new int[256];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int gray = img.getRGB(x, y) & 0xFF;
                hist[gray]++;
            }
        }

        int total = width * height;
        float sum = 0;
        for (int t = 0; t < 256; t++)
            sum += t * hist[t];

        float sumB = 0;
        int wB = 0, wF = 0;
        float varMax = 0;
        int threshold = 0;

        for (int t = 0; t < 256; t++) {
            wB += hist[t];
            if (wB == 0)
                continue;
            wF = total - wB;
            if (wF == 0)
                break;

            sumB += (float) (t * hist[t]);

            float mB = sumB / wB;
            float mF = (sum - sumB) / wF;

            float varBetween = (float) wB * (float) wF * (mB - mF) * (mB - mF);

            if (varBetween > varMax) {
                varMax = varBetween;
                threshold = t;
            }
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int gray = img.getRGB(x, y) & 0xFF;
                int val = (gray > threshold) ? 0xFFFFFF : 0x000000;
                img.setRGB(x, y, val);
            }
        }
    }

    // --- Homography computation ---
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
        return gaussianElimination(A, b);
    }

    // --- Gaussian elimination solver ---
    // ใช้แก้ระบบสมการ linear 8x8 สำหรับหา homography
    public double[] gaussianElimination(double[][] A, double[] b) {
        int n = b.length;
        // Forward elimination
        for (int i = 0; i < n; i++) {
            int max = i;
            for (int j = i + 1; j < n; j++) {
                if (Math.abs(A[j][i]) > Math.abs(A[max][i])) {
                    max = j;
                }
            }

            double[] temp = A[i];
            A[i] = A[max];
            A[max] = temp;

            double t = b[i];
            b[i] = b[max];
            b[max] = t;

            for (int k = i + 1; k < n; k++) {
                double factor = A[k][i] / A[i][i];
                b[k] -= factor * b[i];
                for (int j = i; j < n; j++) {
                    A[k][j] -= factor * A[i][j];
                }
            }
        }

        // Backward substitution
        double[] x = new double[n];
        for (int i = n - 1; i >= 0; i--) {
            double sum = 0.0;
            for (int j = i + 1; j < n; j++) {
                sum += A[i][j] * x[j];
            }
            x[i] = (b[i] - sum) / A[i][i];
        }

        double[] homography = new double[9];
        System.arraycopy(x, 0, homography, 0, 8);
        homography[8] = 1;

        return homography;
    }

    // --- Invert homography ---
    // หลักการ: หา inverse ของ matrix 3x3 สำหรับ mapping กลับ
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

    // --- Apply homography to warp image ---
    // หลักการ: ใช้ inverse mapping เพื่อ warp image
    // แต่ละ pixel ใน output ถูก mapping กลับไปยัง input
    public void applyHomography(double[] H) {
        BufferedImage output = new BufferedImage(width, height, img.getType());

        double[] invH = invertHomography(H);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                double[] sourcePoint = applyHomographyToPoint(invH, x, y);

                int srcX = (int) Math.round(sourcePoint[0]);
                int srcY = (int) Math.round(sourcePoint[1]);

                if (srcX >= 0 && srcX < width && srcY >= 0 && srcY < height) {
                    Color color = new Color(img.getRGB(srcX, srcY));
                    output.setRGB(x, y, color.getRGB());
                } else {
                    output.setRGB(x, y, Color.BLACK.getRGB());
                }
            }
        }

        // คัดลอกกลับไปยัง img
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                img.setRGB(x, y, output.getRGB(x, y));
            }
        }
    }

    private double[] applyHomographyToPoint(double[] H, double x, double y) {
        double xh = H[0] * x + H[1] * y + H[2];
        double yh = H[3] * x + H[4] * y + H[5];
        double w = H[6] * x + H[7] * y + H[8];

        double xPrime = xh / w;
        double yPrime = yh / w;

        return new double[] { xPrime, yPrime };
    }

    // --- Morphological operations ---
    // Closing: Dilation -> Erosion (เติมช่องว่างเล็กๆ)
    public void morphologicalClosing(int kernelSize) {
        img = dilation(img, kernelSize);
        img = erosion(img, kernelSize);
    }

    // Opening: Erosion -> Dilation (ลบ noise เล็กๆ)
    public void morphologicalOpening(int kernelSize) {
        img = erosion(img, kernelSize);
        img = dilation(img, kernelSize);
    }

    // --- Dilation ---
    // หลักการ: pixel ใดๆ ที่มี pixel สีดำใน kernel จะถูกเพิ่มค่า (ดำ → ขาว)
    // เหมาะกับการเติมช่องว่างเล็กๆ และเชื่อม object
    private BufferedImage dilation(BufferedImage input, int kernelSize) {
        BufferedImage output = new BufferedImage(width, height, input.getType());
        int k = kernelSize;
        for (int y = k; y < height - k; y++) {
            for (int x = k; x < width - k; x++) {
                boolean hitBlack = false;
                for (int dy = -k; dy <= k && !hitBlack; dy++) {
                    for (int dx = -k; dx <= k; dx++) {
                        int gray = input.getRGB(x + dx, y + dy) & 0xFF;
                        if (gray == 0) {
                            hitBlack = true;
                            break;
                        }
                    }
                }
                int val = hitBlack ? 0 : 255;
                output.setRGB(x, y, (val << 16) | (val << 8) | val);
            }
        }
        return output;
    }

    // --- Erosion ---
    // หลักการ: pixel ใดๆ ที่มี pixel สีดำใน kernel จะถูกลดค่า (ขาว → ดำ)
    // เหมาะกับการลบ noise ขนาดเล็กและแยก object
    private BufferedImage erosion(BufferedImage input, int kernelSize) {
        BufferedImage output = new BufferedImage(width, height, input.getType());
        int k = kernelSize;
        for (int y = k; y < height - k; y++) {
            for (int x = k; x < width - k; x++) {
                boolean allBlack = true;
                for (int dy = -k; dy <= k && allBlack; dy++) {
                    for (int dx = -k; dx <= k; dx++) {
                        int gray = input.getRGB(x + dx, y + dy) & 0xFF;
                        if (gray != 0) {
                            allBlack = false;
                        }
                    }
                }
                int val = allBlack ? 0 : 255;
                output.setRGB(x, y, (val << 16) | (val << 8) | val);
            }
        }
        return output;
    }

    // --- Segment digits: แยกตัวเลขแต่ละตัวออกมาเป็นไฟล์ PNG ---
    // หลักการ: ใช้ flood fill (BFS) หา connected components ในภาพ binary
    // (foreground = 0, background = 255)
    // แต่ละ component คือกลุ่ม pixel ที่ต่อเนื่องกัน ซึ่งแทนตัวเลขแต่ละตัว
    // จากนั้นบันทึกแต่ละ component เป็นไฟล์ PNG แยกกันในโฟลเดอร์ outputDir
    public void segmentDigits(String outputDir) {
        File dir = new File(outputDir);
        if (!dir.exists())
            dir.mkdirs();

        // visited[y][x] ใช้ติดตามว่า pixel นั้น ๆ ถูกเยี่ยมชมแล้วหรือยัง
        boolean[][] visited = new boolean[height][width];

        // LinkedList เก็บ bounding boxes ของตัวเลขแต่ละตัว
        // แทน bounding box ด้วย int[4] = {minX, minY, maxX, maxY}
        LinkedList<int[]> boxes = new LinkedList<>();

        // --- Loop ผ่านทุก pixel ของภาพ ---
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int gray = img.getRGB(x, y) & 0xFF;
                if (gray == 255 || visited[y][x]) // ข้าม pixel background (255 = ขาว) หรือ pixel ที่ถูกเยี่ยมชมแล้ว
                    continue;

                int minX = width, minY = height, maxX = 0, maxY = 0;

                // ใช้ queue สำหรับ BFS (flood fill)
                LinkedList<int[]> queue = new LinkedList<>();
                queue.add(new int[] { x, y });

                while (!queue.isEmpty()) { // --- BFS flood fill ---
                    int[] p = queue.poll();
                    int px = p[0], py = p[1];
                    if (px < 0 || px >= width || py < 0 || py >= height)
                        continue;
                    if (visited[py][px])
                        continue;
                    int g = img.getRGB(px, py) & 0xFF;
                    if (g == 255)
                        continue;

                    visited[py][px] = true; // ทำเครื่องหมาย pixel ว่าเยี่ยมชมแล้ว

                    // อัปเดต bounding box ของ component
                    minX = Math.min(minX, px);
                    maxX = Math.max(maxX, px);
                    minY = Math.min(minY, py);
                    maxY = Math.max(maxY, py);

                    // เพิ่ม pixel รอบ ๆ เข้า queue สำหรับ BFS
                    queue.add(new int[] { px + 1, py });
                    queue.add(new int[] { px - 1, py });
                    queue.add(new int[] { px, py + 1 });
                    queue.add(new int[] { px, py - 1 });
                }

                if (minX < maxX && minY < maxY) {
                    boxes.add(new int[] { minX, minY, maxX, maxY });
                }
            }
        }

        // --- Merge intersecting boxes ---
        // บางครั้งตัวเลขอาจเชื่อมกันเล็กน้อย ทำให้ flood fill แยกออกเป็นหลาย box
        // เราจะ merge box ที่ intersect กันเพื่อให้แต่ละตัวเลขเป็น 1 box
        boolean merged;
        do {
            merged = false;
            for (int i = 0; i < boxes.size(); i++) {
                for (int j = i + 1; j < boxes.size(); j++) {
                    if (intersects(boxes.get(i), boxes.get(j))) {
                        boxes.set(i, merge(boxes.get(i), boxes.get(j)));
                        boxes.remove(j);
                        merged = true;
                        break;
                    }
                }
                if (merged)
                    break;
            }
        } while (merged);

        // --- Sort boxes ตามตำแหน่ง X (ซ้ายไปขวา) ---
        boxes.sort((a, b) -> Integer.compare(a[0], b[0]));

        // --- Save digits ---
        int saveIndex = 0;
        for (int[] b : boxes) {
            try {
                // ตัด subimage ตาม bounding box
                BufferedImage digit = img.getSubimage(b[0], b[1], b[2] - b[0] + 1, b[3] - b[1] + 1);
                File out = new File(dir, "digit" + (saveIndex++) + ".png");
                ImageIO.write(digit, "png", out);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // --- ตรวจสอบว่า box สอง box ตัดกันหรือไม่ ---
    private boolean intersects(int[] a, int[] b) {
        return !(b[2] < a[0] || b[0] > a[2] || b[3] < a[1] || b[1] > a[3]);
    }

    // --- merge 2 box ที่ intersect กัน ---
    private int[] merge(int[] a, int[] b) {
        return new int[] {
                Math.min(a[0], b[0]), Math.min(a[1], b[1]),
                Math.max(a[2], b[2]), Math.max(a[3], b[3])
        };
    }

    // --- Invert image ---
    // เปลี่ยนสีขาวเป็นดำ, ดำเป็นขาว (ใช้กับ template digits) เพื่อให้ตรงกับภาพหลัก
    public void invert() {
        int w = img.getWidth();
        int h = img.getHeight();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = img.getRGB(x, y);
                int r = 255 - ((rgb >> 16) & 0xFF);
                int g = 255 - ((rgb >> 8) & 0xFF);
                int b = 255 - (rgb & 0xFF);
                int val = (0xFF << 24) | (r << 16) | (g << 8) | b;
                img.setRGB(x, y, val);
            }
        }
    }

    // --- Load templates ---
    // อ่าน template digit (PNG) -> เก็บเป็น binary matrix (1 = black, 0 = white)
    public Map<Integer, int[][]> loadTemplates(String templateFolder, int threshold) {
        Map<Integer, int[][]> templates = new HashMap<>();
        File folder = new File(templateFolder);
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
        if (files == null)
            return templates;

        Arrays.sort(files);

        for (File f : files) {
            try {
                BufferedImage tImg = ImageIO.read(f);
                int h = tImg.getHeight();
                int w = tImg.getWidth();
                int[][] bin = new int[h][w];
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int gray = tImg.getRGB(x, y) & 0xFF;
                        bin[y][x] = (gray < threshold) ? 1 : 0;
                    }
                }
                String name = f.getName().replaceAll("[^0-9]", "");
                if (name.isEmpty())
                    continue; // skip ถ้าไม่มีเลข
                int digit = Integer.parseInt(name);
                templates.put(digit, bin);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return templates;
    }

    // --- Convert image to binary matrix ---
    private int[][] imageToBinaryMatrix(BufferedImage digitImg, int threshold) {
        int h = digitImg.getHeight();
        int w = digitImg.getWidth();
        int[][] bin = new int[h][w];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                int gray = digitImg.getRGB(x, y) & 0xFF;
                bin[y][x] = (gray < threshold) ? 1 : 0;
            }
        return bin;
    }

    // --- Resize digit to match template size ---
    // หลักการ: scale digit ให้พอดีกับ template, ใช้ nearest-neighbor
    private int[][] resizeToTemplate(int[][] src, int targetW, int targetH) {
        int srcH = src.length;
        int srcW = src[0].length;

        double scale = Math.min((double) targetW / srcW, (double) targetH / srcH);
        int newW = (int) Math.round(srcW * scale);
        int newH = (int) Math.round(srcH * scale);

        int[][] resized = new int[targetH][targetW];

        // Fill background
        for (int y = 0; y < targetH; y++)
            for (int x = 0; x < targetW; x++)
                resized[y][x] = 0;

        // Copy scaled digit (nearest neighbor)
        for (int y = 0; y < newH; y++) {
            for (int x = 0; x < newW; x++) {
                int srcX = (int) Math.min(srcW - 1, x / scale);
                int srcY = (int) Math.min(srcH - 1, y / scale);
                resized[(targetH - newH) / 2 + y][(targetW - newW) / 2 + x] = src[srcY][srcX];
            }
        }

        return resized;
    }

    // --- Hamming distance ---
    // หลักการ: เปรียบเทียบความแตกต่างระหว่างสองภาพ binary matrix (0/1)
    // แต่ละ pixel จะถูกนับเป็น 1 ถ้าไม่เหมือนกัน
    // ผลรวมทั้งหมด = Hamming distance = ตัวชี้วัดความเหมือน/ต่างของ digit กับ
    // template
    private int Hamming_distance(int[][] a, int[][] b) {
        int diff = 0;
        for (int y = 0; y < a.length; y++)
            for (int x = 0; x < a[0].length; x++)
                if (a[y][x] != b[y][x])
                    diff++;
        return diff;
    }

    /*
     * 💡 จุดเด่นและเหตุผลที่ใช้ Hamming distance:
     * 
     * 1. **เรียบง่ายและเร็ว:**
     * - เพียงแค่ loop matrix แล้วนับ pixel ที่ต่างกัน → ไม่ต้องคำนวณค่า float
     * หรือใช้ convolution
     * 
     * 2. **เหมาะกับ digit recognition แบบ binary:**
     * - digit ถูกแปลงเป็น 0/1 (background/foreground)
     * - Hamming distance สามารถสะท้อนความใกล้เคียงของรูปตัวเลขได้ตรงไปตรงมา
     * 
     * 3. **ปรับเปรียบเทียบ template ได้ง่าย:**
     * - สามารถใช้ template ของแต่ละตัวเลขแล้วเลือกตัวเลขที่ Hamming distance
     * น้อยที่สุด → match digit
     * 
     * 4. **ทนต่อ noise เล็กน้อย:**
     * - pixel บางจุดผิดเพี้ยนเล็กน้อยจะเพิ่มค่า diff นิดเดียว แต่ตัวเลขส่วนใหญ่ยัง
     * match ได้
     * 
     * 5. **ใช้ร่วมกับ resize/align:**
     * - ก่อนเปรียบเทียบ มัก resize digit ให้เท่ากับ template → ทำให้ Hamming
     * distance ใช้เปรียบเทียบได้แม่นยำ
     */

    // --- Match all digits ---
    // สำหรับ debug: ตรวจทุก digit image, เปรียบเทียบกับทุก template,
    // เลือก templateที่ใกล้เคียงที่สุด
    public String matchAllDigits(String digitsFolder, Map<Integer, int[][]> templates, int threshold) {
        File folder = new File(digitsFolder);
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
        if (files == null || files.length == 0) {
            System.out.println("No digit images found in folder: " + digitsFolder);
            return "";
        }

        Arrays.sort(files);

        StringBuilder result = new StringBuilder();

        for (File f : files) {
            try {
                BufferedImage digitImg = ImageIO.read(f);
                int[][] digitBin = imageToBinaryMatrix(digitImg, threshold);

                int bestDigit = -1;
                int bestScore = Integer.MAX_VALUE;

                System.out.println("\nChecking file: " + f.getName());
                for (Map.Entry<Integer, int[][]> entry : templates.entrySet()) {
                    int templateDigit = entry.getKey();
                    int[][] templateBin = entry.getValue();
                    int[][] resizedDigit = resizeToTemplate(digitBin, templateBin[0].length, templateBin.length);
                    int score = Hamming_distance(resizedDigit, templateBin);
                    System.out.println("  Score with template " + templateDigit + ": " + score);

                    if (score < bestScore) {
                        bestScore = score;
                        bestDigit = templateDigit;
                    }
                }

                result.append(bestDigit); // เพิ่มตัวเลขที่ match ได้
                System.out.println("  -> Best match: " + bestDigit + " (score: " + bestScore + ")");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return result.toString();
    }

}
