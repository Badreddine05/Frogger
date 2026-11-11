package frogger.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Deterministic rules shared by Swing and the TeaVM browser build. No UI or wall-clock dependency. */
public final class GameEngine {
    public static final int COLUMNS = 13, ROWS = 13, LEVELS = 3, STEP_MS = 20, MOVE_COOLDOWN_MS = 120;
    public static final double FROG_RADIUS = 0.28;
    private static final int[] HOME_COLUMNS = {2, 6, 10};

    private GameState state = GameState.MENU;
    private Difficulty difficulty = Difficulty.CLASSIC;
    private final List<Lane> lanes = new ArrayList<>();
    private final boolean[] homes = new boolean[3];
    private int seed = 2021, level = 1, score, lives = 3, saved, remainingMs = 55_000;
    private int accumulatedMs, cooldownMs, bestRow = ROWS - 1, elapsedMs, eventId;
    private double frogX = 6.5;
    private int frogY = ROWS - 1;
    private Direction direction = Direction.UP;
    private String event = "READY", message = "Trois refuges. Une traversée à la fois.";

    public GameEngine() { createLanes(); }

    public void start(Difficulty difficulty, int seed) {
        if (difficulty == null) throw new IllegalArgumentException("Difficulty is required");
        this.difficulty = difficulty; this.seed = seed; level = 1; score = 0;
        lives = difficulty.lives; saved = 0; elapsedMs = 0;
        resetLevel(); state = GameState.PLAYING;
        emit("START", "Rejoignez un refuge libre, tout en haut.");
    }

    public void menu() { state = GameState.MENU; accumulatedMs = 0; emit("MENU", "Choisissez votre rythme."); }

    public void pause() {
        if (state == GameState.PLAYING) { state = GameState.PAUSED; accumulatedMs = 0; emit("PAUSE", "Le marais vous attend."); }
    }

    public void resume() {
        if (state == GameState.PAUSED) { state = GameState.PLAYING; emit("RESUME", "À vous de jouer."); }
    }

    public void continueGame() {
        if (state == GameState.PAUSED) { resume(); return; }
        if (state == GameState.LEVEL_CLEAR) { level++; resetLevel(); }
        else if (state == GameState.HIT || state == GameState.HOME) resetFrog();
        else return;
        state = GameState.PLAYING;
        emit("CONTINUE", "Rejoignez un refuge libre, tout en haut.");
    }

    /** An input never advances traffic or time. The fixed simulation step owns both. */
    public boolean move(Direction requested) {
        if (state != GameState.PLAYING || requested == null || cooldownMs > 0) return false;
        direction = requested;
        double nextX = frogX + requested.dx;
        int nextY = frogY + requested.dy;
        if (nextX < 0.5 || nextX > COLUMNS - 0.5 || nextY < 0 || nextY >= ROWS) return false;
        if (nextY == 0) {
            int slot = homeAt(nextX);
            if (slot < 0 || homes[slot]) { emit("BLOCKED", "Visez un nénuphar libre : les trois cercles lumineux."); return false; }
            frogX = HOME_COLUMNS[slot] + 0.5; frogY = 0;
            claimHome(slot); return true;
        }
        frogX = nextX; frogY = nextY; cooldownMs = MOVE_COOLDOWN_MS;
        checkHazard();
        if (state == GameState.PLAYING && frogY < bestRow) {
            score += (bestRow - frogY) * 10; bestRow = frogY;
        }
        return true;
    }

    /** Exact 20 ms integration; accepts 0..60,000 ms for deterministic replay. */
    public void advance(int milliseconds) {
        if (milliseconds < 0 || milliseconds > 60_000) throw new IllegalArgumentException("Expected 0..60000 ms");
        if (state != GameState.PLAYING) return;
        accumulatedMs += milliseconds;
        while (accumulatedMs >= STEP_MS && state == GameState.PLAYING) {
            accumulatedMs -= STEP_MS;
            step();
        }
        if (state != GameState.PLAYING) accumulatedMs = 0;
    }

    private void step() {
        Lane carrying = laneAt(frogY);
        if (carrying != null && carrying.kind() == Lane.Kind.RIVER) {
            frogX += carrying.speed() * STEP_MS / 1000.0;
        }
        for (Lane lane : lanes) lane.advance(STEP_MS / 1000.0);
        elapsedMs += STEP_MS;
        remainingMs = Math.max(0, remainingMs - STEP_MS);
        cooldownMs = Math.max(0, cooldownMs - STEP_MS);
        checkHazard();
        if (state == GameState.PLAYING && remainingMs == 0) loseLife("TIME", "Le temps est écoulé.");
    }

