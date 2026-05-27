import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;


public class HangmanGame extends JFrame {

    private static final Map<String, String[]> WORD_BANK = new LinkedHashMap<>();
    static {
        WORD_BANK.put("Animals",   new String[]{"ELEPHANT", "GIRAFFE", "PENGUIN", "DOLPHIN", "CHEETAH", "KANGAROO", "CROCODILE", "FLAMINGO"});
        WORD_BANK.put("Countries", new String[]{"BRAZIL", "CANADA", "GERMANY", "JAPAN", "AUSTRALIA", "FRANCE", "MEXICO", "EGYPT"});
        WORD_BANK.put("Fruits",    new String[]{"MANGO", "STRAWBERRY", "WATERMELON", "PINEAPPLE", "BLUEBERRY", "AVOCADO", "COCONUT", "PAPAYA"});
        WORD_BANK.put("Movies",    new String[]{"INCEPTION", "INTERSTELLAR", "GLADIATOR", "AVATAR", "TITANIC", "JOKER", "PARASITE", "DUNKIRK"});
        WORD_BANK.put("Sports",    new String[]{"CRICKET", "BADMINTON", "VOLLEYBALL", "GYMNASTICS", "WRESTLING", "ARCHERY", "SWIMMING", "FENCING"});
        WORD_BANK.put("Science",   new String[]{"GRAVITY", "PHOTON", "NUCLEUS", "QUANTUM", "MOLECULE", "ENZYME", "NEUTRON", "ELECTRON"});
    }

    static final int MAX_WRONG = 6;   

    private String currentWord;
    private String currentCategory;
    private Set<Character> guessedLetters;
    private int wrongGuesses;

    private GamePanel     gamePanel;
    private KeyboardPanel keyboardPanel;

    public HangmanGame() {
        super("Hangman");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        guessedLetters = new HashSet<>();
        initGame();
        buildUI();
        setupKeyboardInput();  

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    void initGame() {
        guessedLetters.clear();
        wrongGuesses = 0;

        List<String> categories = new ArrayList<>(WORD_BANK.keySet());
        currentCategory = categories.get(new Random().nextInt(categories.size()));
        String[] words = WORD_BANK.get(currentCategory);
        currentWord = words[new Random().nextInt(words.length)];
    }

    private void buildUI() {
        getContentPane().setBackground(new Color(18, 20, 30));
        setLayout(new BorderLayout(0, 0));

        gamePanel     = new GamePanel(this);
        keyboardPanel = new KeyboardPanel(this);

        add(gamePanel,     BorderLayout.CENTER);
        add(keyboardPanel, BorderLayout.SOUTH);
    }

    private void setupKeyboardInput() {
        JRootPane root = getRootPane();
        InputMap  im   = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am   = root.getActionMap();

        for (char c = 'A'; c <= 'Z'; c++) {
            final char letter = c;
            String actionKey = "key_" + letter;

            im.put(KeyStroke.getKeyStroke(Character.toLowerCase(letter), 0), actionKey);
            im.put(KeyStroke.getKeyStroke(letter, 0),                        actionKey);

            am.put(actionKey, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    keyboardPanel.pressKey(letter);
                }
            });
        }
    }


    void onLetterGuessed(char letter) {
        if (guessedLetters.contains(letter)) return;
        guessedLetters.add(letter);

        if (currentWord.indexOf(letter) < 0) {
            wrongGuesses++;
        }

        gamePanel.refresh();

        if (isWordGuessed()) {
            showResult(true);
        } else if (wrongGuesses >= MAX_WRONG) {
            showResult(false);
        }
    }

    boolean isWordGuessed() {
        for (char c : currentWord.toCharArray()) {
            if (!guessedLetters.contains(c)) return false;
        }
        return true;
    }

    boolean isLetterGuessed(char c)  { return guessedLetters.contains(c); }
    boolean isLetterCorrect(char c)  { return currentWord.indexOf(c) >= 0; }
    String  getCurrentWord()         { return currentWord; }
    String  getCurrentCategory()     { return currentCategory; }
    int     getWrongGuesses()        { return wrongGuesses; }
    int     getRemainingGuesses()    { return MAX_WRONG - wrongGuesses; }

    private void showResult(boolean won) {
        if (!won) {
            gamePanel.startDeathAnimation();

            javax.swing.Timer delay = new javax.swing.Timer(3500, e -> showResultDialog(false));
            delay.setRepeats(false);
            delay.start();
        } else {
            javax.swing.Timer t = new javax.swing.Timer(180, e -> showResultDialog(true));
            t.setRepeats(false);
            t.start();
        }
    }

    private void showResultDialog(boolean won) {
        String msg   = won
            ? "🎉  You Win!  The word was: " + currentWord
            : "💀  Game Over!  The word was: " + currentWord;
        String title = won ? "Congratulations!" : "Game Over";

        int choice = JOptionPane.showOptionDialog(
            this, msg, title,
            JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE,
            null, new String[]{"Play Again", "Quit"}, "Play Again"
        );
        if (choice == JOptionPane.YES_OPTION) restartGame();
        else System.exit(0);
    }

    void restartGame() {
        initGame();
        gamePanel.refresh();
        keyboardPanel.reset();
    }
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch (Exception ignored) {}

        SwingUtilities.invokeLater(HangmanGame::new);
    }
}


