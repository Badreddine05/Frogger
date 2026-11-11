package frogger.core;

import java.util.ArrayList;
import java.util.List;

/** A repeating lane. Position is measured in cells; speed in cells per second. */
public final class Lane {
    public enum Kind { ROAD, RIVER }
    private final int row;
    private final Kind kind;
    private final double speed;
    private final double length;
    private final double spacing;
    private double phase;

    public Lane(int row, Kind kind, double speed, double length, double gap, double phase) {
        if (row < 1 || row >= GameEngine.ROWS - 1 || length <= 0 || gap <= 0) {
            throw new IllegalArgumentException("Invalid lane geometry");
        }
        this.row = row; this.kind = kind; this.speed = speed;
        this.length = length; this.spacing = length + gap; this.phase = modulo(phase, spacing);
    }
    public int row() { return row; }
    public Kind kind() { return kind; }
    public double speed() { return speed; }
    public double length() { return length; }
    public double phase() { return phase; }
    public double spacing() { return spacing; }

    void advance(double seconds) { phase = modulo(phase + speed * seconds, spacing); }

    public List<Double> positions() {
        List<Double> positions = new ArrayList<>();
        for (double x = phase - spacing; x < GameEngine.COLUMNS; x += spacing) {
            if (x + length >= 0) positions.add(x);
        }
        return positions;
    }

    public boolean hits(double centerX) {
        for (double x : positions()) {
            if (centerX + GameEngine.FROG_RADIUS > x && centerX - GameEngine.FROG_RADIUS < x + length) return true;
        }
        return false;
    }

    public boolean supports(double centerX) {
        for (double x : positions()) {
            // Requiring the frog's centre inside the log, with an explicit 0.1-cell margin,
            // makes a successful landing readable without pixel-perfect input.
            if (centerX >= x + 0.1 && centerX <= x + length - 0.1) return true;
        }
        return false;
    }

    private static double modulo(double n, double divisor) { return (n % divisor + divisor) % divisor; }
}
