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
        String preview = "Sign Text";
        net.kyori.adventure.text.TextComponent textObj = (net.kyori.adventure.text.TextComponent) fullText;
        if (textObj.content().length() > 0) {
            preview = textObj.content().substring(0, Math.min(20, textObj.content().length()));
        }
        // Note: Component.join creates a TextComponent, so casting is *usually* safe if
        // plain text.
        // But better to verify or use PlainTextComponentSerializer for safety.
        String plainPreview = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(fullText)
                .replace("\n", " ").trim();
        if (plainPreview.length() > 20)
            plainPreview = plainPreview.substring(0, 20) + "...";
        if (plainPreview.isEmpty())
            plainPreview = "Empty Sign";

        databaseManager.saveOwnership(textDisplay.getUniqueId(), player.getUniqueId(), "TEXT", plainPreview);

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
            default:
                return 0;
        }
    }

    private org.bukkit.block.BlockFace yawToFace(float yaw) {
        // Normalize yaw to 0-360
        yaw = (yaw % 360 + 360) % 360;
        if (yaw >= 45 && yaw < 135)
            return org.bukkit.block.BlockFace.WEST;
        if (yaw >= 135 && yaw < 225)
            return org.bukkit.block.BlockFace.NORTH;
        if (yaw >= 225 && yaw < 315)
            return org.bukkit.block.BlockFace.EAST;
        return org.bukkit.block.BlockFace.SOUTH;
    }

    public void revertDisplay(UUID entityId, Player player) {
        org.bukkit.entity.Entity entity = null;
        for (org.bukkit.World world : org.bukkit.Bukkit.getWorlds()) {
            entity = world.getEntity(entityId);
            if (entity != null)
                break;
        }

        if (entity == null || !(entity instanceof TextDisplay)) {
            player.sendMessage("Display entity not found or invalid type.");
            return;
        }

        if (!entity.isValid()) {
            player.sendMessage("Display entity is already invalidated.");
            return;
        }

        TextDisplay display = (TextDisplay) entity;
        org.bukkit.Location loc = display.getLocation();
        Component text = display.text();
        float yaw = loc.getYaw();

        // 1. Remove Display
        display.remove();

        // 2. Restore Sign
        Block block = loc.getBlock();

        // Smart Detection Logic
        org.bukkit.block.BlockFace facing = yawToFace(yaw);
        // "facing" is the direction text looks.
        // For Wall Sign, the wall is BEHIND the text.
        // So checking the block in the opposite direction.
        org.bukkit.block.BlockFace wallDirection = facing.getOppositeFace();
        Block supportBlock = block.getRelative(wallDirection);

        if (supportBlock.getType().isSolid()) {
            // It fits on a wall
            block.setType(Material.OAK_WALL_SIGN);
            if (block.getBlockData() instanceof Directional) {
                Directional dir = (Directional) block.getBlockData();
                dir.setFacing(facing); // WallSign facing is direction text points
                block.setBlockData(dir);
            }
        } else {
            // Standing sign
            block.setType(Material.OAK_SIGN);
            if (block.getBlockData() instanceof Rotatable) {
                Rotatable rot = (Rotatable) block.getBlockData();
                rot.setRotation(facing);
                block.setBlockData(rot);
            }
        }

        if (block.getState() instanceof Sign) {
            Sign sign = (Sign) block.getState();
            if (text != null) {
                String plainText = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                        .serialize(text);
                String[] lines = plainText.split("\n");
                SignSide side = sign.getSide(Side.FRONT);
                for (int i = 0; i < Math.min(4, lines.length); i++) {
                    side.line(i, Component.text(lines[i]));
                }
            }
            sign.update();
        }

        // Remove from DB
        databaseManager.deleteOwnership(entityId);
        player.sendMessage("Reverted TextDisplay to Sign!");
    }
}