class GamePanel extends JPanel {

    private static final Color BG           = new Color(18, 20, 30);
    private static final Color CAT_CLR      = new Color(180, 130, 255);
    private static final Color LETTER_CLR   = new Color(240, 240, 255);
    private static final Color REMAIN_GREEN = new Color(80, 220, 130);
    private static final Color REMAIN_RED   = new Color(220, 80, 80);

    private final HangmanGame game;

    private JLabel    categoryLabel;
    private JLabel    wordLabel;
    private JLabel    statusLabel;
    private JButton   restartBtn;
    private HangmanCanvas canvas;

    GamePanel(HangmanGame game) {
        this.game = game;
        setBackground(BG);
        setLayout(new BorderLayout(0, 0));
        setBorder(BorderFactory.createEmptyBorder(18, 18, 10, 18));
        buildComponents();
    }

    private void buildComponents() {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);

        categoryLabel = new JLabel();
        categoryLabel.setFont(new Font("Monospaced", Font.BOLD, 14));
        categoryLabel.setForeground(CAT_CLR);
        categoryLabel.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(CAT_CLR, 1, 10),
            BorderFactory.createEmptyBorder(4, 12, 4, 12)
        ));

        restartBtn = new JButton("⟳  Restart");
        restartBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        restartBtn.setForeground(new Color(240, 240, 255));
        restartBtn.setBackground(new Color(40, 44, 60));
        restartBtn.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(new Color(100, 100, 160), 1, 10),
            BorderFactory.createEmptyBorder(5, 14, 5, 14)
        ));
        restartBtn.setFocusPainted(false);
        restartBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        restartBtn.addActionListener(e -> game.restartGame());

        topBar.add(categoryLabel, BorderLayout.WEST);
        topBar.add(restartBtn,    BorderLayout.EAST);
        topBar.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));

        canvas = new HangmanCanvas(game);
        canvas.setPreferredSize(new Dimension(260, 280));

        wordLabel = new JLabel();
        wordLabel.setFont(new Font("Monospaced", Font.BOLD, 32));
        wordLabel.setForeground(LETTER_CLR);
        wordLabel.setHorizontalAlignment(SwingConstants.CENTER);
        wordLabel.setBorder(BorderFactory.createEmptyBorder(16, 0, 6, 0));

        statusLabel = new JLabel();
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel centrePanel = new JPanel(new BorderLayout(0, 0));
        centrePanel.setOpaque(false);
        centrePanel.add(canvas,    BorderLayout.CENTER);
        centrePanel.add(wordLabel, BorderLayout.SOUTH);

        JPanel mainArea = new JPanel(new BorderLayout());
        mainArea.setOpaque(false);
        mainArea.add(centrePanel, BorderLayout.CENTER);
        mainArea.add(statusLabel, BorderLayout.SOUTH);

        add(topBar,   BorderLayout.NORTH);
        add(mainArea, BorderLayout.CENTER);

        refresh();
    }

    void refresh() {
        categoryLabel.setText("  Category:  " + game.getCurrentCategory() + "  ");

        String word = game.getCurrentWord();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            sb.append(game.isLetterGuessed(c) ? c : '_');
            if (i < word.length() - 1) sb.append("  ");
        }
        wordLabel.setText(sb.toString());

        int rem = game.getRemainingGuesses();
        statusLabel.setForeground(rem <= 2 ? REMAIN_RED : REMAIN_GREEN);
        statusLabel.setText("Remaining guesses: " + rem);

        canvas.resetAnimation();
        canvas.repaint();
    }

    void startDeathAnimation() {
        canvas.startFallingAnimation();
    }
}


