package com.marcpg.tgc.util;

import com.marcpg.tgc.Configuration;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
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
        player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 10, 20, false, false, false));
        player.setHealth(20.0);
        player.setAbsorptionAmount(0.0);
        player.setGameMode(GameMode.SURVIVAL);
        player.setFireTicks(0);
    }
}
