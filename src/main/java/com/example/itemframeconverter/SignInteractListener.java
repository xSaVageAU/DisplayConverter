package com.example.itemframeconverter;

import org.bukkit.Tag;
import org.bukkit.block.sign.Side;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class SignInteractListener implements Listener {

    private final SignConversionManager manager;

    public SignInteractListener(SignConversionManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND)
            return; // Main hand only
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;
        if (event.getClickedBlock() == null)
            return;

        // Check if block is a sign
        if (!Tag.SIGNS.isTagged(event.getClickedBlock().getType()))
            return;

        if (manager.isInConversionMode(event.getPlayer())) {
            event.setCancelled(true); // Don't edit the sign

            // Determine side.
            // In 1.20+, we can use getInteractableSide() or raytracing, but simpler is
            // assuming Front for now
            // OR checks logic.
            // PlayerInteractEvent usually interacts with the front unless specified.
            // Let's default to FRONT for now, or use `event.getBlockFace()` to guess?
            // Unlike interactions, picking the *text* side is tricky without raytrace.
            // We will default to Side.FRONT.

            manager.convertSign(event.getPlayer(), event.getClickedBlock(), Side.FRONT);
        }
    }
}
