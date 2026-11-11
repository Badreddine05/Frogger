package frogger.core;

/** Emits snapshots of a public-input replay for the Node test of the actual TeaVM bundle. */
public final class ParityTrace {
    public static void main(String[] args) {
        GameSession session = new GameSession();
        for (String action : args) {
            String[] parts = action.split(":");
            if (parts[0].equals("start")) session.start(parts[1], Integer.parseInt(parts[2]));
            else if (parts[0].equals("tick")) session.advance(Integer.parseInt(parts[1]));
            else session.command(action);
            System.out.println(session.snapshot());
        }
    }
}
