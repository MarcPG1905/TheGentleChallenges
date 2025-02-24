package com.marcpg.tgc.challenge.challenges;

import com.marcpg.libpg.event.PlayerEvent;
import com.marcpg.libpg.storing.CordMinecraftAdapter;
import com.marcpg.libpg.util.ItemBuilder;
import com.marcpg.libpg.util.Randomizer;
import com.marcpg.tgc.challenge.Challenge;
import com.marcpg.tgc.challenge.ChallengeManager;
import com.marcpg.tgc.util.Configuration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class HideNSeek extends Challenge implements Listener {
    public enum HintType {
        /** Just nothing happens. */
        NONE(p -> {}),
        /** Duration is `HINT_INTERVAL / 20` but also `* 20` to convert to ticks, so that is just HINT_INTERVAL. */
        GLOW(p -> p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Configuration.HNS_HINT_INTERVAL, 0, false, false, false))),
        /** That sound is a mix between a pop and bell sound. See <a href="https://minecraft.wiki/w/Trial_Spawner#Unique">Minecraft Wiki</a>. */
        SOUND(p -> p.getWorld().playSound(p.getLocation(), Sound.BLOCK_TRIAL_SPAWNER_EJECT_ITEM, 1.0f, 1.0f));

        public final Consumer<Player> action;

        HintType(Consumer<Player> action) {
            this.action = action;
        }
    }

    protected Player seeker;
    protected Location spawn;

    @SuppressWarnings("UnstableApiUsage")
    @EventHandler(ignoreCancelled = true)
    public void onPlayerDamage(PlayerEvent.@NotNull PlayerDamageEvent event) {
        if (event.getDamageSource().getCausingEntity() == seeker) return;

        event.setCancelled(true);
        event.setDamage(0.0);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerLogin(@NotNull PlayerLoginEvent event) {
        if (event.getPlayer() == seeker) return;

        event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
        event.kickMessage(Component.text("Du bist schon raus!", NamedTextColor.RED));
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDeath(@NotNull PlayerDeathEvent event) {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        players.remove(seeker);

        event.getPlayer().kick(Component.text("Du wurdest gefangen!", NamedTextColor.RED));
        event.deathMessage(Component.text(event.getPlayer().getName() + " wurde gefangen. Nur noch " + players.size() + " Spieler!", NamedTextColor.RED));

        if (players.size() == 1) {
            end(true);
            ChallengeManager.PROPERTIES.put("last-winner", GsonComponentSerializer.gson().serialize(players.getFirst().name()));
            ChallengeManager.PROPERTIES.put("last-winner-time", timer.timer().get());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(@NotNull PlayerMoveEvent event) {
        if (event.getPlayer() == seeker && timer.timer().get() < Configuration.HNS_HIDING_TIME)
            event.setCancelled(true);
    }

    @Override
    public Component name() {
        return MiniMessage.miniMessage().deserialize("<bold><gradient:#33FF33:#FF5555>Hide n' Seek</gradient></bold>");
    }

    @Override
    public void initLogic() {
        if (Bukkit.getOnlinePlayers().isEmpty())
            throw new RuntimeException("No players online.");

        seeker = Bukkit.getPlayer(Configuration.HNS_SEEKER);
        if (seeker == null)
            seeker = Randomizer.fromCollection(Bukkit.getOnlinePlayers());

        seeker.give(new ItemBuilder(Material.DIAMOND_SWORD)
                .name(Component.text("Catch Sword", NamedTextColor.RED))
                .enchant(Enchantment.SHARPNESS, Short.MAX_VALUE)
                .build());

        spawn = CordMinecraftAdapter.toLocation(Configuration.HNS_SPAWN, seeker.getWorld());
        Bukkit.getOnlinePlayers().forEach(p -> p.teleportAsync(spawn));

        seeker.setRotation(0, 90);
        seeker.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * Configuration.HNS_HIDING_TIME, 0, false, false, false));
    }

    @Override
    public void customSecondTick() {
        long sec = timer.timer().get();

        if (sec == Configuration.HNS_HIDING_TIME) {
            Bukkit.getServer().sendMessage(Component.text(seeker.getName() + " fängt jetzt an zu suchen!", NamedTextColor.YELLOW));
            Bukkit.getOnlinePlayers().forEach(p -> p.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f));
        }

        if (sec >= Configuration.HNS_TOTAL_TIME)
            end(false);

        if (sec >= Configuration.HNS_HINT_TIME && sec - Configuration.HNS_HINT_TIME % Configuration.HNS_HINT_INTERVAL == 0)
            Bukkit.getOnlinePlayers().stream().filter(p -> p != seeker).forEach(p -> Configuration.HNS_HINT_TYPE.action.accept(p));
    }
}
