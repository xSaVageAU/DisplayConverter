package com.example.itemframeconverter;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;

public class DatabaseManager {

    private final ItemFrameConverter plugin;
    private Connection connection;

    public DatabaseManager(ItemFrameConverter plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            File dbFile = new File(dataFolder, "database.db");
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();

            // Load the SQLite driver
            // Load the SQLite driver
            Class.forName("org.sqlite.JDBC");

            connection = DriverManager.getConnection(url);
            createTable();
            plugin.getLogger().info("Database connected successfully.");
        } catch (SQLException | ClassNotFoundException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not connect to SQLite database", e);
        }
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not close database connection", e);
            }
        }
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS display_ownership (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "entity_uuid VARCHAR(36) NOT NULL," +
                "owner_uuid VARCHAR(36) NOT NULL," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ");";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.execute();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not create table", e);
        }
    }

    public void saveOwnership(UUID entityId, UUID ownerId) {
        String sql = "INSERT INTO display_ownership(entity_uuid, owner_uuid) VALUES(?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, entityId.toString());
            stmt.setString(2, ownerId.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save ownership", e);
        }
    }

    public java.util.List<UUID> getDisplays(UUID ownerId) {
        java.util.List<UUID> displays = new java.util.ArrayList<>();
        String sql = "SELECT entity_uuid FROM display_ownership WHERE owner_uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, ownerId.toString());
            java.sql.ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                displays.add(UUID.fromString(rs.getString("entity_uuid")));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not get displays", e);
        }
        return displays;
    }

    public java.util.List<UUID> getAllDisplays() {
        java.util.List<UUID> displays = new java.util.ArrayList<>();
        String sql = "SELECT entity_uuid FROM display_ownership";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            java.sql.ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                displays.add(UUID.fromString(rs.getString("entity_uuid")));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not get all displays", e);
        }
        return displays;
    }

    public void deleteOwnership(UUID entityId) {
        String sql = "DELETE FROM display_ownership WHERE entity_uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, entityId.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not delete ownership", e);
        }
    }
}
