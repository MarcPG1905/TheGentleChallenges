package com.marcpg.tgc.challenge.challenges;

import com.marcpg.tgc.TheGentleChallenges;
import com.marcpg.tgc.challenge.Challenge;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

public class ZeroHeartsChallenge extends Challenge {
    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(@NotNull EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && player.getAbsorptionAmount() <= event.getDamage())
            event.setDamage(500.0);
    }

    @Override
    public void customSecondTick() {
        forEachPlayer(p -> {
            if (p.getGameMode() == GameMode.SURVIVAL && p.getAbsorptionAmount() <= 0.0)
                p.damage(500.0);
        });
    }

    @Override
    public Component name() {
        return MiniMessage.miniMessage().deserialize("<bold><gradient:#FF0000:#FF6600>Zero Hearts</gradient></bold>");
    }

    @Override
    public void initLogic() {
        forEachPlayer(p -> {
            p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 6000, 0, true)); // 6000 Ticks = 5 Minutes
            Bukkit.getScheduler().runTask(TheGentleChallenges.PLUGIN, () -> p.setAbsorptionAmount(2.0));
        });
    }

    @SuppressWarnings("unused")
    public static void reset() {
        forEachPlayer(p -> {
            p.removePotionEffect(PotionEffectType.ABSORPTION);
            p.setHealth(20.0);
        });
    }
}
