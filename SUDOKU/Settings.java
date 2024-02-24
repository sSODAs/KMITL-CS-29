import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.ImageIcon;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Settings extends JPanel implements ActionListener {
    private GameContext gameContext;
    private JButton CloseSettingBtn;
    private JButton OpenMusic;
    private JButton CloseMusic;
    private JButton toggleMusic;
    private JLabel SettingPopup;

    Settings(GameContext t) {
        this.gameContext = t;
        this.setLayout(null);
        CloseSettingBtn = new JButton(new ImageIcon("btn\\CloseSettingBtn.png"));
        CloseSettingBtn.setBounds(620, 680, 300, 100);
        CloseSettingBtn.setBorderPainted(false);
        CloseSettingBtn.setContentAreaFilled(false);
        CloseSettingBtn.addActionListener(this);
        this.add(CloseSettingBtn);

        toggleMusic = new JButton(new ImageIcon("btn\\Open_Off.png"));
        toggleMusic.setBounds(1047, 218, 320, 100);
        toggleMusic.setBorderPainted(false);
        toggleMusic.setContentAreaFilled(false);
        toggleMusic.addActionListener(this);

        this.add(toggleMusic);

        if (!gameContext.isSoundOpen()) {
            toggleMusic.setIcon(new ImageIcon(getClass().getResource("btn\\Open_On.png")));
        }

        SettingPopup = new JLabel(new ImageIcon("src\\SettingBackGround.png"), JLabel.CENTER);
        SettingPopup.setBounds(0, 0, this.gameContext.getScreenWidth(), this.gameContext.getScreenHeight());
        this.add(SettingPopup);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == CloseSettingBtn) {

            gameContext.toPreventPage();
            System.out.println("CloseSetting");
            
        } else if (e.getSource() == toggleMusic) {
            gameContext.toggleSound();
            if (!gameContext.isSoundOpen()) {
                toggleMusic.setIcon(new ImageIcon(getClass().getResource("btn\\Open_Off.png")));
            } else {
                toggleMusic.setIcon(new ImageIcon(getClass().getResource("btn\\Open_On.png")));
            }
        }
    }
}
