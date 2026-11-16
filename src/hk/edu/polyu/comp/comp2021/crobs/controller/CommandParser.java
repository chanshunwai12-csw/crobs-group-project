package hk.edu.polyu.comp.comp2021.crobs.controller;

/** Splits a console line into a command name followed by its arguments. */
final class CommandParser {
    private CommandParser() { }

    /**
     * @param line the console line
     * @return the command name and arguments, or an empty array for blank input
     */
    static String[] parse(String line) {
        String trimmed = line.trim();
        return trimmed.isEmpty() ? new String[0] : trimmed.split("\\s+");
    }
}
