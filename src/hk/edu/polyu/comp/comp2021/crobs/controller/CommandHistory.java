package hk.edu.polyu.comp.comp2021.crobs.controller;

import hk.edu.polyu.comp.comp2021.crobs.model.CROBS;
import hk.edu.polyu.comp.comp2021.crobs.model.User;

import java.util.ArrayDeque;
import java.util.Deque;

/** Session-wide history of successful reversible Crobs commands. */
final class CommandHistory {
    private final Deque<Change> undo = new ArrayDeque<>();
    private final Deque<Change> redo = new ArrayDeque<>();

    void record(String actorId, String command, CROBS.State before, CROBS.State after) {
        record(new Change(actorId, command, before, after, null));
    }

    void recordSchedule(String actorId, String command, Runnable replay) {
        record(new Change(actorId, command, null, null, replay));
    }

    private void record(Change change) {
        undo.push(change);
        redo.clear();
    }

    String undo(CROBS crobs) {
        Change change = requireChange(undo, crobs, "undo");
        if (change.before != null) {
            crobs.restore(change.before);
        }
        redo.push(undo.pop());
        return "Undid: " + change.command;
    }

    String redo(CROBS crobs) {
        Change change = requireChange(redo, crobs, "redo");
        if (change.replay != null) {
            change.replay.run();
        } else {
            crobs.restore(change.after);
        }
        undo.push(redo.pop());
        return "Redid: " + change.command;
    }

    private static Change requireChange(Deque<Change> history, CROBS crobs, String action) {
        User user = crobs.getCurrentUser();
        if (user == null) {
            throw new SecurityException("Login is required.");
        }
        Change change = history.peek();
        if (change == null) {
            throw new IllegalStateException("Nothing to " + action + ".");
        }
        if (!change.actorId.equals(user.getId()) && !crobs.getSuperadminId().equals(user.getId())) {
            throw new SecurityException("Only the original user or the superadmin can " + action
                    + " the latest change.");
        }
        return change;
    }

    private static final class Change {
        private final String actorId;
        private final String command;
        private final CROBS.State before;
        private final CROBS.State after;
        private final Runnable replay;

        private Change(String actorId, String command, CROBS.State before, CROBS.State after, Runnable replay) {
            this.actorId = actorId;
            this.command = command;
            this.before = before;
            this.after = after;
            this.replay = replay;
        }
    }
}
