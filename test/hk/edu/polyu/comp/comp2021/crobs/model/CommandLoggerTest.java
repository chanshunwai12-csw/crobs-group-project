package hk.edu.polyu.comp.comp2021.crobs.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandLoggerTest {
    @TempDir
    private Path temp;

    @Test
    void writesTheSameCommandsInOrderAndEscapesHtml() throws IOException {
        Path html = temp.resolve("commands.html");
        Path txt = temp.resolve("commands.txt");
        String first = "  book R1 2026-11-16 10:00 11:00 A&B <研讨> \"demo\" 'study'  ";
        try (CommandLogger logger = new CommandLogger(html, txt)) {
            logger.log(first);
            logger.log("quit");
        }
        assertEquals(Arrays.asList(first, "quit"), Files.readAllLines(txt, StandardCharsets.UTF_8));
        String markup = new String(Files.readAllBytes(html), StandardCharsets.UTF_8);
        assertTrue(markup.contains("<th scope=\"col\">Index</th><th scope=\"col\">Command</th>"));
        assertTrue(markup.indexOf("<tr><td>1</td>") < markup.indexOf("<tr><td>2</td>"));
        assertTrue(markup.contains("A&amp;B &lt;研讨&gt; &quot;demo&quot; &#39;study&#39;  "));
        assertTrue(markup.endsWith("</table></body></html>\n"));
    }

    @Test
    void startsFreshWhenPathsAreReusedForAnotherSession() throws IOException {
        Path html = temp.resolve("commands.html");
        Path txt = temp.resolve("commands.txt");
        try (CommandLogger logger = new CommandLogger(html, txt)) {
            logger.log("old command");
        }
        try (CommandLogger logger = new CommandLogger(html, txt)) {
            logger.log("quit");
        }
        assertEquals(Collections.singletonList("quit"), Files.readAllLines(txt, StandardCharsets.UTF_8));
        String markup = new String(Files.readAllBytes(html), StandardCharsets.UTF_8);
        assertTrue(markup.contains("<tr><td>1</td>"));
        assertFalse(markup.contains("old command"));
    }

    @Test
    void rejectsTheSameLogPath() {
        Path log = temp.resolve("commands.log");
        assertThrows(IOException.class, () -> new CommandLogger(log, temp.resolve(".").resolve("commands.log")));
        assertFalse(Files.exists(log));
    }

    @Test
    void rejectsDifferentPathsToTheSameFileWithoutTruncatingIt() throws IOException {
        Path html = temp.resolve("commands.html");
        Files.write(html, Collections.singletonList("existing"), StandardCharsets.UTF_8);
        Path alias = Files.createLink(temp.resolve("commands.txt"), html);
        assertThrows(IOException.class, () -> new CommandLogger(html, alias));
        assertEquals(Collections.singletonList("existing"), Files.readAllLines(html, StandardCharsets.UTF_8));
    }

    @Test
    void closingTheLoggerAgainLeavesTheCompleteDocumentIntact() throws IOException {
        Path html = temp.resolve("commands.html");
        CommandLogger logger = new CommandLogger(html, temp.resolve("commands.txt"));
        try (CommandLogger session = logger) {
            session.log("quit");
        }
        byte[] completeDocument = Files.readAllBytes(html);
        logger.close();
        assertArrayEquals(completeDocument, Files.readAllBytes(html));
    }

    @Test
    void leavesExistingHtmlContentWhenTxtPathCannotBeOpened() throws IOException {
        Path html = temp.resolve("commands.html");
        Files.write(html, Collections.singletonList("existing"), StandardCharsets.UTF_8);
        Path invalid = temp.resolve("missing").resolve("commands.txt");
        assertThrows(IOException.class, () -> new CommandLogger(html, invalid));
        assertEquals(Collections.singletonList("existing"), Files.readAllLines(html, StandardCharsets.UTF_8));
    }
}
