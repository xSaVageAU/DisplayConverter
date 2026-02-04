package com.example.itemframeconverter;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class ManagementCommand implements CommandExecutor {

    private final ConversionManager conversionManager;
    private final SignConversionManager signConversionManager;
    private final DatabaseManager databaseManager;
    private final boolean isAdmin;

    public ManagementCommand(ConversionManager conversionManager, SignConversionManager signConversionManager,
            DatabaseManager databaseManager, boolean isAdmin) {
        this.conversionManager = conversionManager;
        this.signConversionManager = signConversionManager;
        this.databaseManager = databaseManager;
        this.isAdmin = isAdmin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        Player player = (Player) sender;

        if (args.length > 0) {
            String sub = args[0].equalsIgnoreCase("delete") ? "delete"
                    : args[0].equalsIgnoreCase("tp") ? "tp" : args[0].equalsIgnoreCase("revert") ? "revert" : null;

            if (sub != null && args.length == 2) {
                try {
                    UUID uuid = UUID.fromString(args[1]);

                    // Ideally we'd know the type to call the right manager, or managers would
                    // check.
                    // But we can check the DB first to get the type? Or just try both?
                    // Let's try to get info from DB to know type.
                    // But databaseManager.getDisplays returns a list.
                    // Let's iterate all displays to find it, or add getDisplay(UUID) to DB.
                    // For simply, let's just assume we can get it from the generic list or
                    // try-catch.
                    // Better: The Managers' methods check validity.

                    if (sub.equals("delete")) {
                        conversionManager.deleteDisplay(uuid); // This handles DB delete too
                        // Also try sign manager for consistency?
                        // Actually both managers just delete from World then DB.
                        // But we should probably use the type if possible.
                        player.sendMessage(Component.text("Display deleted.", NamedTextColor.GREEN));
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                    } else if (sub.equals("tp")) {
                        Location loc = conversionManager.getDisplayLocation(uuid); // Checks all worlds
                        if (loc != null) {
                            player.teleport(loc);
                            player.sendMessage(Component.text("Teleported.", NamedTextColor.GREEN));
                            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f,
                                    1.0f);
                        } else {
                            player.sendMessage(Component.text("Display not found.", NamedTextColor.RED));
                        }
                    } else if (sub.equals("revert")) {
                        // We need to know which manager to call to RESTORE the block/item.
                        // ItemDisplay -> ConversionManager
                        // TextDisplay -> SignConversionManager
                        // Let's check the entity type in the world first?
                        // Managers' revertDisplay checks type. So we can try both or check entity.
                        // Let's check entity.
                        org.bukkit.entity.Entity entity = org.bukkit.Bukkit.getEntity(uuid);
                        if (entity instanceof org.bukkit.entity.ItemDisplay) {
                            conversionManager.revertDisplay(uuid, player);
                        } else if (entity instanceof org.bukkit.entity.TextDisplay) {
                            signConversionManager.revertDisplay(uuid, player);
                        } else {
                            // Maybe chunk unloaded?
                            // If unloaded, we can't revert anyway (can't spawn/mod blocks reliably without
                            // loading).
                            // So just say unknown.
                            player.sendMessage(
                                    Component.text("Entity not found (chunk unloaded?)", NamedTextColor.RED));
                        }
                    }
                } catch (IllegalArgumentException e) {
                    player.sendMessage(Component.text("Invalid UUID.", NamedTextColor.RED));
                }
                return true;
            }
        }

        // List logic
        int page = 1;
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
            }
        }

        List<DatabaseManager.DisplayData> displays;
        if (isAdmin) {
            displays = databaseManager.getAllDisplays();
        } else {
            displays = databaseManager.getDisplays(player.getUniqueId());
        }

        if (displays.isEmpty()) {
            player.sendMessage(Component.text("No displays found.", NamedTextColor.YELLOW));
            return true;
        }

        int start = (page - 1) * 10;
        int end = Math.min(start + 10, displays.size());

        if (start >= displays.size()) {
            player.sendMessage(Component.text("Page not found.", NamedTextColor.RED));
            return true;
        }

        player.sendMessage(Component.text("Displays (Page " + page + "):", NamedTextColor.GOLD));

        for (int i = start; i < end; i++) {
            DatabaseManager.DisplayData data = displays.get(i);
            UUID uuid = data.entityId();

            // 1. Buttons (Fixed width-ish)
            Component tpBtn = Component.text("[TP] ", NamedTextColor.AQUA)
                    .decoration(net.kyori.adventure.text.format.TextDecoration.BOLD, true)
                    .hoverEvent(net.kyori.adventure.text.event.HoverEvent
                            .showText(Component.text("Teleport", NamedTextColor.GRAY)))
                    .clickEvent(ClickEvent.runCommand("/" + label + " tp " + uuid.toString()));

            Component revBtn = Component.text("[REV] ", NamedTextColor.GOLD)
                    .decoration(net.kyori.adventure.text.format.TextDecoration.BOLD, true)
                    .hoverEvent(net.kyori.adventure.text.event.HoverEvent
                            .showText(Component.text("Revert", NamedTextColor.GRAY)))
                    .clickEvent(ClickEvent.runCommand("/" + label + " revert " + uuid.toString()));

            Component delBtn = Component.text("[DEL] ", NamedTextColor.RED)
                    .decoration(net.kyori.adventure.text.format.TextDecoration.BOLD, true)
                    .hoverEvent(net.kyori.adventure.text.event.HoverEvent
                            .showText(Component.text("Delete", NamedTextColor.GRAY)))
                    .clickEvent(ClickEvent.runCommand("/" + label + " delete " + uuid.toString()));

            // 2. Info with Tooltip
            String typeStr = data.type() != null ? data.type() : "UNKNOWN";
            NamedTextColor typeColor = "ITEM".equals(typeStr) ? NamedTextColor.AQUA : NamedTextColor.LIGHT_PURPLE;
            String previewText = data.preview() != null && !data.preview().isEmpty() ? data.preview() : "No Preview";

            Component infoText = Component.text("[" + typeStr + "]", typeColor)
                    .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                            Component.text("Content: ", NamedTextColor.GRAY)
                                    .append(Component.text(previewText, NamedTextColor.WHITE))));

            // Upgrade to Native Item Tooltip if entity is loaded and matches
            org.bukkit.entity.Entity entity = org.bukkit.Bukkit.getEntity(uuid);
            if (entity instanceof org.bukkit.entity.ItemDisplay) {
                org.bukkit.inventory.ItemStack item = ((org.bukkit.entity.ItemDisplay) entity).getItemStack();
                if (item != null) {
                    // Use Paper's native hover event generation for 1:1 rendering (Lore, Enchants,
                    // etc.)
                    infoText = infoText.hoverEvent(item.asHoverEvent());
                }
            }

            // 3. Location
            Location loc = conversionManager.getDisplayLocation(uuid);
            String locStr = (loc == null) ? "Unloaded"
                    : String.format("%d, %d, %d", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

            Component coordsText = Component.text(" (" + locStr + ")", NamedTextColor.DARK_GRAY);

            // Combine
            player.sendMessage(Component.text((i + 1) + ". ", NamedTextColor.GRAY)
                    .append(tpBtn).append(revBtn).append(delBtn)
                    .append(Component.text("- ", NamedTextColor.DARK_GRAY))
                    .append(infoText)
                    .append(coordsText));
        }

        return true;
    }
}
