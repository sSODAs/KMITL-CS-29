import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import java.awt.event.*;

public class Game extends JPanel implements ActionListener {
    private GameContext gameContext;
    private SudoTable table;
    private Difficulty difficulty = Difficulty.EASY;
    private JLabel Background;
    private JButton SettingBtn;
    private JLabel Logo;

    Game(GameContext c, int d) {
        this.gameContext = c;
        this.difficulty = Difficulty.values()[d - 1];
        running();
        resetTable();
    }

    private void running() {

        this.setPreferredSize(new Dimension(gameContext.getScreenWidth(), gameContext.getScreenHeight()));
        this.setFocusable(true);
        this.setLayout(null);

        SettingBtn = new JButton(new ImageIcon("btn\\SettingBtn.png"));
        SettingBtn.setBorderPainted(false);
        SettingBtn.setContentAreaFilled(false);
        SettingBtn.setBounds(gameContext.getScreenWidth() - 80, 20, 60, 60);
        SettingBtn.addActionListener(this);
        this.add(SettingBtn);

        Logo = new JLabel(new ImageIcon("src\\MiniLogo.png"));
        Logo.setBounds(gameContext.getScreenWidth() - 750, -150, 600, 600);
        this.add(Logo);
    }

    public void background() {
        if (Background != null) {
            this.remove(Background);
        }
        Background = new JLabel(new ImageIcon("src\\BackgroundMenu.png"), JLabel.LEFT);
        Background.setBounds(0, 0, this.gameContext.getScreenWidth(), this.gameContext.getScreenHeight());
        this.add(Background);
    }

    public void resetTable() {
        if (this.table != null) {
            this.remove(this.table);
        }
        table = new SudoTable(this.gameContext, this, 9, difficulty.Random());
        // table = new SudoTable(this.gameContext, this, 9, 5);
        this.add(table);
        this.background();
        this.validate();
        this.repaint();
    }

    public int getScreenWidth() {
        return this.gameContext.getScreenWidth();
    }

    public int getScreenHeight() {
        return this.gameContext.getScreenHeight();
    }

    public SudoTable getTable() {
        return this.table;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == SettingBtn) {
            gameContext.toSetting();
        }
    }
}

class SudoTable extends JPanel {
    private GameContext gameContext;
    private Algorithm mySudoku;
    private int[][] myMat;
    private SudoButton curNumber;
    private Game gamePanel;
    private SudoNumberPad numberPad;
    private SudoControl control;

    SudoTable(GameContext c, Game gn, int size, int missingDigits) {
        try {
            this.gameContext = c;
            this.gamePanel = gn;
            this.mySudoku = new Algorithm(size, missingDigits);
            this.numberPad = new SudoNumberPad();
            this.control = new SudoControl();
            running();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void running() {
        this.setPreferredSize(new Dimension(this.gameContext.getScreenWidth(), this.gameContext.getScreenHeight()));
        this.setFocusable(true);
        this.setLayout(null);

        this.setLayout(new GridLayout(3, 3));
        this.setBounds(200, 100, 600, 600); // set table
        this.setBackground(Color.WHITE);
        this.setOpaque(false);

        gamePanel.add(this.control);
        resetTable();
    }

    private ActionListener numberPadListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            SudoButton btn = (SudoButton) e.getSource();

            if (curNumber != null) {
                curNumber.setNumber(curNumber.getNumber(), false);
                if (curNumber.getNumber() == btn.getNumber()) {
                    curNumber = null;
                    return;
                }
            }

            curNumber = btn;
            btn.setNumber(btn.getNumber(), true);

        }
    };

    private ActionListener btnListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            SudoButton btn = (SudoButton) e.getSource();

