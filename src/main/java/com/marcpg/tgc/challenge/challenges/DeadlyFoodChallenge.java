package com.marcpg.tgc.challenge.challenges;

import com.marcpg.tgc.challenge.Challenge;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class DeadlyFoodChallenge extends Challenge {
    @EventHandler(ignoreCancelled = true)
    public void onPlayerItemConsume(@NotNull PlayerItemConsumeEvent event) {
        if (event.getItem().getType().name().toLowerCase().contains("potion")) return;

        AttributeInstance health = Objects.requireNonNull(event.getPlayer().getAttribute(Attribute.MAX_HEALTH));
        if (health.getBaseValue() <= 2.0)
            event.getPlayer().setHealth(0);
        health.setBaseValue(health.getBaseValue() - 2.0);
    }

    @Override
    public Component name() {
        return MiniMessage.miniMessage().deserialize("<bold><gradient:#FF0000:#FF6600>Deadly Food</gradient></bold>");
    }

    @SuppressWarnings("unused")
    public static void reset() {
        forEachPlayer(p -> {
            Objects.requireNonNull(p.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(20.0);
            p.setHealth(20.0);
        });
    }
}
