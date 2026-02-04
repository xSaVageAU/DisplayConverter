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
        if (!frame.isValid())
            return; // Anti-dupe: Ensure frame is still valid

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
        String preview = item.getType().toString();
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            preview = item.getItemMeta().getDisplayName(); // Getting raw display name for simplicity, potentially
                                                           // convert to plain text?
        }
        databaseManager.saveOwnership(display.getUniqueId(), player.getUniqueId(), "ITEM", preview);

        // Remove frame
        frame.remove();

        player.sendMessage("Converted ItemFrame to ItemDisplay!");
    }

    public void deleteDisplay(UUID entityId) {
        // Remove from DB
        databaseManager.deleteOwnership(entityId);

        // Remove from World
        for (org.bukkit.World world : org.bukkit.Bukkit.getWorlds()) {
            org.bukkit.entity.Entity entity = world.getEntity(entityId);
            if (entity != null) {
                entity.remove();
                return; // Found and removed
            }
        }
    }

    public void revertDisplay(UUID entityId, Player player) {
        org.bukkit.entity.Entity entity = null;
        for (org.bukkit.World world : org.bukkit.Bukkit.getWorlds()) {
            entity = world.getEntity(entityId);
            if (entity != null)
                break;
        }

        if (entity == null || !(entity instanceof ItemDisplay)) {
            player.sendMessage("Display entity not found or invalid type.");
            return;
        }

        if (!entity.isValid()) {
            player.sendMessage("Display entity is already invalidated.");
            return;
        }

        ItemDisplay display = (ItemDisplay) entity;
        Location loc = display.getLocation();
        ItemStack item = display.getItemStack();

        // 1. Remove Display
        display.remove();

        // 2. Spawn ItemFrame
        // We need to try to place it on the block surface.
        // ItemDisplay is usually centered.
        // Let's check for a solid block nearby? Or just spawn it?
        // ItemFram requires a block face.
        // Logic: Check the 6 directions, see which block is solid.
        // This is tricky without the original facing data.
        // Approximation: Just spawn it. If it pops off, it pops off.
        ItemFrame frame = (ItemFrame) loc.getWorld().spawnEntity(loc, EntityType.ITEM_FRAME);
        if (item != null)
            frame.setItem(item);

        // Remove from DB
        databaseManager.deleteOwnership(entityId);
        player.sendMessage("Reverted ItemDisplay to ItemFrame!");
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
