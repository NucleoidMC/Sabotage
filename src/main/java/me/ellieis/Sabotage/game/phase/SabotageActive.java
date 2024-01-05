package me.ellieis.Sabotage.game.phase;

import com.google.common.collect.ImmutableSet;
import eu.pb4.sidebars.api.Sidebar;
import eu.pb4.sidebars.api.lines.SidebarLine;
import me.ellieis.Sabotage.game.GameStates;
import me.ellieis.Sabotage.game.Roles;
import me.ellieis.Sabotage.game.config.DetectiveConfig;
import me.ellieis.Sabotage.game.config.InnocentConfig;
import me.ellieis.Sabotage.game.config.SabotageConfig;
import me.ellieis.Sabotage.game.config.SaboteurConfig;
import me.ellieis.Sabotage.game.map.SabotageMap;
import me.ellieis.Sabotage.game.statistics.KarmaManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.game.GameActivity;
import xyz.nucleoid.plasmid.game.GameCloseReason;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.game.common.widget.SidebarWidget;
import xyz.nucleoid.plasmid.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.game.player.MutablePlayerSet;
import xyz.nucleoid.plasmid.game.player.PlayerSet;
import xyz.nucleoid.plasmid.game.rule.GameRuleType;
import xyz.nucleoid.plasmid.util.PlayerRef;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class SabotageActive {
    private final SabotageConfig config;
    private final GameSpace gameSpace;
    private final SabotageMap map;
    private final ServerWorld world;
    private long startTime;
    private GameActivity activity;
    private GameStates gameState = GameStates.COUNTDOWN;
    private final MutablePlayerSet saboteurs;
    private final MutablePlayerSet detectives;
    private final MutablePlayerSet innocents;
    private GlobalWidgets widgets;
    private SidebarWidget globalSidebar;
    private SidebarWidget innocentSidebar;
    private SidebarWidget detectiveSidebar;
    private SidebarWidget saboteurSidebar;
    private KarmaManager karmaManager;

    public SabotageActive(SabotageConfig config, GameSpace gameSpace, SabotageMap map, ServerWorld world) {
        this.config = config;
        this.gameSpace = gameSpace;
        this.map = map;
        this.world = world;

        this.saboteurs = new MutablePlayerSet(gameSpace.getServer());
        this.detectives = new MutablePlayerSet(gameSpace.getServer());
        this.innocents = new MutablePlayerSet(gameSpace.getServer());
        this.karmaManager = new KarmaManager(gameSpace);
    }
    private static void gameStartedRules(GameActivity activity) {
        activity.allow(GameRuleType.FALL_DAMAGE);
        activity.allow(GameRuleType.PVP);
    }
    private static void rules(GameActivity activity) {
        activity.allow(GameRuleType.INTERACTION);
        activity.allow(GameRuleType.PICKUP_ITEMS);
        activity.allow(GameRuleType.MODIFY_ARMOR);
        activity.allow(GameRuleType.MODIFY_INVENTORY);
        activity.allow(GameRuleType.THROW_ITEMS);
        activity.deny(GameRuleType.FALL_DAMAGE);
        activity.deny(GameRuleType.SATURATED_REGENERATION);
        activity.deny(GameRuleType.PVP);
        activity.deny(GameRuleType.PORTALS);
        activity.deny(GameRuleType.HUNGER);
        activity.deny(GameRuleType.ICE_MELT);
        activity.deny(GameRuleType.PLACE_BLOCKS);
        activity.deny(GameRuleType.FIRE_TICK);
        activity.deny(GameRuleType.BREAK_BLOCKS);
        activity.deny(GameRuleType.CRAFTING);
    }

    public void updateSidebars() {
        long timeLeft = (long) Math.abs(Math.floor((world.getTime() / 20) - (startTime / 20)) - config.getCountdownTime() - config.getGracePeriod() - config.getTimeLimit());
        long minutes = timeLeft / 60;
        String seconds = Long.toString(timeLeft % 60);
        if (seconds.length() == 1) {
            // Pad seconds with an extra 0 when only one digit
            // If someone knows a better way to do this, be my guest
            seconds = "0" + seconds;
        }
        saboteurSidebar.setLine(SidebarLine.create(0, Text.translatable("sabotage.sidebar.time_left", minutes, seconds)));        saboteurSidebar.setLine(SidebarLine.create(0, Text.translatable("sabotage.sidebar.time_left", minutes, seconds)));
        detectiveSidebar.setLine(SidebarLine.create(0, Text.translatable("sabotage.sidebar.time_left", minutes, seconds)));
        innocentSidebar.setLine(SidebarLine.create(0, Text.translatable("sabotage.sidebar.time_left", minutes, seconds)));
    }

    public void setSidebars() {
        saboteurSidebar = widgets.addSidebar(Text.translatable("gameType.sabotage.sabotage").formatted(Formatting.GOLD));
        saboteurSidebar.setPriority(Sidebar.Priority.MEDIUM);
        detectiveSidebar = widgets.addSidebar(Text.translatable("gameType.sabotage.sabotage").formatted(Formatting.GOLD));
        detectiveSidebar.setPriority(Sidebar.Priority.MEDIUM);
        innocentSidebar = widgets.addSidebar(Text.translatable("gameType.sabotage.sabotage").formatted(Formatting.GOLD));
        innocentSidebar.setPriority(Sidebar.Priority.MEDIUM);

        // saboteurs
        saboteurSidebar.addLines(ScreenTexts.EMPTY,
                Text.translatable("sabotage.sidebar.role", Text.translatable("sabotage.saboteur").formatted(Formatting.RED)),
                Text.translatable("sabotage.sidebar.role.desc", Text.translatable("sabotage.innocents").formatted(Formatting.GREEN)),
                ScreenTexts.EMPTY,
                // this will be where the time left is located
                ScreenTexts.EMPTY
        );

        // detectives
        detectiveSidebar.addLines(ScreenTexts.EMPTY,
                Text.translatable("sabotage.sidebar.role", Text.translatable("sabotage.detective").formatted(Formatting.DARK_BLUE)),
                Text.translatable("sabotage.sidebar.role.desc", Text.translatable("sabotage.saboteurs").formatted(Formatting.RED)),
                ScreenTexts.EMPTY,
                // this will be where the time left is located
                ScreenTexts.EMPTY
        );

        // innocents
        innocentSidebar.addLines(ScreenTexts.EMPTY,
                Text.translatable("sabotage.sidebar.role", Text.translatable("sabotage.innocent").formatted(Formatting.GREEN)),
                Text.translatable("sabotage.sidebar.role.desc", Text.translatable("sabotage.saboteurs").formatted(Formatting.RED)),
                ScreenTexts.EMPTY,
                // this will be where the time left is located
                ScreenTexts.EMPTY
        );

        gameSpace.getPlayers().forEach(plr -> {
            saboteurSidebar.removePlayer(plr);
            detectiveSidebar.removePlayer(plr);
            innocentSidebar.removePlayer(plr);
        });
        saboteurs.forEach(plr -> saboteurSidebar.addPlayer(plr));
        detectives.forEach(plr -> detectiveSidebar.addPlayer(plr));
        innocents.forEach(plr -> innocentSidebar.addPlayer(plr));
        updateSidebars();
    }

    public void pickRoles() {
        PlayerSet plrs = gameSpace.getPlayers();
        int playerCount = plrs.size();
        // need to make a new list from .toList to make it mutable
        List<ServerPlayerEntity> plrList = new ArrayList<>(plrs.stream().toList());
        Collections.shuffle(plrList);
        int sabCount = playerCount / 3;
        if (sabCount < 1) {
            sabCount = 1;
        }
        int detCount = playerCount / 8;

        for (ServerPlayerEntity plr : plrList) {
            if (detCount >= 1) {
                detectives.add(plr);
                detCount--;
            } else if (sabCount >= 1) {
                saboteurs.add(plr);
                sabCount--;
            } else {
                innocents.add(plr);
            }
        }
        innocents.showTitle(Text.translatable("sabotage.role_reveal", Text.translatable("sabotage.innocent").formatted(Formatting.GREEN)), 10, 80, 10);
        innocents.playSound(SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL);
        detectives.showTitle(Text.translatable("sabotage.role_reveal", Text.translatable("sabotage.detective").formatted(Formatting.DARK_BLUE)), 10, 80, 10);
        detectives.playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME);
        saboteurs.showTitle(Text.translatable("sabotage.role_reveal", Text.translatable("sabotage.saboteur").formatted(Formatting.RED)), 10, 80, 10);
        saboteurs.playSound(SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL);
        saboteurs.playSound(SoundEvents.AMBIENT_SOUL_SAND_VALLEY_MOOD.value());
        setSidebars();
    }
    public Roles getPlayerRole(ServerPlayerEntity plr) {
        if (innocents.contains(plr)) {
            return Roles.INNOCENT;
        } else if (detectives.contains(plr)) {
            return Roles.DETECTIVE;
        } else if (saboteurs.contains(plr)) {
            return Roles.SABOTEUR;
        }
        return Roles.NONE;
    }
    public void Start() {
        gameState = GameStates.ACTIVE;
        pickRoles();
        gameStartedRules(activity);
        gameSpace.getPlayers().forEach(plr -> {
            karmaManager.setKarma(plr, 20);
            plr.setExperiencePoints(plr.getNextLevelExperience() - 1);
        });
        // to-do: chest spawns
    }

    public void End() {
        gameState = GameStates.ENDED;
        rules(activity);
    }
    public static void Open(GameSpace gameSpace, ServerWorld world, SabotageMap map, SabotageConfig config) {
        gameSpace.setActivity(activity -> {
            SabotageActive game = new SabotageActive(config, gameSpace, map, world);
            game.startTime = world.getTime();
            game.activity = activity;
            game.widgets = GlobalWidgets.addTo(activity);
            game.globalSidebar = game.widgets.addSidebar(Text.translatable("gameType.sabotage.sabotage").formatted(Formatting.GOLD));
            game.globalSidebar.setPriority(Sidebar.Priority.LOW);
            game.globalSidebar.setLine(SidebarLine.create(0, Text.translatable("sabotage.sidebar.countdown")));
            rules(activity);
            activity.listen(GameActivityEvents.TICK, game::onTick);
            activity.listen(PlayerDeathEvent.EVENT, game::onDeath);

            PlayerSet plrs = game.gameSpace.getPlayers();

            plrs.showTitle(Text.literal(Integer.toString(game.config.getCountdownTime())).formatted(Formatting.GOLD), 20);
            plrs.playSound(SoundEvents.BLOCK_NOTE_BLOCK_HARP.value(), SoundCategory.PLAYERS, 1.0F, 2.0F);
            plrs.forEach(plr -> {
                game.map.spawnEntity(world, plr);
                game.globalSidebar.addPlayer(plr);
            });
        });
    }

    private ActionResult onDeath(ServerPlayerEntity plr, DamageSource damageSource) {
        Entity entityAttacker = damageSource.getAttacker();

        plr.changeGameMode(GameMode.SPECTATOR);
        if (entityAttacker instanceof ServerPlayerEntity attacker) {
            Roles plrRole = getPlayerRole(plr);
            Roles attackerRole = getPlayerRole(attacker);
            // surely there's a better way to do this..
            System.out.println(attackerRole);
            System.out.println(plrRole);
            switch(attackerRole) {
                case SABOTEUR -> {
                    SaboteurConfig config = this.config.getSaboteurConfig();
                    switch(plrRole) {
                        case INNOCENT -> {
                            System.out.printf("Saboteur killed innocent, awarding %s karma%n", config.innocentKarmaAward());
                            karmaManager.incrementKarma(attacker, config.innocentKarmaAward());
                        }

                        case DETECTIVE -> {
                            System.out.printf("Saboteur killed detective, awarding %s karma%n", config.detectiveKarmaAward());
                            karmaManager.incrementKarma(attacker, config.detectiveKarmaAward());
                        }

                        case SABOTEUR -> {
                            System.out.printf("Saboteur killed saboteur, awarding %s karma penalty%n", config.saboteurKarmaPenalty());
                            karmaManager.decrementKarma(attacker, config.saboteurKarmaPenalty());
                        }
                    }
                }

                case DETECTIVE -> {
                    DetectiveConfig config = this.config.getDetectiveConfig();
                    awardPlayerKill(attacker, plrRole, config.innocentKarmaPenalty(), config.detectiveKarmaPenalty(), config.saboteurKarmaAward());
                }

                case INNOCENT -> {
                    InnocentConfig config = this.config.getInnocentConfig();
                    awardPlayerKill(attacker, plrRole, config.innocentKarmaPenalty(), config.detectiveKarmaPenalty(), config.saboteurKarmaAward());
                }
            }
        }
        return ActionResult.FAIL;
    }

    private void awardPlayerKill(ServerPlayerEntity attacker, Roles plrRole, int innocentKarma, int detectiveKarma, int saboteurKarma) {
        switch(plrRole) {
            case INNOCENT -> {
                System.out.printf("Innocent killed innocent, awarding %s karma penalty%n", innocentKarma);
                karmaManager.decrementKarma(attacker, innocentKarma);
            }

            case DETECTIVE -> {
                System.out.printf("Innocent killed detective, awarding %s karma penalty%n", detectiveKarma);
                karmaManager.decrementKarma(attacker, detectiveKarma);
            }

            case SABOTEUR -> {
                System.out.printf("Innocent killed saboteur, awarding %s karma%n", saboteurKarma);
                karmaManager.incrementKarma(attacker, saboteurKarma);
            }
        }
    }

    public void onTick() {
        long time = world.getTime();
        switch(gameState) {
            case COUNTDOWN -> {
                if (time % 20 == 0) {
                    // second has passed
                    PlayerSet plrs = gameSpace.getPlayers();
                    int secondsSinceStart = (int) Math.floor((time / 20) - (startTime / 20));
                    int countdownTime = config.getCountdownTime();
                    if (secondsSinceStart >= countdownTime) {
                        gameState = GameStates.GRACE_PERIOD;
                        plrs.playSound(SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE);
                        plrs.sendMessage(Text.translatable("sabotage.game_start", config.getGracePeriod()).formatted(Formatting.GOLD));
                    } else {
                        plrs.showTitle(Text.literal(Integer.toString(countdownTime - secondsSinceStart)).formatted(Formatting.GOLD), 20);
                        plrs.playSound(SoundEvents.BLOCK_NOTE_BLOCK_HARP.value(), SoundCategory.PLAYERS, 1.0F, 2.0F);
                    }
                }
                // Make sure players don't move during countdown
                for (ServerPlayerEntity plr : gameSpace.getPlayers()) {
                    Vec3d pos = map.getPlayerSpawns().get(new PlayerRef(plr.getUuid()));
                    // Set X and Y as relative so it will send 0 change when we pass yaw (yaw - yaw = 0) and pitch
                    Set<PositionFlag> flags = ImmutableSet.of(PositionFlag.X_ROT, PositionFlag.Y_ROT);
                    // Teleport without changing the pitch and yaw
                    plr.networkHandler.requestTeleport(pos.getX(), pos.getY(), pos.getZ(), plr.getYaw(), plr.getPitch(), flags);
                }
            }

            case GRACE_PERIOD -> {
                int secondsSinceStart = (int) Math.floor((time / 20) - (startTime / 20)) - config.getCountdownTime();
                int gracePeriod = config.getGracePeriod();
                if (secondsSinceStart >= gracePeriod) {
                    Start();
                } else {
                    int secondsLeft = gracePeriod - secondsSinceStart;
                    this.globalSidebar.setLine(SidebarLine.create(0, Text.translatable("sabotage.sidebar.grace_period." + ((secondsLeft == 1) ? "singular" : "plural"), secondsLeft)));
                }
            }

            case ACTIVE -> {
                // to-do: implement game loop
                if (time % 20 == 0) {
                    // second has passed
                    double timePassed = Math.floor((world.getTime() / 20) - (startTime / 20)) - config.getCountdownTime() - config.getGracePeriod();
                    int timeLimit = config.getTimeLimit();
                    if (timePassed >= timeLimit) {
                        End();
                        return;
                    }
                    updateSidebars();
                    double factor = ((timeLimit - timePassed) / timeLimit);
                    gameSpace.getPlayers().forEach(plr -> {
                        plr.setExperiencePoints((int) (plr.getNextLevelExperience() * factor));
                    });
                }
            }

            case ENDED -> {
                // to-do: countdown before ending
                gameSpace.close(GameCloseReason.FINISHED);
            }
            default -> {
                // unknown state, noop.
            }
        }
    }
}
