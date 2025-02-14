package com.marcpg.tgc.util;

import com.marcpg.libpg.storing.Cord;
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
    
    @SuppressWarnings("unchecked")
    public static void init(@NotNull JavaPlugin plugin) {
        plugin.saveDefaultConfig();
        CONFIG = plugin.getConfig();

        MAB_TEAM_RANDOMIZATION = CONFIG.getBoolean("random-teams");
        MAB_ITEM_RANDOMIZATION = MonsterArmyBattle.RandomizerType.valueOf(Objects.requireNonNullElse(CONFIG.getString("monster-army-battle.randomizer-mode"), "team").toUpperCase());
        MAB_SPAWN = Cord.ofList(CONFIG.getIntegerList("monster-army-battle.arena-spawn"));

        MAB_SPAWN_AREA = new Polygon();
        for (java.util.List<Integer> cord : (List<List<Integer>>) Objects.requireNonNull(CONFIG.getList("monster-army-battle.arena-bounds"))) {
            MAB_SPAWN_AREA.addPoint(cord.getFirst(), cord.getLast());
        }
    }
}