class HangmanCanvas extends JPanel {

    private static final Color BG_CLR   = new Color(22, 25, 38);
    private static final Color GALLOWS  = new Color(160, 140, 100);
    private static final Color BODY_CLR = new Color(240, 210, 130);
    private static final Color ROPE_CLR = new Color(200, 170, 90);
    private static final Color BLOOD    = new Color(220, 60, 60);

    private final HangmanGame game;

    private boolean isFalling   = false;
    private int     fallOffset  = 0;
    private int     crumbleStep = 0;

 
    private static final int PARTS = 6;
    private final double[] partY    = new double[PARTS];  
    private final double[] partVY   = new double[PARTS];  
    private final double[] partAngle= new double[PARTS]; 
    private final double[] partAV   = new double[PARTS];  
    private final double[] partX    = new double[PARTS]; 
    private final boolean[]landed   = new boolean[PARTS]; 
    private double[] groundY;                              

    private static final double GRAVITY    = 1.0;
    private static final double BOUNCE     = 0.18;   
    private static final double ANG_DAMP   = 0.85;   

    HangmanCanvas(HangmanGame game) {
        this.game = game;
        setBackground(BG_CLR);
        setBorder(new RoundedBorder(new Color(50, 55, 80), 1, 16));
    }

    void resetAnimation() {
        isFalling   = false;
        fallOffset  = 0;
        crumbleStep = 0;
        Arrays.fill(landed, false);
    }

