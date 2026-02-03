package Lab_Week5;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import javax.imageio.ImageIO;

public class DIP_Lab5 { //66050170
    public static void main(String[] args) {
        ImageManager im = new ImageManager();
        im.read("Lab_Week5\\images\\mandril.bmp");

        // Quest 008
        FrequencyDomainManager fft = im.getFrequencyDomain(); 

        fft.ILPF(3);
        fft.getInverse();
        im.write("Lab_Week5\\images\\mandril_ILPF_3.bmp");
        im.restoreToOriginal();
        fft.resetFrequency();

        fft.ILPF(5);
        fft.getInverse();
        im.write("Lab_Week5\\images\\mandril_ILPF_5.bmp");
        im.restoreToOriginal();
        fft.resetFrequency();

        fft.ILPF(10);
        fft.getInverse();
        im.write("Lab_Week5\\images\\mandril_ILPF_10.bmp");
        im.restoreToOriginal();
        fft.resetFrequency();

        fft.ILPF(20);
        fft.getInverse();
        im.write("Lab_Week5\\images\\mandril_ILPF_20.bmp");
        im.restoreToOriginal();
        fft.resetFrequency();

        // fft.ILPF(3) ทำลายค่าความถี่ไปแล้ว → พอ fft.ILPF(5) มันเอาค่าที่ถูก filter แล้วมา filter ซ้ำอีกครั้ง → ข้อมูลเสียหายหนัก → ภาพที่ได้กลายเป็นดำ
        // อย่าใช้ object fft ซ้ำหลังจากทำ ILPF แล้ว ให้ใส่ตัว fft.resetFrequency(); ไปด้วย
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

    public BufferedImage getImage() {
        return img;
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

    public FrequencyDomainManager getFrequencyDomain() {
        convertToGray();
        FrequencyDomainManager fft = new FrequencyDomainManager(this);
        restoreToOriginal();

        return fft;
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

}

class FrequencyDomainManager {
    Complex[][] img;
    int width, height, type;
    int imgWidth, imgHeight;
    private Complex[][] original;
    ImageManager im;

    public FrequencyDomainManager(ImageManager im) {
        this.im = im;
        imgWidth = im.width;
        imgHeight = im.height;
        width = nextPowerOf2(imgWidth);
        height = nextPowerOf2(imgHeight);
        type = im.getImage().getType();
        img = new Complex[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int color = im.getImage().getRGB(x, y);
                int gray = color & 0xff;
                img[y][x] = new Complex(gray, 0);
            }
        }

        fft2d(false);
        shifting();

        // store original
        original = new Complex[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                original[y][x] = new Complex(img[y][x].re, img[y][x].im);
            }
        }
    }

    public static int nextPowerOf2(final int a) {
        int b = 1;
        while (b < a) {
            b = b << 1;
        }
        return b;
    }

    public static Complex[] fft(Complex[] x) {
        int n = nextPowerOf2(x.length);
        // padding
        Complex[] fftArray = new Complex[n];
        for (int i = 0; i < n; i++) {
            fftArray[i] = new Complex();
            if (i < x.length) {
                fftArray[i].re = x[i].re;
                fftArray[i].im = x[i].im;
            }
        }
        // base case
        if (n == 1)
            return new Complex[] { fftArray[0] };
        // compute FFT of even terms
        Complex[] even = new Complex[n / 2];
        for (int k = 0; k < n / 2; k++) {
            even[k] = fftArray[2 * k];
        }
        Complex[] evenFFT = fft(even);
        // compute FFT of odd terms
        Complex[] odd = even; // reuse the array (to avoid n log n space)
        for (int k = 0; k < n / 2; k++) {
            odd[k] = fftArray[2 * k + 1];
        }
        Complex[] oddFFT = fft(odd);
        // combine
        Complex[] y = new Complex[n];
        for (int k = 0; k < n / 2; k++) {
            double kth = -2 * k * Math.PI / n;
            Complex wk = new Complex(Math.cos(kth), Math.sin(kth));
            y[k] = evenFFT[k].add(wk.mult(oddFFT[k]));
            y[k + n / 2] = evenFFT[k].sub(wk.mult(oddFFT[k]));
        }
        return y;
    }

    private void fft2d(boolean invert) {
        Complex[] temp = new Complex[width];
        // horizontal first
        for (int y = 0; y < height; y++) {
            for (int u = 0; u < width; u++) {
                temp[u] = new Complex();
                temp[u].re = img[y][u].re;
                temp[u].im = img[y][u].im;
            }
            if (!invert) {
                temp = fft(temp);
            } else {
                temp = ifft(temp);
            }
            for (int u = 0; u < width; u++) {
                img[y][u].re = temp[u].re;
                img[y][u].im = temp[u].im;
            }
        }
        // then vertical
        temp = new Complex[height];
        for (int x = 0; x < width; x++) {
            for (int v = 0; v < height; v++) {
                temp[v] = new Complex();
                temp[v].re = img[v][x].re;
                temp[v].im = img[v][x].im;
            }
            if (!invert) {
                temp = fft(temp);
            } else {
                temp = ifft(temp);
            }
            for (int v = 0; v < height; v++) {
                img[v][x].re = temp[v].re;
                img[v][x].im = temp[v].im;
            }
        }
    }

    // Writing images in Spectrum and Phase
    public boolean writeSpectrumLogScaled(String fileName) {

        try {
            BufferedImage tempimg = new BufferedImage(width, height, type);
            double max = Double.MIN_VALUE, min = Double.MAX_VALUE;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    double spectrum = img[y][x].length();
                    if (spectrum > max)
                        max = spectrum;
                    if (spectrum < min)
                        min = spectrum;
                }
            }

            min = min < 1.0f ? 0f : Math.log10(min);
            max = max < 1.0f ? 0f : Math.log10(max);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    double spectrum = img[y][x].length();
                    spectrum = spectrum < 1.0f ? 0f : Math.log10(spectrum);
                    spectrum = ((spectrum - min) * 255 / (max - min));

                    int color = ((int) spectrum << 16) | ((int) spectrum << 8) | (int) spectrum;
                    tempimg.setRGB(x, y, color);
                }
            }
            BufferedImage img = new BufferedImage(width, height, type);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    img.setRGB(x, y, tempimg.getRGB(x, y));
                }
            }
            ImageIO.write(img, "bmp", new File(fileName));
            System.out.println("Image " + fileName + " has been written!");
            return true;
        } catch (IOException e) {
            System.out.println(e);
            return false;
        }
    }

    public boolean writePhase(String fileName) {
        try {
            BufferedImage imgOut = new BufferedImage(width, height, type);
            double max = Double.MIN_VALUE, min = Double.MAX_VALUE;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    double phase = (Math.atan2(img[y][x].im, img[y][x].re));
                    if (phase > max)
                        max = phase;
                    if (phase < min)
                        min = phase;
                }
            }
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    double phase = (Math.atan2(img[y][x].im, img[y][x].re));
                    phase = ((phase - min) * 255 / (max - min));
                    int color = ((int) phase << 16) | ((int) phase << 8) | (int) phase;
                    imgOut.setRGB(x, y, color);
                }
            }
            ImageIO.write(imgOut, "bmp", new File(fileName));
            System.out.println("Image " + fileName + " has been written!");
            return true;
        } catch (IOException e) {
            System.out.println(e);
            return false;
        }
    }

    // Shifting the coefficient
    private void shifting() {
        int halfWidth = width / 2;
        int halfHeight = height / 2;
        for (int y = 0; y < halfHeight; y++) {
            for (int x = 0; x < width; x++) {
                Complex temp = new Complex(img[y][x].re, img[y][x].im);
                img[y][x].re = img[y + halfHeight][x].re;
                img[y][x].im = img[y + halfHeight][x].im;
                img[y + halfHeight][x].re = temp.re;
                img[y + halfHeight][x].im = temp.im;
            }
        }
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < halfWidth; x++) {
                Complex temp = new Complex(img[y][x].re, img[y][x].im);
                img[y][x].re = img[y][x + halfWidth].re;
                img[y][x].im = img[y][x + halfWidth].im;
                img[y][x + halfWidth].re = temp.re;
                img[y][x + halfWidth].im = temp.im;
            }
        }
    }

    // Inverse the frequency domain
    public void getInverse() {
        shifting();
        fft2d(true);
        for (int y = 0; y < imgHeight; y++) {
            for (int x = 0; x < imgWidth; x++) {
                int gray = (int) img[y][x].re;
                gray = gray > 255 ? 255 : gray;
                gray = gray < 0 ? 0 : gray;
                int color = (gray << 16) | (gray << 8) | gray;
                im.getImage().setRGB(x, y, color);
            }
        }
    }

    // compute the inverse FFT of x[]
    public static Complex[] ifft(Complex[] x) {
        int n = x.length;
        Complex[] y = new Complex[n];
        // take conjugate
        for (int i = 0; i < n; i++) {
            y[i] = x[i].conjugate();
        }
        // compute forward FFT
        y = fft(y);
        // take conjugate again
        for (int i = 0; i < n; i++) {
            y[i] = y[i].conjugate();
        }
        // divide by n
        for (int i = 0; i < n; i++) {
            y[i] = y[i].scale(1.0 / n);
        }
        return y;
    }

    // Ideal Lowpass Filter
    public void ILPF(double radius) {
        if (radius <= 0 || radius > Math.min(width / 2, height / 2)) {
            System.out.println("INVALID Radius!");
            return;
        }
        int centerX = width / 2;
        int centerY = height / 2;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if ((x - centerX) * (x - centerX) + (y - centerY) * (y - centerY) > radius *

                        radius) {
                    img[y][x].re = 0;
                    img[y][x].im = 0;
                }
            }
        }
    }

    // Ideal Highpass Filter
    public void IHPF(double radius) {
        if (radius <= 0 || radius > Math.min(width / 2, height / 2)) {
            System.out.println("INVALID Radius!");
            return;
        }
        int centerX = width / 2;
        int centerY = height / 2;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if ((x - centerX) * (x - centerX) + (y - centerY) * (y - centerY) <= radius *

                        radius) {
                    img[y][x].re = 0;
                    img[y][x].im = 0;
                }
            }
        }
    }

    public void resetFrequency() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                img[y][x].re = original[y][x].re;
                img[y][x].im = original[y][x].im;
            }
        }
    }
}

class Complex {
    public double re;
    public double im;

    public Complex() {
        this(0, 0);
    }

    public Complex(double re, double im) {
        this.re = re;
        this.im = im;
    }

    public Complex add(Complex b) {
        return new Complex(this.re + b.re, this.im + b.im);
    }

    public Complex sub(Complex b) {
        return new Complex(this.re - b.re, this.im - b.im);
    }

    public Complex mult(Complex b) {
        return new Complex((this.re * b.re) - (this.im * b.im),
                (this.re * b.im) + (this.im * b.re));

    }

    public Complex conjugate() {
        return new Complex(re, -im);
    }

    public Complex scale(double b) {
        return new Complex(re * b, im * b);
    }

    public double length() {
        return Math.sqrt((re * re) + (im * im));
    }
}