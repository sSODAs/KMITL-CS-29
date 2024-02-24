
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JButton;

import java.io.File;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

public class Frame extends JFrame implements SoundEvents {
    public GameContext gameContext;
    private Sounds soundCtrl;

    Frame() {
        this.gameContext = new GameContext(this,1535,850);
        this.gameContext.init();

        this.setTitle("SUDOKU");
        this.setIconImage(new ImageIcon("src\\Gameicon.png").getImage());
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setUndecorated(true);
        this.setResizable(false);
        this.pack();
        this.setVisible(true);

        this.gameContext.toggleSound();
    }

    @Override
    public void playSounds(String soundPath) {
        soundCtrl = new Sounds(soundPath);
        soundCtrl.run();
    }

    @Override
    public void closeSounds() {
        if (soundCtrl != null) {
            soundCtrl.closeSound();
        }
    }

    public class Sounds {
        private String soundPath;
        private AudioInputStream audioInputStream;

        private Clip clip;

        public Sounds(String soundPath) {
            this.soundPath = soundPath;
        }

        public void run() {
            try {
                audioInputStream = AudioSystem.getAudioInputStream(new File(soundPath).getAbsoluteFile());
                clip = AudioSystem.getClip();
                clip.open(audioInputStream);
                clip.start();
            } catch (Exception e) {
                System.out.println("Error with playing sound.");
                e.printStackTrace();
            }
        }

        public void closeSound() {
            try {
                if (clip.isOpen())
                    clip.stop();
            } catch (Exception e) {
                System.out.println("Error with closing sound.");
                e.printStackTrace();
            }
        }
    }

}
