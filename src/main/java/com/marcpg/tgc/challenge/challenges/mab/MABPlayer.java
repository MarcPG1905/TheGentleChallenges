package com.marcpg.tgc.challenge.challenges.mab;

import com.marcpg.libpg.util.ItemBuilder;
import com.marcpg.tgc.TheGentleChallenges;
import com.marcpg.tgc.util.Items;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntitySnapshot;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.ScrollGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.window.Window;

import java.util.*;
import java.util.function.Consumer;

@SuppressWarnings("UnstableApiUsage")
public class MABPlayer {
    public final UUID uuid;
    public final MABTeam team;
    public final Map<Material, Material> randomMaterialMap;

    public final Map<EntityType, List<EntitySnapshot>> entities = new HashMap<>();
    public final Waves waves = new Waves();

    public boolean configured;

    public MABPlayer(@NotNull Player player, MABTeam team, @Nullable Map<Material, Material> randomMaterialMap) {
        this(player.getUniqueId(), team, randomMaterialMap);
    }

    public MABPlayer(@NotNull UUID uuid, MABTeam team, @Nullable Map<Material, Material> randomMaterialMap) {
        this.uuid = uuid;
        this.team = team;
        this.randomMaterialMap = randomMaterialMap == null ? MonsterArmyBattle.randomShuffle(uuid) : randomMaterialMap;
    }

    public void kill(@NotNull LivingEntity entity) {
        if (entity.getType() == EntityType.CREAKING || entity.getType() == EntityType.ENDER_DRAGON) return;

        entity.setHealth(Objects.requireNonNull(entity.getAttribute(Attribute.MAX_HEALTH)).getBaseValue());
        if (entities.containsKey(entity.getType())) {
            entities.get(entity.getType()).add(entity.createSnapshot());
        } else {
            entities.put(entity.getType(), new ArrayList<>(List.of(Objects.requireNonNull(entity.createSnapshot()))));
        }
        entity.setHealth(0.0);
        team.addEntity(entity);
    }

    public void playerAction(Consumer<Player> action) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) action.accept(player);
    }

    public void openConfiguration() {
        Bukkit.getScheduler().runTask(TheGentleChallenges.PLUGIN, () -> playerAction(player -> Window.single()
                .setTitle("Wähle Wave zum Konfigurieren")
                .addCloseHandler(() -> Bukkit.getScheduler().runTaskLater(TheGentleChallenges.PLUGIN, () -> {
                    if (player.getOpenInventory().getType() == InventoryType.CHEST) return;

                    configured = true;
                    if (team.mab.players.values().stream().allMatch(mabPlayer -> mabPlayer.configured))
                        team.mab.startBattle();
                }, 5))
                .setGui(Gui.normal().setStructure(
                                ". . . . . . . . .",
                                ". . 1 . 2 . 3 . .",
                                ". . . . D . . . .")
                        .setBackground(Items.BLACK_BACKGROUND)
                        .addIngredient('1', new Items.ClickItem(new ItemBuilder(Material.DIAMOND_SWORD).name(Component.text("Wave 1", NamedTextColor.YELLOW)).lore(lore(waves.wave1())).build(), (p, e) -> openConfiguration(waves.wave1(), 1, p)))
                        .addIngredient('2', new Items.ClickItem(new ItemBuilder(Material.DIAMOND_SWORD).name(Component.text("Wave 2", NamedTextColor.GOLD)).lore(lore(waves.wave2())).build(), (p, e) -> openConfiguration(waves.wave2(), 2, p)))
                        .addIngredient('3', new Items.ClickItem(new ItemBuilder(Material.DIAMOND_SWORD).name(Component.text("Wave 3", NamedTextColor.RED)).lore(lore(waves.wave3())).build(), (p, e) -> openConfiguration(waves.wave3(), 3, p)))
                        .addIngredient('D', new Items.ClickItem(new ItemBuilder(Material.LIME_STAINED_GLASS_PANE).name(Component.text("Fertig!", NamedTextColor.GREEN)).build(), (p, e) -> Bukkit.getScheduler().runTask(TheGentleChallenges.PLUGIN, () -> player.closeInventory())))
                        .build())
                .open(player)));
    }

    public void openConfiguration(@NotNull Map<EntityType, ArrayList<EntitySnapshot>> wave, int waveNumber, Player player) {
        Bukkit.getScheduler().runTask(TheGentleChallenges.PLUGIN, () -> Window.single()
                .setTitle("Konfiguriere Wave " + waveNumber)
                .addCloseHandler(this::openConfiguration)
                .setGui(ScrollGui.items().setStructure(
                                "B # # # # # # # #",
                                "* * * * * * * * ^",
                                "* * * * * * * * #",
                                "* * * * * * * * #",
                                "* * * * * * * * #",
                                "* * * * * * * * v")
                        .addIngredient('#', Items.BLACK_BACKGROUND)
                        .addIngredient('B', new Items.ClickItem(new ItemBuilder(Material.RED_STAINED_GLASS_PANE).name(Component.text("Zurück", NamedTextColor.RED)).build(), (p, e) -> Bukkit.getScheduler().runTask(TheGentleChallenges.PLUGIN, () -> player.closeInventory())))
                        .addIngredient('^', new Items.ScrollItem(true))
                        .addIngredient('v', new Items.ScrollItem(false))
                        .addIngredient('*', Markers.CONTENT_LIST_SLOT_VERTICAL)
                        .setContent(entities.entrySet().parallelStream().map(entry -> {
                            if (!wave.containsKey(entry.getKey())) {
                                wave.put(entry.getKey(), new ArrayList<>());
                            }
                            return (Item) new Items.EntityItem(entities.get(entry.getKey()), entry.getKey(), wave.get(entry.getKey()));
                        }).toList())
                        .build())
                .open(player));
    }

    private List<Component> lore(@NotNull Map<EntityType, ArrayList<EntitySnapshot>> wave) {
        return wave.entrySet().stream()
                .filter(e -> !e.getValue().isEmpty())
                .map(e -> (Component) Component.text(e.getValue().size() + "x ", NamedTextColor.YELLOW).append(entityName(e.getKey())))
                .toList();
    }

    public static @NotNull Component entityName(@NotNull EntityType entity) {
        return Component.translatable(entity.translationKey(), NamedTextColor.WHITE);
    }
}
