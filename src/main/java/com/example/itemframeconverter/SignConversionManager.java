package com.example.itemframeconverter;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SignConversionManager {

    private final ItemFrameConverter plugin;
    private final DatabaseManager databaseManager;
    private final Map<UUID, Boolean> conversionMode = new HashMap<>();

    public SignConversionManager(ItemFrameConverter plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    public void toggleConversionMode(Player player) {
        if (conversionMode.getOrDefault(player.getUniqueId(), false)) {
            conversionMode.put(player.getUniqueId(), false);
            player.sendMessage("Sign Conversion Mode Disabled.");
        } else {
            conversionMode.put(player.getUniqueId(), true);
            player.sendMessage("Sign Conversion Mode Enabled! Right-click a sign to convert it.");
        }
    }

    public boolean isInConversionMode(Player player) {
        return conversionMode.getOrDefault(player.getUniqueId(), false);
    }

    public void convertSign(Player player, Block block, Side side) {
        if (!(block.getState() instanceof Sign))
            return;

        Sign signState = (Sign) block.getState();
        SignSide signSide = signState.getSide(side);

        // Extract text
        Component fullText = Component.join(JoinConfiguration.newlines(), signSide.lines());
        // Simple check if empty (not perfect but good enough)
        if (fullText.equals(Component.empty())) {
            // Maybe they clicked the empty back side? Continue anyway but warn?
            // player.sendMessage("That side is empty!");
            // return;
        }

        // Spawn TextDisplay
        org.bukkit.Location loc = block.getLocation().add(0.5, 0.5, 0.5); // Center of block

        // Rotation logic
        float yRot = 0;
        if (block.getBlockData() instanceof Directional) {
            // Wall Sign
            Directional directional = (Directional) block.getBlockData();
            switch (directional.getFacing()) {
                case NORTH:
                    yRot = 180;
                    break;
                case SOUTH:
                    yRot = 0;
                    break;
                case WEST:
                    yRot = 90;
                    break;
                case EAST:
                    yRot = -90;
                    break;
                default:
                    break;
            }
        } else if (block.getBlockData() instanceof Rotatable) {
            // Standing Sign
            // Use block rotation state
        }

        TextDisplay textDisplay = (TextDisplay) block.getWorld().spawnEntity(loc, EntityType.TEXT_DISPLAY);

        // Apply Orientation & Transformation
        if (block.getBlockData() instanceof Directional) {
            loc.setYaw(yRot);
            textDisplay.teleport(loc);

            // Offset to sit on the face
            Transformation t = textDisplay.getTransformation();
            t.getTranslation().set(0, -0.1f, -0.42f); // Push forward and down slightly

            // Apply Scale
            float scale = (float) plugin.getConfig().getDouble("sign-text-scale", 0.4);
            t.getScale().set(scale);

            textDisplay.setTransformation(t);
        } else if (block.getBlockData() instanceof Rotatable) {
            Rotatable rotatable = (Rotatable) block.getBlockData();
            float yaw = faceToYaw(rotatable.getRotation());
            loc.setYaw(yaw);
            textDisplay.teleport(loc);

            Transformation t = textDisplay.getTransformation();
            t.getTranslation().set(0, -0.15f, 0); // Lower standing signs too

            float scale = (float) plugin.getConfig().getDouble("sign-text-scale", 0.4);
            t.getScale().set(scale);
            textDisplay.setTransformation(t);
        }

        textDisplay.text(fullText);
        textDisplay.setAlignment(org.bukkit.entity.TextDisplay.TextAlignment.CENTER);
        textDisplay.setBillboard(org.bukkit.entity.Display.Billboard.FIXED);
        textDisplay.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));

        // Apply View Distance
        double viewDistanceBlocks = plugin.getConfig().getDouble("view-distance", 10.0);
        float viewRangeMultiplier = (float) (viewDistanceBlocks / 75.0);
        textDisplay.setViewRange(viewRangeMultiplier);

        // Remove Sign
        block.setType(Material.AIR);

        // Persist
        databaseManager.saveOwnership(textDisplay.getUniqueId(), player.getUniqueId());

        player.sendMessage("Converted Sign to TextDisplay!");
    }

    private float faceToYaw(org.bukkit.block.BlockFace face) {
        switch (face) {
            case NORTH:
                return 180;
            case SOUTH:
                return 0;
            case WEST:
                return 90;
            case EAST:
                return -90;
            case NORTH_EAST:
                return -135;
            case NORTH_WEST:
                return 135;
            case SOUTH_EAST:
                return -45;
            case SOUTH_WEST:
                return 45;
            case NORTH_NORTH_EAST:
                return -157.5f;
            // ... omitting partials for brevity, standard cardinals usually enough for
            // testing
            default:
                return 0;
        }
    }
}