    void startFallingAnimation() {
        isFalling   = true;
        fallOffset  = 0;
        crumbleStep = 0;

        javax.swing.Timer slideTimer = new javax.swing.Timer(16, null);
        slideTimer.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                fallOffset += 4;
                repaint();
                if (fallOffset >= 60) {
                    slideTimer.stop();
                    initParticles();
                    startPhysicsTimer();
                }
            }
        });
        slideTimer.start();
    }

    private void initParticles() {
        int w = getWidth(), h = getHeight();
        int[] hi = headInfoRaw();
        int cx   = hi[0];
        int cy   = hi[1] + fallOffset;   
        int r    = hi[2];
        int base = h - 34;               

        Random rng = new Random(System.nanoTime());

        partX[0]     = cx;                partY[0] = cy;
        partVY[0]    = 1 + rng.nextDouble() * 2;
        partAngle[0] = 0;                 partAV[0] = (rng.nextDouble() - 0.5) * 0.25;
        groundY      = new double[PARTS];
        groundY[0]   = base - r;        

        partX[1]     = cx + (rng.nextDouble() - 0.5) * 10;
        partY[1]     = cy + r + 30;      
        partVY[1]    = 0.8 + rng.nextDouble() * 1.5;
        partAngle[1] = 0;                 partAV[1] = (rng.nextDouble() - 0.5) * 0.20;
        groundY[1]   = base - 2;

        partX[2]     = cx - 14 + (rng.nextDouble() - 0.5) * 8;
        partY[2]     = cy + r + 28;
        partVY[2]    = 1.2 + rng.nextDouble() * 2;
        partAngle[2] = -0.3;              partAV[2] = -(rng.nextDouble() * 0.22 + 0.05);
        groundY[2]   = base - 2;

        partX[3]     = cx + 14 + (rng.nextDouble() - 0.5) * 8;
        partY[3]     = cy + r + 28;
        partVY[3]    = 1.2 + rng.nextDouble() * 2;
        partAngle[3] = 0.3;              partAV[3] = (rng.nextDouble() * 0.22 + 0.05);
        groundY[3]   = base - 2;

        partX[4]     = cx - 14 + (rng.nextDouble() - 0.5) * 8;
        partY[4]     = cy + r + 75;
        partVY[4]    = 1.5 + rng.nextDouble() * 2;
        partAngle[4] = -0.2;             partAV[4] = -(rng.nextDouble() * 0.20 + 0.03);
        groundY[4]   = base - 2;

        partX[5]     = cx + 14 + (rng.nextDouble() - 0.5) * 8;
        partY[5]     = cy + r + 75;
        partVY[5]    = 1.5 + rng.nextDouble() * 2;
        partAngle[5] = 0.2;              partAV[5] = (rng.nextDouble() * 0.20 + 0.03);
        groundY[5]   = base - 2;
    }

    private void startPhysicsTimer() {
        crumbleStep = 1;  

        javax.swing.Timer physTimer = new javax.swing.Timer(16, null);
        physTimer.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                boolean allLanded = true;
                for (int i = 0; i < PARTS; i++) {
                    if (landed[i]) continue;
                    allLanded = false;

                    partVY[i] += GRAVITY;
                    partY[i]  += partVY[i];
                    partAngle[i] += partAV[i];

                    if (partY[i] >= groundY[i]) {
                        partY[i]  = groundY[i];
                        partVY[i] = -partVY[i] * BOUNCE;  
                        partAV[i] *= ANG_DAMP;

                        if (Math.abs(partVY[i]) < 0.6) {
                            partVY[i] = 0;
                            double rest = restAngle(i);
                            partAngle[i] += (rest - partAngle[i]) * 0.12;
                            if (Math.abs(partAngle[i] - rest) < 0.01 &&
                                Math.abs(partAV[i]) < 0.005) {
                                partAngle[i] = rest;
                                partAV[i]    = 0;
                                landed[i]    = true;
                            }
                        }
                    }
                }
                repaint();
                if (allLanded) physTimer.stop();
            }
        });
        physTimer.start();
    }

    private double restAngle(int part) {
        switch (part) {
            case 0: return 0.0;          
            case 1: return Math.PI / 2;  
            case 2: return  Math.PI / 3;  
            case 3: return -Math.PI / 3;  
            case 4: return  Math.PI / 2.5;
            case 5: return -Math.PI / 2.5;
            default: return 0;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();

        drawGallows(g2, w, h);

        if (isFalling && crumbleStep > 0) {
            drawPileUp(g2, w, h);
        } else {
            g2.translate(0, fallOffset);
            int wrong = game.getWrongGuesses();
            if (wrong >= 1) drawHead    (g2, w, h);
            if (wrong >= 2) drawBody    (g2, w, h);
            if (wrong >= 3) drawLeftArm (g2, w, h);
            if (wrong >= 4) drawRightArm(g2, w, h);
            if (wrong >= 5) drawLeftLeg (g2, w, h);
            if (wrong >= 6) drawRightLeg(g2, w, h);
        }

        g2.dispose();
    }

    private void drawPileUp(Graphics2D g, int w, int h) {
        int[] order = {4, 5, 2, 3, 1, 0};
        for (int i : order) {
            Graphics2D pg = (Graphics2D) g.create();
            pg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int px = (int) partX[i];
            int py = (int) partY[i];
            pg.translate(px, py);
            pg.rotate(partAngle[i]);
            drawLimbAtOrigin(pg, i);
            pg.dispose();
        }

        int landed_count = 0;
        for (boolean b : landed) if (b) landed_count++;
        if (landed_count > 0) {
            int[] hi = headInfoRaw();
            int poolX = hi[0];
            int poolY = h - 34;
            int radius = 6 + landed_count * 5;
            g.setColor(new Color(160, 20, 20, 130));
            g.fillOval(poolX - radius, poolY - 4, radius * 2, 8);
        }
    }

    private void drawLimbAtOrigin(Graphics2D g, int part) {
        g.setColor(BODY_CLR);
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int r = headInfoRaw()[2];

        switch (part) {
            case 0: 
                g.drawOval(-r, -r, r * 2, r * 2);
                g.setColor(BLOOD);
                g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawLine(-7, -6, -3, -2);  g.drawLine(-3, -6, -7, -2);
                g.drawLine( 3, -6,  7, -2);  g.drawLine( 7, -6,  3, -2);
                g.setColor(BODY_CLR);
                g.setStroke(new BasicStroke(2f));
                g.drawLine(-5, 6, 5, 8);
                break;

            case 1: 
                g.setColor(BODY_CLR);
                g.drawLine(0, 0, 0, 60);
                break;

            case 2: 
                g.drawLine(0, 0, -28, 27);
                break;

            case 3: 
                g.drawLine(0, 0, 28, 27);
                break;

            case 4: 
                g.drawLine(0, 0, -28, 30);
                break;

            case 5: 
                g.drawLine(0, 0, 28, 30);
                break;
        }
    }

    private void drawGallows(Graphics2D g, int w, int h) {
        g.setColor(GALLOWS);
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int baseY = h - 30, poleX = 60, topY = 30, armEndX = 160;
        g.drawLine(30, baseY, w - 30, baseY);
        g.drawLine(poleX, baseY, poleX, topY);
        g.drawLine(poleX, topY, armEndX, topY);
        g.drawLine(poleX, topY + 40, poleX + 40, topY);

        g.setColor(ROPE_CLR);
        g.setStroke(new BasicStroke(2.5f));
        int ropeEnd = (isFalling) ? topY + 10 : topY + 28;
        g.drawLine(armEndX, topY, armEndX, ropeEnd);
    }

    private int[] headInfoRaw() { return new int[]{160, 30 + 28 + 20, 20}; }

    private int[] headInfo(int w, int h) { return headInfoRaw(); }

    private void drawHead(Graphics2D g, int w, int h) {
        int[] hi = headInfoRaw();
        int cx = hi[0], cy = hi[1], r = hi[2];
        g.setColor(BODY_CLR);
        g.setStroke(new BasicStroke(3f));
        g.drawOval(cx - r, cy - r, r * 2, r * 2);
        g.fillOval(cx - 7, cy - 5, 4, 4);
        g.fillOval(cx + 3, cy - 5, 4, 4);
        if (game.getWrongGuesses() >= HangmanGame.MAX_WRONG) {
            g.setColor(BLOOD);
            g.setStroke(new BasicStroke(2f));
            g.drawArc(cx - 6, cy + 4, 12, 8, 0, -180);
        } else {
            g.setStroke(new BasicStroke(2f));
            g.drawArc(cx - 6, cy + 3, 12, 7, 0, 180);
        }
    }

    private void drawBody(Graphics2D g, int w, int h) {
        int[] hi = headInfoRaw();
        int cx = hi[0], cy = hi[1], r = hi[2];
        g.setColor(BODY_CLR);
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(cx, cy + r, cx, cy + r + 60);
    }

    private void drawLeftArm(Graphics2D g, int w, int h) {
        int[] hi = headInfoRaw();
        int cx = hi[0], cy = hi[1], r = hi[2];
        g.setColor(BODY_CLR);
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(cx, cy + r + 15, cx - 28, cy + r + 42);
    }

    private void drawRightArm(Graphics2D g, int w, int h) {
        int[] hi = headInfoRaw();
        int cx = hi[0], cy = hi[1], r = hi[2];
        g.setColor(BODY_CLR);
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(cx, cy + r + 15, cx + 28, cy + r + 42);
    }

    private void drawLeftLeg(Graphics2D g, int w, int h) {
        int[] hi = headInfoRaw();
        int cx = hi[0], cy = hi[1], r = hi[2];
        g.setColor(BODY_CLR);
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(cx, cy + r + 60, cx - 28, cy + r + 90);
    }

    private void drawRightLeg(Graphics2D g, int w, int h) {
        int[] hi = headInfoRaw();
        int cx = hi[0], cy = hi[1], r = hi[2];
        g.setColor(BODY_CLR);
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(cx, cy + r + 60, cx + 28, cy + r + 90);
    }
}

