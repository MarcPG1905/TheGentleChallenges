package com.marcpg.tgc.challenge;

import com.marcpg.tgc.challenge.challenges.AggressiveMobsChallenge;
import com.marcpg.tgc.challenge.challenges.CustomChallenge;
import com.marcpg.tgc.challenge.challenges.DeadlyFoodChallenge;
import com.marcpg.tgc.challenge.challenges.ZeroHeartsChallenge;
import com.marcpg.tgc.challenge.challenges.mab.MonsterArmyBattle;

import java.util.HashMap;
import java.util.Map;

public final class ChallengeManager {
    public static final Map<String, Class<? extends Challenge>> AVAILABLE_CHALLENGES = Map.of(
            "aggressive-mobs", AggressiveMobsChallenge.class,
            "custom", CustomChallenge.class,
            "deadly-food", DeadlyFoodChallenge.class,
            "monster-army-battle", MonsterArmyBattle.class,
            "zero-hearts", ZeroHeartsChallenge.class);

    public static Challenge CURRENT_CHALLENGE;
    public static Map<String, Object> PROPERTIES = new HashMap<>();

    public static boolean running() {
        return CURRENT_CHALLENGE != null && CURRENT_CHALLENGE.running;
    }
}
