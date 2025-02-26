package com.marcpg.tgc;

import com.marcpg.libpg.MinecraftLibPG;
import com.marcpg.libpg.storage.JsonUtils;
import com.marcpg.tgc.challenge.ChallengeManager;
import com.marcpg.tgc.util.Configuration;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Material;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import xyz.xenondevs.invui.InvUI;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
public final class TheGentleChallenges extends JavaPlugin implements Listener {
    public static TheGentleChallenges PLUGIN;
    public static Logger LOG;
    public static Path DATA_DIR;

    @Override
    public void onEnable() {
        MinecraftLibPG.init(this);
        Configuration.init(this);
        InvUI.getInstance().setPlugin(this);

        PLUGIN = this;
        LOG = getSLF4JLogger();
        DATA_DIR = getDataPath();

        // noinspection deprecation - Initialize legacy material support just in case.
        new ItemStack(Material.LEGACY_AIR);

        getServer().getPluginManager().registerEvents(this, this);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register(Commands.challengeCommand(), "Manage the different challenges.", List.of("tgc"));
            event.registrar().register(Commands.resourcePackCommand(), "Manage the applied texture/resource pack.", List.of("texture-pack"));
        });

        ChallengeManager.PROPERTIES.putAll(JsonUtils.loadSafe(DATA_DIR.resolve("properties.json").toFile(), Map.of(), Map.class));
    }

    @Override
    public void onDisable() {
        if (ChallengeManager.CURRENT_CHALLENGE != null)
            ChallengeManager.PROPERTIES.put("last-time", ChallengeManager.CURRENT_CHALLENGE.timer.timer().get());

        JsonUtils.saveSafe(ChallengeManager.PROPERTIES, DATA_DIR.resolve("properties.json").toFile());
    }
}
