package com.hft.trading.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Slf4j
@Service
public class RocksDBService {

    private static final String DEFAULT_DB_PATH = "data/rocksdb/account_state";
    private RocksDB db;
    private boolean enabled = true;

    @Value("${rocksdb.path:data/rocksdb/account_state}")
    private String dbPath;

    @Value("${rocksdb.enabled:true}")
    private boolean rocksdbEnabled;

    static {
        RocksDB.loadLibrary();
    }

    @PostConstruct
    public void init() {
        if (!rocksdbEnabled) {
            log.info("RocksDB is disabled via configuration");
            enabled = false;
            return;
        }

        try {
            File dbDir = new File(dbPath);
            if (!dbDir.exists()) {
                Files.createDirectories(dbDir.toPath());
            }

            Options options = new Options().setCreateIfMissing(true);
            db = RocksDB.open(options, dbPath);
            log.info("RocksDB initialized at {}", dbPath);
        } catch (RocksDBException | IOException e) {
            log.error("Failed to initialize RocksDB at {}", dbPath, e);
            enabled = false;
            log.warn("RocksDB disabled due to initialization failure");
        }
    }

    public void put(String key, byte[] value) {
        if (!enabled || db == null) {
            log.trace("RocksDB not available, skipping put for key {}", key);
            return;
        }
        try {
            db.put(key.getBytes(), value);
        } catch (RocksDBException e) {
            log.error("Failed to put key {}", key, e);
        }
    }

    public byte[] get(String key) {
        if (!enabled || db == null) {
            return null;
        }
        try {
            return db.get(key.getBytes());
        } catch (RocksDBException e) {
            log.error("Failed to get key {}", key, e);
            return null;
        }
    }

    public void delete(String key) {
        try {
            db.delete(key.getBytes());
        } catch (RocksDBException e) {
            log.error("Failed to delete key {}", key, e);
            throw new RuntimeException("RocksDB delete failed", e);
        }
    }

    @PreDestroy
    public void close() {
        if (db != null) {
            db.close();
            log.info("RocksDB closed");
        }
    }
}
