package com.marcpg.tgc.challenge.challenges.mab;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import com.marcpg.libpg.event.PlayerEvent;
import com.marcpg.libpg.storing.tuple.triple.Triple;
import com.marcpg.tgc.Configuration;
import com.marcpg.tgc.TheGentleChallenges;
import com.marcpg.tgc.challenge.Challenge;
import io.papermc.paper.event.entity.EntityPortalReadyEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.block.Container;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EnderDragonChangePhaseEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class MonsterArmyBattle extends Challenge implements Listener {
    public enum Stage { COLLECTION, CONFIGURATION, BATTLE }
    public enum RandomizerType { GLOBAL, TEAM, PLAYER }

    public static final List<Material> VALID_MATERIALS = Arrays.stream(Material.values())
            .filter(m -> !m.isEmpty() && m.isItem() && !m.isLegacy())
            .toList();

    public LinkedHashMap<UUID, MABTeam> teams;
    public LinkedHashMap<UUID, MABPlayer> players;
    public LinkedHashMap<MABTeam, MABTeam> teamLinks;

    protected Stage currentStage = Stage.COLLECTION;

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(@NotNull EntityDeathEvent event) {
        if (currentStage == Stage.COLLECTION) {
            Player player = event.getEntity().getKiller();
            if (player == null || event.getEntityType() == EntityType.PLAYER) return;

            MABPlayer mabPlayer = players.get(player.getUniqueId());
            if (mabPlayer == null) return;

            randomizeDrops(mabPlayer, event.getDrops());

            mabPlayer.kill(event.getEntity());
        } else if (currentStage == Stage.BATTLE) {
            if (!event.getEntity().getWorld().getName().startsWith("mab-team-battle-")) return;

            MABTeam team = teams.get(UUID.fromString(event.getEntity().getWorld().getName().replace("mab-team-battle-", "")));
            if (team == null || team.finished != null) return;

            event.getDrops().clear();
            team.mobDeath();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(@NotNull BlockBreakEvent event) {
        MABPlayer mabPlayer = players.get(event.getPlayer().getUniqueId());
        if (mabPlayer == null) return;

        if (currentStage == Stage.BATTLE && event.getPlayer().getGameMode() == GameMode.SURVIVAL && !event.getBlock().hasMetadata("player-placed")) {
            event.getPlayer().sendMessage(Component.text("Du kannst die Arena nicht zerstören!", NamedTextColor.RED));
            event.setCancelled(true);
        }

        if (currentStage == Stage.COLLECTION) {
            Location l = event.getBlock().getLocation();

            if (event.getBlock().getState() instanceof Container container && !container.getInventory().isEmpty()) {
                l.getWorld().dropItemNaturally(l, new ItemStack(mabPlayer.randomMaterialMap.getOrDefault(container.getType(), container.getType())));
                event.setDropItems(false);

                for (ItemStack item : container.getInventory().getContents()) {
                    if (item != null && !item.isEmpty())
                        l.getWorld().dropItemNaturally(l, item);
                }
                return;
            }

            List<ItemStack> items = randomizeDrops(mabPlayer, new ArrayList<>(event.getBlock().getDrops(event.getPlayer().getInventory().getItemInMainHand(), event.getPlayer())));
            event.setDropItems(false);
            items.forEach(drop -> l.getWorld().dropItemNaturally(l, drop));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(@NotNull BlockPlaceEvent event) {
        event.getBlock().setMetadata("player-placed", new FixedMetadataValue(TheGentleChallenges.PLUGIN, true));
    }

    @Override
    @EventHandler(ignoreCancelled = true)
    public void onPlayerPostRespawn(@NotNull PlayerPostRespawnEvent event) {
        if (currentStage == Stage.BATTLE) {
            event.getPlayer().setGameMode(GameMode.SPECTATOR);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (currentStage == Stage.BATTLE) return;

        MABPlayer player = players.get(event.getPlayer().getUniqueId());
        if (player == null) return;

        if (!player.team.collectionWorlds.contains(event.getRespawnLocation().getWorld())) {
            event.setRespawnLocation(player.team.collectionWorlds.left().getSpawnLocation());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDamage(PlayerEvent.PlayerDamageEvent event) {
        if (currentStage == Stage.CONFIGURATION) {
            event.setDamage(0.0);
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityPortalReady(@NotNull EntityPortalReadyEvent event) {
        if (!(event.getEntity() instanceof Player player) || event.getTargetWorld() == null) return;

        MABPlayer mabPlayer = players.get(player.getUniqueId());
        if (mabPlayer == null) return;

        switch (event.getTargetWorld().getEnvironment()) {
            case NORMAL -> event.setTargetWorld(mabPlayer.team.collectionWorlds.left());
            case NETHER -> event.setTargetWorld(mabPlayer.team.collectionWorlds.middle());
            case THE_END -> event.setTargetWorld(mabPlayer.team.collectionWorlds.right());
        }
    }

    // Make events do nothing.
    @Override @EventHandler(ignoreCancelled = true)
    public void onEnderDragonChangePhaseEvent(@NotNull EnderDragonChangePhaseEvent event) {}
    @Override @EventHandler(ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {}

    @Override
    public Component name() {
        return MiniMessage.miniMessage().deserialize("<bold><gradient:#FF3000:#CC0000:#FF3000>Monster Army Battle</gradient></bold>");
    }

    @Override
    public void initLogic() {
        teams = new LinkedHashMap<>();
        players = new LinkedHashMap<>();

        Triple<World, World, World> collectionWorlds = Triple.of(
                Bukkit.createWorld(WorldCreator.name("base-collection")),
                Bukkit.createWorld(WorldCreator.name("base-collection-nether")),
                Bukkit.createWorld(WorldCreator.name("base-collection-end")));

        Map<Material, Material> randomMaterials = Configuration.MAB_ITEM_RANDOMIZATION == RandomizerType.GLOBAL ? randomShuffle(UUID.randomUUID()) : null;

        if (Configuration.MAB_TEAM_RANDOMIZATION) {
            List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers().stream().filter(p -> p.getGameMode() != GameMode.SPECTATOR).toList());
            Collections.shuffle(onlinePlayers, new Random());

            // Divide into two teams with the first one being bigger if the number of players is odd.
            int teamSize = Math.ceilDiv(onlinePlayers.size(), 2);
            for (int i = 0; i < onlinePlayers.size(); i += teamSize) {
                List<Player> teamPlayers = onlinePlayers.subList(i, Math.min(i + teamSize, onlinePlayers.size()));
                MABTeam team = new MABTeam(this, collectionWorlds, teamPlayers, randomMaterials);
                teams.put(team.uuid, team);
                team.players.forEach(t -> players.put(t.uuid, t));
            }
        } else {
            //noinspection unchecked
            for (List<String> teamPlayers : (List<List<String>>) Objects.requireNonNull(Configuration.CONFIG.getList("teams"))) {
                MABTeam team = new MABTeam(this, collectionWorlds, teamPlayers.stream().map(Bukkit::getPlayer).filter(Objects::nonNull).toList(), randomMaterials);
                teams.put(team.uuid, team);
                team.players.forEach(t -> players.put(t.uuid, t));
            }
        }

        teamLinks = new LinkedHashMap<>();
        List<MABTeam> t = new ArrayList<>(teams.sequencedValues());
        for (int i = 0; i < teams.size(); i++) {
            if (i == t.size() - 1) {
                teamLinks.put(t.get(i), t.getFirst());
            } else {
                teamLinks.put(t.get(i), t.get(i + 1));
            }
        }

        timer.reversed(true);
        timer.timer().set(Configuration.CONFIG.getInt("monster-army-battle.collection-time-seconds"));

        currentStage(Stage.COLLECTION);
    }

    @Override
    public void customSecondTick() {
        if (timer.timer().get() <= 0 && currentStage == Stage.COLLECTION)
            startConfiguration();
    }

    public void startConfiguration() {
        running = false; // Pause Timer
        currentStage(Stage.CONFIGURATION);
    }

    public void startBattle() {
        timer.reversed(false);
        timer.timer().set(0);
        running = true; // Resume Timer

        currentStage(Stage.BATTLE);
    }

    public void currentStage(Stage currentStage) {
        this.currentStage = currentStage;
        teams.values().forEach(t -> t.transfer(currentStage));
    }

    public static @NotNull Map<Material, Material> randomShuffle(@NotNull UUID uuid) {
        Map<Material, Material> map = new HashMap<>();

        List<Material> shuffledItems = new ArrayList<>(VALID_MATERIALS);
        Collections.shuffle(shuffledItems, new Random(uuid.hashCode()));

        for (int i = 0; i < VALID_MATERIALS.size(); i++) {
            map.put(VALID_MATERIALS.get(i), shuffledItems.get(i));
        }
        return map;
    }

    public List<ItemStack> randomizeDrops(@NotNull MABPlayer player, @NotNull List<ItemStack> items) {
        items.replaceAll(item -> {
            Material mapped = player.randomMaterialMap.get(item.getType());
            return mapped != null ? new ItemStack(mapped, item.getAmount()) : item;
        });
        return items;
    }

    public MABTeam nextTeam(MABTeam team) {
        return teamLinks.get(team);
    }
}
