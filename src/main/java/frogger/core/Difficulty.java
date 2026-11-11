package frogger.core;

/** Difficulty changes traffic speed, crossing time and starting lives, never collision rules. */
public enum Difficulty {
    WALK("Balade", 0.72, 70_000, 5),
    CLASSIC("Classique", 1.0, 55_000, 3),
    EXPRESS("Express", 1.25, 45_000, 3);

    public final String label;
    public final double speed;
    public final int timeLimitMs;
    public final int lives;
    Difficulty(String label, double speed, int timeLimitMs, int lives) {
        this.label = label; this.speed = speed; this.timeLimitMs = timeLimitMs; this.lives = lives;
    }
}
