package com.example.itemframeconverter;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ConversionCommand implements CommandExecutor {

    private final ConversionManager conversionManager;

    public ConversionCommand(ConversionManager conversionManager) {
        this.conversionManager = conversionManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        conversionManager.toggleMode(player);
        return true;
    }
}
