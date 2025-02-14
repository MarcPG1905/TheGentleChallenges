package com.marcpg.tgc.util;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Utilities {
    public static final double DEFAULT_DAMAGE = 2.0;

    public static double damage(String entity) {
        if (Configuration.CONFIG.contains("aggressive-mobs." + entity))
            return Configuration.CONFIG.getDouble("aggressive-mobs." + entity);
        return DEFAULT_DAMAGE;
    }

    public static void reset(@NotNull Player player) {
        player.clearActivePotionEffects();
        player.setSaturation(2.5f);
        player.setFoodLevel(20);
        player.setHealth(20.0);
        player.setAbsorptionAmount(0.0);
        player.setGameMode(GameMode.SURVIVAL);
        player.setFireTicks(0);
    }
}
