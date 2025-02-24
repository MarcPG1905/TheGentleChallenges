package com.marcpg.tgc;

import com.marcpg.libpg.data.time.Time;
import com.marcpg.tgc.challenge.Challenge;
import com.marcpg.tgc.challenge.ChallengeManager;
import com.marcpg.tgc.challenge.challenges.RandomWorldChallenge;
import com.marcpg.tgc.challenge.challenges.ZeroHeartsChallenge;
import com.marcpg.tgc.challenge.challenges.mab.MABPlayer;
import com.marcpg.tgc.challenge.challenges.mab.MonsterArmyBattle;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.FinePositionResolver;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.math.FinePosition;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;

@SuppressWarnings("UnstableApiUsage")
public final class Commands {
    public static final List<String> DIMENSIONS = List.of("battle", "overworld", "nether", "end");

    public static LiteralCommandNode<CommandSourceStack> challengeCommand() {
        return LiteralArgumentBuilder.<CommandSourceStack>literal("challenge")
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("start")
                        .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("type", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    ChallengeManager.AVAILABLE_CHALLENGES.keySet().forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    String challengeName = context.getArgument("type", String.class);
                                    Class<? extends Challenge> challenge = ChallengeManager.AVAILABLE_CHALLENGES.get(challengeName);

                                    if (challenge == null) {
                                        context.getSource().getSender().sendMessage(Component.text("Die challenge " + challengeName + " existiert nicht!", NamedTextColor.RED));
                                    } else {
                                        try {
                                            context.getSource().getSender().sendMessage(Component.text("Die challenge " + challengeName + " startet...", NamedTextColor.GRAY));
                                            challenge.getConstructor().newInstance();
                                            context.getSource().getSender().sendMessage(Component.text("Die challenge " + challengeName + " wurde gestartet!", NamedTextColor.GREEN));
                                        } catch (ReflectiveOperationException e) {
                                            context.getSource().getSender().sendMessage(Component.text("Die challenge " + challengeName + " konnte nicht gestartet werden!", NamedTextColor.RED));
                                            TheGentleChallenges.LOG.error("Could not start challenge {}", challengeName, e);
                                        }
                                    }
                                    return 1;
                                })
                        )
                )
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("reset")
                        .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("type", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    ChallengeManager.AVAILABLE_CHALLENGES.keySet().forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    String challengeName = context.getArgument("type", String.class);
                                    Class<? extends Challenge> challenge = ChallengeManager.AVAILABLE_CHALLENGES.get(challengeName);

                                    if (challenge == null) {
                                        context.getSource().getSender().sendMessage(Component.text("Die challenge " + challengeName + " existiert nicht!", NamedTextColor.RED));
                                    } else {
                                        try {
                                            challenge.getMethod("reset").invoke(null);
                                            context.getSource().getSender().sendMessage(Component.text("Die challenge " + challengeName + " wurde reset!", NamedTextColor.GREEN));
                                        } catch (ReflectiveOperationException e) {
                                            context.getSource().getSender().sendMessage(Component.text("Die challenge " + challengeName + " kann nicht reset werden!", NamedTextColor.RED));
                                        }
                                    }
                                    return 1;
                                })
                        )
                )
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("mab")
                        .then(LiteralArgumentBuilder.<CommandSourceStack>literal("remove-player")
                                .requires(source -> source.getSender().isOp())
                                .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("player", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            if (ChallengeManager.CURRENT_CHALLENGE instanceof MonsterArmyBattle mab)
                                                mab.players.forEach((uuid, p) -> builder.suggest(Bukkit.getOfflinePlayer(uuid).getName()));
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> {
                                            if (!(ChallengeManager.CURRENT_CHALLENGE instanceof MonsterArmyBattle mab)) {
                                                context.getSource().getSender().sendMessage(Component.text("Es läuft gerade kein Monster-Army-Battle.", NamedTextColor.RED));
                                                return 1;
                                            }

                                            OfflinePlayer target = Bukkit.getOfflinePlayer(context.getArgument("player", String.class));
                                            MABPlayer mabTarget = mab.players.get(target.getUniqueId());
                                            if (mabTarget == null) {
                                                context.getSource().getSender().sendMessage(Component.text("Der Spieler " + target.getName() + " ist nicht im Monster-Army-Battle.", NamedTextColor.RED));
                                                return 1;
                                            }

                                            mab.players.remove(mabTarget.uuid);
                                            mab.teams.forEach((u, t) -> t.players.remove(mabTarget));

                                            context.getSource().getSender().sendMessage(Component.text("Der Spieler " + target.getName() + " ist nun nicht mehr im Monster-Army-Battle.", NamedTextColor.YELLOW));
                                            return 1;
                                        })
                                )
                        )
                        .then(LiteralArgumentBuilder.<CommandSourceStack>literal("tp-world")
                                .requires(source -> source.getSender().isOp())
                                .then(RequiredArgumentBuilder.<CommandSourceStack, PlayerSelectorArgumentResolver>argument("player", ArgumentTypes.player())
                                        .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("target", StringArgumentType.word())
                                                .suggests((context, builder) -> {
                                                    if (ChallengeManager.CURRENT_CHALLENGE instanceof MonsterArmyBattle mab)
                                                        mab.players.forEach((uuid, p) -> builder.suggest(Bukkit.getOfflinePlayer(uuid).getName()));
                                                    return builder.buildFuture();
                                                })
                                                .then(RequiredArgumentBuilder.<CommandSourceStack, FinePositionResolver>argument("location", ArgumentTypes.finePosition(true))
                                                        .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("dimension", StringArgumentType.word())
                                                                .suggests((context, builder) -> {
                                                                    DIMENSIONS.forEach(builder::suggest);
                                                                    return builder.buildFuture();
                                                                })
                                                                .executes(context -> {
                                                                    if (!(ChallengeManager.CURRENT_CHALLENGE instanceof MonsterArmyBattle mab)) {
                                                                        context.getSource().getSender().sendMessage(Component.text("Es läuft gerade kein Monster-Army-Battle.", NamedTextColor.RED));
                                                                        return 1;
                                                                    }

                                                                    Player player = context.getArgument("player", PlayerSelectorArgumentResolver.class).resolve(context.getSource()).getFirst();
                                                                    FinePosition position = context.getArgument("location", FinePositionResolver.class).resolve(context.getSource());
                                                                    String dimension = context.getArgument("dimension", String.class).toLowerCase();

                                                                    if (!DIMENSIONS.contains(dimension)) {
                                                                        context.getSource().getSender().sendMessage(Component.text("Die Dimension " + dimension + " existiert nicht!", NamedTextColor.RED));
                                                                        return 1;
                                                                    }

                                                                    OfflinePlayer target = Bukkit.getOfflinePlayer(context.getArgument("target", String.class));
                                                                    MABPlayer mabTarget = mab.players.get(target.getUniqueId());
                                                                    if (mabTarget == null) {
                                                                        context.getSource().getSender().sendMessage(Component.text("Der Spieler " + target.getName() + " ist nicht im Monster-Army-Battle.", NamedTextColor.RED));
                                                                        return 1;
                                                                    }

                                                                    World world = switch (dimension) {
                                                                        case "battle" -> mabTarget.team.battleWorld;
                                                                        case "nether" -> mabTarget.team.collectionWorlds.middle();
                                                                        case "end" -> mabTarget.team.collectionWorlds.right();
                                                                        default -> mabTarget.team.collectionWorlds.left();
                                                                    };
                                                                    player.teleport(new Location(world, position.x(), position.y(), position.z()));
                                                                    context.getSource().getSender().sendMessage(Component.text("Der Spieler " + player.getName() + " wurde in die Welt von " + target.getName() + " teleportiert.", NamedTextColor.YELLOW));
                                                                    return 1;
                                                                })
                                                        )
                                                )
                                        )
                                )
                        )
                        .then(LiteralArgumentBuilder.<CommandSourceStack>literal("config")
                                .executes(context -> {
                                    if (!(ChallengeManager.CURRENT_CHALLENGE instanceof MonsterArmyBattle mab)) {
                                        context.getSource().getSender().sendMessage(Component.text("Es läuft gerade kein Monster-Army-Battle.", NamedTextColor.RED));
                                        return 1;
                                    }

                                    if (!(context.getSource().getSender() instanceof Player player)) return 1;

                                    if (mab.currentStage() != MonsterArmyBattle.Stage.CONFIGURATION) {
                                        player.sendMessage(Component.text("Das Monster-Army-Battle ist nicht in der Konfiguration!", NamedTextColor.RED));
                                        return 1;
                                    }

                                    MABPlayer p = mab.players.get(player.getUniqueId());
                                    if (p == null) {
                                        player.sendMessage(Component.text("Du bist nicht im Monster-Army-Battle!", NamedTextColor.RED));
                                        return 1;
                                    }

                                    p.configured = false;
                                    p.openConfiguration();
                                    return 1;
                                })
                        )
                        .then(LiteralArgumentBuilder.<CommandSourceStack>literal("stage")
                                .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("stage", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            for (MonsterArmyBattle.Stage s : MonsterArmyBattle.Stage.values())
                                                builder.suggest(s.name().toLowerCase());
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> {
                                            if (!(ChallengeManager.CURRENT_CHALLENGE instanceof MonsterArmyBattle mab)) {
                                                context.getSource().getSender().sendMessage(Component.text("Es läuft gerade kein Monster-Army-Battle.", NamedTextColor.RED));
                                                return 1;
                                            }

                                            MonsterArmyBattle.Stage stage = MonsterArmyBattle.Stage.valueOf(context.getArgument("stage", String.class).toUpperCase());
                                            switch (stage) {
                                                case COLLECTION -> mab.initLogic();
                                                case CONFIGURATION -> mab.startConfiguration();
                                                case BATTLE -> mab.startBattle();
                                            }
                                            return 1;
                                        })
                                )
                        )
                )
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("rw")
                        .then(LiteralArgumentBuilder.<CommandSourceStack>literal("success")
                                .requires(source -> source.getSender().isOp())
                                .executes(context -> {
                                    if (!(ChallengeManager.CURRENT_CHALLENGE instanceof RandomWorldChallenge rw)) {
                                        context.getSource().getSender().sendMessage(Component.text("Es läuft gerade keine Random-World-Challenge.", NamedTextColor.RED));
                                        return 1;
                                    }

                                    if (!rw.inWorld()) {
                                        context.getSource().getSender().sendMessage(Component.text("Die challenge ist gerade nicht in einer Random-World.", NamedTextColor.YELLOW));
                                        return 1;
                                    }

                                    rw.success();
                                    return 1;
                                })
                        )
                        .then(LiteralArgumentBuilder.<CommandSourceStack>literal("fail")
                                .requires(source -> source.getSender().isOp())
                                .executes(context -> {
                                    if (!(ChallengeManager.CURRENT_CHALLENGE instanceof RandomWorldChallenge rw)) {
                                        context.getSource().getSender().sendMessage(Component.text("Es läuft gerade keine Random-World-Challenge.", NamedTextColor.RED));
                                        return 1;
                                    }

                                    if (!rw.inWorld()) {
                                        context.getSource().getSender().sendMessage(Component.text("Die challenge ist gerade nicht in einer Random-World.", NamedTextColor.YELLOW));
                                        return 1;
                                    }

                                    rw.fail();
                                    return 1;
                                })
                        )
                )
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("end")
                        .executes(context -> {
                            if (ChallengeManager.CURRENT_CHALLENGE == null) {
                                context.getSource().getSender().sendMessage(Component.text("Es läuft gerade keine challenge die du beenden kannst!", NamedTextColor.GOLD));
                            } else {
                                ChallengeManager.CURRENT_CHALLENGE.end(false);
                                context.getSource().getSender().sendMessage(Component.text("Die challenge wurde beendet!", NamedTextColor.YELLOW));
                            }
                            return 1;
                        })
                )
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("winner")
                        .executes(context -> {
                            if (!ChallengeManager.PROPERTIES.containsKey("last-winner")) {
                                context.getSource().getSender().sendMessage(Component.text("Es gibt gerade keinen Gewinner!", NamedTextColor.RED));
                            } else {
                                String winner = (String) ChallengeManager.PROPERTIES.get("last-winner");
                                Bukkit.getServer().showTitle(Title.title(GsonComponentSerializer.gson().deserialize(winner),
                                        Component.text((winner.contains(" & ") ? "Hat" : "Haben") + " die challenge in " + Time.preciselyFormat((int) ChallengeManager.PROPERTIES.get("last-winner-time")) + " gewonnen!", NamedTextColor.GRAY)));
                            }
                            return 1;
                        })
                )
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("last-time")
                        .executes(context -> {
                            if (!ChallengeManager.PROPERTIES.containsKey("last-time")) {
                                context.getSource().getSender().sendMessage(Component.text("Es wurde keine Challenge-Zeit vor dem letzten Server Stop gespeichert!", NamedTextColor.GOLD));
                            } else {
                                try {
                                    context.getSource().getSender().sendMessage(Component.text("Die letzte Challenge-Zeit war: ", NamedTextColor.GREEN)
                                            .append(Component.text(Time.preciselyFormat((long) ChallengeManager.PROPERTIES.get("last-time")), NamedTextColor.WHITE)));
                                } catch (Exception e) {
                                    context.getSource().getSender().sendMessage(Component.text("Die letzte Challenge-Zeit konnte nicht gelesen werden!", NamedTextColor.RED));
                                }
                            }
                            return 1;
                        })
                )
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("timer")
                        .then(LiteralArgumentBuilder.<CommandSourceStack>literal("pause")
                                .executes(context -> {
                                    if (!ChallengeManager.running()) {
                                        context.getSource().getSender().sendMessage(Component.text("Es läuft gerade keine challenge, oder sie ist schon pausiert!", NamedTextColor.GOLD));
                                    } else {
                                        ChallengeManager.CURRENT_CHALLENGE.running = false;
                                        context.getSource().getSender().sendMessage(Component.text("Die challenge wurde pausiert!", NamedTextColor.YELLOW));
                                    }
                                    return 1;
                                })
                        )
                        .then(LiteralArgumentBuilder.<CommandSourceStack>literal("resume")
                                .executes(context -> {
                                    if (ChallengeManager.running()) {
                                        context.getSource().getSender().sendMessage(Component.text("Die aktuelle challenge ist bereits am laufen!", NamedTextColor.GOLD));
                                    } else if (ChallengeManager.CURRENT_CHALLENGE == null) {
                                        context.getSource().getSender().sendMessage(Component.text("Es läuft gerade keine challenge! Du kannst eine challenge mit \"/challenge start\" starten!", NamedTextColor.RED));
                                    } else {
                                        ChallengeManager.CURRENT_CHALLENGE.running = true;
                                        context.getSource().getSender().sendMessage(Component.text("Die challenge wurde pausiert!", NamedTextColor.YELLOW));
                                    }
                                    return 1;
                                })
                        )
                        .then(timeCommand("add", (time, sender) -> {
                            ChallengeManager.CURRENT_CHALLENGE.timer.timer().increment(time.get());
                            sender.sendMessage(Component.text("Der timer ist jetzt " + time.getPreciselyFormatted() + " höher!", NamedTextColor.GREEN));
                        }))
                        .then(timeCommand("remove", (time, sender) -> {
                            ChallengeManager.CURRENT_CHALLENGE.timer.timer().decrement(time.get());
                            sender.sendMessage(Component.text("Der timer ist jetzt " + time.getPreciselyFormatted() + " niedriger!", NamedTextColor.GREEN));
                        }))
                        .then(timeCommand("set", (time, sender) -> {
                            Time oldTime = ChallengeManager.CURRENT_CHALLENGE.timer.timer();
                            oldTime.set(oldTime.get());
                            sender.sendMessage(Component.text("Der timer ist jetzt " + time.getPreciselyFormatted() + "!", NamedTextColor.GREEN));
                        }))
                        .then(LiteralArgumentBuilder.<CommandSourceStack>literal("reset")
                                .executes(context -> {
                                    if (ChallengeManager.CURRENT_CHALLENGE == null) {
                                        context.getSource().getSender().sendMessage(Component.text("Es läuft gerade keine challenge! Du kannst eine challenge mit \"/challenge start\" starten!", NamedTextColor.RED));
                                    } else {
                                        Time oldTime = ChallengeManager.CURRENT_CHALLENGE.timer.timer();
                                        oldTime.decrement(oldTime.get());
                                        context.getSource().getSender().sendMessage(Component.text("Der timer ist wieder auf null!", NamedTextColor.GREEN));
                                    }
                                    return 1;
                                })
                        )
                        .then(LiteralArgumentBuilder.<CommandSourceStack>literal("toggle-reverse")
                                .executes(context -> {
                                    if (ChallengeManager.CURRENT_CHALLENGE == null) {
                                        context.getSource().getSender().sendMessage(Component.text("Es läuft gerade keine challenge! Du kannst eine challenge mit \"/challenge start\" starten!", NamedTextColor.RED));
                                    } else {
                                        ChallengeManager.CURRENT_CHALLENGE.timer.reversed(!ChallengeManager.CURRENT_CHALLENGE.timer.reversed());
                                        context.getSource().getSender().sendMessage(Component.text("Der timer ist jetzt " + (ChallengeManager.CURRENT_CHALLENGE.timer.reversed() ? "umgekehrt" : "normal") + "!", NamedTextColor.GREEN));
                                    }
                                    return 1;
                                })
                        )
                )
                .build();
    }

    public static final UUID PACK_UUID = UUID.randomUUID();
    public static final String PACK_LINK = "https://www.dropbox.com/scl/fi/3g8bj5aw13neb3y0dmvr3/TheGentleChallengesPack.zip?rlkey=dpegywms7igne27i0nz8bh1pv&st=tb8vlpz0&dl=1";
    public static final String PACK_HASH = "d8a924d162f55144b7b62eea2c725003710a3ebd";

    public static LiteralCommandNode<CommandSourceStack> resourcePackCommand() {
        return LiteralArgumentBuilder.<CommandSourceStack>literal("resource-pack")
                .requires(source -> source.getSender() instanceof Player)
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("apply")
                        .executes(context -> {
                            ((Player) context.getSource().getSender()).setResourcePack(PACK_UUID, PACK_LINK, PACK_HASH, Component.text("Wichtig, damit die Herzen richtig gerendert werden!", NamedTextColor.GREEN), false);
                            return 1;
                        })
                )
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("remove")
                        .executes(context -> {
                            ZeroHeartsChallenge.forEachPlayer(p -> p.removeResourcePack(PACK_UUID));
                            return 1;
                        })
                )
                .build();
    }

    private static LiteralArgumentBuilder<CommandSourceStack> timeCommand(String name, BiConsumer<Time, CommandSender> action) {
        return LiteralArgumentBuilder.<CommandSourceStack>literal(name)
                .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("time", StringArgumentType.greedyString())
                        .suggests((context, builder) -> {
                            String input = Objects.requireNonNullElse(builder.getInput().split(" ")[0], "");
                            for (Time.Unit unit : Time.Unit.values()) {
                                builder.suggest(input.replaceAll("[^-\\d.]+", "") + unit);
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            if (ChallengeManager.CURRENT_CHALLENGE == null) {
                                context.getSource().getSender().sendMessage(Component.text("Es läuft gerade keine challenge! Du kannst eine challenge mit \"/challenge start\" starten!", NamedTextColor.RED));
                            } else {
                                String args = context.getArgument("time", String.class);
                                if (args.isBlank()) {
                                    context.getSource().getSender().sendMessage(Component.text("Du musst eine Zeit angeben!", NamedTextColor.RED));
                                    return 1;
                                }

                                Time time = new Time(0);
                                for (String arg : args.split(" *")) {
                                    time.increment(Time.parse(arg).get());
                                }

                                if (time.get() <= 0) {
                                    context.getSource().getSender().sendMessage(Component.text("Die angegebene Zeit ist entweder unter 0 oder falsch geschrieben!", NamedTextColor.RED));
                                    return 1;
                                }

                                action.accept(time, context.getSource().getSender());
                            }
                            return 1;
                        })
                );
    }
}
