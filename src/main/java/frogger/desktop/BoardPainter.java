package frogger.desktop;

import frogger.core.*;
import java.awt.*;
import java.awt.geom.*;

/** Original vector artwork. The painter observes the model and never changes it. */
public final class BoardPainter {
    private BoardPainter() {}
    public static void paint(Graphics2D original, GameEngine engine, int width, int height) {
        Graphics2D g = (Graphics2D) original.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double size = Math.min(width, height), cell = size / GameEngine.COLUMNS;
        g.translate((width - size) / 2, (height - size) / 2); g.scale(cell, cell);
        g.setClip(new Rectangle2D.Double(0, 0, 13, 13));
        for (int row = 0; row < 13; row++) {
            Lane lane = engine.laneAt(row);
            g.setColor(Color.decode(lane == null ? (row == 0 ? "#244b39" : "#345541")
                    : lane.kind() == Lane.Kind.RIVER ? "#17464c" : "#27343d"));
            g.fill(new Rectangle2D.Double(0, row, 13, 1));
            if (lane == null) {
                for (int x = 0; x < 13; x++) {
                    g.setColor(new Color(140, 177, 118, 50));
                    g.fill(new RoundRectangle2D.Double(x + 0.18, row + 0.65, 0.12, 0.06, 0.03, 0.03));
                    g.fill(new RoundRectangle2D.Double(x + 0.7, row + 0.2, 0.06, 0.14, 0.03, 0.03));
                }
            } else if (lane.kind() == Lane.Kind.ROAD) {
                g.setColor(new Color(224, 231, 211, 45));
                for (int x = 0; x < 13; x++) g.fill(new Rectangle2D.Double(x + 0.15, row + 0.97, 0.4, 0.025));
            } else {
                g.setColor(new Color(127, 198, 193, 45));
                for (int x = 0; x < 13; x += 2) g.fill(new RoundRectangle2D.Double(x + 0.2, row + 0.18, 0.65, 0.025, 0.02, 0.02));
            }
        }
        for (Lane lane : engine.lanes()) for (double x : lane.positions()) {
            if (lane.kind() == Lane.Kind.RIVER) log(g, x, lane.row(), lane.length());
            else car(g, x, lane.row(), lane.length(), lane.speed());
        }
        for (int slot = 0; slot < 3; slot++) {
            double x = engine.homeColumn(slot) + 0.5;
            g.setColor(Color.decode(engine.homeFilled(slot) ? "#bde477" : "#1c3529"));
            g.fill(new Ellipse2D.Double(x - .43, .08, .86, .84));
            g.setColor(Color.decode("#bde477")); g.setStroke(new BasicStroke(.035f));
            g.draw(new Ellipse2D.Double(x - .43, .08, .86, .84));
            if (engine.homeFilled(slot)) frog(g, x, .52, Direction.UP, .57);
            else { g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 1).deriveFont(.28f)); g.drawString(Integer.toString(slot + 1), (float) x - .08f, .61f); }
        }
        if (engine.state() != GameState.HOME && engine.state() != GameState.LEVEL_CLEAR && engine.state() != GameState.WON) {
            frog(g, engine.frogX(), engine.frogY() + .5, engine.direction(), .9);
        }
        if (engine.state() == GameState.HIT || engine.state() == GameState.LOST) {
            g.setColor(new Color(255, 143, 118, 200)); g.setStroke(new BasicStroke(.07f));
            g.draw(new Ellipse2D.Double(engine.frogX() - .48, engine.frogY() + .02, .96, .96));
        }
        g.dispose();
    }

    private static void log(Graphics2D g, double x, double row, double length) {
        g.setColor(Color.decode("#163635")); g.fill(new RoundRectangle2D.Double(x, row + .18, length, .7, .25, .25));
        g.setColor(Color.decode("#b18a5b")); g.fill(new RoundRectangle2D.Double(x, row + .13, length, .65, .22, .22));
        g.setColor(Color.decode("#d1ae77")); g.setStroke(new BasicStroke(.035f));
        g.draw(new Line2D.Double(x + .25, row + .32, x + length - .25, row + .32));
        g.draw(new Line2D.Double(x + .5, row + .57, x + length - .4, row + .57));
        g.draw(new Ellipse2D.Double(x + .08, row + .23, .17, .4));
        g.draw(new Ellipse2D.Double(x + length - .25, row + .23, .17, .4));
    }

    private static void car(Graphics2D g, double x, double row, double length, double speed) {
        g.setColor(Color.decode("#101e27"));
        g.fill(new RoundRectangle2D.Double(x + .2, row + .13, .25, .72, .06, .06));
        g.fill(new RoundRectangle2D.Double(x + length - .45, row + .13, .25, .72, .06, .06));
        g.setColor(Color.decode(((int) row % 2 == 0) ? "#e4ad71" : "#e27e60"));
        g.fill(new RoundRectangle2D.Double(x, row + .21, length, .58, .14, .14));
        double cabin = speed > 0 ? x + length - .64 : x + .2;
        g.setColor(Color.decode("#28434b"));
        g.fill(new RoundRectangle2D.Double(cabin, row + .27, .44, .46, .07, .07));
        g.setColor(Color.decode("#e8e2ba"));
        double light = speed > 0 ? x + length - .07 : x + .03;
        g.fill(new Rectangle2D.Double(light, row + .29, .05, .11));
        g.fill(new Rectangle2D.Double(light, row + .6, .05, .11));
    }

    private static void frog(Graphics2D original, double x, double y, Direction direction, double scale) {
        Graphics2D g = (Graphics2D) original.create(); g.translate(x, y);
        g.rotate(direction.ordinal() * Math.PI / 2); g.scale(scale, scale);
        g.setColor(new Color(6, 29, 25, 100)); g.fill(new Ellipse2D.Double(-.33, -.2, .66, .58));
        g.setColor(Color.decode("#87ba55"));
        for (int sign : new int[]{-1, 1}) {
            g.fill(new Ellipse2D.Double(sign * .28 - .12, -.18, .24, .19));
            g.fill(new Ellipse2D.Double(sign * .27 - .12, .13, .26, .18));
        }
        g.setColor(Color.decode("#caed8d")); g.fill(new RoundRectangle2D.Double(-.26, -.27, .52, .57, .25, .25));
        for (int sign : new int[]{-1, 1}) {
            g.setColor(Color.decode("#e5f7be")); g.fill(new Ellipse2D.Double(sign * .18 - .1, -.33, .2, .2));
            g.setColor(Color.decode("#19382d")); g.fill(new Ellipse2D.Double(sign * .18 - .04, -.29, .08, .08));
        }
        g.dispose();
    }
}
