package com.gdvn.db;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private final String connectionUrl;

    public DatabaseManager(Path dbPath) {
        this.connectionUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath();
    }

    public void init() throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS linked_accounts ("
                    + "uuid TEXT PRIMARY KEY,"
                    + "token TEXT NOT NULL"
                    + ")");
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(connectionUrl);
    }

    public void saveToken(String uuid, String token) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT OR REPLACE INTO linked_accounts (uuid, token) VALUES (?, ?)")) {
            ps.setString(1, uuid);
            ps.setString(2, token);
            ps.executeUpdate();
        }
    }

    public String getToken(String uuid) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT token FROM linked_accounts WHERE uuid = ?")) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("token");
                }
                return null;
            }
        }
    }

    public void removeToken(String uuid) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM linked_accounts WHERE uuid = ?")) {
            ps.setString(1, uuid);
            ps.executeUpdate();
        }
    }
}
