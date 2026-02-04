package com.example.itemframeconverter;

import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class FrameInteractListener implements Listener {

    private final ConversionManager conversionManager;

    public FrameInteractListener(ConversionManager conversionManager) {
        this.conversionManager = conversionManager;
    }

    @EventHandler(ignoreCancelled = true, priority = org.bukkit.event.EventPriority.HIGH)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (!conversionManager.isConverting(player))
            return;

        Entity clicked = event.getRightClicked();
        if (clicked instanceof ItemFrame) {
            event.setCancelled(true); // Prevent rotating the item normally
            conversionManager.convert(player, (ItemFrame) clicked);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = org.bukkit.event.EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player))
            return;
        Player player = (Player) event.getDamager();

        if (!conversionManager.isConverting(player))
            return;

        Entity clicked = event.getEntity();
        if (clicked instanceof ItemFrame) {
            event.setCancelled(true); // Prevent breaking the frame
            conversionManager.convert(player, (ItemFrame) clicked);
        }
    }
}
