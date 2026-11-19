package hk.edu.polyu.comp.comp2021.crobs.model;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Writes commands to UTF-8 HTML and TXT logs. */
public final class CommandLogger implements AutoCloseable {
    /** Error for identical log destinations. */
    private static final String SAME_LOG_FILE = "HTML and TXT logs must use different files.";
    /** HTML document and table header. */
    private static final String HTML_HEADER = "<!doctype html>\n<html lang=\"en\"><head>"
            + "<meta charset=\"utf-8\"><title>Crobs command log</title></head>\n<body><table>\n"
            + "<thead><tr><th scope=\"col\">Index</th><th scope=\"col\">Command</th></tr></thead>\n<tbody>\n";
    /** HTML closing markup. */
    private static final String HTML_FOOTER = "</tbody>\n</table></body></html>\n";

    /** HTML log channel. */
    private final FileChannel htmlLog;
    /** TXT log channel. */
    private final FileChannel txtLog;
    /** Next one-based operation index. */
    private long nextIndex = 1;

    /**
     * Starts fresh logs at two distinct paths.
     *
     * @param htmlPath HTML log path
     * @param txtPath TXT log path
     * @throws IOException if a file cannot open or both paths name the same file
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
     * Writes the original command to both logs; rolls back failed writes.
     *
     * @param command input line without its line separator
     * @throws IOException if either write fails
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
     * Finishes the HTML document and closes both logs.
     *
     * @throws IOException if finishing or closing fails
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
     * Opens a log without truncating it.
     *
     * @param path log path
     * @return the writable channel
     * @throws IOException if the file cannot open
     */
    private static FileChannel openLog(Path path) throws IOException {
        return FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    }

    /**
     * Writes all UTF-8 bytes, retrying partial writes.
     *
     * @param file output channel
     * @param text text to write
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
     * Restores the log position and attaches any rollback error.
     *
     * @param file affected channel
     * @param position position before writing
     * @param failure original write error
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
     * Closes a channel and attaches any close error.
     *
     * @param file channel to close
     * @param failure original error
     */
    private static void closeAfterFailure(FileChannel file, Throwable failure) {
        try {
            file.close();
        } catch (IOException closeFailure) {
            failure.addSuppressed(closeFailure);
        }
    }

    /**
     * Escapes command text for HTML.
     *
     * @param command original command
     * @return HTML-safe text
     */
    private static String escapeHtml(String command) {
        return command.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
