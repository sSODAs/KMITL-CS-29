import java.awt.Component;
import java.awt.Dimension;
import javax.swing.ImageIcon;

public class GameContext {

    // object of Frame, Menu and Settings
    private Frame frame;
    private Menu menu;
    private Settings setting;
    private Game game;
    private GameOver gameOver;
    private Component[] preventPage = new Component[2];

    // state of the game
    private boolean isGameStarted = false;
    private int difficulty = -1;

    // game board
    private boolean soundOpen = false;
    private String soundPath = "sound\\Song1.wav";
    private int screenWidth = 1535;
    private int screenHeight = 850;

    GameContext(Frame f, int w, int h) {
        this.frame = f;
        this.screenWidth = w;
        this.screenHeight = h;
        menu = new Menu(this);
        setting = new Settings(this);
        init();
    }

    public boolean init() {
        this.updateComponent(this.menu);
        return true;
    }

    public boolean toggleSound() {
        soundOpen = !soundOpen;
        if (soundOpen) {
            frame.playSounds(soundPath);
        } else {
            frame.closeSounds();
        }
        return true;
    }

    public Game getGame() {
        return game;
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    public boolean toMenu() {
        for (Component c : frame.getContentPane().getComponents())
            if (c instanceof Menu)
                return false;

        this.updateComponent(this.menu);
        return true;
    }

    public boolean toSetting() {
        for (Component c : frame.getContentPane().getComponents())
            if (c instanceof Settings)
                return false;

        this.updateComponent(this.setting);
        return true;
    }

    public boolean toGame(int difficulty) {
        for (Component c : frame.getContentPane().getComponents())
            if (c instanceof Game)
                return false;

        if (!this.isGameStarted) {
            this.game = new Game(this, difficulty);
            this.isGameStarted = true;
            System.out.println("to game");
        }
        this.updateComponent(this.game);
        return true;
    }

    public boolean toGame() {
        return toGame(this.difficulty);
    }

    public boolean toGameOver() {
        for (Component c : frame.getContentPane().getComponents())
            if (c instanceof GameOver)
                return false;

        if (this.gameOver == null)
            return false;
        this.updateComponent(this.gameOver);
        return true;
    }

    public boolean toPreventPage() {
        if (preventPage[0] != null) {
            this.updateComponent(preventPage[0]);
            return true;
        } else {
            this.updateComponent(this.menu);
        }
        return false;
    }

    public boolean setGameOver(boolean isWin) {
        for (Component c : frame.getContentPane().getComponents())
            if (c instanceof GameOver)
                return false;

        this.gameOver = new GameOver(this, isWin);
        return true;
    }

    public boolean endGame() {
        System.out.println("...");

        if (this.isGameStarted) {
            System.out.println("end game!!");
            this.isGameStarted = false;
            this.game = null;
            // this.updateComponent(this.menu);
            return true;
        }
        return false;
    }

    private boolean updateComponent(Component c) {
        preventPage[0] = preventPage[1];
        preventPage[1] = c;
        frame.getContentPane().removeAll();
        frame.getContentPane().add(c);
        frame.getContentPane().validate();
        frame.getContentPane().repaint();
        return true;
    }

    public Frame getFrame() {
        return frame;
    }

    public Menu getMenu() {
        return menu;
    }

    public Settings getSetting() {
        return setting;
    }

    public boolean isGameStarted() {
        return isGameStarted;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public boolean isSoundOpen() {
        return soundOpen;
    }

    public String getSoundPath() {
        return soundPath;
    }

}

interface SoundEvents {
    void playSounds(String soundPath);

    void closeSounds();
}

enum Number {
    EMPTY("number\\0.png"), ONE("number\\1.png"), TWO("number\\2.png"), THREE("number\\3.png"), FOUR("number\\4.png"),
    FIVE("number\\5.png"),
    SIX("number\\6.png"), SEVEN("number\\7.png"), EIGHT("number\\8.png"), NINE("number\\9.png");

    private String path;
    private ImageIcon icon;

    Number(String path) {
        this.path = path;
        this.icon = new ImageIcon(this.path);
    }

    public ImageIcon getIcon() {
        return this.icon;
    }
}

enum NumberPicked {
    EMPTY("number\\0.png"), ONE("number\\Pick1.png"), TWO("number\\Pick2.png"), THREE("number\\Pick3.png"),
    FOUR("number\\Pick4.png"),
    FIVE("number\\Pick5.png"),
    SIX("number\\Pick6.png"), SEVEN("number\\Pick7.png"), EIGHT("number\\Pick8.png"), NINE("number\\Pick9.png");

    private String path;
    private ImageIcon icon;

    NumberPicked(String path) {
        this.path = path;
        this.icon = new ImageIcon(this.path);
    }

    public ImageIcon getIcon() {
        return this.icon;
    }
}

enum NumberSet {
    EMPTY("number\\0.png"), ONE("number\\Set1.png"), TWO("number\\Set2.png"), THREE("number\\Set3.png"),
    FOUR("number\\Set4.png"),
    FIVE("number\\Set5.png"),
    SIX("number\\Set6.png"), SEVEN("number\\Set7.png"), EIGHT("number\\Set8.png"), NINE("number\\Set9.png");

    private String path;
    private ImageIcon icon;

    NumberSet(String path) {
        this.path = path;
        this.icon = new ImageIcon(this.path);
    }

    public ImageIcon getIcon() {
        return this.icon;
    }
}