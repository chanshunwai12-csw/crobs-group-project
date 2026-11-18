package hk.edu.polyu.comp.comp2021.crobs.model;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Writes the same command sequence to an HTML table and a UTF-8 text file. */
public final class CommandLogger implements AutoCloseable {
    /** Error shown when both logs resolve to the same destination. */
    private static final String SAME_LOG_FILE = "HTML and TXT logs must use different files.";
    /** HTML document and command-table header written when a session starts. */
    private static final String HTML_HEADER = "<!doctype html>\n<html lang=\"en\"><head>"
            + "<meta charset=\"utf-8\"><title>Crobs command log</title></head>\n<body><table>\n"
            + "<thead><tr><th scope=\"col\">Index</th><th scope=\"col\">Command</th></tr></thead>\n<tbody>\n";
    /** Markup that closes the command table and document. */
    private static final String HTML_FOOTER = "</tbody>\n</table></body></html>\n";

    /** Writable channel for the HTML command table. */
    private final FileChannel htmlLog;
    /** Writable channel for the plain-text command log. */
    private final FileChannel txtLog;
    /** One-based index of the next logged operation. */
    private long nextIndex = 1;

    /**
     * Opens the two log files for a new session.
     *
     * @param htmlPath path to the HTML log
     * @param txtPath path to the text log
     * @throws IOException if the files cannot be opened or have the same destination
     */
    public CommandLogger(Path htmlPath, Path txtPath) throws IOException {
        if (htmlPath.toAbsolutePath().normalize().equals(txtPath.toAbsolutePath().normalize())) {
            throw new IOException(SAME_LOG_FILE);
        }

        FileChannel first = openLog(htmlPath);
        FileChannel second;
        try {
            second = openLog(txtPath);
        } catch (IOException | RuntimeException failure) {
            closeAfterFailure(first, failure);
            throw failure;
        }
        try {
            if (Files.isSameFile(htmlPath, txtPath)) {
                throw new IOException(SAME_LOG_FILE);
            }
            // Opening both files first preserves existing content if one path cannot be opened.
            first.truncate(0);
            second.truncate(0);
            write(first, HTML_HEADER);
        } catch (IOException | RuntimeException failure) {
            closeAfterFailure(second, failure);
            closeAfterFailure(first, failure);
            throw failure;
        }
        htmlLog = first;
        txtLog = second;
    }

    /**
     * Records one unmodified input line in both logs.
     *
     * @param command the input line, without its line separator
     * @throws IOException if either log write fails
     */
    public synchronized void log(String command) throws IOException {
        long htmlPosition = htmlLog.position();
        long txtPosition = txtLog.position();
        try {
            write(htmlLog, "<tr><td>" + nextIndex + "</td><td><code style=\"white-space: pre-wrap\">"
                    + escapeHtml(command) + "</code></td></tr>\n");
            write(txtLog, command + System.lineSeparator());
            nextIndex++;
        } catch (IOException failure) {
            rollback(htmlLog, htmlPosition, failure);
            rollback(txtLog, txtPosition, failure);
            throw failure;
        }
    }

    /**
     * Finishes the HTML table and closes both logs.
     *
     * @throws IOException if finishing or closing a log fails
     */
    @Override
    public synchronized void close() throws IOException {
        if (!htmlLog.isOpen()) {
            return;
        }
        try (FileChannel html = htmlLog) {
            try {
                write(html, HTML_FOOTER);
            } catch (IOException | RuntimeException | Error failure) {
                closeAfterFailure(txtLog, failure);
                throw failure;
            }
            txtLog.close();
        }
    }

    /**
     * Opens a writable log without truncating existing content.
     * @param path the destination path
     * @return the opened channel
     * @throws IOException if the destination cannot be opened
     */
    private static FileChannel openLog(Path path) throws IOException {
        return FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    }

    /**
     * Writes the entire UTF-8 representation of a string, including partial writes.
     * @param file the destination channel
     * @param text the text to encode and write
     * @throws IOException if writing fails or makes no progress
     */
    private static void write(FileChannel file, String text) throws IOException {
        ByteBuffer bytes = StandardCharsets.UTF_8.encode(text);
        while (bytes.hasRemaining()) {
            if (file.write(bytes) == 0) {
                throw new IOException("Could not make progress writing the command log.");
            }
        }
    }

    /**
     * Restores a log's position after a failed command write.
     * @param file the affected log channel
     * @param position its position before the command
     * @param failure the original failure to which rollback errors are attached
     */
    private static void rollback(FileChannel file, long position, IOException failure) {
        try {
            file.truncate(position);
            file.position(position);
        } catch (IOException rollbackFailure) {
            failure.addSuppressed(rollbackFailure);
        }
    }

    /**
     * Closes a channel while preserving the original failure.
     * @param file the channel to close
     * @param failure the original failure to which close errors are attached
     */
    private static void closeAfterFailure(FileChannel file, Throwable failure) {
        try {
            file.close();
        } catch (IOException closeFailure) {
            failure.addSuppressed(closeFailure);
        }
    }

    /**
     * Escapes command text before placing it in the HTML table.
     * @param command the original command line
     * @return text safe for inclusion inside the table cell
     */
    private static String escapeHtml(String command) {
        return command.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
