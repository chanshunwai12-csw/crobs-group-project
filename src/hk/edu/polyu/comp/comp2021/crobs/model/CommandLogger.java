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
    private static final String SAME_LOG_FILE = "HTML and TXT logs must use different files.";
    private static final String HTML_HEADER = "<!doctype html>\n<html lang=\"en\"><head>"
            + "<meta charset=\"utf-8\"><title>Crobs command log</title></head>\n<body><table>\n"
            + "<thead><tr><th scope=\"col\">Index</th><th scope=\"col\">Command</th></tr></thead>\n<tbody>\n";
    private static final String HTML_FOOTER = "</tbody>\n</table></body></html>\n";

    private final FileChannel htmlLog;
    private final FileChannel txtLog;
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
        FileChannel second = null;
        try {
            second = openLog(txtPath);
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
    @SuppressWarnings("try")
    public synchronized void close() throws IOException {
        if (!htmlLog.isOpen()) {
            return;
        }
        try (FileChannel html = htmlLog; FileChannel txt = txtLog) {
            write(html, HTML_FOOTER);
        }
    }

    private static FileChannel openLog(Path path) throws IOException {
        return FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    }

    private static void write(FileChannel file, String text) throws IOException {
        ByteBuffer bytes = StandardCharsets.UTF_8.encode(text);
        while (bytes.hasRemaining()) {
            file.write(bytes);
        }
    }

    private static void rollback(FileChannel file, long position, IOException failure) {
        try {
            file.truncate(position);
            file.position(position);
        } catch (IOException rollbackFailure) {
            failure.addSuppressed(rollbackFailure);
        }
    }

    private static void closeAfterFailure(FileChannel file, Throwable failure) {
        if (file != null) {
            try {
                file.close();
            } catch (IOException closeFailure) {
                failure.addSuppressed(closeFailure);
            }
        }
    }

    private static String escapeHtml(String command) {
        return command.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
