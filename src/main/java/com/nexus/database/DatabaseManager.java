package com.nexus.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseManager {
    private static final String JDBC_URL = "jdbc:sqlite:nexus.db";
    private static final DatabaseManager INSTANCE = new DatabaseManager();

    private Connection connection;

    private DatabaseManager() {
    }

    public static DatabaseManager getInstance() {
        return INSTANCE;
    }

    public synchronized void init() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(JDBC_URL);

                try (Statement statement = connection.createStatement()) {
                    statement.execute("PRAGMA foreign_keys = ON");

                    statement.executeUpdate("""
                            CREATE TABLE IF NOT EXISTS pages (
                                id INTEGER PRIMARY KEY AUTOINCREMENT,
                                title TEXT NOT NULL
                            )
                            """);

                    statement.executeUpdate("""
                            CREATE TABLE IF NOT EXISTS blocks (
                                id INTEGER PRIMARY KEY AUTOINCREMENT,
                                page_id INTEGER NOT NULL,
                                type TEXT NOT NULL,
                                content TEXT NOT NULL DEFAULT '',
                                order_index INTEGER NOT NULL,
                                FOREIGN KEY(page_id) REFERENCES pages(id) ON DELETE CASCADE
                            )
                            """);

                    statement.executeUpdate("""
                            CREATE TABLE IF NOT EXISTS links (
                                id INTEGER PRIMARY KEY AUTOINCREMENT,
                                source_block_id INTEGER NOT NULL,
                                target_block_id INTEGER NOT NULL,
                                UNIQUE(source_block_id, target_block_id),
                                FOREIGN KEY(source_block_id) REFERENCES blocks(id) ON DELETE CASCADE,
                                FOREIGN KEY(target_block_id) REFERENCES blocks(id) ON DELETE CASCADE
                            )
                            """);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialize database", e);
        }
    }

    public synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                init();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to acquire database connection", e);
        }

        return connection;
    }
}