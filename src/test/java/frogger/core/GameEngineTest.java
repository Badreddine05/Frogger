package frogger.core;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Rule tests use explicit lane/position fixtures; no debug setters are shipped in the public engine. */
class GameEngineTest {
    private static GameEngine game() { GameEngine g = new GameEngine(); g.start(Difficulty.CLASSIC, 42); return g; }
    private static void set(GameEngine g, String name, Object value) throws Exception {
        Field f = GameEngine.class.getDeclaredField(name); f.setAccessible(true); f.set(g, value);
    }
    @SuppressWarnings("unchecked")
    private static void lanes(GameEngine g, Lane... lanes) throws Exception {
        Field f = GameEngine.class.getDeclaredField("lanes"); f.setAccessible(true);
        List<Lane> values = (List<Lane>) f.get(g); values.clear(); values.addAll(List.of(lanes));
    }
    private static void position(GameEngine g, double x, int y) throws Exception { set(g, "frogX", x); set(g, "frogY", y); set(g, "cooldownMs", 0); }

    @Test void movingAndBlockedMovesNeverAdvanceTrafficOrTime() {
        GameEngine g = game(); double before = g.lanes().get(0).phase();
        assertTrue(g.move(Direction.LEFT));
        assertEquals(Direction.LEFT, g.direction());
        assertEquals(before, g.lanes().get(0).phase()); assertEquals(0, g.elapsedMs());
        assertFalse(g.move(Direction.LEFT), "input rate is bounded");
        GameEngine edge = game(); assertFalse(edge.move(Direction.DOWN));
        assertEquals(0, edge.elapsedMs()); assertEquals(Direction.DOWN, edge.direction());
    }

    @Test void fixedStepIsIndependentOfFramePartitioning() {
        GameEngine one = game(), many = game(); one.advance(1000);
        for (int i = 0; i < 100; i++) many.advance(10);
        assertEquals(Snapshot.json(one), Snapshot.json(many));
    }

    @Test void pauseFreezesTrafficTimerAndInput() {
        GameEngine g = game(); g.advance(100); g.pause(); String frozen = Snapshot.json(g);
        g.advance(5000); assertFalse(g.move(Direction.UP)); assertEquals(frozen, Snapshot.json(g));
        g.resume(); g.advance(20); assertEquals(120, g.elapsedMs());
    }

    @Test void carIsCheckedImmediatelyOnLandingAndOnlyCostsOneLife() throws Exception {
        GameEngine g = game(); lanes(g, new Lane(11, Lane.Kind.ROAD, 1, 2, 4, 6));
        g.move(Direction.UP); assertEquals(GameState.HIT, g.state()); assertEquals(2, g.lives());
        g.advance(1000); g.move(Direction.UP); assertEquals(2, g.lives()); assertEquals(0, g.elapsedMs());
        g.continueGame(); assertEquals(GameState.PLAYING, g.state()); assertEquals(12, g.frogY());
    }

    @Test void movingCarCanHitAStationaryFrog() throws Exception {
        GameEngine g = game(); lanes(g, new Lane(11, Lane.Kind.ROAD, 1, 1, 8, 4.5));
        position(g, 6.5, 11); g.advance(800);
        assertEquals(GameState.HIT, g.state()); assertEquals("CAR", g.event());
    }

    @Test void unsupportedRiverLandingLosesALife() throws Exception {
        GameEngine g = game(); lanes(g, new Lane(6, Lane.Kind.RIVER, 1, 2, 6, 0));
        position(g, 6.5, 7); g.move(Direction.UP);
        assertEquals(GameState.HIT, g.state()); assertEquals("WATER", g.event());
    }

    @Test void logCarriesFrogAtItsOwnSpeed() throws Exception {
        GameEngine g = game(); lanes(g, new Lane(6, Lane.Kind.RIVER, 1.5, 4, 3, 4));
        position(g, 6.5, 6); g.advance(1000);
        assertEquals(GameState.PLAYING, g.state()); assertEquals(8.0, g.frogX(), 1e-9);
    }