            if (curNumber != null && btn.isPuzzle()) {
                btn.setNumber(curNumber.getNumber(), false);
            } else {

            }

        }
    };

    public void resetTable() {
        this.mySudoku.generate();
        this.mySudoku.print();
        this.myMat = this.mySudoku.getMat();

        for (Component c : this.getComponents()) {
            if (c instanceof SudoBox)
                this.remove(c);
        }

        if (this.numberPad != null) {
            curNumber = null;
            gamePanel.remove(this.numberPad);
        }
        this.numberPad = new SudoNumberPad();
        gamePanel.add(this.numberPad);

        for (int i = 0; i < this.mySudoku.getSizePerBox(); i++) {
            for (int j = 0; j < this.mySudoku.getSizePerBox(); j++) {
                SudoBox box = new SudoBox(mySudoku.getBox(i, j), i, j);
                this.add(box);
            }
        }

        gamePanel.background();
        gamePanel.validate();
        gamePanel.repaint();
    }

    public void clearTable() {
        for (Component c : this.getComponents()) {
            SudoBox box = (SudoBox) c;
            for (Component c2 : box.getComponents()) {
                if (c2 instanceof SudoButton) {
                    SudoButton btn = (SudoButton) c2;
                    if (btn.isPuzzle()) {
                        btn.setNumber(0, false);
                    }
                }
            }
        }

        gamePanel.background();
        gamePanel.validate();
        gamePanel.repaint();
    }

    public boolean checkTable() {
        try {
            int[][] thismat = new int[this.mySudoku.getSize()][this.mySudoku.getSize()];

            for (Component c : this.getComponents()) {
                SudoBox box = (SudoBox) c;
                for (Component c2 : box.getComponents()) {
                    if (c2 instanceof SudoButton) {
                        SudoButton btn = (SudoButton) c2;
                        thismat[btn.getRow()][btn.getCol()] = btn.getNumber();
                    }
                }
            }

            return this.mySudoku.isTrue(thismat);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private class SudoNumberPad extends JPanel {
        SudoNumberPad() {
            this.setLayout(new GridLayout(3, 3));
            this.setBackground(Color.WHITE);
            this.setOpaque(false);

            for (int i : new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }) {
                SudoButton btn = new SudoButton(i, -1, -1, true, false);
                btn.addEventOnNumberClick(numberPadListener);
                this.add(btn);
            }
            this.setBounds(gameContext.getScreenWidth() - 544, 280, 200, 200); // set Numpad
        }
    }

    private class SudoBox extends JPanel {
        private int[][] matBox;
        private int row;
        private int col;

        SudoBox(int[][] numBox, int row, int col) {
            System.out.println("SudoBox");
            this.setOpaque(false);
            this.setBorder(new EmptyBorder(5, 5, 5, 5));
            this.setLayout(new GridLayout(numBox.length, numBox.length));

            for (int i = 0; i < numBox.length; i++) {
                for (int j = 0; j < numBox.length; j++) {
                    SudoButton btn = new SudoButton(numBox[i][j], (row * numBox.length) + i, (col * numBox.length) + j,
                            true, numBox[i][j] == 0, 120, 120);
                    btn.addEventOnNumberClick(btnListener);
                    btn.setContentAreaFilled(false);
                    this.add(btn);
                }
            }
        }

        public int getRow() {
            return row;
        }

        public int getCol() {
            return col;
        }
    }

    private class SudoControl extends JPanel {
        SudoControl() {
            this.setLayout(new GridLayout(2, 2));
            this.setOpaque(false);

            // Menu
            JButton btn1 = new JButton(new ImageIcon("btn\\MenuBtn.png"));
            btn1.setBorderPainted(false);
            btn1.setContentAreaFilled(false);
            btn1.addActionListener((l) -> {
                gameContext.toMenu();
                gameContext.endGame();
            });

            // Reset
            JButton btn2 = new JButton(new ImageIcon("btn\\ResetBtn.png"));
            btn2.setBorderPainted(false);
            btn2.setContentAreaFilled(false);
            btn2.addActionListener((l) -> {
                resetTable();
            });

            // Clear
            JButton btn3 = new JButton(new ImageIcon("btn\\ClearBtn.png"));
            btn3.setBorderPainted(false);
            btn3.setContentAreaFilled(false);
            btn3.addActionListener((l) -> {
                clearTable();
            });

            // Submit
            JButton btn4 = new JButton(new ImageIcon("btn\\SubmitBtn.png"));
            btn4.setBorderPainted(false);
            btn4.setContentAreaFilled(false);
            btn4.addActionListener((l) -> {
                gameContext.setGameOver(checkTable());
                gameContext.toGameOver();
            });

            this.add(btn1);
            this.add(btn2);
            this.add(btn3);
            this.add(btn4);
            this.setBounds(gameContext.getScreenWidth() - 600, 550, 320, 150);
        }
    }
}

class SudoButton extends JButton {
    private int number;
    private int row;
    private int col;
    private boolean isPuzzle = false;
    private boolean isInsertImage = false;
    private int img_width;
    private int img_height;
    private boolean isPicked = false;
    private boolean isRed = false;

    SudoButton(int number, int row, int col, boolean isInsertImage, boolean isRed, int img_width, int img_height) {
        this.row = row;
        this.col = col;
        this.isInsertImage = isInsertImage;
        this.img_width = img_width;
        this.img_height = img_height;
        this.isRed = isRed;
        this.setBorderPainted(false);
        this.setContentAreaFilled(false);
        this.setBorder(null);
        this.setNumber(number, false);
        this.isPuzzle = number == 0;
    }

    SudoButton(int number, int row, int col, boolean isInsertImage, boolean isRed) {
        this(number, row, col, isInsertImage, isRed, 110, 110);
    }

    public int getNumber() {
        return number;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public boolean isPuzzle() {
        return isPuzzle;
    }

    public void setNumber(int number, boolean isPicked) {
        this.number = number;
        this.isPicked = isPicked;

        if (isInsertImage)
            if (isPicked)
                this.setIcon(new ImageIcon(NumberPicked.values()[number].getIcon().getImage()
                        .getScaledInstance(this.img_width, this.img_height, Image.SCALE_SMOOTH)));
            else if (isRed)
                this.setIcon(new ImageIcon(NumberSet.values()[number].getIcon().getImage()
                        .getScaledInstance(this.img_width, this.img_height, Image.SCALE_SMOOTH)));
            else
                this.setIcon(new ImageIcon(Number.values()[number].getIcon().getImage()
                        .getScaledInstance(this.img_width, this.img_height, Image.SCALE_SMOOTH)));
        else
            this.setText(String.valueOf(number));

    }

    public void setIsPicked(boolean isPicked) {
        this.isPicked = isPicked;
    }

    public void addEventOnNumberClick(ActionListener listener) {
        this.addActionListener(listener);
    }
}

enum Difficulty {
    EASY(35, 45), MEDIUM(46, 49), HARD(50, 53);

    private int min;
    private int max;

    Difficulty(int min, int max) {
        this.min = min;
        this.max = max;
    }

    int Random() {
        return (int) (Math.random() * (max - min + 1) + min);
    }

    @Override
    public String toString() {
        return this.name().toLowerCase();
    }
}