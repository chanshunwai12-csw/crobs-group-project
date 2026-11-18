package hk.edu.polyu.comp.comp2021.crobs.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

/** Deterministic disk-failure tests using delegating channels over real temporary files. */
class CommandLoggerFailureTest {
    /** Creates a fixture whose temporary directory is initialized by JUnit. */
    CommandLoggerFailureTest() {
    }

    /** Temporary directory supplied by JUnit for isolated log files. */
    @TempDir
    private Path temp;

    private FaultChannel replaceChannel(CommandLogger logger, String name) throws Exception {
        Field field = CommandLogger.class.getDeclaredField(name);
        field.setAccessible(true);
        FaultChannel channel = new FaultChannel((FileChannel) field.get(logger));
        field.set(logger, channel);
        return channel;
    }

    /**
     * Verifies paired log writes roll back together and preserve operation numbering.
     * @throws Exception if channel replacement or temporary-file access fails
     */
    @Test
    void failedSecondLogWriteRollsBackBothFilesAndDoesNotAdvanceIndex() throws Exception {
        Path html = temp.resolve("log.html");
        Path txt = temp.resolve("log.txt");
        try (CommandLogger logger = new CommandLogger(html, txt)) {
            logger.log("first");
            byte[] beforeHtml = Files.readAllBytes(html);
            byte[] beforeTxt = Files.readAllBytes(txt);
            FaultChannel channel = replaceChannel(logger, "txtLog");
            channel.setWriteFailure(true);
            assertThrows(IOException.class, () -> logger.log("rejected"));
            assertArrayEquals(beforeHtml, Files.readAllBytes(html));
            assertArrayEquals(beforeTxt, Files.readAllBytes(txt));
            channel.setWriteFailure(false);
            logger.log("second");
        }
        String markup = new String(Files.readAllBytes(html), StandardCharsets.UTF_8);
        assertTrue(markup.contains("<td>2</td><td><code"));
        assertFalse(markup.contains("rejected"));
        assertEquals(java.util.Arrays.asList("first", "second"), Files.readAllLines(txt));
    }

    /**
     * Verifies a stalled write reports an error without recording a partial command.
     * @throws Exception if channel replacement or temporary-file access fails
     */
    @Test
    void zeroProgressWriteFailsWithoutRecordingTheCommand() throws Exception {
        Path html = temp.resolve("log.html");
        Path txt = temp.resolve("log.txt");
        try (CommandLogger logger = new CommandLogger(html, txt)) {
            byte[] beforeHtml = Files.readAllBytes(html);
            FaultChannel channel = replaceChannel(logger, "htmlLog");
            channel.setZeroProgress(true);
            IOException error = assertThrows(IOException.class, () -> logger.log("rejected"));
            assertTrue(error.getMessage().contains("progress"));
            assertArrayEquals(beforeHtml, Files.readAllBytes(html));
            assertEquals(0, Files.size(txt));
            channel.setZeroProgress(false);
        }
    }

    /**
     * Verifies rollback failures are retained as suppressed exceptions on the write error.
     * @throws Exception if channel replacement or temporary-file access fails
     */
    @Test
    void rollbackFailuresRemainAttachedToTheOriginalWriteFailure() throws Exception {
        try (CommandLogger logger = new CommandLogger(temp.resolve("log.html"), temp.resolve("log.txt"))) {
            FaultChannel channel = replaceChannel(logger, "txtLog");
            channel.setWriteFailure(true);
            channel.setTruncateFailure(true);
            IOException error = assertThrows(IOException.class, () -> logger.log("rejected"));
            assertEquals("write failed", error.getMessage());
            assertEquals(1, error.getSuppressed().length);
            assertEquals("truncate failed", error.getSuppressed()[0].getMessage());
            channel.setWriteFailure(false);
            channel.setTruncateFailure(false);
        }
    }

    /**
     * Verifies cleanup closes both logs and retains close errors after a footer failure.
     * @throws Exception if channel replacement or temporary-file access fails
     */
    @Test
    void footerFailureStillClosesBothChannelsAndRetainsCloseFailure() throws Exception {
        CommandLogger logger = new CommandLogger(temp.resolve("log.html"), temp.resolve("log.txt"));
        FaultChannel html = replaceChannel(logger, "htmlLog");
        FaultChannel txt = replaceChannel(logger, "txtLog");
        html.setWriteFailure(true);
        txt.setCloseFailure(true);
        IOException error = assertThrows(IOException.class, logger::close);
        assertEquals("write failed", error.getMessage());
        assertEquals(1, error.getSuppressed().length);
        assertEquals("close failed", error.getSuppressed()[0].getMessage());
        assertFalse(html.isOpen());
        assertFalse(txt.isOpen());
        logger.close();
    }

    private static final class FaultChannel extends FileChannel {
        private final FileChannel delegate;
        private boolean failWrite;
        private boolean zeroWrite;
        private boolean failTruncate;
        private boolean failClose;

        private FaultChannel(FileChannel delegate) { this.delegate = delegate; }

        private void setWriteFailure(boolean enabled) {
            failWrite = enabled;
        }

        private void setZeroProgress(boolean enabled) {
            zeroWrite = enabled;
        }

        private void setTruncateFailure(boolean enabled) {
            failTruncate = enabled;
        }

        private void setCloseFailure(boolean enabled) {
            failClose = enabled;
        }
        @Override public int write(ByteBuffer buffer) throws IOException {
            if (failWrite) throw new IOException("write failed");
            if (zeroWrite) return 0;
            return delegate.write(buffer);
        }
        @SuppressWarnings("ReturnOfThis") // FileChannel requires returning this channel for chaining.
        @Override public FileChannel truncate(long size) throws IOException {
            if (failTruncate) throw new IOException("truncate failed");
            delegate.truncate(size);
            return this;
        }
        @Override protected void implCloseChannel() throws IOException {
            delegate.close();
            if (failClose) throw new IOException("close failed");
        }
        @Override public long position() throws IOException { return delegate.position(); }
        @SuppressWarnings("ReturnOfThis") // FileChannel requires returning this channel for chaining.
        @Override public FileChannel position(long value) throws IOException {
            delegate.position(value);
            return this;
        }
        @Override public long size() throws IOException { return delegate.size(); }
        @Override public void force(boolean metadata) throws IOException { delegate.force(metadata); }
        @Override public int read(ByteBuffer buffer) throws IOException { return delegate.read(buffer); }
        @Override public long read(ByteBuffer[] buffers, int offset, int length) throws IOException {
            return delegate.read(buffers, offset, length);
        }
        @Override public long write(ByteBuffer[] buffers, int offset, int length) throws IOException {
            return delegate.write(buffers, offset, length);
        }
        @Override public int read(ByteBuffer buffer, long position) throws IOException {
            return delegate.read(buffer, position);
        }
        @Override public int write(ByteBuffer buffer, long position) throws IOException {
            return delegate.write(buffer, position);
        }
        @Override public long transferTo(long position, long count, WritableByteChannel target) throws IOException {
            return delegate.transferTo(position, count, target);
        }
        @Override public long transferFrom(ReadableByteChannel source, long position, long count) throws IOException {
            return delegate.transferFrom(source, position, count);
        }
        @Override public MappedByteBuffer map(MapMode mode, long position, long size) throws IOException {
            return delegate.map(mode, position, size);
        }
        @Override public FileLock lock(long position, long size, boolean shared) throws IOException {
            return delegate.lock(position, size, shared);
        }
        @Override public FileLock tryLock(long position, long size, boolean shared) throws IOException {
            return delegate.tryLock(position, size, shared);
        }
    }
}
