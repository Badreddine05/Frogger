package frogger.desktop;

import frogger.core.GameEngine;
import frogger.core.GameState;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

/** Explicit headful integration check, launched separately from ordinary unit tests. */
public final class DesktopSmoke {
    private static Object app;
    private static JFrame frame;
    private static GameEngine engine;
    private DesktopSmoke() {}

    private static Object field(String name) throws Exception { Field f = Main.class.getDeclaredField(name); f.setAccessible(true); return f.get(app); }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static void capture(String directory, String name) throws Exception {
        if (directory == null) return;
        BufferedImage image = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics(); frame.paint(graphics); graphics.dispose();
        ImageIO.write(image, "png", new File(directory, name));
    }
    public static void main(String[] args) throws Exception {
        if (GraphicsEnvironment.isHeadless()) throw new IllegalStateException("A desktop or Xvfb display is required");
        String captures = args.length > 0 ? args[0] : null;
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    Constructor<Main> constructor = Main.class.getDeclaredConstructor(); constructor.setAccessible(true);
                    app = constructor.newInstance(); frame = (JFrame) field("frame"); engine = (GameEngine) field("engine");
                    require(frame.isVisible(), "Desktop frame must be visible");
                    require(engine.state() == GameState.MENU, "Starts in menu");
                    frame.validate(); capture(captures, "native-menu.png");
                    ((JButton) field("primary")).doClick(0);
                    require(engine.state() == GameState.PLAYING, "Start button starts a game");
                    frame.getRootPane().getActionMap().get("LEFT").actionPerformed(new ActionEvent(frame, 0, "LEFT"));
                    require(engine.frogX() == 5.5 && engine.elapsedMs() == 0, "Key binding moves frog without ticking world");
                    ((JButton) field("pause")).doClick(0);
                    require(engine.state() == GameState.PAUSED, "Pause button freezes game");
                    int time = engine.remainingMs(); engine.advance(2000);
                    require(engine.remainingMs() == time, "Paused time stays fixed");
                    ((JButton) field("primary")).doClick(0);
                    require(engine.state() == GameState.PLAYING, "Overlay resumes game");
                    frame.validate(); capture(captures, "native-playing.png");
                    frame.dispose();
                    require(!((Timer) field("timer")).isRunning() || !frame.isDisplayable(), "Closing releases the display");
                } catch (Exception e) { throw new RuntimeException(e); }
            });
            System.out.println("Desktop integration: visible menu, start, left key, pause, frozen clock, resume, close — OK");
        } finally {
            SwingUtilities.invokeAndWait(() -> { for (Window window : Window.getWindows()) window.dispose(); });
        }
    }
}
