package frogger.desktop;

import frogger.core.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;

/** Desktop adapter. All Swing creation, input and painting stay on the EDT. */
public final class Main {
    private final GameEngine engine = new GameEngine();
    private final JFrame frame = new JFrame("Frogger · La grande traversée");
    private final JLabel score = label("0", 19), lives = label("3 vies", 16), level = label("Étape 1 / 3", 16);
    private final JLabel event = label("Trois refuges. Une traversée à la fois.", 13);
    private final JProgressBar time = new JProgressBar(0, 55_000);
    private final JComboBox<Difficulty> mode = new JComboBox<>(Difficulty.values());
    private final JButton pause = button("Pause  ·  P", () -> togglePause());
    private final JPanel overlay = new JPanel();
    private final JLabel overlayTitle = label("La grande traversée", 26), overlayCopy = label(" ", 14);
    private final JButton primary = button("Commencer", () -> primary());
    private final JPanel board = new JPanel() {
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics); BoardPainter.paint((Graphics2D) graphics, engine, getWidth(), getHeight());
        }
    };
    private final Timer timer;
    private long lastFrame = System.nanoTime();
    private long fractionalNanos;
    private GameState previousState;
    private int lastEvent = -1;

    private Main() {
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        JPanel root = new JPanel(new BorderLayout(0, 14)); root.setBackground(Color.decode("#132a25")); root.setBorder(new EmptyBorder(18, 20, 16, 20));
        frame.setContentPane(root);
        JPanel top = row(); top.add(label("FROGGER", 25)); top.add(Box.createHorizontalGlue()); top.add(level); top.add(Box.createHorizontalStrut(25)); top.add(lives); top.add(Box.createHorizontalStrut(25)); top.add(score);
        root.add(top, BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout()); center.setOpaque(false);
        JPanel arena = new JPanel(); arena.setLayout(new OverlayLayout(arena)); arena.setBackground(Color.decode("#132a25"));
        board.setPreferredSize(new Dimension(650, 650)); board.setMinimumSize(new Dimension(300, 300)); board.setBackground(Color.decode("#132a25"));
        JPanel overlayHolder = new JPanel(new GridBagLayout()); overlayHolder.setOpaque(false); overlayHolder.add(overlay);
        overlay.setLayout(new BoxLayout(overlay, BoxLayout.Y_AXIS)); overlay.setBackground(Color.decode("#142f29")); overlay.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.decode("#729868")), new EmptyBorder(26, 30, 26, 30)));
        overlayTitle.setAlignmentX(.5f); overlayCopy.setAlignmentX(.5f); primary.setAlignmentX(.5f); mode.setAlignmentX(.5f);
        overlay.add(overlayTitle); overlay.add(Box.createVerticalStrut(12)); overlay.add(overlayCopy); overlay.add(Box.createVerticalStrut(18));
        mode.setSelectedItem(Difficulty.CLASSIC); mode.setMaximumSize(new Dimension(240, 36));
        mode.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                return super.getListCellRendererComponent(list, value instanceof Difficulty d ? d.label : value, index, selected, focus);
            }
        });
        overlay.add(mode); overlay.add(Box.createVerticalStrut(14)); overlay.add(primary);
        arena.add(overlayHolder); arena.add(board); center.add(arena, BorderLayout.CENTER);
        time.setStringPainted(true); time.setForeground(Color.decode("#bde477")); time.setBackground(Color.decode("#243b34")); time.setBorderPainted(false); time.setPreferredSize(new Dimension(300, 26));
        time.setUI(new javax.swing.plaf.basic.BasicProgressBarUI() {
            @Override protected Color getSelectionForeground() { return Color.decode("#173126"); }
            @Override protected Color getSelectionBackground() { return Color.decode("#e9efdb"); }
        });
        center.add(time, BorderLayout.SOUTH); root.add(center, BorderLayout.CENTER);
        JPanel bottom = new JPanel(); bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS)); bottom.setOpaque(false);
        JPanel info = row(); info.add(event); bottom.add(info); bottom.add(Box.createVerticalStrut(12));
        JPanel controls = row(); controls.add(pause); controls.add(Box.createHorizontalStrut(8)); controls.add(button("Nouvelle partie", () -> newGame()));
        controls.add(Box.createHorizontalStrut(8)); controls.add(button("Aide  ·  H", () -> help())); controls.add(Box.createHorizontalGlue());
        controls.add(label("Flèches / ZQSD / WASD", 12)); bottom.add(controls); root.add(bottom, BorderLayout.SOUTH);
        bind("UP", () -> engine.move(Direction.UP)); bind("W", () -> engine.move(Direction.UP)); bind("Z", () -> engine.move(Direction.UP));
        bind("DOWN", () -> engine.move(Direction.DOWN)); bind("S", () -> engine.move(Direction.DOWN));
        bind("LEFT", () -> engine.move(Direction.LEFT)); bind("A", () -> engine.move(Direction.LEFT)); bind("Q", () -> engine.move(Direction.LEFT));
        bind("RIGHT", () -> engine.move(Direction.RIGHT)); bind("D", () -> engine.move(Direction.RIGHT));
        bind("P", this::togglePause); bind("ESCAPE", this::togglePause); bind("ENTER", this::primary); bind("H", this::help);
        frame.addWindowFocusListener(new WindowAdapter() { @Override public void windowLostFocus(WindowEvent e) { engine.pause(); sync(); } });
        timer = new Timer(16, e -> {
            long now = System.nanoTime(); fractionalNanos += now - lastFrame; lastFrame = now;
            int delta = (int) (fractionalNanos / 1_000_000); fractionalNanos %= 1_000_000;
            if (engine.state() == GameState.PLAYING) {
                engine.advance(Math.min(100, delta)); sync(); board.repaint();
            } else fractionalNanos = 0;
        });
        frame.addWindowListener(new WindowAdapter() { @Override public void windowClosed(WindowEvent e) { timer.stop(); } });
        frame.setMinimumSize(new Dimension(650, 780)); frame.pack(); frame.setLocationByPlatform(true); sync(); frame.setVisible(true); timer.start();
    }

    private void bind(String key, Runnable action) {
        JRootPane root = frame.getRootPane(); root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), key);
        root.getActionMap().put(key, new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { action.run(); sync(); board.repaint(); } });
    }
    private void togglePause() { if (engine.state() == GameState.PAUSED) engine.resume(); else engine.pause(); sync(); }
    private void primary() {
        if (engine.state() == GameState.MENU || engine.state() == GameState.WON || engine.state() == GameState.LOST)
            engine.start((Difficulty) mode.getSelectedItem(), (int) (System.nanoTime() & 0x7fffffff));
        else engine.continueGame();
        sync(); board.repaint(); frame.getRootPane().requestFocusInWindow();
    }
    private void newGame() {
        boolean active = engine.state() == GameState.PLAYING;
        engine.pause(); sync();
        if (engine.state() == GameState.MENU || JOptionPane.showConfirmDialog(frame, "Revenir au menu et abandonner cette partie ?", "Nouvelle partie", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            engine.menu(); sync();
        } else if (active) { engine.resume(); sync(); }
    }
    private void help() {
        engine.pause(); sync();
        JOptionPane.showMessageDialog(frame,
                "Objectif : remplir 3 refuges différents, pendant 3 étapes.\n\n"
                + "Flèches, ZQSD ou WASD : un saut.\nP / Échap : pause. Entrée : commencer ou continuer.\n\n"
                + "Évitez les véhicules. Sur l'eau, restez sur les troncs :\nils vous transportent, mais le bord est dangereux.\n"
                + "Les bandes vertes permettent d'attendre. Le temps reste limité.\n\n"
                + "Score : 10 points par nouvelle rangée, 200 par refuge\n+ 2 par seconde restante, 500 pour une étape complète.\n\n"
                + "La partie reste en pause après fermeture de l'aide.", "Comment jouer", JOptionPane.INFORMATION_MESSAGE);
    }
    private void sync() {
        score.setText(String.format("%,d pts", engine.score())); lives.setText(engine.lives() + (engine.lives() == 1 ? " vie" : " vies")); level.setText("Étape " + engine.level() + " / 3");
        time.setMaximum(engine.difficulty().timeLimitMs); time.setValue(engine.remainingMs()); time.setString((int) Math.ceil(engine.remainingMs() / 1000.0) + " s · " + engine.saved() + " / 9 à l'abri");
        if (engine.eventId() != lastEvent) { event.setText(engine.message()); lastEvent = engine.eventId(); }
        pause.setText(engine.state() == GameState.PAUSED ? "Reprendre  ·  P" : "Pause  ·  P");
        pause.setEnabled(engine.state() == GameState.PLAYING || engine.state() == GameState.PAUSED);
        if (previousState != engine.state()) {
            previousState = engine.state(); overlay.setVisible(engine.state() != GameState.PLAYING);
            mode.setVisible(engine.state() == GameState.MENU || engine.state() == GameState.LOST || engine.state() == GameState.WON);
            String title, copy, action;
            switch (engine.state()) {
                case MENU -> { title = "La grande traversée"; copy = "<html><center>Routes, rivière et petits sauts.<br>Ramenez 9 grenouilles à l'abri.</center></html>"; action = "Commencer  ·  Entrée"; }
                case PAUSED -> { title = "Une petite pause"; copy = "Le trafic et le chronomètre sont arrêtés."; action = "Reprendre"; }
                case HIT -> { title = "On retente ?"; copy = "<html><center>" + engine.message() + "<br>Il reste " + engine.lives() + " vie(s).</center></html>"; action = "Continuer"; }
                case HOME -> { title = "Bien arrivé !"; copy = "Une grenouille de plus à l'abri."; action = "Traversée suivante"; }
                case LEVEL_CLEAR -> { title = "Étape réussie"; copy = "La circulation accélère à l'étape suivante."; action = "Étape " + (engine.level() + 1); }
                case WON -> { title = "Tout le monde est à l'abri"; copy = engine.score() + " points · 9 sauvetages"; action = "Rejouer"; }
                case LOST -> { title = "Fin de la traversée"; copy = engine.score() + " points · " + engine.saved() + " sauvetage(s)"; action = "Rejouer"; }
                default -> { title = ""; copy = ""; action = "Continuer"; }
            }
            overlayTitle.setText(title); overlayCopy.setText(copy); primary.setText(action);
            overlay.revalidate(); overlay.repaint();
        }
    }
    private static JPanel row() { JPanel p = new JPanel(); p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS)); p.setOpaque(false); return p; }
    private static JLabel label(String text, int size) { JLabel l = new JLabel(text); l.setForeground(Color.decode("#e9efdb")); l.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, size)); return l; }
    private static JButton button(String text, Runnable action) { JButton b = new JButton(text); b.addActionListener(e -> action.run()); b.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13)); b.setMargin(new Insets(10, 14, 10, 14)); return b; }

    public static void main(String[] args) throws Exception {
        if (args.length == 2 && args[0].equals("--render")) {
            BufferedImage image = new BufferedImage(780, 780, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics(); BoardPainter.paint(g, new GameEngine(), 780, 780); g.dispose();
            ImageIO.write(image, "png", new File(args[1])); return;
        }
        SwingUtilities.invokeLater(() -> {
            Main app = new Main();
            if (args.length > 0 && args[0].equals("--smoke")) {
                Timer close = new Timer(1500, e -> { app.frame.dispose(); System.out.println("Swing EDT startup and shutdown: OK"); });
                close.setRepeats(false); close.start();
            }
        });
    }
}
