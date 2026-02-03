package Lab_Week8;

import java.util.ArrayList;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import javax.imageio.ImageIO;
 
public class DIP_Lab8 { // 66050170
    public static void main(String[] args) {

        ImageManager im = new ImageManager();
        im.read("Lab_Week8/images/MandrilB.bmp");

        StructuringElement se = new StructuringElement(3, 3, new Point(1, 1));
        for (int i = 0; i < se.height; i++) {
            for (int j = 0; j < se.width; j++) {
                se.elements[j][i] = 255;
            }
        }

        // Erosion
        im.erosion(se);
        im.write("Lab_Week8/images/mandril_erosion.bmp");
        im.restoreToOriginal();

        // Dilation
        im.dilation(se);
        im.write("Lab_Week8/images/mandril_dilation.bmp");
        im.restoreToOriginal();


        // Quest 012: Boundary Extraction = Original - Erosion
        im.boundaryExtraction(se);
        im.write("Lab_Week8/images/mandril_boundary.bmp");
    }
}

// Structuring Elements
class StructuringElement {
    public int[][] elements;

    public int width, height;
    public Point origin;

    public ArrayList<Point> ignoreElements;

    public StructuringElement(int width, int height, Point origin) {
        this.width = width;
        this.height = height;

        if (origin.x < 0 || origin.x >= width || origin.y < 0 || origin.y >= height) {
            this.origin = new Point();
        } else {
            this.origin = new Point(origin);
        }

        ignoreElements = new ArrayList<>();
        elements = new int[width][height];
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

    // Erosion
    public void erosion(StructuringElement se) {
        if (img == null)
            return;

        convertToGrayscale();

        BufferedImage tempBuf = new BufferedImage(width, height, img.getType());

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boolean isEroded = true;
                int min = Integer.MAX_VALUE;

                se_check: for (int i = y - se.origin.y; i < y + se.height - se.origin.y; i++) {
                    for (int j = x - se.origin.x; j < x + se.width - se.origin.x; j++) {
                        int seCurrentX = j - (x - se.origin.x);
                        int seCurrentY = i - (y - se.origin.y);
                        if (i >= 0 && i < height && j >= 0 && j < width) {
                            if (!se.ignoreElements.contains(new Point(seCurrentX, seCurrentY))) {
                                int color = img.getRGB(j, i);
                                int gray = color & 0xff;

                                if (se.elements[seCurrentX][seCurrentY] != gray) {
                                    isEroded = false;
                                    break se_check;
                                } else if (min > gray)
                                    min = gray;
                            }
                        } else {
                            isEroded = false;
                            break se_check;
                        }
                    }
                }

                int newGray = 0;

                if (isEroded) {
                    newGray = min;
                }

                int newColor = (newGray << 16) | (newGray << 8) | newGray;
                tempBuf.setRGB(x, y, newColor);
            }
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                img.setRGB(x, y, tempBuf.getRGB(x, y));
            }
        }
    }

    
    // Dilation
    public void dilation(StructuringElement se) {
        if (img == null)
        return;
        
        convertToGrayscale();
        
        BufferedImage tempBuf = new BufferedImage(width, height, img.getType());
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boolean isDilated = false;
                
                se_check: for (int i = y - (se.height - se.origin.y - 1); i < y + se.height -
                        (se.height - se.origin.y - 1); i++) {
                    for (int j = x - (se.width - se.origin.x - 1); j < x + se.width -
                    (se.width - se.origin.x - 1); j++) {
                        int seCurrentX = se.width - (j - x + se.origin.x) - 1;
                        int seCurrentY = se.height - (i - y + se.origin.y) - 1;
                        
                        if (i >= 0 && i < height && j >= 0 && j < width) {
                            if (!se.ignoreElements.contains(new Point(seCurrentX, seCurrentY))) {
                                int color = img.getRGB(j, i);
                                int gray = color & 0xff;
                                
                                if (se.elements[seCurrentX][seCurrentY] == gray) {
                                    isDilated = true;
                                    break se_check;
                                }
                            }
                        } else {
                            isDilated = false;
                            break se_check;
                        }
                    }
                }
                
                if (isDilated) {
                    int max = Integer.MIN_VALUE;
                    for (int i = y - (se.height - se.origin.y - 1); i < y + se.height -
                    (se.height - se.origin.y - 1); i++) {
                        for (int j = x - (se.width - se.origin.x - 1); j < x + se.width -
                        (se.width - se.origin.x - 1); j++) {
                            if (i >= 0 && i < height && j >= 0 && j < width) {
                                int color = img.getRGB(j, i);
                                int gray = color & 0xff;
                                
                                if (max < gray)
                                max = gray;
                            }
                        }
                    }

                    int newGray = max;
                    int newColor = (newGray << 16) | (newGray << 8) | newGray;
                    tempBuf.setRGB(x, y, newColor);
                }
            }
        }
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                img.setRGB(x, y, tempBuf.getRGB(x, y));
            }
        }
    }
    
    public void convertToGrayscale() {
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

    // Boundary extraction
    public void boundaryExtraction(StructuringElement se) {
        if (img == null)
            return;

        BufferedImage temp = new BufferedImage(width, height, img.getType());
        BufferedImage eroded = new BufferedImage(width, height, img.getType());

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                temp.setRGB(x, y, img.getRGB(x, y));
            }
        }

        erosion(se);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                eroded.setRGB(x, y, img.getRGB(x, y));
            }
        }

        img = temp;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int grayOrig = img.getRGB(x, y) & 0xff;
                int grayErod = eroded.getRGB(x, y) & 0xff;
                int diff = grayOrig - grayErod;
                diff = diff < 0 ? 0 : diff;
                int newColor = (diff << 16) | (diff << 8) | diff;
                img.setRGB(x, y, newColor);
            }
        }
    }
}
