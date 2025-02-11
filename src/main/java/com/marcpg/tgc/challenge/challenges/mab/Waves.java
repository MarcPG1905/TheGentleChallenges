package com.marcpg.tgc.challenge.challenges.mab;

import org.bukkit.entity.EntitySnapshot;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record Waves(Map<EntityType, ArrayList<EntitySnapshot>> wave1, Map<EntityType, ArrayList<EntitySnapshot>> wave2, Map<EntityType, ArrayList<EntitySnapshot>> wave3) {
    public Waves() {
        this(new HashMap<>(), new HashMap<>(), new HashMap<>());
    }

    public @NotNull ArrayList<EntitySnapshot> entitiesWave1() {
        return new ArrayList<>(wave1.values().stream().flatMap(List::stream).toList());
    }

    public @NotNull ArrayList<EntitySnapshot> entitiesWave2() {
        return new ArrayList<>(wave2.values().stream().flatMap(List::stream).toList());
    }

    public @NotNull ArrayList<EntitySnapshot> entitiesWave3() {
        return new ArrayList<>(wave3.values().stream().flatMap(List::stream).toList());
    }

    public ArrayList<EntitySnapshot> entities(int wave) {
        return switch (wave) {
            case 1 -> entitiesWave1();
            case 2 -> entitiesWave2();
            case 3 -> entitiesWave3();
            default -> new ArrayList<>();
        };
    }

    public void addFrom(@NotNull Waves waves) {
        waves.wave1.forEach((t, l) -> {
            if (wave1.containsKey(t)) {
                wave1.get(t).addAll(l);
            } else {
                wave1.put(t, l);
            }
        });
        waves.wave2.forEach((t, l) -> {
            if (wave2.containsKey(t)) {
                wave2.get(t).addAll(l);
            } else {
                wave2.put(t, l);
            }
        });
        waves.wave3.forEach((t, l) -> {
            if (wave3.containsKey(t)) {
                wave3.get(t).addAll(l);
            } else {
                wave3.put(t, l);
            }
        });
    }
}
