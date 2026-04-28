
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import javax.imageio.ImageIO;

public class Main {

    public static void main(String[] args) {
        ImageProcessor im = new ImageProcessor();


        // ขั้นแรกเริ่มจากให้ระบบไปหาไฟล์รูปในโฟลเดอร์ ./raw ก่อน
        // ถ้าในโฟลเดอร์มีหลายรูป ระบบจะเรียงชื่อไฟล์แล้วหยิบรูปแรกมาเข้า process
        File input = findInputImage("./raw");
        if (input == null || !im.read(input.getPath())) {
            return;
        }

        // ขั้นที่ 2 รูปนี้ไม่มี noise เลยไม่ต้อง blur หรือ remove noise แล้วเหมือนตอน Final เราข้ามาหามุมกระดาษเลย
        // สิ่งที่ต้องรู้ก่อนคือกระดาษอยู่ตรงไหน เพราะรูปถ่ายเอียงและมีพื้นสนามติดมาด้วย เราเลยเอารูปเข้า func. findPaperCorners ก่อน
        // findPaperCorners จะหามุมกระดาษทั้ง 4 มุม แล้วเซฟรูป check ไว้ที่ ./output/step0_detectedCorners.jpg
        double[][] src = im.findPaperCorners();
        if (src == null) {
            System.out.println("Cannot find paper corners in the image");
            return;
        }
        im.writeCornerPreview(src, "./output/step0_detectedCorners.jpg");

        double[][] dst = {
                { 0, 0 }, { 800, 0 }, { 800, 600 }, { 0, 600 }
        };
        int outW = (int) dst[1][0];
        int outH = (int) dst[2][1];
        int margin = 20;

        // ขั้นที่  3 เอามุมกระดาษจากรูปจริงไปเทียบกับมุมกระดาษปลายทางขนาด 800x600 ที่เราอยากได้
        // จากนั้นคำนวณ Homography เพื่อดึงกระดาษเอียง ๆ ให้กลายเป็นภาพตรงเหมือนสแกน
        double[] H = im.HomographyMatrix(src, dst);

        im.warpPerspective(H, outW, outH);
        im.write("./output/step1_warped.jpg");

        // ขั้นที่ 4 แปลงเป็น grayscale เพราะตอนอ่านเลขเราไม่สนใจสีแล้ว
        // เราสนแค่ว่าส่วนไหนมืดเป็นตัวเลข และส่วนไหนสว่างเป็นพื้นกระดาษ
        im.toGrayscale();
        im.keepOnlyPaperRegion(outW, outH, margin);
        im.write("./output/step2_toGrayscale.jpg");

        // ขั้นที่ 5 ทำ binary threshold ให้ภาพเหลือแค่ขาวกับดำ
        // pixel ที่มืดกว่า 170 จะกลายเป็นสีดำ ส่วนที่เหลือจะเป็นสีขาว
        im.binaryThreshold(170);
        im.write("./output/step3_fixedThreshold.jpg");

        // ขั้นที่ 6 ล้างขอบกระดาษรอบนอกอีกครั้ง เพื่อให้ตอนหา component ไม่ติดขอบภาพเข้ามา
        im.keepOnlyPaperRegion(outW, outH, margin);
        im.write("./output/step4_roiCrop.jpg");

        // ขั้นที่ 7 หา component สีดำแต่ละกลุ่ม แล้ว crop ออกมาเป็น digit0.png, digit1.png ...
        // หลังจากนั้นเรียงจากซ้ายไปขวา เพื่อให้ลำดับเลขเหมือนที่เห็นในภาพจริง
        String digitDir = "./output/digits_output";
        im.extractDigitImages(digitDir);

        // ขั้นที่ 8 อ่านเลขแต่ละตัวด้วย segment detection และ structural rules
        // ผลลัพธ์สุดท้ายจะเป็นการเอาเลขแต่ละหลักมาต่อกัน เช่น 184325
        String result = im.recognizeDigitsBySegmentDetection(digitDir, 128);
        System.out.println("Final output: " + result);
    }

    private static File findInputImage(String rawFolder) {
        File folder = new File(rawFolder);
        File[] files = folder.listFiles((dir, name) -> { // หาเฉพาะไฟล์รูปที่นามสกุลเป็น bmp jpg jpeg png
            String lower = name.toLowerCase();
            return lower.endsWith(".bmp")
                    || lower.endsWith(".jpg")
                    || lower.endsWith(".jpeg")
                    || lower.endsWith(".png");
        });

        if (files == null || files.length == 0) {
            System.out.println("No input image found in folder: " + rawFolder); // ถ้าไม่เจอรูปเลย จะ print บอกแล้ว return null
            return null;
        }

        Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        return files[0];
    }
}

