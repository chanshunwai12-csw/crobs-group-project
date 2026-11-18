package hk.edu.polyu.comp.comp2021.crobs.controller;

/** Splits a console line into a command name followed by its arguments. */
final class CommandParser {
    /** Prevents instantiation of the command tokenizer. */
    private CommandParser() { }

    /**
     * Splits a command at whitespace while ignoring leading and trailing whitespace.
     *
     * @param line the console line
     * @return the command name and arguments, or an empty array for blank input
     * @throws NullPointerException if the line is {@code null}
     */
    static String[] parse(String line) {
        String trimmed = line.trim();
        return trimmed.isEmpty() ? new String[0] : trimmed.split("\\s+");
    }
}
