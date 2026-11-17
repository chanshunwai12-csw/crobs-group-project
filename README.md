# crobs-group-project

## Bonus features

- Run `hk.edu.polyu.comp.comp2021.crobs.Application` in IntelliJ IDEA for the CLI.
- Add `--gui` as the first program argument for the Swing interface. Select a command,
  enter its parameters, and select **Run command**. Both modes accept
  `-html <log.html> -txt <log.txt>` after the mode flag, if present.
- Use `undo` and `redo` to step through successful user, room, and booking changes.
  Undoing a deletion also restores the bookings canceled by that deletion.
  Undoing a schedule command leaves the data as it was, and redoing it displays the schedule again.
- History lasts for one session. The original user or the superadmin can undo or redo
  the latest entry. A new history entry clears redo. Use the superadmin ID displayed at startup.
