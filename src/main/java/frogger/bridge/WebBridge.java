package frogger.bridge;

import frogger.core.GameSession;
import org.teavm.jso.JSExport;

/** TeaVM exports: all game rules remain in the Java core, with no JavaScript reimplementation. */
public final class WebBridge {
    private static final GameSession SESSION = new GameSession();
    private WebBridge() {}
    @JSExport public static void start(String difficulty, int seed) { SESSION.start(difficulty, seed); }
    @JSExport public static void command(String action) { SESSION.command(action); }
    @JSExport public static void advance(int milliseconds) { SESSION.advance(milliseconds); }
    @JSExport public static String snapshot() { return SESSION.snapshot(); }
}
