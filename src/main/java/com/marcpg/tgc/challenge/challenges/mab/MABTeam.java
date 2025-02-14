package com.marcpg.tgc.challenge.challenges.mab;

import com.marcpg.libpg.data.time.Time;
import com.marcpg.libpg.storing.Cord;
import com.marcpg.libpg.storing.CordMinecraftAdapter;
import com.marcpg.libpg.storing.tuple.triple.Triple;
import com.marcpg.libpg.util.FileUtils;
import com.marcpg.tgc.Configuration;
import com.marcpg.tgc.TheGentleChallenges;
import com.marcpg.tgc.challenge.ChallengeManager;
import com.marcpg.tgc.util.Utilities;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntitySnapshot;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

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

    public MABTeam(MonsterArmyBattle mab, @NotNull Triple<World, World, World> baseCollectionWorlds, @NotNull Collection<Player> players, @Nullable Map<Material, Material> randomMaterialMap) {
        this.uuid = UUID.randomUUID();
        this.mab = mab;

        Map<Material, Material> randomMaterials = Configuration.MAB_ITEM_RANDOMIZATION == MonsterArmyBattle.RandomizerType.TEAM ? MonsterArmyBattle.randomShuffle(uuid) : randomMaterialMap;
        this.players.addAll(players.stream().map(p -> new MABPlayer(p, this, randomMaterials)).toList());

        this.collectionWorlds = Triple.of(
                Bukkit.createWorld(WorldCreator.name("mab-team-collection-" + uuid).copy(baseCollectionWorlds.left())),
                Bukkit.createWorld(WorldCreator.name("mab-team-collection-nether-" + uuid).copy(baseCollectionWorlds.middle())),
                Bukkit.createWorld(WorldCreator.name("mab-team-collection-end-" + uuid).copy(baseCollectionWorlds.right())));

        try {
            Path world = Bukkit.getWorldContainer().toPath().resolve("mab-team-battle-" + uuid);
            FileUtils.moveRecursively(Bukkit.getWorldContainer().toPath().resolve("base-battle"), world);
            Files.deleteIfExists(world.resolve("uid.dat"));
            Files.deleteIfExists(world.resolve("session.lock"));
        } catch (Exception e) {
            TheGentleChallenges.LOG.error("Could not copy battle world!", e);
        }

        this.battleWorld = Bukkit.createWorld(WorldCreator.name("mab-team-battle-" + uuid));

        if (battleWorld == null || !collectionWorlds.isFull()) throw new RuntimeException("Worlds could not be created.");
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
            }
        }
    }

    public void mobDeath() {
        updateBossBar();
        update();
    }

    public void updateBossBar() {
        long mobsLeft = battleWorld.getLivingEntities().stream().filter(e -> e.hasMetadata("battle")).count();
        float progress = (float) mobsLeft / currentWaveTotal;

        bossBar.name(Component.text(mobsLeft + " Mobs Übrig", TextColor.color(progress, Math.abs(progress - 1.0f), 0.0f)));
        bossBar.progress(progress);
        bossBar.color(color(progress));
    }

    public void update() {
        long mobsLeft = battleWorld.getLivingEntities().stream().filter(e -> e.hasMetadata("battle")).count();
        if (mobsLeft <= 0 && currentWaveEntities.isEmpty())
            waveDone();
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

            int oldWave = wave;

            currentWaveEntities = waves.entities(wave);
            currentWaveTotal = currentWaveEntities.size();
            Bukkit.getScheduler().runTaskTimer(TheGentleChallenges.PLUGIN, r -> {
                if (currentWaveEntities.isEmpty()) {
                    Bukkit.getScheduler().runTaskLater(TheGentleChallenges.PLUGIN, () -> {
                        if (wave == oldWave) {
                            battleWorld.getLivingEntities().stream().filter(e -> e.hasMetadata("battle"))
                                    .forEach(e -> e.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 1200, 0)));
                        }
                    }, 1200); // 1 Minute
                    r.cancel();
                } else {
                    Entity e = currentWaveEntities.removeLast().createEntity(battleWorld);
                    e.setMetadata("battle", new FixedMetadataValue(TheGentleChallenges.PLUGIN, true));
                    e.spawnAt(randomSpawnLocation(battleWorld));

                    updateBossBar();
                }
            }, 20, 10);
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

        if (mab.teams.values().stream().allMatch(t -> t.finished == null))
            ChallengeManager.PROPERTIES.put("last-winner", GsonComponentSerializer.gson().serialize(Component.text(String.join(" & ", players.stream().map(p -> Bukkit.getOfflinePlayer(p.uuid).getName()).toList()), NamedTextColor.GREEN)));

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
}
