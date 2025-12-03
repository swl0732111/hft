package com.hft.trading.persistence;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

/**
 * Write-Ahead Log (WAL) for durability and crash recovery.
 * Provides fast sequential writes with fsync for guaranteed persistence.
 */
@Slf4j
@Component
public class WriteAheadLog {
    private static final String WAL_FILE = System.getProperty("user.home") + "/hft-data/wal.log";
    private static final int BUFFER_SIZE = 8192;

    private FileChannel walChannel;
    private ByteBuffer buffer;
    private long totalBytesWritten = 0;

    @PostConstruct
    public void init() {
        try {
            File walFile = new File(WAL_FILE);
            walFile.getParentFile().mkdirs();

            RandomAccessFile raf = new RandomAccessFile(walFile, "rw");
            walChannel = raf.getChannel();
            buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);

            log.info("WAL initialized: {}", WAL_FILE);
        } catch (Exception e) {
            log.error("Failed to initialize WAL", e);
            throw new RuntimeException("WAL initialization failed", e);
        }
    }

    /**
     * Append event to WAL.
     * Format: [timestamp][type][data]\n
     */
    public synchronized void append(String eventType, String eventData) {
        try {
            long timestamp = System.currentTimeMillis();
            String logEntry = String.format("%d|%s|%s\n", timestamp, eventType, eventData);
            byte[] bytes = logEntry.getBytes(StandardCharsets.UTF_8);

            // Write to buffer
            buffer.clear();
            buffer.put(bytes);
            buffer.flip();

            // Write to channel
            while (buffer.hasRemaining()) {
                walChannel.write(buffer);
            }

            // Force sync to disk (fsync)
            walChannel.force(true);

            totalBytesWritten += bytes.length;

        } catch (Exception e) {
            log.error("WAL append failed", e);
            throw new RuntimeException("WAL append failed", e);
        }
    }

    /**
     * Truncate WAL after successful DB sync.
     */
    public synchronized void truncate() {
        try {
            walChannel.truncate(0);
            walChannel.force(true);
            totalBytesWritten = 0;
            log.info("WAL truncated");
        } catch (Exception e) {
            log.error("WAL truncate failed", e);
        }
    }

    /**
     * Get WAL file size.
     */
    public long getSize() {
        try {
            return walChannel.size();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Get total bytes written.
     */
    public long getTotalBytesWritten() {
        return totalBytesWritten;
    }

    @PreDestroy
    public void shutdown() {
        try {
            if (walChannel != null) {
                walChannel.force(true);
                walChannel.close();
            }
            log.info("WAL shutdown complete");
        } catch (Exception e) {
            log.error("WAL shutdown failed", e);
        }
    }
}
