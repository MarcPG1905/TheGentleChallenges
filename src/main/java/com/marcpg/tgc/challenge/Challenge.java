package com.marcpg.tgc.challenge;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import com.destroystokyo.paper.event.server.ServerTickStartEvent;
import com.marcpg.tgc.TheGentleChallenges;
import com.marcpg.tgc.util.Timer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EnderDragonChangePhaseEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

@SuppressWarnings("EmptyMethod")
public abstract class Challenge implements Listener {
    public final Timer timer = new Timer(this);
    public boolean running = false;
    public boolean beaten = false;

    @EventHandler(ignoreCancelled = true)
    public void onPlayerPostRespawn(@NotNull PlayerPostRespawnEvent event) {
        event.getPlayer().setGameMode(GameMode.SPECTATOR);
    }

    @EventHandler
    public void onServerTickStart(ServerTickStartEvent event) {
        tick();
    }

    @EventHandler(ignoreCancelled = true)
    public void onEnderDragonChangePhaseEvent(@NotNull EnderDragonChangePhaseEvent event) {
        if (event.getNewPhase() == EnderDragon.Phase.DYING && running) end(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (running) running = false;
    }

    public Challenge() {
        initLogic();
        forEachPlayer(p -> p.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f));
        Bukkit.getServer().showTitle(Title.title(name(), Component.text("Challenge startet jetzt!")));

        running = true;
        ChallengeManager.CURRENT_CHALLENGE = this;
        Bukkit.getServer().getPluginManager().registerEvents(this, TheGentleChallenges.PLUGIN);
    }

    // ++++++++++++++++++++++++++++++
    // ++++++ Abstract Methods ++++++
    // ++++++++++++++++++++++++++++++
    public abstract Component name();
    public void initLogic() {}
    public void endLogic() {}
    public void playerTick(@SuppressWarnings("unused") Player player) {}
    public void customTick() {}
    public void customSecondTick() {}

    // ++++++++++++++++++++++++++++++
    // +++++++++ Game Flow ++++++++++
    // ++++++++++++++++++++++++++++++
    public final void end(boolean beaten) {
        this.running = false;

        if (beaten) {
            this.beaten = true;
            return;
        }

        endLogic();
        forEachPlayer(p -> p.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f));
        Bukkit.getServer().showTitle(Title.title(name(), Component.text("Challenge ist beendet!")));

        ChallengeManager.CURRENT_CHALLENGE = null;
        HandlerList.unregisterAll(this);
    }

    public final void tick() {
        timer.tick();
        customTick();
    }

    // ++++++++++++++++++++++++++++++
    // ++++++++++ Utility +++++++++++
    // ++++++++++++++++++++++++++++++
    public static void forEachPlayer(Consumer<Player> action) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() != GameMode.SPECTATOR)
                action.accept(player);
        }
    }
}
