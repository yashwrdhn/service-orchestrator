package com.poc.orchestrator.rocksdb;

import org.rocksdb.*;
import org.springframework.scheduling.annotation.Async;

public class RocksDBFunc {

    static RocksDB db;
    @Async
    public static void initialize() {
        try {
            RocksDB.loadLibrary();
            db = RocksDB.open(new Options().setCreateIfMissing(true), "/temp/rocks-db");
            System.out.println("RocksDB initialized.");
        } catch (Exception e) {
            System.err.println("Error initializing RocksDB: " + e.getMessage());
        }
    }
    public static void save(String key, String value) {
        try {
            db.put(key.getBytes(), value.getBytes());
            System.out.println("Saved: (" + key + ", " + value + ")");
        } catch (Exception e) {
            System.err.println("Error saving data: " + e.getMessage());
        }
    }

    // Find data by key
    public static String find(String key) {
        try {
            byte[] value = db.get(key.getBytes());
            return (value != null) ? new String(value) : null;
        } catch (Exception e) {
            System.err.println("Error finding data: " + e.getMessage());
            return null;
        }
    }

    // Delete data by key
    public static void delete(String key) {
        try {
            db.delete(key.getBytes());
            System.out.println("Deleted key: " + key);
        } catch (Exception e) {
            System.err.println("Error deleting data: " + e.getMessage());
        }
    }
}
