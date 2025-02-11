package com.marcpg.tgc.challenge.challenges;

import com.destroystokyo.paper.entity.ai.MobGoals;
import com.destroystokyo.paper.entity.ai.PaperVanillaGoal;
import com.destroystokyo.paper.entity.ai.VanillaGoal;
import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.marcpg.tgc.TheGentleChallenges;
import com.marcpg.tgc.challenge.Challenge;
import com.marcpg.tgc.challenge.ChallengeManager;
import com.marcpg.tgc.util.Utilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class AggressiveMobsChallenge extends Challenge {
    @EventHandler(ignoreCancelled = true)
    public void onEntityAddToWorld(@NotNull EntityAddToWorldEvent event) {
        if (ChallengeManager.CURRENT_CHALLENGE == this)
            makeAggressive(event.getEntity());
    }

    public void makeAggressive(Entity entity) {
        if (!(entity instanceof Creature mob)) return;

        try {
            MobGoals goals = Bukkit.getMobGoals();
            net.minecraft.world.entity.Entity nmsEntity = (net.minecraft.world.entity.Entity) mob.getClass().getMethod("getHandle").invoke(mob);
            if (!(nmsEntity instanceof PathfinderMob handle)) return;

            if (!goals.hasGoal(mob, VanillaGoal.MELEE_ATTACK) || goals.hasGoal(mob, VanillaGoal.PANIC) || !goals.hasGoal(mob, VanillaGoal.NEAREST_ATTACKABLE)) return;

            if (mob.getAttribute(Attribute.ATTACK_DAMAGE) == null) {
                mob.registerAttribute(Attribute.ATTACK_DAMAGE);
                Objects.requireNonNull(mob.getAttribute(Attribute.ATTACK_DAMAGE)).setBaseValue(Utilities.damage(mob.getType().name().toLowerCase()));
            }

            goals.addGoal(mob, 1, new PaperVanillaGoal<>(new MeleeAttackGoal(handle, 1.0, false)));
            goals.removeGoal(mob, VanillaGoal.PANIC);
            goals.addGoal(mob, 3, new PaperVanillaGoal<>(new NearestAttackableTargetGoal<>(handle, Player.class, true)));
        } catch (Exception e) {
            TheGentleChallenges.LOG.warn("Mob AI konnte nicht geändert werden!", e);
        }
    }

    @Override
    public Component name() {
        return MiniMessage.miniMessage().deserialize("<bold><gradient:#AA0000:#FF0000>Aggressive Mobs</gradient></bold>");
    }

    @Override
    public void initLogic() {
        Bukkit.getScheduler().runTaskTimer(TheGentleChallenges.PLUGIN, r -> {
            if (running) {
                Bukkit.getWorlds().forEach(w -> w.getEntities().forEach(this::makeAggressive));
            } else {
                r.cancel();
            }
        }, 1, 20);
    }
}
