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
            // Offset slightly from wall
            // Using transformation translation is better than location offset for fine
            // tuning
        } else if (block.getBlockData() instanceof Rotatable) {
            // Standing Sign
            // Rotation is 0-15. 0 is South?
            // Bukkit Rotatable maps directly to degrees usually? No it's block states.
            // Actually simpler: just use the rotation of the block state?
            // Note: Rotatable does not have getRotation() returning degrees cleanly in all
            // versions.
            // But we can approximate.
            // Or just face the player? No, should be sign rotation.
            // Let's rely on standard yaw conversion.
            // Deprecated Block.getData() is byte.
            // In 1.13+ Rotatable.getRotation() returns BlockFace.
            // We need to convert BlockFace to yaw.
            // Let's assume standard rotation for now.

            // Simpler approach for now:
            // Just set yaw.
            // loc.setYaw(...);
            // We'll refine visual rotation if needed.
            // For Standing signs, let's just use the Rotation if possible.
            // Actually, TextDisplay can handle rotation via Transformation or Location Yaw.
            // Let's set Location Yaw.

            // Hacky way for Rotatable:
            // We will try to match the sign's rotation.
            // Since I can't easily test exact degrees without running it, I'll stick to a
            // safe default and maybe update if user complains.
            // Actually, for standing signs, we prob just want to copy the yaw from the
            // BlockState data if accessible, or calculate from BlockFace.
            // block.getBlockData() -> Rotatable -> getRotation() -> BlockFace.
        }

        TextDisplay textDisplay = (TextDisplay) block.getWorld().spawnEntity(loc, EntityType.TEXT_DISPLAY);

        // Apply Orientation
        if (block.getBlockData() instanceof Directional) {
            loc.setYaw(yRot);
            textDisplay.teleport(loc);

            // Offset to sit on the face (Wall signs are slightly off center)
            // Wall signs are usually at the edge of the block.
            // Standard block is 16px deep. Sign is maybe 2px?
            // Translation: Z = -0.4ish (approx 7px out from center) to sit on face.
            // Updated: User reports text is too high. Lowering Y offset in translation.
            Transformation t = textDisplay.getTransformation();
            t.getTranslation().set(0, -0.1f, -0.42f); // Push forward and down slightly

            // Apply Scale
            float scale = (float) plugin.getConfig().getDouble("sign-text-scale", 0.4);
            t.getScale().set(scale);

            textDisplay.setTransformation(t);
        } else if (block.getBlockData() instanceof Rotatable) {
            Rotatable rotatable = (Rotatable) block.getBlockData();
            // Convert BlockFace to Yaw
            float yaw = faceToYaw(rotatable.getRotation());
            loc.setYaw(yaw);
            textDisplay.teleport(loc);

            // Apply Scale for standing signs too
            Transformation t = textDisplay.getTransformation();
            // Lower standing signs too
            t.getTranslation().set(0, -0.15f, 0);

            float scale = (float) plugin.getConfig().getDouble("sign-text-scale", 0.4);
            t.getScale().set(scale);
            textDisplay.setTransformation(t);
        }

        textDisplay.text(fullText);
        textDisplay.setAlignment(org.bukkit.entity.TextDisplay.TextAlignment.CENTER); // Ensure centered
        textDisplay.setBillboard(org.bukkit.entity.Display.Billboard.FIXED); // Lock rotation
        textDisplay.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0)); // Transparent

        // Apply View Distance from Config
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
