import java.util.Random;
import java.util.Arrays;

public class Algorithm {
    private int size;
    private int missingDigits;
    private int mat[][];
    private int sizePerBox;

    public Algorithm(int s, int m) throws Exception {
        if (Math.sqrt(s) % 1.0 != 0.0) {
            throw new Exception("Invalid size");
        }
        this.size = s;
        this.sizePerBox = (int) Math.floor(Math.sqrt(s));
        this.missingDigits = m;
        this.mat = new int[size][size];
    }

    private int randomNumber(int start, int end) throws IllegalArgumentException {
        if (start >= end)
            throw new IllegalArgumentException("Start cannot exceed End.");

        return new Random().nextInt(end - start + 1) + start;
    }

    public void generate() {
        do {
            mat = new int[size][size];
            fillDiagonal();
            fillRemaining(0, sizePerBox);
        } while (!puzzleCheck());

        removeRandom();
    }

    private boolean fillDiagonal() {
        for (int i = 0; i < this.sizePerBox; i++) {
            if (!fillBox(i)) {
                return false;
            }
        }
        return true;
    }

    private boolean fillBox(int i) {
        for (int row = i * sizePerBox; row < (i * sizePerBox) + sizePerBox; row++) {
            for (int col = i * sizePerBox; col < (i * sizePerBox) + sizePerBox; col++) {
                do {
                    try {
                        this.mat[row][col] = randomNumber(1, size);
                    } catch (Exception e) {
                        return false;
                    }
                } while (!checkInBox(mat, row, col));
            }
        }
        return true;
    }

    private boolean fillRemaining(int i, int j) {
        if (j >= size && i < size - 1) {
            i = i + 1;
            j = 0;
        }
        if (i >= size && j >= size)
            return true;

        if (i < sizePerBox) {
            if (j < sizePerBox)
                j = sizePerBox;
        } else if (i < size - sizePerBox) {
            if (j == (int) (i / sizePerBox) * sizePerBox)
                j = j + sizePerBox;
        } else {
            if (j == size - sizePerBox) {
                i = i + 1;
                j = 0;
                if (i >= size)
                    return true;
            }
        }

        for (int num = 1; num <= size; num++) {
            if (this.allCheck(this.mat, i, j, num)) {
                mat[i][j] = num;
                if (this.fillRemaining(i, j + 1))
                    return true;
                mat[i][j] = 0;
            }
        }
        return false;
    }

    private void removeRandom() {
        int i = 0;
        do {
            int row = randomNumber(0, size - 1);
            int col = randomNumber(0, size - 1);
            if (mat[row][col] != 0) {
                mat[row][col] = 0;
                i++;
            }
        } while (i < missingDigits);
    }

    private boolean checkInBox(int mat[][], int row, int col, int num) {
        int myNum = num <= -1 ? mat[row][col] : num;
        int startRow = (int) (this.sizePerBox * (row / this.sizePerBox));
        int startCol = (int) (this.sizePerBox * (col / this.sizePerBox));
        for (int i = startRow; i < startRow + this.sizePerBox; i++) {
            for (int j = startCol; j < startCol + this.sizePerBox; j++) {
                if (i == row && j == col) {
                    continue;
                }

                if (mat[i][j] == myNum) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean checkInBox(int mat[][], int row, int col) {
        return checkInBox(mat, row, col, -1);
    }

    private boolean checkInCol(int mat[][], int row, int col, int num) {
        int myNum = num <= -1 ? mat[row][col] : num;
        for (int j = 0; j < size; j++) {
            if (j == row) {
                continue;
            }
            if (mat[j][col] == myNum) {
                return false;
            }
        }
        return true;
    }

    private boolean checkInCol(int mat[][], int row, int col) {
        return checkInCol(mat, row, col, -1);
    }

    private boolean checkInRow(int mat[][], int row, int col, int num) {
        int myNum = num <= -1 ? mat[row][col] : num;
        for (int i = 0; i < size; i++) {
            if (i == col) {
                continue;
            }
            if (mat[row][i] == myNum) {
                return false;
            }
        }
        return true;
    }

    private boolean checkInRow(int mat[][], int row, int col) {
        return checkInRow(mat, row, col, -1);
    }

    public boolean allCheck(int mat[][], int row, int col, int num) {
        return (checkInRow(mat, row, col, num) && checkInCol(mat, row, col, num) && checkInBox(mat, row, col, num));
    }

    public boolean allCheck(int mat[][], int row, int col) {
        return allCheck(mat, row, col, -1);
    }

    private boolean puzzleCheck() {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (mat[i][j] == 0 || !allCheck(mat, i, j)) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean isTrue(int[][] answer) throws Exception {
        if (answer.length != size) {
            throw new Exception("Not a square matrix");
        }
        print();

        for (int i = 0; i < size; i++) {
            if (answer[i].length != size) {
                throw new Exception("Not a square matrix");
            }

            for (int j = 0; j < size; j++) {
                System.out.println(i + " " + j + " " + mat[i][j] + " " + allCheck(answer, i, j));

                if (this.mat[i][j] == 0) {
                    System.out.println("*****");
                    if (answer[i][j] == 0 || !allCheck(answer, i, j)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public int getSize() {
        return size;
    }

    public int getMissingDigits() {
        return missingDigits;
    }

    public int[][] getMat() {
        return mat;
    }

    public int getSizePerBox() {
        return sizePerBox;
    }

    public void print() {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++)
                System.out.print(mat[i][j] + " ");
            System.out.println();
        }
        System.out.println();
    }

    public void flushprint() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
        print();
    }

    public int[][] getBox(int i, int j) {
        if (i < 0 || i >= this.sizePerBox || j < 0 || j >= this.sizePerBox) {
            return null;
        }
        int[][] box = new int[this.sizePerBox][this.sizePerBox];

        for (int k = 0; k < this.sizePerBox; k++) {
            for (int l = 0; l < this.sizePerBox; l++) {
                box[k][l] = this.mat[(i * this.sizePerBox) + k][(j * this.sizePerBox) + l];
            }
        }

        return box;
    }
}
