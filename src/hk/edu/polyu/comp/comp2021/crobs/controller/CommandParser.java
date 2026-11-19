package hk.edu.polyu.comp.comp2021.crobs.controller;

/** Whitespace-based command tokenizer. */
final class CommandParser {
    /** Prevents instantiation. */
    private CommandParser() { }

    /**
     * Splits input on whitespace.
     *
     * @param line input line
     * @return command tokens, or an empty array for blank input
     * @throws NullPointerException if the line is null
     */
    static String[] parse(String line) {
        String trimmed = line.trim();
        return trimmed.isEmpty() ? new String[0] : trimmed.split("\\s+");
    }
}
