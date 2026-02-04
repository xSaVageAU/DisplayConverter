package com.example.itemframeconverter;

import org.bukkit.plugin.java.JavaPlugin;

public class ItemFrameConverter extends JavaPlugin {

    private DatabaseManager databaseManager;
    private ConversionManager conversionManager;

    @Override
    public void onEnable() {
        getLogger().info("ItemFrameConverter enabled for 1.21.10/1.21.11!");

        saveDefaultConfig(); // Save config.yml if not exists

        // Initialize Database
        databaseManager = new DatabaseManager(this);
        databaseManager.initialize();

        // Initialize Managers
        conversionManager = new ConversionManager(this, databaseManager);

        // Initialize Sign Manager
        SignConversionManager signManager = new SignConversionManager(this, databaseManager);

        // Register Commands
        getCommand("convertframe").setExecutor(new ConversionCommand(conversionManager));
        getCommand("convertsign").setExecutor(new SignConversionCommand(signManager));
        getCommand("managedisplays").setExecutor(new ManagementCommand(conversionManager, databaseManager, false));
        getCommand("adminmanagedisplays").setExecutor(new ManagementCommand(conversionManager, databaseManager, true));

        // Register Listeners
        getServer().getPluginManager().registerEvents(new FrameInteractListener(conversionManager), this);
        getServer().getPluginManager().registerEvents(new SignInteractListener(signManager), this);
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("ItemFrameConverter disabled!");
    }
}
