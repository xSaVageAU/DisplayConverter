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
    private final DatabaseManager databaseManager;
    private final boolean isAdmin;

    public ManagementCommand(ConversionManager conversionManager, DatabaseManager databaseManager, boolean isAdmin) {
        this.conversionManager = conversionManager;
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
            if (args[0].equalsIgnoreCase("delete") && args.length == 2) {
                try {
                    UUID uuid = UUID.fromString(args[1]);
                    conversionManager.deleteDisplay(uuid);
                    player.sendMessage(Component.text("Display deleted.", NamedTextColor.GREEN));
                } catch (IllegalArgumentException e) {
                    player.sendMessage(Component.text("Invalid UUID.", NamedTextColor.RED));
                }
                return true;
            }
            if (args[0].equalsIgnoreCase("tp") && args.length == 2) {
                try {
                    UUID uuid = UUID.fromString(args[1]);
                    Location loc = conversionManager.getDisplayLocation(uuid);
                    if (loc != null) {
                        player.teleport(loc);
                        player.sendMessage(Component.text("Teleported to display.", NamedTextColor.GREEN));
                    } else {
                        player.sendMessage(Component.text("Display not found (it might be in an unloaded chunk).",
                                NamedTextColor.RED));
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

        List<UUID> displays;
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
            UUID uuid = displays.get(i);
            Location loc = conversionManager.getDisplayLocation(uuid);
            String locStr = (loc == null) ? "Unknown/Unloaded"
                    : String.format("%.0f, %.0f, %.0f", loc.getX(), loc.getY(), loc.getZ());

            Component message = Component.text((i + 1) + ". ", NamedTextColor.GRAY)
                    .append(Component.text(locStr, NamedTextColor.WHITE))
                    .append(Component.text(" [TP] ", NamedTextColor.AQUA)
                            .clickEvent(ClickEvent.runCommand("/" + label + " tp " + uuid.toString())))
                    .append(Component.text(" [DELETE]", NamedTextColor.RED)
                            .clickEvent(ClickEvent.runCommand("/" + label + " delete " + uuid.toString())));

            player.sendMessage(message);
        }

        return true;
    }
}