    private void checkHazard() {
        if (frogX < FROG_RADIUS || frogX > COLUMNS - FROG_RADIUS) {
            loseLife("EDGE", "Le courant vous a emporté hors du terrain."); return;
        }
        Lane lane = laneAt(frogY);
        if (lane == null) return;
        if (lane.kind() == Lane.Kind.ROAD && lane.hits(frogX)) loseLife("CAR", "Attention à la circulation.");
        else if (lane.kind() == Lane.Kind.RIVER && !lane.supports(frogX)) loseLife("WATER", "Pour traverser l'eau, sautez sur les troncs.");
    }

    private void loseLife(String reason, String explanation) {
        lives--;
        state = lives == 0 ? GameState.LOST : GameState.HIT;
        emit(reason, explanation);
    }

    private void claimHome(int slot) {
        homes[slot] = true; saved++;
        score += 200 + (remainingMs / 1000) * 2;
        if (homes[0] && homes[1] && homes[2]) {
            score += 500;
            state = level == LEVELS ? GameState.WON : GameState.LEVEL_CLEAR;
            emit(state == GameState.WON ? "VICTORY" : "LEVEL", state == GameState.WON
                    ? "Les neuf grenouilles sont à l'abri. Belle traversée !" : "Les trois refuges sont occupés. Cap sur la suite !");
        } else {
            state = GameState.HOME;
            emit("HOME", "Une grenouille à l'abri. Les autres vous attendent.");
        }
    }

    private void resetLevel() {
        for (int i = 0; i < homes.length; i++) homes[i] = false;
        createLanes(); resetFrog();
    }

    private void resetFrog() {
        frogX = 6.5; frogY = ROWS - 1; direction = Direction.UP;
        remainingMs = difficulty.timeLimitMs; accumulatedMs = 0; cooldownMs = 0; bestRow = ROWS - 1;
    }

    private void createLanes() {
        lanes.clear();
        // Seeded phase offsets keep a replay reproducible; lane geometry remains legible.
        int random = seed ^ (level * 0x9e3779b9);
        int[] rows = {11, 10, 9, 8, 6, 5, 4, 2, 1};
        for (int i = 0; i < rows.length; i++) {
            random ^= random << 13; random ^= random >>> 17; random ^= random << 5;
            double fraction = (random >>> 1) / 2147483647.0;
            boolean river = i >= 4;
            double speed = (river ? 0.6 + (i % 3) * 0.12 : 0.9 + (i % 3) * 0.3)
                    * difficulty.speed * (1 + (level - 1) * 0.16) * (i % 2 == 0 ? 1 : -1);
            double length = river ? 3.5 + (i % 2) * 0.6 : 1.35 + (i % 2) * 0.65;
            double gap = river ? 1.6 : 3.8 - (level - 1) * 0.25;
            lanes.add(new Lane(rows[i], river ? Lane.Kind.RIVER : Lane.Kind.ROAD, speed, length, gap, fraction * (length + gap)));
        }
    }

    private static int homeAt(double x) {
        for (int i = 0; i < HOME_COLUMNS.length; i++) if (Math.abs(x - HOME_COLUMNS[i] - 0.5) <= 0.65) return i;
        return -1;
    }
    private void emit(String type, String text) { event = type; message = text; eventId++; }
    public Lane laneAt(int row) { for (Lane lane : lanes) if (lane.row() == row) return lane; return null; }
    public List<Lane> lanes() { return Collections.unmodifiableList(lanes); }
    public GameState state() { return state; }
    public Difficulty difficulty() { return difficulty; }
    public int seed() { return seed; }
    public int level() { return level; }
    public int score() { return score; }
    public int lives() { return lives; }
    public int saved() { return saved; }
    public int remainingMs() { return remainingMs; }
    public int elapsedMs() { return elapsedMs; }
    public double frogX() { return frogX; }
    public int frogY() { return frogY; }
    public Direction direction() { return direction; }
    public boolean homeFilled(int i) { return homes[i]; }
    public int homeColumn(int i) { return HOME_COLUMNS[i]; }
    public String event() { return event; }
    public String message() { return message; }
    public int eventId() { return eventId; }
}
