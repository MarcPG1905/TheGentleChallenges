package com.marcpg.tgc.util;

import com.marcpg.libpg.storing.Cord;
import com.marcpg.tgc.challenge.challenges.HideNSeek;
import com.marcpg.tgc.challenge.challenges.RandomWorldChallenge;
import com.marcpg.tgc.challenge.challenges.mab.MonsterArmyBattle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.List;
import java.util.Objects;

public class Configuration {
    public static FileConfiguration CONFIG;
    
    public static boolean MAB_TEAM_RANDOMIZATION;
    public static MonsterArmyBattle.RandomizerType MAB_ITEM_RANDOMIZATION;
    public static Cord MAB_SPAWN;
    public static Polygon MAB_SPAWN_AREA;

    public static int RW_INTERVAL_MIN;
    public static int RW_INTERVAL_MAX;
    public static boolean RW_CLEAR_INV;
    public static List<RandomWorldChallenge.RandomWorld> RW_RANDOM_WORLDS;

    public static String HNS_SEEKER;
    public static Cord HNS_SPAWN;
    public static int HNS_HIDING_TIME;
    public static int HNS_TOTAL_TIME;
    public static int HNS_HINT_TIME;
    public static int HNS_HINT_INTERVAL;
    public static HideNSeek.HintType HNS_HINT_TYPE;

    @SuppressWarnings("unchecked")
    public static void init(@NotNull JavaPlugin plugin) {
        plugin.saveDefaultConfig();
        CONFIG = plugin.getConfig();

        MAB_TEAM_RANDOMIZATION = CONFIG.getBoolean("random-teams");
        MAB_ITEM_RANDOMIZATION = MonsterArmyBattle.RandomizerType.valueOf(CONFIG.getString("monster-army-battle.randomizer-mode", "team").toUpperCase());
        MAB_SPAWN = Cord.ofList(CONFIG.getIntegerList("monster-army-battle.arena-spawn"));

        MAB_SPAWN_AREA = new Polygon();
        for (java.util.List<Integer> cord : (List<List<Integer>>) Objects.requireNonNull(CONFIG.getList("monster-army-battle.arena-bounds"))) {
            MAB_SPAWN_AREA.addPoint(cord.getFirst(), cord.getLast());
        }

        RW_INTERVAL_MIN = CONFIG.getInt("random-worlds.interval-min");
        RW_INTERVAL_MAX = CONFIG.getInt("random-worlds.interval-max");
        RW_CLEAR_INV = CONFIG.getBoolean("random-worlds.clear-inv");
        RW_RANDOM_WORLDS = CONFIG.getMapList("random-worlds.worlds").stream().map(RandomWorldChallenge.RandomWorld::of).toList();

        HNS_SEEKER = CONFIG.getString("hide-n-seek.seeker");
        HNS_SPAWN = Cord.ofList(CONFIG.getIntegerList("hide-n-seek.spawn"));
        HNS_HIDING_TIME = CONFIG.getInt("hide-n-seek.hiding-time");
        HNS_TOTAL_TIME = CONFIG.getInt("hide-n-seek.total-time");
        HNS_HINT_TIME = CONFIG.getInt("hide-n-seek.hint-time");
        HNS_HINT_INTERVAL = CONFIG.getInt("hide-n-seek.hint-interval");
        HNS_HINT_TYPE = HideNSeek.HintType.valueOf(CONFIG.getString("hide-n-seek.hint-type", "glow").toUpperCase());
    }
}
