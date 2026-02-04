package com.example.itemframeconverter;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SignConversionCommand implements CommandExecutor {

    private final SignConversionManager manager;

    public SignConversionCommand(SignConversionManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (sender instanceof Player) {
            manager.toggleConversionMode((Player) sender);
        } else {
            sender.sendMessage("Only players can use this command.");
        }
        return true;
    }
}
