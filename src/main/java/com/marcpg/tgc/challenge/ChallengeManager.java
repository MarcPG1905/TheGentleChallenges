package com.marcpg.tgc.challenge;

import com.marcpg.tgc.challenge.challenges.*;
import com.marcpg.tgc.challenge.challenges.mab.MonsterArmyBattle;

import java.util.HashMap;
import java.util.Map;

public final class ChallengeManager {
    public static final Map<String, Class<? extends Challenge>> AVAILABLE_CHALLENGES = Map.of(
            "fishing", FishingChallenge.class,
            "aggressive-mobs", AggressiveMobsChallenge.class,
            "custom", CustomChallenge.class,
            "deadly-food", DeadlyFoodChallenge.class,
            "monster-army-battle", MonsterArmyBattle.class,
            "zero-hearts", ZeroHeartsChallenge.class);

    public static Challenge CURRENT_CHALLENGE;
    public static final Map<String, Object> PROPERTIES = new HashMap<>();

    public static boolean running() {
        return CURRENT_CHALLENGE != null && CURRENT_CHALLENGE.running;
    }
}
