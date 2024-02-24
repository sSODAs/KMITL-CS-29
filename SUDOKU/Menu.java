import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

import javax.swing.*;

public class Menu extends JPanel implements ActionListener {

    private JButton StartBtn;
    private JButton modeBtn_plus;
    private JButton modeBtn_minus;
    private JButton ExitBtn;
    private JLabel Logo;
    private JLabel Background;
    private JLabel setMode;
    private JButton SettingBtn;

    protected int mode = 1;

    GameContext gameContext;
    Game game;

    Menu(GameContext t) {
        this.gameContext = t;
        this.setPreferredSize(new Dimension(this.gameContext.getScreenWidth(), this.gameContext.getScreenHeight()));
        this.setFocusable(true);
        Start();
    }

    private void Start() {

        Gamemode();

        // Start
        StartBtn = new JButton("");
        StartBtn.setIcon(new ImageIcon("btn\\StartBtn.png"));
        StartBtn.setBorderPainted(false);
        StartBtn.setContentAreaFilled(false);
        StartBtn.setBounds(670, 500, 200, 80);
        StartBtn.addActionListener(this);

        // Mode
        modeBtn_plus = new JButton(new ImageIcon("btn\\\\PlusBtn_work.png"));
        modeBtn_minus = new JButton(new ImageIcon("btn\\\\MinusBtn_not.png"));
        modeBtn_plus.setBorderPainted(false);
        modeBtn_plus.setContentAreaFilled(false);
        modeBtn_minus.setBorderPainted(false);
        modeBtn_minus.setContentAreaFilled(false);
        modeBtn_plus.setBounds(980, 370, 80, 80);
        modeBtn_minus.setBounds(480, 370, 80, 80);

        // Setting
        SettingBtn = new JButton(new ImageIcon("btn\\SettingBtn.png"));
        SettingBtn.setBorderPainted(false);
        SettingBtn.setContentAreaFilled(false);
        SettingBtn.setBounds(this.gameContext.getScreenWidth() - 80, 20, 60, 60);
        SettingBtn.addActionListener(this);
        this.add(SettingBtn);

        modeBtn_minus.addActionListener(this);
        modeBtn_plus.addActionListener(this);

        this.setLayout(null);
        this.add(StartBtn);
        this.add(modeBtn_plus);
        this.add(modeBtn_minus);

        // Exit
        ExitBtn = new JButton(new ImageIcon("btn\\ExitBtn.png"));
        ExitBtn.setBounds(675, 570, 200, 80);
        ExitBtn.setBorderPainted(false);
        ExitBtn.setContentAreaFilled(false);
        ExitBtn.addActionListener(this);
        this.add(ExitBtn);

        Logo = new JLabel(new ImageIcon("src\\GameLogo2.png"));

        final int LOGO_X = (460);
        final int LOGO_Y = (-100);

        Logo.setBounds(LOGO_X, LOGO_Y, 600, 600);
        this.add(Logo);

        Backgroundmode();

    }

    // Mode

    private void Gamemode() {
        setMode = new JLabel(new ImageIcon("src\\ModeEasy.png"));
        setMode.setBounds(570, 320, 400, 200);
        this.add(setMode);
    }

    private void Backgroundmode() {
        // Background
        Background = new JLabel(new ImageIcon("src\\BackgroundMenu.png"), JLabel.LEFT);
        Background.setBounds(0, 0, this.gameContext.getScreenWidth(), this.gameContext.getScreenHeight());
        this.add(Background);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Start
        if (e.getSource() == StartBtn) {
            gameContext.toGame(mode);
            System.out.println("StartGame");
        }

        // Plus Mode
        if (e.getSource() == modeBtn_plus) {
            mode += 1;
            if (mode >= 4) {
                mode = 3;
            }

            // Change Mode setMode
            if (mode == 1) {
                setMode.setIcon(new ImageIcon(getClass().getResource("src\\ModeEasy.png")));
                setMode.setBounds(570, 320, 400, 200);
            } else if (mode == 2) {
                setMode.setIcon(new ImageIcon(getClass().getResource("src\\ModeNormal.png")));
                setMode.setBounds(580, 320, 400, 200);
            } else {
                setMode.setIcon(new ImageIcon(getClass().getResource("src\\ModeHard.png")));
                setMode.setBounds(570, 320, 400, 200);
            }
            
            if (mode != 1) {
                modeBtn_minus.setIcon(new ImageIcon(getClass().getResource("btn\\\\MinusBtn_work.png")));
            } else {
                modeBtn_minus.setIcon(new ImageIcon(getClass().getResource("btn\\\\MinusBtn_not.png")));
            }

            if (mode != 3) {
                modeBtn_plus.setIcon(new ImageIcon(getClass().getResource("btn\\\\PlusBtn_work.png")));
            } else {
                modeBtn_plus.setIcon(new ImageIcon(getClass().getResource("btn\\\\PlusBtn_not.png")));
            }

            System.out.println(mode);
        }

        // Minus Mode
        if (e.getSource() == modeBtn_minus) {
            mode -= 1;
            if (mode <= 0) {
                mode = 1;
            }

            // Change Mode setMode
            if (mode == 1) {
                setMode.setIcon(new ImageIcon(getClass().getResource("src\\ModeEasy.png")));
                setMode.setBounds(570, 320, 400, 200);
            } else if (mode == 2) {
                setMode.setIcon(new ImageIcon(getClass().getResource("src\\ModeNormal.png")));
                setMode.setBounds(580, 320, 400, 200);
            } else {
                setMode.setIcon(new ImageIcon(getClass().getResource("src\\ModeHard.png")));
                setMode.setBounds(570, 320, 400, 200);
            }

            if (mode != 1) {
                modeBtn_minus.setIcon(new ImageIcon(getClass().getResource("btn\\\\MinusBtn_work.png")));
            } else {
                modeBtn_minus.setIcon(new ImageIcon(getClass().getResource("btn\\\\MinusBtn_not.png")));
            }

            if (mode != 3) {
                modeBtn_plus.setIcon(new ImageIcon(getClass().getResource("btn\\\\PlusBtn_work.png")));
            } else {
                modeBtn_plus.setIcon(new ImageIcon(getClass().getResource("btn\\\\PlusBtn_not.png")));
            }

            System.out.println(mode);
        }

        if (e.getSource() == SettingBtn) {
            gameContext.toSetting();
        }

        // Exit
        if (e.getSource() == ExitBtn) {
            System.exit(0); // Close the program
            System.out.println("Close");
        }

    }

}