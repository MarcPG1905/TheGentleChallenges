package com.marcpg.tgc.challenge.challenges;

import com.marcpg.libpg.data.time.Time;
import com.marcpg.libpg.storing.Cord;
import com.marcpg.libpg.storing.CordMinecraftAdapter;
import com.marcpg.tgc.TheGentleChallenges;
import com.marcpg.tgc.challenge.Challenge;
import com.marcpg.tgc.util.Configuration;
import com.marcpg.tgc.util.Timer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class RandomWorldChallenge extends Challenge implements Listener {
    public record RandomWorld(String name, Cord spawn, String title, String creator) {
        public World asWorld() {
            return Bukkit.createWorld(WorldCreator.name(name));
        }

        @SuppressWarnings("unchecked")
        public static @NotNull RandomWorld of(@NotNull Map<?, ?> map) {
            return new RandomWorld(
                    (String) map.get("name"),
                    Cord.ofList((List<Integer>) map.get("spawn")),
                    (String) map.get("title"),
                    (String) map.get("creator")
            );
        }
    }

    private final List<RandomWorld> left = new ArrayList<>();
    private final Map<UUID, ItemStack[]> invSnapshots = new HashMap<>();
    private final Map<UUID, Location> locationSnapshots = new HashMap<>();
    private final Time untilNext = new Time(Short.MAX_VALUE);

    private boolean inWorld;
    private Timer tempTimer;

    @Override
    @EventHandler(ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (inWorld()) {
            fail();
            return;
        }

        super.onPlayerDeath(event);
    }

    @Override
    public Component name() {
        return MiniMessage.miniMessage().deserialize("<bold><gradient:#33FF33:#FF5555>Vanilla Minecraft</gradient></bold>");
    }

    @Override
    public void initLogic() {
        left.addAll(Configuration.RW_RANDOM_WORLDS);
        Collections.shuffle(left);
        left.forEach(RandomWorld::asWorld); // Load all worlds.

        scheduleNewWorld();
    }

    public boolean inWorld() {
        return inWorld;
    }

    public void fail() {
        Location spawn = Objects.requireNonNull(Bukkit.getWorld("world")).getSpawnLocation();
        Bukkit.getOnlinePlayers().forEach(p -> p.teleportAsync(spawn));

        done();
        Bukkit.getServer().showTitle(Title.title(Component.text("Fail!", NamedTextColor.RED), Component.text("Ihr habt verloren!", NamedTextColor.GRAY)));
    }

    public void success() {
        Bukkit.getOnlinePlayers().forEach(p -> {
            if (locationSnapshots.containsKey(p.getUniqueId())) {
                p.teleportAsync(locationSnapshots.get(p.getUniqueId()));
            } else {
                p.teleportAsync(Objects.requireNonNull(Bukkit.getWorld("world")).getSpawnLocation());
            }

            if (invSnapshots.containsKey(p.getUniqueId()))
                p.getInventory().setContents(invSnapshots.get(p.getUniqueId()));
        });

        done();
        Bukkit.getServer().showTitle(Title.title(Component.text("Success!", NamedTextColor.RED), Component.text("Ihr habt die Welt überlebt!", NamedTextColor.GRAY)));
    }

    public void done() {
        locationSnapshots.clear();
        invSnapshots.clear();
        inWorld = false;

        timer.show(true);
        tempTimer.show(false);

        if (left.isEmpty()) {
            TheGentleChallenges.LOG.error("No more worlds available for random world challenge!");
            Bukkit.getServer().sendMessage(Component.text("Es sind keine weiteren Welten verfügbar!", NamedTextColor.RED));
        } else {
            scheduleNewWorld();
        }
    }

    @Override
    public void customSecondTick() {
        tempTimer.tick();

        untilNext.decrement();
        if (untilNext.get() <= 0) {
            if (!running) return;
            untilNext.set(Integer.MAX_VALUE);
            randomWorld();
        }
    }

    private void randomWorld() {
        RandomWorld world = left.removeFirst();
        Location loc = CordMinecraftAdapter.toLocation(world.spawn(), world.asWorld());

        Bukkit.getOnlinePlayers().forEach(p -> {
            invSnapshots.put(p.getUniqueId(), p.getInventory().getContents());
            locationSnapshots.put(p.getUniqueId(), p.getLocation().clone());
            p.teleportAsync(loc);
        });
        inWorld = true;

        tempTimer = new Timer(this);
        timer.show(false);
        tempTimer.show(true);
    }

    public void scheduleNewWorld() {
        untilNext.set(ThreadLocalRandom.current().nextInt(Configuration.RW_INTERVAL_MIN, Configuration.RW_INTERVAL_MAX));
    }
}
