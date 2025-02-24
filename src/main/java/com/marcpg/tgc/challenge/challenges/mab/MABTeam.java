package com.marcpg.tgc.challenge.challenges.mab;

import com.marcpg.libpg.data.time.Time;
import com.marcpg.libpg.storing.Cord;
import com.marcpg.libpg.storing.CordMinecraftAdapter;
import com.marcpg.libpg.storing.tuple.triple.Triple;
import com.marcpg.libpg.util.MinecraftTime;
import com.marcpg.libpg.util.WorldUtils;
import com.marcpg.tgc.TheGentleChallenges;
import com.marcpg.tgc.challenge.ChallengeManager;
import com.marcpg.tgc.util.Configuration;
import com.marcpg.tgc.util.Utilities;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.util.TriState;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class MABTeam {
    public static final Random RANDOM = new Random();

    public final UUID uuid;
    public final MonsterArmyBattle mab;
    public final List<MABPlayer> players = new ArrayList<>();

    public final @NotNull Triple<World, World, World> collectionWorlds;
    public final World battleWorld;

    public final BossBar bossBar = BossBar.bossBar(Component.text("Loading..."), 1.0f, color(1.0f), BossBar.Overlay.PROGRESS);
    public final Waves waves = new Waves();

    protected int wave = 0;
    protected int currentWaveTotal;
    protected ArrayList<EntitySnapshot> currentWaveEntities;
    protected Time finished;

    public MABTeam(MonsterArmyBattle mab, @NotNull Collection<Player> players, @Nullable Map<Material, Material> randomMaterialMap) {
        this.uuid = UUID.randomUUID();
        this.mab = mab;

        Map<Material, Material> randomMaterials = Configuration.MAB_ITEM_RANDOMIZATION == MonsterArmyBattle.RandomizerType.TEAM ? MonsterArmyBattle.randomShuffle(uuid) : randomMaterialMap;
        this.players.addAll(players.stream().map(p -> new MABPlayer(p, this, randomMaterials)).toList());

        this.collectionWorlds = Triple.of(
                loadWorld("base-collection", "mab-team-collection-" + uuid, false),
                loadWorld("base-collection-nether", "mab-team-collection-nether-" + uuid, false),
                loadWorld("base-collection-end", "mab-team-collection-end-" + uuid, false));
        battleWorld = loadWorld("base-battle", "mab-team-battle-" + uuid, true);

        if (!collectionWorlds.isFull() || battleWorld == null)
            throw new RuntimeException("Worlds could not be created.");

        collectionWorlds.all(o -> {
            World w = (World) o;
            w.setDifficulty(Difficulty.NORMAL);
            w.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        });

        this.battleWorld.setDifficulty(Difficulty.NORMAL);
        this.battleWorld.setGameRule(GameRule.DO_MOB_LOOT, false);
        this.battleWorld.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        this.battleWorld.setGameRule(GameRule.MOB_GRIEFING, false);
        this.battleWorld.setGameRule(GameRule.DO_FIRE_TICK, false);

        this.battleWorld.setTime(MinecraftTime.MIDNIGHT.time);
        this.battleWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        this.battleWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
    }

    public void addEntity(LivingEntity entity) {
        playerActions(p -> {
            p.sendMessage(Component.text("[KILL] ", NamedTextColor.GRAY).append(Component.text("1x ", NamedTextColor.GOLD)).append(entity.name().color(NamedTextColor.WHITE)));
            p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        });
    }

    public void transfer(@NotNull MonsterArmyBattle.Stage target) {
        switch (target) {
            case COLLECTION -> playerActions(p -> p.teleport(collectionWorlds.left().getSpawnLocation()));
            case CONFIGURATION -> {
                playerActions(p -> p.sendMessage(Component.text("Die Konfiguration startet...", NamedTextColor.DARK_GRAY)));
                players.forEach(MABPlayer::openConfiguration);
                playerActions(Utilities::reset);
            }
            case BATTLE -> {
                Bukkit.createWorld(WorldCreator.name("mab-team-battle-" + uuid));

                battleWorld.getLivingEntities().forEach(Entity::remove);

                playerActions(p -> {
                    Utilities.reset(p);
                    p.teleport(CordMinecraftAdapter.toLocation(Configuration.MAB_SPAWN, battleWorld));
                    p.showTitle(Title.title(Component.text("Das Battle beginnt!", NamedTextColor.RED), Component.text("Viel Glück!", NamedTextColor.GRAY)));
                    p.playSound(p, Sound.BLOCK_TRIAL_SPAWNER_SPAWN_MOB, 1.0f, 1.0f);
                });

                for (MABPlayer player : mab.nextTeam(this).players) {
                    waves.addFrom(player.waves);
                }

                waveDone();
                playerActions(bossBar::addViewer);

                Bukkit.getScheduler().runTaskTimer(TheGentleChallenges.PLUGIN, r -> {
                    if (finished == null) {
                        updateBossBar();
                        update();
                    } else {
                        r.cancel();
                    }
                }, 20, 20);

                Bukkit.getScheduler().runTaskTimer(TheGentleChallenges.PLUGIN, r -> {
                    if (finished == null) {
                        battleWorld.getLivingEntities().stream().filter(e -> !e.hasMetadata("battle") && e.getType() == EntityType.BAT).forEach(Entity::remove);
                    } else {
                        r.cancel();
                    }
                }, 20, 100);
            }
        }
    }

    public void mobDeath() {
        updateBossBar();
        update();
    }

    public void updateBossBar() {
        long mobsLeft = entitiesRemaining().count();
        float progress = (float) mobsLeft / currentWaveTotal;

        bossBar.name(Component.text(mobsLeft + " Mobs Übrig", TextColor.color(progress, Math.abs(progress - 1.0f), 0.0f)));
        bossBar.progress(Math.clamp(progress, 0.0f, 1.0f));
        bossBar.color(color(progress));

        if (progress <= 0.10f && mab.timer.timer().get() > currentWaveTotal / 1.8)
            entitiesRemaining().forEach(e -> e.setGlowing(true));
    }

    public void update() {
        Bukkit.getScheduler().runTaskLater(TheGentleChallenges.PLUGIN, () -> {
            if (entitiesRemaining().findAny().isEmpty() && currentWaveEntities.isEmpty())
                waveDone();
        }, 20);
    }

    public void waveDone() {
        wave++;
        if (wave >= 4) {
            done(mab.timer.timer());
        } else {
            players.forEach(mabP -> mabP.playerAction(p -> {
                if (wave <= 3) {
                    p.showTitle(Title.title(Component.text("Wave " + wave + " startet!", NamedTextColor.GREEN), Component.text("Viel Glück!", NamedTextColor.GRAY)));
                    p.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                }

                if (p.getGameMode() == GameMode.SPECTATOR) {
                    p.setGameMode(GameMode.SURVIVAL);
                    p.teleport(CordMinecraftAdapter.toLocation(Configuration.MAB_SPAWN, Objects.requireNonNull(battleWorld)));
                }
            }));

            currentWaveEntities = waves.entities(wave);
            currentWaveTotal = currentWaveEntities.size();

            if (currentWaveTotal == 0) {
                playerActions(p -> p.sendMessage(Component.text("Diese Wave hatte keine Monster!", NamedTextColor.YELLOW)));
                waveDone();
            } else if (currentWaveTotal == 1) {
                playerActions(p -> p.sendMessage(Component.text("Diese Wave hat nur ein Monster!", NamedTextColor.YELLOW)));

                spawn(currentWaveEntities.removeLast());
                entitiesRemaining().forEach(e -> e.setGlowing(true));
                updateBossBar();
            } else {
                Bukkit.getScheduler().runTaskTimer(TheGentleChallenges.PLUGIN, r -> {
                    if (currentWaveEntities.isEmpty()) {
                        r.cancel();
                        update();
                    } else {
                        spawn(currentWaveEntities.removeLast());
                        updateBossBar();
                    }
                }, 20, 10);
            }
        }
    }

    private BossBar.Color color(float progress) {
        if (progress > 0.75f) return BossBar.Color.PURPLE;
        if (progress > 0.50f) return BossBar.Color.RED;
        if (progress > 0.25f) return BossBar.Color.YELLOW;
        return BossBar.Color.GREEN;
    }

    public void done(Time time) {
        if (finished != null) return;

        if (mab.teams.values().stream().allMatch(t -> t.finished == null)) {
            ChallengeManager.PROPERTIES.put("last-winner", GsonComponentSerializer.gson().serialize(Component.text(String.join(" & ", players.stream().map(p -> Bukkit.getOfflinePlayer(p.uuid).getName()).toList()), NamedTextColor.GREEN)));
            ChallengeManager.PROPERTIES.put("last-winner-time", time.get());
        }

        finished = new Time(time);
        playerActions(p -> {
            p.setGameMode(GameMode.SPECTATOR);
            p.showTitle(Title.title(Component.text("Gut gemacht!", NamedTextColor.GREEN), Component.text("Ihr habt das Battle überlebt.", NamedTextColor.GRAY)));
            p.sendMessage(Component.text("Ihr habt ", NamedTextColor.YELLOW).append(Component.text(finished.getPreciselyFormatted(), NamedTextColor.WHITE)).append(Component.text(" gebraucht.", NamedTextColor.YELLOW)));
            bossBar.removeViewer(p);
        });

        if (mab.teams.values().stream().allMatch(t -> t.finished != null))
            mab.end(true);
    }

    public void playerActions(Consumer<Player> action) {
        for (MABPlayer player : players)
            player.playerAction(action);
    }

    public Location randomSpawnLocation(@NotNull World world) {
        int x, y, z;
        do {
            x = Configuration.MAB_SPAWN_AREA.getBounds().x + RANDOM.nextInt(Configuration.MAB_SPAWN_AREA.getBounds().width);
            z = Configuration.MAB_SPAWN_AREA.getBounds().y + RANDOM.nextInt(Configuration.MAB_SPAWN_AREA.getBounds().height);
            y = world.getHighestBlockYAt(x, z);
        } while (!Configuration.MAB_SPAWN_AREA.contains(x, z) || y >= 75 || y <= 50 || Configuration.MAB_SPAWN.distance(new Cord(x, y, z)) < 10);
        return new Location(battleWorld, x, y + 1.5, z);
    }

    private void spawn(@NotNull EntitySnapshot snapshot) {
        Entity e = snapshot.createEntity(battleWorld);
        if (!(e instanceof LivingEntity livingEntity)) return;

        livingEntity.setMetadata("battle", new FixedMetadataValue(TheGentleChallenges.PLUGIN, true));
        MonsterArmyBattle.GLOW_TEAM.addEntity(livingEntity);

        if (livingEntity instanceof Zombie zombie)
            zombie.setShouldBurnInDay(false);
        if (livingEntity instanceof AbstractSkeleton skeleton)
            skeleton.setShouldBurnInDay(false);
        if (livingEntity instanceof Phantom phantom)
            phantom.setShouldBurnInDay(false);

        livingEntity.setRemoveWhenFarAway(false);

        e.spawnAt(randomSpawnLocation(battleWorld));
    }

    private Stream<LivingEntity> entitiesRemaining() {
        return battleWorld.getLivingEntities().stream().filter(e -> e.hasMetadata("battle"));
    }

    private World loadWorld(String base, String target, boolean load) {
        try {
            WorldUtils.copy(base, target);
        } catch (IOException e) {
            TheGentleChallenges.LOG.error("Failed to copy world.", e);
        }
        if (load) {
            return Bukkit.createWorld(WorldCreator.name(target));
        } else {
            return Bukkit.createWorld(WorldCreator.name(target).keepSpawnLoaded(TriState.FALSE));
        }
    }
}