class KeyboardPanel extends JPanel {

    private static final Color BG_CLR      = new Color(14, 16, 26);
    private static final Color BTN_DEFAULT = new Color(38, 42, 62);
    private static final Color BTN_CORRECT = new Color(30, 140, 80);
    private static final Color BTN_WRONG   = new Color(140, 30, 40);

    private final HangmanGame game;
    private final Map<Character, JButton> buttons = new LinkedHashMap<>();

    private static final String[] ROWS = {"QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM"};

    KeyboardPanel(HangmanGame game) {
        this.game = game;
        setBackground(BG_CLR);
        setBorder(BorderFactory.createEmptyBorder(10, 18, 16, 18));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        buildButtons();
    }

    private void buildButtons() {
        for (String row : ROWS) {
            JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 3));
            rowPanel.setOpaque(false);
            for (char c : row.toCharArray()) {
                JButton btn = createLetterButton(c);
                buttons.put(c, btn);
                rowPanel.add(btn);
            }
            add(rowPanel);
        }
    }

    void pressKey(char letter) {
        JButton btn = buttons.get(Character.toUpperCase(letter));
        if (btn != null && btn.isEnabled()) {
            btn.doClick();
        }
    }

    private JButton createLetterButton(char letter) {
        JButton btn = new JButton(String.valueOf(letter)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);

                g2.setFont(getFont());
                g2.setColor(getForeground());
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth(getText())) / 2;
                int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), tx, ty);

                if (!isEnabled()) {
                    boolean correct = game.isLetterCorrect(letter) && game.isLetterGuessed(letter);
                    if (correct) {
                        g2.setColor(new Color(80, 220, 130, 200));
                        g2.setStroke(new BasicStroke(2.5f));
                        int pad = 4;
                        g2.drawOval(pad, pad, getWidth() - pad * 2, getHeight() - pad * 2);
                    } else {
                        g2.setColor(new Color(255, 80, 80, 220));
                        g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        int m = 7;
                        g2.drawLine(m, m, getWidth() - m, getHeight() - m);
                        g2.drawLine(getWidth() - m, m, m, getHeight() - m);
                    }
                }
                g2.dispose();
            }

            @Override
            protected void paintBorder(Graphics g) {  }
        };

        btn.setPreferredSize(new Dimension(42, 42));
        btn.setFont(new Font("Monospaced", Font.BOLD, 14));
        btn.setForeground(new Color(220, 225, 255));
        btn.setBackground(BTN_DEFAULT);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btn.addActionListener(e -> {
            btn.setEnabled(false);
            boolean correct = game.getCurrentWord().indexOf(letter) >= 0;
            btn.setBackground(correct ? BTN_CORRECT : BTN_WRONG);
            btn.setForeground(Color.WHITE);
            game.onLetterGuessed(letter);
            btn.repaint();
        });

        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                if (btn.isEnabled()) btn.setBackground(new Color(60, 65, 95));
            }
            @Override public void mouseExited(MouseEvent e) {
                if (btn.isEnabled()) btn.setBackground(BTN_DEFAULT);
            }
        });

        return btn;
    }

    void reset() {
        for (Map.Entry<Character, JButton> entry : buttons.entrySet()) {
            JButton btn = entry.getValue();
            btn.setEnabled(true);
            btn.setBackground(BTN_DEFAULT);
            btn.setForeground(new Color(220, 225, 255));
            btn.repaint();
        }
    }
}

class RoundedBorder extends AbstractBorder {
    private final Color color;
    private final int   thickness;
    private final int   radius;

    RoundedBorder(Color color, int thickness, int radius) {
        this.color     = color;
        this.thickness = thickness;
        this.radius    = radius;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(thickness));
        g2.drawRoundRect(x + thickness / 2, y + thickness / 2,
                         w - thickness, h - thickness, radius, radius);
        g2.dispose();
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(radius / 2, radius / 2, radius / 2, radius / 2);
    }

    @Override
    public boolean isBorderOpaque() { return false; }
}   
