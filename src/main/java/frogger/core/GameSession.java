package frogger.core;

/** Small input protocol used by the web bridge and replay verification. */
public final class GameSession {
    private final GameEngine engine = new GameEngine();
    public GameEngine engine() { return engine; }
    public void start(String difficulty, int seed) { engine.start(Difficulty.valueOf(difficulty), seed); }
    public void command(String command) {
        switch (command) {
            case "up" -> engine.move(Direction.UP);
            case "right" -> engine.move(Direction.RIGHT);
            case "down" -> engine.move(Direction.DOWN);
            case "left" -> engine.move(Direction.LEFT);
            case "pause" -> engine.pause();
            case "resume" -> engine.resume();
            case "continue" -> engine.continueGame();
            case "menu" -> engine.menu();
            default -> throw new IllegalArgumentException("Unknown command: " + command);
        }
    }
    public void advance(int milliseconds) { engine.advance(milliseconds); }
    public String snapshot() { return Snapshot.json(engine); }
}
