package com.marcpg.tgc.challenge.challenges;

import com.destroystokyo.paper.event.entity.EntityKnockbackByEntityEvent;
import com.marcpg.libpg.util.ItemBuilder;
import com.marcpg.tgc.TheGentleChallenges;
import com.marcpg.tgc.challenge.Challenge;
import com.marcpg.tgc.util.Utilities;
import io.papermc.paper.entity.TeleportFlag;
import io.papermc.paper.event.entity.EntityKnockbackEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// TODO: Cancel manual movement, except for fishing hooks.
public class FishingChallenge extends Challenge implements Listener {
    public static final double MULTIPLIER = 1.0;

    private final Map<Player, Long> allowed = new ConcurrentHashMap<>();

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(@NotNull EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player by && event.getEntity() instanceof Player to) {
            if (by.getInventory().getItemInMainHand().isEmpty() || by.getInventory().getItemInMainHand().getType() == Material.FISHING_ROD) {
                event.setDamage(0.0);
                allowed.put(to, System.currentTimeMillis());
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerFish(@NotNull PlayerFishEvent event) {
        if (event.getCaught() instanceof Player to && event.getState() == PlayerFishEvent.State.CAUGHT_ENTITY) {
            allowed.put(to, System.currentTimeMillis());
            Bukkit.getScheduler().runTask(TheGentleChallenges.PLUGIN, () -> to.setVelocity(to.getVelocity().multiply(MULTIPLIER)));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityKnockback(@NotNull EntityKnockbackEvent event) {
        if (event instanceof EntityKnockbackByEntityEvent e) {
            if (e.getHitBy() instanceof Player && e.getEntity() instanceof Player to) {
                allowed.put(to, System.currentTimeMillis());
                event.setCancelled(false);
                event.setKnockback(event.getKnockback().multiply(MULTIPLIER));
                return;
            }
        }
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(@NotNull PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getWorld() != to.getWorld()) return;
        if (from.getX() == to.getX() && from.getZ() == to.getZ()) return;

        Vector v = event.getPlayer().getVelocity();
        if (event.getPlayer().isOnGround() && allowed.containsKey(event.getPlayer()) && allowed.get(event.getPlayer()) + 1000 < System.currentTimeMillis()) {
            allowed.remove(event.getPlayer());
            return;
        }

        if (!allowed.containsKey(event.getPlayer())) {
            event.getPlayer().teleport(new Location(from.getWorld(), from.x(), to.y(), from.z(), to.getYaw(), to.getPitch()), TeleportFlag.Relative.VELOCITY_ROTATION, TeleportFlag.Relative.VELOCITY_Y);
        }
    }

    @Override
    public Component name() {
        return MiniMessage.miniMessage().deserialize("<bold><gradient:#33FF33:#FF5555>Fishing Challenge</gradient></bold>");
    }

    @Override
    public void initLogic() {
        int distance = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            Utilities.reset(p);
            p.setWalkSpeed(0.0f);
            distance += 2;
            p.teleport(p.getWorld().getSpawnLocation().clone().add(distance, 0, 0).toHighestLocation().add(0, 1, 0));
            p.give(new ItemBuilder(Material.FISHING_ROD)
                    .name(Component.text("Playing Rod"))
                    .lore(List.of(Component.text("Die einzige Möglichkeit, dich zu bewegen!", NamedTextColor.DARK_GRAY)))
                    .editMeta(m -> m.setUnbreakable(true))
                    .build());
        }
    }
}
