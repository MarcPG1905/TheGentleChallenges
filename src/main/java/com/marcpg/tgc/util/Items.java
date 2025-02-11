package com.marcpg.tgc.util;

import com.marcpg.libpg.util.ItemBuilder;
import com.marcpg.tgc.challenge.challenges.mab.MABPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EntitySnapshot;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.inventoryaccess.component.AdventureComponentWrapper;
import xyz.xenondevs.invui.gui.ScrollGui;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.ItemWrapper;
import xyz.xenondevs.invui.item.builder.SkullBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.util.MojangApiUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class Items {
    public static final ItemStack BLACK_BACKGROUND = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(Component.empty()).build();

    public static class ClickItem extends AbstractItem {
        protected final ItemStack item;
        protected final BiConsumer<Player, InventoryClickEvent> clickConsumer;

        public ClickItem(ItemStack item, BiConsumer<Player, InventoryClickEvent> clickConsumer) {
            this.item = item;
            this.clickConsumer = clickConsumer;
        }

        @Override
        public ItemProvider getItemProvider() {
            return new ItemWrapper(item);
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            clickConsumer.accept(player, event);
        }
    }

    public static class EntityItem extends AbstractItem {
        public static final List<Component> KEY_BINDS = List.of(
                Component.empty(),
                Component.text("[LMB] 1x Hinzufügen", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("[RMB] Entfernen", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("+ [SHIFT] Alle anstatt nur 1x", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
        );

        protected final List<EntitySnapshot> entitiesAvailable;
        protected final List<EntitySnapshot> entities;
        protected final SimpleItemProvider itemProvider;

        public EntityItem(List<EntitySnapshot> entitiesAvailable, EntityType type, List<EntitySnapshot> entities) {
            this.entitiesAvailable = entitiesAvailable;
            this.entities = entities;
            this.itemProvider = () -> new ItemBuilder(Material.valueOf(type.name() +"_SPAWN_EGG"))
                    .name(MABPlayer.entityName(type))
                    .lore(lore(entities.size(), entitiesAvailable.size()))
                    .amount(Math.clamp(entities.size(), 1, 64))
                    .build();
        }

        @Override
        public ItemProvider getItemProvider() {
            return itemProvider;
        }

        private static @NotNull List<Component> lore(int used, int available) {
            List<Component> lore = new ArrayList<>();

            int totalEntities = used + available;
            lore.add(Component.text("Eingesetzt: ", NamedTextColor.WHITE).append(Component.text(used,
                    TextColor.color((float) used / totalEntities, Math.abs((float) used / totalEntities - 1.0f), 0.0f))));
            lore.add(Component.text("Verfügbar: ", NamedTextColor.WHITE).append(Component.text(available,
                    TextColor.color((float) available / totalEntities, Math.abs((float) available / totalEntities - 1.0f), 0.0f))));

            lore.addAll(KEY_BINDS);
            return lore;
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            if ((clickType.isLeftClick() && entitiesAvailable.isEmpty()) || (clickType.isRightClick() && entities.isEmpty())) {
                player.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }

            switch (clickType) {
                case LEFT -> entities.add(entitiesAvailable.removeLast());
                case RIGHT -> entitiesAvailable.add(entities.removeLast());
                case SHIFT_LEFT -> {
                    entities.addAll(entitiesAvailable);
                    entitiesAvailable.clear();
                }
                case SHIFT_RIGHT -> {
                    entitiesAvailable.addAll(entities);
                    entities.clear();
                }
            }

            notifyWindows();
        }
    }

    @FunctionalInterface
    public interface SimpleItemProvider extends ItemProvider {
        @NotNull ItemStack get();
        default @NotNull ItemStack get(String lang) { return get(); }
    }
    
    public static class ScrollItem extends xyz.xenondevs.invui.item.impl.controlitem.ScrollItem {
        protected final boolean up;

        public ScrollItem(boolean up) {
            super(up ? -1 : 1);
            this.up = up;
        }

        @Override
        public ItemProvider getItemProvider(ScrollGui<?> gui) {
            try {
                return new SkullBuilder("MHF_Arrow" + (up ? "Up" : "Down")).setDisplayName(new AdventureComponentWrapper(Component.text("Scroll " + (up ? "Up" : "Down"))));
            } catch (MojangApiUtils.MojangApiException | IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