class ImageProcessor {
    private int width, height;
    private BufferedImage image;

    public boolean read(String fileName) {
        try {
            image = ImageIO.read(new File(fileName));
            if (image == null) {
                System.out.println("Cannot read image file: " + fileName);
                return false;
            }

            width = image.getWidth();
            height = image.getHeight();
            int depth = image.getColorModel().getPixelSize();

            System.out.println("Image " + fileName + " read: " + width + "x" + height + " (" + depth + " bpp)");
            return true;
        } catch (IOException exception) {
            System.out.println(exception);
            return false;
        }
    }

    public boolean write(String fileName) {
        try {
            File out = new File(fileName);
            File parent = out.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            ImageIO.write(toRgbImage(image), getImageFormat(fileName), out);
            System.out.println("Image " + fileName + " written!");
            return true;
        } catch (IOException | NullPointerException exception) {
            System.out.println(exception);
            return false;
        }
    }

    // การหามุมกระดาษ
    // ระบบจะหา pixel ที่น่าจะเป็นกระดาษด้วยเงื่อนไขง่าย ๆ คือสว่างพอและไม่ค่อยมีสี โดยระบบเราจะไล่ดูทุก pixel ในภาพแล้วถามว่า pixel นี้น่าจะเป็นกระดาษไหมถ้าใช่ จะเรียก floodFillPaperComponent(x, y, visited)
    // จากนั้น flood fill เพื่อหากลุ่มกระดาษที่ใหญ่ที่สุด แล้วใช้ตำแหน่งสุดขอบของกลุ่มนั้นเป็นมุมทั้ง 4 ของกระดาษ
    public double[][] findPaperCorners() {
        boolean[][] visited = new boolean[height][width];
        PaperComponent best = null;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (visited[y][x] || !isPaperPixel(x, y))
                    continue;

                PaperComponent comp = floodFillPaperComponent(x, y, visited);
                if (best == null || comp.area > best.area) {
                    best = comp;
                }
            }
        }

        if (best == null || best.area < width * height * 0.05) {
            return null;
        }

        double[][] corners = {
                { best.topLeftX, best.topLeftY },
                { best.topRightX, best.topRightY },
                { best.bottomRightX, best.bottomRightY },
                { best.bottomLeftX, best.bottomLeftY }
        };

        System.out.println("Detected paper corners:");
        for (int i = 0; i < corners.length; i++) {
            System.out.println("  P" + i + ": (" + corners[i][0] + ", " + corners[i][1] + ")");
        }

        return corners;
    }

    private PaperComponent floodFillPaperComponent(int startX, int startY, boolean[][] visited) {
        PaperComponent comp = new PaperComponent();
        LinkedList<int[]> queue = new LinkedList<>();
        queue.add(new int[] { startX, startY });

        while (!queue.isEmpty()) {
            int[] p = queue.poll();
            int x = p[0];
            int y = p[1];

            if (x < 0 || x >= width || y < 0 || y >= height)
                continue;
            if (visited[y][x] || !isPaperPixel(x, y))
                continue;

            visited[y][x] = true;
            comp.addPoint(x, y);

            queue.add(new int[] { x + 1, y });
            queue.add(new int[] { x - 1, y });
            queue.add(new int[] { x, y + 1 });
            queue.add(new int[] { x, y - 1 });
        }

        return comp;
    }


    // อย่างที่บอกไป isPaperPixel(x, y) ตัวนี้จะเป้นตัวตัดสินว่า pixel นี้เป็นกระดาษหรือป่าว
    private boolean isPaperPixel(int x, int y) {
        int rgb = image.getRGB(x, y);
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        int max = Math.max(red, Math.max(green, blue));
        int min = Math.min(red, Math.min(green, blue));
        int brightness = (red + green + blue) / 3;
        double saturation = max == 0 ? 0.0 : (double) (max - min) / max;

        return brightness > 125 && saturation < 0.35; // โดยเงื่อนไขที่ใช้คือความสว่างต้องมากกว่า 125 และความอิ่มตัวของสีต้องน้อยกว่า 0.35 ซึ่งจะช่วยกรองเอาเฉพาะ pixel ที่ดูเหมือนกระดาษออกมาได้
    }

    public void writeCornerPreview(double[][] corners, String fileName) {
        try {
            BufferedImage preview = new BufferedImage(width, height, image.getType());
            for (int y = 0; y < height; y++)
                for (int x = 0; x < width; x++)
                    preview.setRGB(x, y, image.getRGB(x, y));

            for (double[] corner : corners) {
                drawMarker(preview, (int) Math.round(corner[0]), (int) Math.round(corner[1]));
            }

            File out = new File(fileName);
            File parent = out.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            ImageIO.write(toRgbImage(preview), getImageFormat(fileName), out);
            System.out.println("Image " + fileName + " written!");
        } catch (IOException | NullPointerException exception) {
            System.out.println(exception);
        }
    }

    private String getImageFormat(String fileName) { // อันนี้ทำมาเไว้เป็นตัวช่วยตอนเซฟรูป ว่าไฟล์ที่เซฟเป็นนามสกุลอะไร เพื่อให้ ImageIO.write รู้ว่าจะใช้ format ไหนในการเซฟ
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "jpg";
        }
        if (lower.endsWith(".png")) {
            return "png";
        }
        return "bmp";
    }

    private BufferedImage toRgbImage(BufferedImage source) { // ส่วนอันนี้ไว้แปลงภาพให้เป็น RGB ก่อนเซฟ เพราะบาง format โดยเฉพาะ JPG ชอบมีปัญหาถ้าภาพไม่ใช่ RGB
        if (source.getType() == BufferedImage.TYPE_INT_RGB) {
            return source;
        }

        BufferedImage rgb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                rgb.setRGB(x, y, source.getRGB(x, y));
            }
        }
        return rgb;
    }

    private void drawMarker(BufferedImage preview, int centerX, int centerY) {
        int red = Color.RED.getRGB();
        for (int d = -12; d <= 12; d++) {
            setPixelIfInside(preview, centerX + d, centerY, red);
            setPixelIfInside(preview, centerX, centerY + d, red);
        }
    }

    private void setPixelIfInside(BufferedImage preview, int x, int y, int color) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            preview.setRGB(x, y, color);
        }
    }

    private static class PaperComponent { // คลาสนี้เอาไว้เก็บข้อมูลของกลุ่ม pixel ที่น่าจะเป็นกระดาษ ซึ่งเราจะใช้ข้อมูลนี้ในการหามุมกระดาษทั้ง 4 มุม
        int area = 0;
        int topLeftX, topLeftY;
        int topRightX, topRightY;
        int bottomRightX, bottomRightY;
        int bottomLeftX, bottomLeftY;
        int minSum = Integer.MAX_VALUE;
        int maxSum = Integer.MIN_VALUE;
        int minDiff = Integer.MAX_VALUE;
        int maxDiff = Integer.MIN_VALUE;

        void addPoint(int x, int y) {
            area++;
            int sum = x + y;
            int diff = x - y;

            // ใช้ x+y และ x-y ช่วยแยกมุมของกระดาษออกเป็นซ้ายบน ขวาบน ขวาล่าง และซ้ายล่าง
            if (sum < minSum) {
                minSum = sum;
                topLeftX = x;
                topLeftY = y;
            }
            if (sum > maxSum) {
                maxSum = sum;
                bottomRightX = x;
                bottomRightY = y;
            }
            if (diff < minDiff) {
                minDiff = diff;
                bottomLeftX = x;
                bottomLeftY = y;
            }
            if (diff > maxDiff) {
                maxDiff = diff;
                topRightX = x;
                topRightY = y;
            }
        }
    }

    // แปลงภาพสีให้เป็นภาพเทา โดยเอาค่า R G B มาคิดเป็นความสว่างของ pixel
    // ขั้นนี้ทำให้การ threshold ง่ายขึ้น เพราะเหลือแค่ค่าความสว่าง 0-255
    public void toGrayscale() {
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;
                int gray = (int) (0.3 * red + 0.59 * green + 0.11 * blue);
                int newPixel = (gray << 16) | (gray << 8) | gray;
                image.setRGB(x, y, newPixel);
            }
    }

    // ลบขอบรอบนอกของกระดาษให้เป็นสีขาว
    // เพราะขอบภาพหรือเงาที่ติดมานิด ๆ อาจทำให้ระบบเข้าใจผิดว่าเป็นตัวเลข
    public void keepOnlyPaperRegion(int paperWidth, int paperHeight, int margin) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (x < margin || x >= paperWidth - margin || y < margin || y >= paperHeight - margin) {
                    image.setRGB(x, y, 0xFFFFFF);
                }
            }
        }
    }

    // เปลี่ยนภาพเทาให้เหลือแค่ขาวกับดำ โดยดำคือส่วนที่น่าจะเป็นตัวเลข ขาวคือพื้นหลัง
    public void binaryThreshold(int threshold) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int gray = image.getRGB(x, y) & 0xFF;
                int binaryColor = (gray < threshold) ? 0x000000 : 0xFFFFFF;
                image.setRGB(x, y, binaryColor);
            }
        }
    }

    // สร้างสมการจากมุมกระดาษ 4 จุด เพื่อหา matrix ที่ใช้แปลงภาพเอียงให้ตรง
    // src คือมุมในรูปจริง ส่วน dst คือมุมปลายทางของกระดาษขนาด 800x600
    public double[] HomographyMatrix(double[][] src, double[][] dst) {
        double[][] A = new double[8][8];
        double[] b = new double[8];
        for (int i = 0; i < 4; i++) {
            double sx = src[i][0];
            double sy = src[i][1];
            double dx = dst[i][0];
            double dy = dst[i][1];

            A[2 * i][0] = sx;
            A[2 * i][1] = sy;
            A[2 * i][2] = 1;
            A[2 * i][3] = 0;
            A[2 * i][4] = 0;
            A[2 * i][5] = 0;
            A[2 * i][6] = -sx * dx;
            A[2 * i][7] = -sy * dx;

            A[2 * i + 1][0] = 0;
            A[2 * i + 1][1] = 0;
            A[2 * i + 1][2] = 0;
            A[2 * i + 1][3] = sx;
            A[2 * i + 1][4] = sy;
            A[2 * i + 1][5] = 1;
            A[2 * i + 1][6] = -sx * dy;
            A[2 * i + 1][7] = -sy * dy;

            b[2 * i] = dx;
            b[2 * i + 1] = dy;
        }
        return solveLinearSystem(A, b);
    }

    // แก้ระบบสมการเพื่อหาค่าทั้ง 8 ตัวของ Homography
    // ค่าตัวที่ 9 fix เป็น 1 เพราะ matrix แบบนี้คูณ scale แล้วให้ผลเทียบเท่ากัน
    public double[] solveLinearSystem(double[][] A, double[] b) {
        int n = b.length;
        for (int i = 0; i < n; i++) {
            int pivotRow = i;
            for (int j = i + 1; j < n; j++) {
                if (Math.abs(A[j][i]) > Math.abs(A[pivotRow][i])) {
                    pivotRow = j;
                }
            }

            double[] row = A[i];
            A[i] = A[pivotRow];
            A[pivotRow] = row;

            double value = b[i];
            b[i] = b[pivotRow];
            b[pivotRow] = value;

            for (int r = i + 1; r < n; r++) {
                double factor = A[r][i] / A[i][i];
                b[r] -= factor * b[i];
                for (int c = i; c < n; c++) {
                    A[r][c] -= factor * A[i][c];
                }
            }
        }

        double[] x = new double[n];
        for (int i = n - 1; i >= 0; i--) {
            double sum = 0.0;
            for (int j = i + 1; j < n; j++) {
                sum += A[i][j] * x[j];
            }
            x[i] = (b[i] - sum) / A[i][i];
        }

        double[] H = new double[9];
        System.arraycopy(x, 0, H, 0, 8);
        H[8] = 1;

        return H;
    }

    public static double[] invertHomographyMatrix(double[] H) {
        double[] inv = new double[9];

        double det = H[0] * (H[4] * H[8] - H[5] * H[7])
                - H[1] * (H[3] * H[8] - H[5] * H[6])
                + H[2] * (H[3] * H[7] - H[4] * H[6]);

        if (det == 0)
            throw new IllegalArgumentException("Matrix is not invertible");

        double invDet = 1.0 / det;
        inv[0] = invDet * (H[4] * H[8] - H[5] * H[7]);
        inv[1] = invDet * (H[2] * H[7] - H[1] * H[8]);
        inv[2] = invDet * (H[1] * H[5] - H[2] * H[4]);

        inv[3] = invDet * (H[5] * H[6] - H[3] * H[8]);
        inv[4] = invDet * (H[0] * H[8] - H[2] * H[6]);
        inv[5] = invDet * (H[2] * H[3] - H[0] * H[5]);

        inv[6] = invDet * (H[3] * H[7] - H[4] * H[6]);
        inv[7] = invDet * (H[1] * H[6] - H[0] * H[7]);
        inv[8] = invDet * (H[0] * H[4] - H[1] * H[3]);

        return inv;
    }

    public void warpPerspective(double[] H, int outW, int outH) {
        // สร้างภาพใหม่ขนาด 800x600 แล้ว map แต่ละ pixel กลับไปหา pixel ในรูปต้นฉบับ
        // วิธีนี้ทำให้กระดาษที่เอียงอยู่ถูกดึงให้ตรงก่อนเข้าสู่ขั้นตอนอ่านเลข
        BufferedImage out = new BufferedImage(outW, outH, image.getType());

        double[] invH = invertHomographyMatrix(H);

        for (int y = 0; y < outH; y++) {
            for (int x = 0; x < outW; x++) {

                double[] src = mapPointWithHomography(invH, x, y);

                int sx = (int) Math.round(src[0]);
                int sy = (int) Math.round(src[1]);

                if (sx >= 0 && sx < width && sy >= 0 && sy < height) {
                    Color color = new Color(image.getRGB(sx, sy));
                    out.setRGB(x, y, color.getRGB());
                } else {
                    out.setRGB(x, y, Color.BLACK.getRGB());
                }
            }
        }

        image = out;
        width = outW;
        height = outH;
    }

    private double[] mapPointWithHomography(double[] H, double x, double y) {
        double tx = H[0] * x + H[1] * y + H[2];
        double ty = H[3] * x + H[4] * y + H[5];
        double scale = H[6] * x + H[7] * y + H[8];

        double sx = tx / scale;
        double sy = ty / scale;

        return new double[] { sx, sy };
    }

    public void extractDigitImages(String outputDir) {
        // หา component สีดำในภาพ binary ซึ่งแต่ละ component คือกลุ่ม pixel ที่ติดกัน
        // ถ้าขนาดดูเหมือนตัวเลขก็ crop ออกมาเก็บไว้ใน output/digits_output
        File dir = new File(outputDir);
        if (!dir.exists())
            dir.mkdirs();
        File[] oldDigitFiles = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".png"));
        if (oldDigitFiles != null) {
            for (File oldDigitFile : oldDigitFiles) {
                oldDigitFile.delete();
            }
        }

        boolean[][] visited = new boolean[height][width];

        LinkedList<int[]> boxes = new LinkedList<>();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int gray = image.getRGB(x, y) & 0xFF;
                if (gray == 255 || visited[y][x])
                    continue;

                int minX = width, minY = height, maxX = 0, maxY = 0;

                LinkedList<int[]> queue = new LinkedList<>();
                queue.add(new int[] { x, y });

                while (!queue.isEmpty()) {
                    int[] point = queue.poll();
                    int pointX = point[0], pointY = point[1];
                    if (pointX < 0 || pointX >= width || pointY < 0 || pointY >= height)
                        continue;
                    if (visited[pointY][pointX])
                        continue;
                    int pixelValue = image.getRGB(pointX, pointY) & 0xFF;
                    if (pixelValue == 255)
                        continue;

                    visited[pointY][pointX] = true;

                    minX = Math.min(minX, pointX);
                    maxX = Math.max(maxX, pointX);
                    minY = Math.min(minY, pointY);
                    maxY = Math.max(maxY, pointY);

                    queue.add(new int[] { pointX + 1, pointY });
                    queue.add(new int[] { pointX - 1, pointY });
                    queue.add(new int[] { pointX, pointY + 1 });
                    queue.add(new int[] { pointX, pointY - 1 });
                }

                int boxWidth = maxX - minX + 1;
                int boxHeight = maxY - minY + 1;
                if (minX < maxX && minY < maxY && isDigitCandidate(boxWidth, boxHeight)) {
                    boxes.add(new int[] { minX, minY, maxX, maxY });
                }
            }
        }

        boolean merged;
        // บางเลขอาจถูกแยกเป็นหลาย component เช่นเลข 1 ที่มีหัวกับฐาน
        // จึงรวมกล่องที่ซ้อนกันหรืออยู่ใกล้กันมากให้กลับมาเป็นเลขตัวเดียว
        do {
            merged = false;
            for (int i = 0; i < boxes.size(); i++) {
                for (int j = i + 1; j < boxes.size(); j++) {
                    if (boxesOverlap(boxes.get(i), boxes.get(j)) || likelySameDigit(boxes.get(i), boxes.get(j))) {
                        boxes.set(i, mergeBoxes(boxes.get(i), boxes.get(j)));
                        boxes.remove(j);
                        merged = true;
                        break;
                    }
                }
                if (merged)
                    break;
            }
        } while (merged);

        boxes.sort((a, b) -> Integer.compare(a[0], b[0]));

        int saveIndex = 0;
        for (int[] digitBox : boxes) {
            try {
                BufferedImage digit = image.getSubimage(digitBox[0], digitBox[1], digitBox[2] - digitBox[0] + 1,
                        digitBox[3] - digitBox[1] + 1);
                File out = new File(dir, "digit" + (saveIndex++) + ".png");
                ImageIO.write(digit, "png", out);
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
    }

    private boolean isDigitCandidate(int boxWidth, int boxHeight) {
        if (boxWidth < 8 || boxHeight < 15)
            return false;
        return boxWidth <= 120 && boxHeight <= 140;
    }

    private boolean boxesOverlap(int[] firstBox, int[] secondBox) {
        return !(secondBox[2] < firstBox[0] || secondBox[0] > firstBox[2] || secondBox[3] < firstBox[1] || secondBox[1] > firstBox[3]);
    }

    private boolean likelySameDigit(int[] firstBox, int[] secondBox) {
        int left = Math.min(firstBox[0], secondBox[0]);
        int right = Math.max(firstBox[2], secondBox[2]);
        int top = Math.min(firstBox[1], secondBox[1]);
        int bottom = Math.max(firstBox[3], secondBox[3]);
        int combinedWidth = right - left + 1;
        int combinedHeight = bottom - top + 1;
        int horizontalGap = Math.max(0, Math.max(firstBox[0], secondBox[0]) - Math.min(firstBox[2], secondBox[2]));
        int horizontalOverlap = Math.min(firstBox[2], secondBox[2]) - Math.max(firstBox[0], secondBox[0]) + 1;

        return combinedWidth <= 120 && combinedHeight <= 140 && (horizontalOverlap > 0 || horizontalGap <= 10);
    }

    private int[] mergeBoxes(int[] firstBox, int[] secondBox) {
        return new int[] {
                Math.min(firstBox[0], secondBox[0]), Math.min(firstBox[1], secondBox[1]),
                Math.max(firstBox[2], secondBox[2]), Math.max(firstBox[3], secondBox[3])
        };
    }

    private int[][] convertImageToBinaryMatrix(BufferedImage digitImage, int threshold) {
        // แปลงรูปเลข 1 รูปให้เป็น matrix 0 กับ 1
        // 1 หมายถึง pixel ดำของตัวเลข และ 0 หมายถึงพื้นหลังสีขาว
        int imageHeight = digitImage.getHeight();
        int imageWidth = digitImage.getWidth();
        int[][] binaryMatrix = new int[imageHeight][imageWidth];
        for (int y = 0; y < imageHeight; y++)
            for (int x = 0; x < imageWidth; x++) {
                int gray = digitImage.getRGB(x, y) & 0xFF;
                binaryMatrix[y][x] = (gray < threshold) ? 1 : 0;
            }
        return binaryMatrix;
    }

    public String recognizeDigitsBySegmentDetection(String digitsFolder, int threshold) {
        // อ่านรูปเลขที่ crop แล้วทีละไฟล์ตามลำดับ digit0, digit1, digit2 ...
        // แต่ละรูปจะถูก trim ให้พอดีกับตัวเลข แล้วส่งไปวัด segment A-G
        File folder = new File(digitsFolder);
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
        if (files == null || files.length == 0) {
            System.out.println("No digit images found in folder: " + digitsFolder);
            return "";
        }

        Arrays.sort(files, (a, b) -> Integer.compare(extractNumberFromFileName(a.getName()),
                extractNumberFromFileName(b.getName())));

        StringBuilder result = new StringBuilder();

        for (File digitFile : files) {
            try {
                BufferedImage digitImage = ImageIO.read(digitFile);
                int[][] digit = trimBinary(convertImageToBinaryMatrix(digitImage, threshold));
                double[] fills = measureSegmentFillRatios(digit);
                SegmentResult match = recognizeDigitFromSegments(digit, fills);

                System.out.println("\nChecking file: " + digitFile.getName());
                printSegmentDebug(fills, match);

                result.append(match.digit);
                System.out.println("  -> Best match: " + match.digit + " (segment score: " + match.score + ")");
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }

        return result.toString();
    }

    private double[] measureSegmentFillRatios(int[][] digit) {
        // แบ่งพื้นที่ของตัวเลขออกเป็น 7 โซนตามหลัก seven-segment คือ A B C D E F G
        // แต่ละค่า fill ratio คือสัดส่วน pixel ดำในโซนนั้น ยิ่งเยอะแปลว่า segment นั้นน่าจะติด
        double[][] regions = {
                { 0.20, 0.00, 0.80, 0.22 },
                { 0.62, 0.10, 1.00, 0.50 },
                { 0.62, 0.50, 1.00, 0.90 },
                { 0.20, 0.78, 0.80, 1.00 },
                { 0.00, 0.50, 0.38, 0.90 },
                { 0.00, 0.10, 0.38, 0.50 },
                { 0.20, 0.39, 0.80, 0.61 }
        };

        double[] fills = new double[regions.length];
        int h = digit.length;
        int w = digit[0].length;

        for (int i = 0; i < regions.length; i++) {
            int x1 = clamp((int) Math.round(regions[i][0] * w), 0, w - 1);
            int y1 = clamp((int) Math.round(regions[i][1] * h), 0, h - 1);
            int x2 = clamp((int) Math.round(regions[i][2] * w), x1 + 1, w);
            int y2 = clamp((int) Math.round(regions[i][3] * h), y1 + 1, h);
            int black = 0;
            int total = 0;

            for (int y = y1; y < y2; y++) {
                for (int x = x1; x < x2; x++) {
                    if (digit[y][x] == 1) {
                        black++;
                    }
                    total++;
                }
            }

            fills[i] = total == 0 ? 0.0 : (double) black / total;
        }

        return fills;
    }

    private SegmentResult recognizeDigitFromSegments(int[][] digit, double[] fills) {
        // ก่อนเทียบ pattern ปกติ จะให้ structural rules ตรวจเลขที่ font หลอกง่ายก่อน
        // ถ้าไม่เข้า rule พิเศษ ค่อยเอา segment on/off ไปเทียบกับ pattern ของเลข 0-9
        SegmentResult rule = recognizeDigitByStructuralRules(digit, fills);
        if (rule != null) {
            return rule;
        }

        boolean[] active = detectActiveSegments(fills);
        boolean[][] patterns = getSevenSegmentPatterns();
        int bestDigit = -1;
        int bestMiss = Integer.MAX_VALUE;
        double bestScore = Double.MAX_VALUE;

        for (int number = 0; number <= 9; number++) {
            int mismatch = 0;
            double score = 0.0;

            for (int i = 0; i < active.length; i++) {
                if (active[i] != patterns[number][i]) {
                    mismatch++;
                }
                score += patterns[number][i] ? (1.0 - fills[i]) : fills[i];
            }

            if (mismatch < bestMiss || (mismatch == bestMiss && score < bestScore)) {
                bestDigit = number;
                bestMiss = mismatch;
                bestScore = score;
            }
        }

        return new SegmentResult(bestDigit, active, bestScore, "segment detection");
    }

    private SegmentResult recognizeDigitByStructuralRules(int[][] digit, double[] fills) {
        // rule พิเศษช่วยแยกเลขที่ segment score อย่างเดียวอาจเดาผิด
        // เช่นเลข 1 จาก font นี้มีฐานล่าง ทำให้ดูเหมือนมีหลาย segment มากกว่าปกติ
        double aspectRatio = (double) digit[0].length / digit.length;

        if (aspectRatio < 0.42 && fills[2] > 0.70 && fills[3] > 0.80) {
            return new SegmentResult(1, detectActiveSegments(fills), 0.0, "structural rule: narrow digit with lower-right stroke and base");
        }

        if (fills[0] < 0.30 && fills[3] < 0.30 && fills[2] > 0.60
                && fills[4] > 0.45 && fills[5] > 0.65) {
            return new SegmentResult(4, detectActiveSegments(fills), 0.0, "structural rule: open top/bottom with left/right strokes");
        }

        return null;
    }

    private boolean[] detectActiveSegments(double[] fills) {
        // ถ้า fill ratio ของ segment มากกว่าหรือเท่ากับ 0.35 จะถือว่า segment นั้นเปิดอยู่
        boolean[] active = new boolean[fills.length];
        double threshold = 0.35;
        for (int i = 0; i < fills.length; i++) {
            active[i] = fills[i] >= threshold;
        }
        return active;
    }

    private void printSegmentDebug(double[] fills, SegmentResult result) {
        String[] names = getSegmentNames();
        StringBuilder fillText = new StringBuilder("  Segment fill: ");
        StringBuilder onOffText = new StringBuilder("  Segment on/off: ");

        for (int i = 0; i < names.length; i++) {
            if (i > 0) {
                fillText.append(" ");
                onOffText.append(" ");
            }
            fillText.append(names[i]).append("=").append(String.format("%.2f", fills[i]));
            onOffText.append(names[i]).append("=").append(result.active[i] ? "1" : "0");
        }

        System.out.println(fillText);
        System.out.println(onOffText);
        System.out.println("  Decision method: " + result.method);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private int[][] trimBinary(int[][] binaryMatrix) {
        // ตัดพื้นที่ขาวรอบตัวเลขออก เพื่อให้ segment detection วัดจากกรอบที่พอดีกับเลขจริง
        int matrixHeight = binaryMatrix.length;
        int matrixWidth = binaryMatrix[0].length;
        int minX = matrixWidth, minY = matrixHeight, maxX = -1, maxY = -1;

        for (int y = 0; y < matrixHeight; y++) {
            for (int x = 0; x < matrixWidth; x++) {
                if (binaryMatrix[y][x] == 1) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }

        if (maxX < minX || maxY < minY) {
            return binaryMatrix;
        }

        int[][] trimmed = new int[maxY - minY + 1][maxX - minX + 1];
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                trimmed[y - minY][x - minX] = binaryMatrix[y][x];
            }
        }

        return trimmed;
    }

    private String[] getSegmentNames() {
        return new String[] { "A", "B", "C", "D", "E", "F", "G" };
    }

    private boolean[][] getSevenSegmentPatterns() {
        // pattern มาตรฐานของเลข 0-9 ในรูปแบบ A B C D E F G
        // true คือ segment ติด และ false คือ segment ดับ
        return new boolean[][] {
                { true, true, true, true, true, true, false },
                { false, true, true, false, false, false, false },
                { true, true, false, true, true, false, true },
                { true, true, true, true, false, false, true },
                { false, true, true, false, false, true, true },
                { true, false, true, true, false, true, true },
                { true, false, true, true, true, true, true },
                { true, true, true, false, false, false, false },
                { true, true, true, true, true, true, true },
                { true, true, true, true, false, true, true }
        };
    }

    private static class SegmentResult {
        int digit;
        boolean[] active;
        double score;
        String method;

        SegmentResult(int digit, boolean[] active, double score, String method) {
            this.digit = digit;
            this.active = active;
            this.score = score;
            this.method = method;
        }
    }

    private int extractNumberFromFileName(String name) {
        String digits = name.replaceAll("[^0-9]", "");
        if (digits.isEmpty())
            return -1;
        return Integer.parseInt(digits);
    }

}

/*
 ตัวของลำดับการ smapling หาตัวเลขคือ อ่านเลขด้วย Segment Detection ก่อนเนื่องจากรอบนี้ต่างจาก Final เพราะไม่มีตัวอย่างเลขทั้งหมดเลยใช้เป้นการวัด segment A-G แล้วเทียบกับ pattern ของเลข 0-9 แทน 
 ระบบแบ่งเลขแต่ละตัวเป็น 7 โซนใน func. measureSegmentFillRatios() แล้วดูว่าแต่ละโซนมีสีดำกี่เปอร์เซ็นต์ ถ้าเกิน 35% จะถือว่า segment นั้นติดอยู่ จากนั้นก็เทียบกับ pattern ของเลข 0-9 เพื่อหาว่าเลขตัวนั้นน่าจะเป็นอะไร
 ex. Segment fill: A=0.46 B=0.61 C=0.08 D=0.47 E=0.60 F=0.11 G=0.47 -> A=1 B=1 C=0 D=1 E=1 F=0 G=1

 ตอนนี้เราจะได้ข้อมุลของเลขแต่ละตัวที่อ่านได้จาก Segment Detection มาแล้ว ซึ่งจะมีความแม่นยำค่อนข้างดี แต่ก็ยังมีโอกาสผิดพลาดอยู่บ้าง เช่น เลข 1 ที่มีฐานล่างทำให้ดูเหมือนมีหลาย segment ติด หรือเลข 4 ที่เปิดบนล่างแต่มีขีดซ้ายขวา ทำให้ดูเหมือนเลขอื่นๆ ได้

 เราเลยเพิ่ม Structural Rules เพื่อแก้ปัญหาบางเลข font ทำให้ segment เพี้ยน เช่น 1 กับ 4
 เลข 1 ในภาพนี้ไม่ได้เป็นแค่เส้นตรง มันมีฐานด้านล่าง ทำให้ segment detection ปกติอาจงง ระบบเลยมี rule เสริม ถ้าเลขแคบมาก + มีเส้นล่าง/ขวาล่างชัด → อ่านเป็น 1
 แต่ถ้าบน/ล่างเปิด แต่มีเส้นตั้งซ้าย/ขวาชัด → อ่านเป็น 4
*/