    @Test void driftOutsideBoardCostsOneLife() throws Exception {
        GameEngine g = game(); lanes(g, new Lane(6, Lane.Kind.RIVER, 2, 4, 3, 10));
        position(g, 12.7, 6); g.advance(100);
        assertEquals(GameState.HIT, g.state()); assertEquals("EDGE", g.event());
    }

    @Test void timeoutStopsAtZeroAndLastLifeEndsCampaign() throws Exception {
        GameEngine g = game(); set(g, "remainingMs", 20); set(g, "lives", 1); g.advance(100);
        assertEquals(GameState.LOST, g.state()); assertEquals(0, g.remainingMs()); assertEquals(0, g.lives());
        String finalState = Snapshot.json(g); g.advance(60_000); g.continueGame();
        assertEquals(finalState, Snapshot.json(g));
    }

    @Test void backwardMovementCannotFarmProgressPoints() throws Exception {
        GameEngine g = game(); lanes(g); g.move(Direction.UP); assertEquals(10, g.score());
        g.advance(120); g.move(Direction.DOWN); g.advance(120); g.move(Direction.UP);
        assertEquals(10, g.score());
    }

    @Test void occupiedRefugeCannotScoreTwice() throws Exception {
        GameEngine g = game(); position(g, 2.5, 1); g.move(Direction.UP);
        assertEquals(GameState.HOME, g.state()); assertEquals(1, g.saved()); int score = g.score();
        g.continueGame(); position(g, 2.5, 1); assertFalse(g.move(Direction.UP));
        assertEquals(score, g.score()); assertEquals(1, g.saved()); assertEquals(3, g.lives());
    }

    @Test void threeDistinctRefugesAdvanceLevelAndNineWin() throws Exception {
        GameEngine g = game();
        for (int level = 1; level <= 3; level++) {
            for (int slot = 0; slot < 3; slot++) {
                position(g, g.homeColumn(slot) + 0.5, 1); g.move(Direction.UP);
                assertTrue(g.homeFilled(slot));
                assertEquals(slot == 2 ? (level == 3 ? GameState.WON : GameState.LEVEL_CLEAR) : GameState.HOME, g.state());
                if (slot < 2) g.continueGame();
            }
            if (level < 3) { g.continueGame(); assertEquals(level + 1, g.level()); assertFalse(g.homeFilled(0)); }
        }
        assertEquals(9, g.saved()); assertEquals(3, g.level());
        String finalState = Snapshot.json(g); g.advance(2000); assertEquals(finalState, Snapshot.json(g));
    }

    @Test void newGameResetsCampaignAndReplaysSameSeed() throws Exception {
        GameEngine g = game(); position(g, 2.5, 1); g.move(Direction.UP);
        g.start(Difficulty.CLASSIC, 42); GameEngine fresh = game();
        assertEquals(fresh.score(), g.score()); assertEquals(fresh.saved(), g.saved());
        assertEquals(fresh.lanes().get(0).phase(), g.lanes().get(0).phase()); assertEquals(3, g.lives());
    }

    @Test void difficultyChangesOnlyDeclaredParameters() {
        GameEngine walk = new GameEngine(), express = new GameEngine();
        walk.start(Difficulty.WALK, 99); express.start(Difficulty.EXPRESS, 99);
        assertEquals(5, walk.lives()); assertEquals(3, express.lives());
        assertEquals(70_000, walk.remainingMs()); assertEquals(45_000, express.remainingMs());
        assertEquals(walk.lanes().get(0).length(), express.lanes().get(0).length());
        assertTrue(Math.abs(walk.lanes().get(0).speed()) < Math.abs(express.lanes().get(0).speed()));
    }

    @Test void invalidTimeCannotRewindSimulation() {
        GameEngine g = game(); assertThrows(IllegalArgumentException.class, () -> g.advance(-1));
        assertThrows(IllegalArgumentException.class, () -> g.advance(60_001)); assertEquals(0, g.elapsedMs());
    }

    @Test void wrappedLaneGeometryRemainsConsistent() {
        Lane lane = new Lane(1, Lane.Kind.RIVER, -2, 4, 2, 0);
        assertTrue(lane.supports(1)); lane.advance(3);
        assertEquals(0, lane.phase(), 1e-9); assertTrue(lane.supports(1));
    }
}
