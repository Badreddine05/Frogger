package frogger.core;

/** Explicit, versioned JSON boundary. The browser receives values, never mutable engine internals. */
public final class Snapshot {
    private Snapshot() {}
    public static String json(GameEngine g) {
        StringBuilder b = new StringBuilder(2500);
        b.append("{\"version\":1,\"state\":\"").append(g.state()).append("\",\"difficulty\":\"").append(g.difficulty())
            .append("\",\"seed\":").append(g.seed()).append(",\"level\":").append(g.level())
            .append(",\"score\":").append(g.score()).append(",\"lives\":").append(g.lives())
            .append(",\"saved\":").append(g.saved()).append(",\"remainingMs\":").append(g.remainingMs())
            .append(",\"timeLimitMs\":").append(g.difficulty().timeLimitMs).append(",\"elapsedMs\":").append(g.elapsedMs())
            .append(",\"frog\":{\"x\":").append(g.frogX()).append(",\"y\":").append(g.frogY())
            .append(",\"direction\":\"").append(g.direction()).append("\"},\"homes\":[");
        for (int i = 0; i < 3; i++) { if (i > 0) b.append(','); b.append(g.homeFilled(i)); }
        b.append("],\"event\":{\"id\":").append(g.eventId()).append(",\"type\":\"").append(g.event())
            .append("\",\"message\":\"").append(escape(g.message())).append("\"},\"lanes\":[");
        for (int i = 0; i < g.lanes().size(); i++) {
            if (i > 0) b.append(',');
            Lane lane = g.lanes().get(i);
            b.append("{\"row\":").append(lane.row()).append(",\"type\":\"").append(lane.kind())
                .append("\",\"speed\":").append(lane.speed()).append(",\"length\":").append(lane.length())
                .append(",\"positions\":[");
            int position = 0;
            for (double x : lane.positions()) { if (position++ > 0) b.append(','); b.append(x); }
            b.append("]}");
        }
        return b.append("]}").toString();
    }
    private static String escape(String s) { return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n"); }
}
