# crobs-group-project

## Bonus features

- Run `hk.edu.polyu.comp.comp2021.crobs.Application` in IntelliJ IDEA for the CLI.
- Add `--gui` as the first program argument for the Swing interface. Select a command,
  enter its parameters, and select **Run command**. Both modes accept
  `-html <log.html> -txt <log.txt>` after the mode flag, if present.
- Use `undo` and `redo` to step through successful user, room, and booking changes.
  Undoing a deletion also restores the bookings canceled by that deletion.
- Undoing `schedule` restores the previously selected schedule, or clears the selection
  if there was none. Redo restores the next selection. The GUI has a separate
  **Current schedule** panel; the CLI prints the restored schedule or
  `No schedule selected.` The command transcript and logs are retained.
  Restored schedules use current bookings and current login permissions. The panel
  clears on logout or when the selected room no longer exists, and refreshes after
  login and data changes.
- History lasts for one session. The original user or the superadmin can undo or redo
  the latest entry. A new history entry clears redo. Use the superadmin ID displayed at startup.

## Empty feature lists

In the CLI, enter two literal double quotation marks (`""`) for an empty feature list:

```text
addRoom meetingRoom 10 ""
modifyRoom R001 features ""
search 2099-01-04 10:00 11:00 0 ""
```

The search example imposes no feature restriction but still checks capacity and availability.
In the GUI, leave **features** blank, or leave **newValue** blank when modifying the
`features` field. Other required fields must still be filled in. Nonempty feature lists
remain comma-separated without spaces.
