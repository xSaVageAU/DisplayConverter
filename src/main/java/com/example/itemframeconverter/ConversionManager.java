package com.example.itemframeconverter;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Rotation;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ConversionManager {

    private final ItemFrameConverter plugin;
    private final DatabaseManager databaseManager;
    private final Map<UUID, Boolean> playersInMode = new HashMap<>();

    public ConversionManager(ItemFrameConverter plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    public boolean isConverting(Player player) {
        return playersInMode.getOrDefault(player.getUniqueId(), false);
    }

    public void toggleMode(Player player) {
        boolean newState = !isConverting(player);
        playersInMode.put(player.getUniqueId(), newState);
        player.sendMessage("Conversion mode " + (newState ? "enabled" : "disabled") + ".");
    }

    public void convert(Player player, ItemFrame frame) {
        ItemStack item = frame.getItem();
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage("Item Frame is empty!");
            return;
        }

        Location loc = frame.getLocation();
        // Calculate spawn location
        Location spawnLoc = loc.clone();

        // Spawn ItemDisplay
        ItemDisplay display = (ItemDisplay) loc.getWorld().spawnEntity(spawnLoc, EntityType.ITEM_DISPLAY);
        display.setItemStack(item);

        // Mimic item rotation
        Rotation rotation = frame.getRotation();

        // Align display to face the same way as the frame
        display.setRotation(loc.getYaw(), loc.getPitch());

        // Apply rotation from the item frame interaction
        int rotOrd = rotation.ordinal();
        float zRot = (float) Math.toRadians(rotOrd * 45);

        Transformation t = display.getTransformation();
        t.getScale().set(0.5f);
        t.getLeftRotation().set(new AxisAngle4f(zRot, 0, 0, 1));

        display.setTransformation(t);

        // Apply View Distance from Calibrated Config
        double viewDistanceBlocks = plugin.getConfig().getDouble("view-distance", 10.0);
        float viewRangeMultiplier = (float) (viewDistanceBlocks / 75.0);
        display.setViewRange(viewRangeMultiplier);

        // Persist ownership
        databaseManager.saveOwnership(display.getUniqueId(), player.getUniqueId());

        // Remove frame
        frame.remove();

        player.sendMessage("Converted ItemFrame to ItemDisplay!");
    }

    public void deleteDisplay(UUID entityId) {
        // Remove from DB
        databaseManager.deleteOwnership(entityId);

        // Remove from World
        // Note: This requires searching loaded worlds or knowing the world.
        // Since we don't store world/location in DB, we have to iterate worlds or just
        // hope it's loaded.
        // A better approach for the future is to store World/X/Y/Z in DB.
        // For now, we scan all worlds.
        for (org.bukkit.World world : org.bukkit.Bukkit.getWorlds()) {
            org.bukkit.entity.Entity entity = world.getEntity(entityId);
            if (entity != null) {
                entity.remove();
                return; // Found and removed
            }
        }
    }

    public org.bukkit.Location getDisplayLocation(UUID entityId) {
        for (org.bukkit.World world : org.bukkit.Bukkit.getWorlds()) {
            org.bukkit.entity.Entity entity = world.getEntity(entityId);
            if (entity != null) {
                return entity.getLocation();
            }
        }
        return null;
    }
}
