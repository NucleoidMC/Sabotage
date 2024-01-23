package me.ellieis.Sabotage.game.phase;

import com.google.common.collect.ImmutableSet;
import eu.pb4.sidebars.api.Sidebar;
import eu.pb4.sidebars.api.lines.SidebarLine;
import me.ellieis.Sabotage.Sabotage;
import me.ellieis.Sabotage.game.EndReason;
import me.ellieis.Sabotage.game.GameStates;
import me.ellieis.Sabotage.game.Roles;
import me.ellieis.Sabotage.game.config.DetectiveConfig;
import me.ellieis.Sabotage.game.config.InnocentConfig;
import me.ellieis.Sabotage.game.config.SabotageConfig;
import me.ellieis.Sabotage.game.config.SaboteurConfig;
import me.ellieis.Sabotage.game.map.SabotageMap;
import me.ellieis.Sabotage.game.statistics.KarmaManager;
import me.ellieis.Sabotage.game.utils.Task;
import me.ellieis.Sabotage.game.utils.TaskScheduler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SentMessage;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.network.packet.s2c.play.TeamS2CPacket;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.game.GameActivity;
import xyz.nucleoid.plasmid.game.GameCloseReason;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.game.common.widget.SidebarWidget;
import xyz.nucleoid.plasmid.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.game.player.MutablePlayerSet;
import xyz.nucleoid.plasmid.game.player.PlayerOffer;
import xyz.nucleoid.plasmid.game.player.PlayerOfferResult;
import xyz.nucleoid.plasmid.game.player.PlayerSet;
import xyz.nucleoid.plasmid.game.rule.GameRuleType;
import xyz.nucleoid.plasmid.util.PlayerRef;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;
import xyz.nucleoid.stimuli.event.player.ReplacePlayerChatEvent;

import java.util.*;
import java.util.function.Consumer;

import static me.ellieis.Sabotage.game.custom.SabotageItems.DETECTIVE_SHEARS;

public class SabotageActive {

    private final SabotageConfig config;
    public final GameSpace gameSpace;
    private final SabotageMap map;
    private final ServerWorld world;
    private final MutablePlayerSet saboteurs;
    private final MutablePlayerSet detectives;
    private final MutablePlayerSet innocents;
    private final MutablePlayerSet dead;
    // used in the game end message to list all saboteurs
    private PlayerSet initialSaboteurs;
    private final Map<ServerPlayerEntity, Team> playersOldTeams = new HashMap<>();
    private final KarmaManager karmaManager;
    private final TaskScheduler taskScheduler;
    private boolean isTesterOnCooldown = false;
    private long startTime;
    private long endTime;
    private GameActivity activity;
    public GameStates gameState = GameStates.COUNTDOWN;
    private GlobalWidgets widgets;
    private SidebarWidget globalSidebar;
    private SidebarWidget innocentSidebar;
    private SidebarWidget detectiveSidebar;
    private SidebarWidget saboteurSidebar;

    public SabotageActive(SabotageConfig config, GameSpace gameSpace, SabotageMap map, ServerWorld world) {
        this.config = config;
        this.gameSpace = gameSpace;
        this.map = map;
        this.world = world;

        this.saboteurs = new MutablePlayerSet(gameSpace.getServer());
        this.detectives = new MutablePlayerSet(gameSpace.getServer());
        this.innocents = new MutablePlayerSet(gameSpace.getServer());
        this.dead = new MutablePlayerSet(gameSpace.getServer());
        this.karmaManager = new KarmaManager(gameSpace);
        this.taskScheduler = new TaskScheduler(gameSpace, world);
        Sabotage.activeGames.add(this);
    }
    public ServerWorld getWorld() {
        return world;
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
    private static String getPlayerNamesInSet(PlayerSet plrs) {
        String result = "";
        for (ServerPlayerEntity plr : plrs) {
            result = result + plr.getName().getString() + ", ";
        }
        // get rid of the last comma
        if (!result.isBlank()) {
            result = result.substring(0, result.length() - 2);
        }
        return result;
    }

    private PlayerSet getAlivePlayers() {
        MutablePlayerSet plrs = gameSpace.getPlayers().copy(gameSpace.getServer());
        plrs.forEach(plr -> {
            if (plr.isSpectator()) {
                plrs.remove(plr);
            }
        });
        return plrs;
    }
    public void updateSidebars() {
        long timeLeft = (long) Math.abs(Math.floor((world.getTime() / 20) - (startTime / 20)) - config.countdownTime() - config.gracePeriod() - config.timeLimit());
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
        globalSidebar.setLine(SidebarLine.create(0, Text.translatable("sabotage.sidebar.time_left", minutes, seconds)));
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

        // global (spectators and dead players)
        globalSidebar.setLine(0, Text.empty());

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
        PlayerSet plrs = getAlivePlayers();
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
        initialSaboteurs = saboteurs.copy(gameSpace.getServer());
        innocents.showTitle(Text.translatable("sabotage.role_reveal", Text.translatable("sabotage.innocent").formatted(Formatting.GREEN)), 10, 80, 10);
        innocents.playSound(SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL);
        detectives.showTitle(Text.translatable("sabotage.role_reveal", Text.translatable("sabotage.detective").formatted(Formatting.DARK_BLUE)), 10, 80, 10);
        detectives.playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME);
        saboteurs.showTitle(Text.translatable("sabotage.role_reveal", Text.translatable("sabotage.saboteur").formatted(Formatting.RED)), 10, 80, 10);
        saboteurs.playSound(SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL);
        saboteurs.playSound(SoundEvents.AMBIENT_SOUL_SAND_VALLEY_MOOD.value());

        // give detectives their portable tester
        for (ServerPlayerEntity detective : detectives) {
            detective.getInventory().insertStack(new ItemStack(DETECTIVE_SHEARS));
        }

        // name tag coloring
        Scoreboard scoreboard = gameSpace.getServer().getScoreboard();
        Team unknown = scoreboard.getTeam("unknown");
        unknown.setColor(Formatting.YELLOW);
        Team saboteur = scoreboard.getTeam("saboteur");
        saboteur.setColor(Formatting.RED);
        Team detective = scoreboard.getTeam("detective");
        detective.setColor(Formatting.DARK_BLUE);
        Team innocent = scoreboard.getTeam("innocent");
        innocent.setColor(Formatting.GREEN);
        for (ServerPlayerEntity player : getAlivePlayers()) {
            Roles role = getPlayerRole(player);
            if (role == Roles.DETECTIVE) {
                innocents.sendPacket(TeamS2CPacket.changePlayerTeam(detective, player.getGameProfile().getName(), TeamS2CPacket.Operation.ADD));
                detectives.sendPacket(TeamS2CPacket.changePlayerTeam(detective, player.getGameProfile().getName(), TeamS2CPacket.Operation.ADD));
                saboteurs.sendPacket(TeamS2CPacket.changePlayerTeam(detective, player.getGameProfile().getName(), TeamS2CPacket.Operation.ADD));
            } else {
                innocents.sendPacket(TeamS2CPacket.changePlayerTeam(unknown, player.getGameProfile().getName(), TeamS2CPacket.Operation.ADD));
                detectives.sendPacket(TeamS2CPacket.changePlayerTeam(unknown, player.getGameProfile().getName(), TeamS2CPacket.Operation.ADD));
                saboteurs.sendPacket(TeamS2CPacket.changePlayerTeam((role == Roles.INNOCENT) ? innocent : (role == Roles.SABOTEUR) ? saboteur : unknown, player.getGameProfile().getName(), TeamS2CPacket.Operation.ADD));
            }
        }

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
    public static Formatting getRoleColor(Roles role) {
        return (role == Roles.INNOCENT) ? Formatting.GREEN :
                (role == Roles.DETECTIVE) ? Formatting.DARK_BLUE :
                        (role == Roles.SABOTEUR) ? Formatting.RED : Formatting.RESET;
    }
    public void changeTeam(ServerPlayerEntity plr, ServerPlayerEntity other, TeamS2CPacket.Operation operation) {
        Scoreboard scoreboard = gameSpace.getServer().getScoreboard();
        Team unknown = scoreboard.getTeam("unknown");
        Team innocent = scoreboard.getTeam("innocent");
        Team detective = scoreboard.getTeam("detective");
        Team saboteur = scoreboard.getTeam("saboteur");
        if (getPlayerRole(plr) == Roles.SABOTEUR) {
            Roles otherPlrRole = getPlayerRole(other);
            plr.networkHandler.sendPacket(TeamS2CPacket.changePlayerTeam((otherPlrRole == Roles.INNOCENT) ? innocent : (otherPlrRole == Roles.SABOTEUR) ? saboteur : (otherPlrRole == Roles.DETECTIVE) ? detective : unknown, other.getGameProfile().getName(), operation));
        } else {
            if (getPlayerRole(other) == Roles.DETECTIVE) {
                plr.networkHandler.sendPacket(TeamS2CPacket.changePlayerTeam(detective, other.getGameProfile().getName(), operation));
            } else {
                plr.networkHandler.sendPacket(TeamS2CPacket.changePlayerTeam(unknown, other.getGameProfile().getName(), operation));
            }
        }
    }
    private Text createAttackerKillMessage(ServerPlayerEntity plr, int karma) {
        Roles role = getPlayerRole(plr);
        Formatting victimColor = getRoleColor(role);
        return Text.translatable(
                "sabotage.kill_message_attacker",
                plr.getName().copy().formatted(victimColor),
                Text.literal("(" + karma + " karma)").formatted((karma >= 0) ? Formatting.GREEN : Formatting.RED)).formatted(Formatting.YELLOW);
    }
    public EndReason checkWinCondition() {
        if (saboteurs.isEmpty()) {
            return EndReason.INNOCENT_WIN;
        } else if (innocents.isEmpty() && detectives.isEmpty()) {
            return EndReason.SABOTEUR_WIN;
        }
        return EndReason.NONE;
    }

    private void changeTesterWool(Roles role) {
        Block wool = (role == Roles.SABOTEUR) ? Blocks.RED_WOOL :
                (role == Roles.DETECTIVE) ? Blocks.BLUE_WOOL :
                        (role == Roles.INNOCENT) ? Blocks.GREEN_WOOL : Blocks.WHITE_WOOL;
        for (BlockPos testerWool : map.getTesterWools()) {
            world.setBlockState(testerWool, wool.getDefaultState());
        }
        taskScheduler.addTask(new Task((int) (world.getTime() + 200), (gameSpace) -> {
            for (BlockPos testerWool : map.getTesterWools()) {
                world.setBlockState(testerWool, Blocks.WHITE_WOOL.getDefaultState());
            }
            isTesterOnCooldown = false;
        }));

    }

    // portable tester onlu
    public void testEntity(ServerPlayerEntity plr, LivingEntity entity) {
        if (plr.isSpectator() || entity.isSpectator()) return;
        if (plr.hasStatusEffect(StatusEffects.SLOWNESS) || entity.hasStatusEffect(StatusEffects.SLOWNESS)) {
            // either player is already testing or being tested, abort
            return;
        }
        Roles role = getPlayerRole(plr);
        if (role == Roles.DETECTIVE) {
            if (entity.isPlayer()) {
                final ServerPlayerEntity playerEntity = (ServerPlayerEntity) entity;
                plr.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 100));
                plr.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 200));
                playerEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 100));
                playerEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 200));
                int revealTime = (int) world.getTime() + 200;
                Consumer<GameSpace> func = (gameSpace) -> {
                    Roles plrRole = getPlayerRole(playerEntity);
                    gameSpace.getPlayers().sendMessage(
                            Text.translatable("sabotage.detective_shears_reveal",
                                    playerEntity.getName(),
                                    Text.translatable("sabotage." + (
                                            (plrRole == Roles.SABOTEUR) ? "saboteur" :
                                                    (plrRole == Roles.DETECTIVE) ? "detective" : "innocent")
                                    ).formatted(getRoleColor(plrRole))));
                };
                taskScheduler.addTask(new Task(revealTime, func));
            }
        }
    }

    // tester only
    public boolean testEntity(ServerPlayerEntity plr, Vec3d pos) {
        if (plr.isSpectator()) return true;
        if (gameState == GameStates.ACTIVE) {
            if (isTesterOnCooldown) {
                return false;
            }
            isTesterOnCooldown = true;
            plr.teleport(pos.getX(), pos.getY(), pos.getZ());
            plr.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 100));
            plr.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 200));
            for (BlockPos blockPos : map.getTesterCloseRegion().getBounds()) {
                world.setBlockState(blockPos, Blocks.IRON_BARS.getDefaultState());
            }
            world.playSound(null, plr.getBlockPos(), SoundEvents.BLOCK_IRON_DOOR_CLOSE, SoundCategory.BLOCKS, 1, 0.5f);
            gameSpace.getPlayers().sendMessage(Text.translatable("sabotage.tester.message", plr.getName(), 10).formatted(Formatting.YELLOW));
            int revealTime = (int) world.getTime() + 200;
            Consumer<GameSpace> reminder = (gameSpace) -> {
                gameSpace.getPlayers().sendMessage(Text.translatable("sabotage.tester.message", plr.getName(), 5).formatted(Formatting.YELLOW));
            };
            Consumer<GameSpace> reveal = (gameSpace) -> {
                Roles plrRole = getPlayerRole(plr);
                // isTesterOnCooldown is changed in this method
                changeTesterWool(plrRole);
                for (BlockPos blockPos : map.getTesterCloseRegion().getBounds()) {
                    world.setBlockState(blockPos, Blocks.AIR.getDefaultState());
                }
                world.playSound(null, plr.getBlockPos(), SoundEvents.BLOCK_IRON_DOOR_OPEN, SoundCategory.BLOCKS, 1, 0.5f);
            };
            taskScheduler.addTask(new Task(revealTime - 100, reminder));
            taskScheduler.addTask(new Task(revealTime, reveal));
        }
        return true;
    }
    private void awardPlayerKill(ServerPlayerEntity attacker, ServerPlayerEntity plr, Roles plrRole, int innocentKarma, int detectiveKarma, int saboteurKarma) {
        // attacker is confirmed innocent or detective
        switch(plrRole) {
            case INNOCENT -> {
                karmaManager.decrementKarma(attacker, innocentKarma);
                attacker.sendMessage(createAttackerKillMessage(plr, -innocentKarma));
            }

            case DETECTIVE -> {
                karmaManager.decrementKarma(attacker, detectiveKarma);
                attacker.sendMessage(createAttackerKillMessage(plr, -detectiveKarma));
            }

            case SABOTEUR -> {
                karmaManager.incrementKarma(attacker, saboteurKarma);
                attacker.sendMessage(createAttackerKillMessage(plr, saboteurKarma));
            }
        }
    }

    public void Start() {
        gameState = GameStates.ACTIVE;
        pickRoles();
        gameStartedRules(activity);
        getAlivePlayers().forEach(plr -> {
            karmaManager.setKarma(plr, 20);
            plr.setExperiencePoints(plr.getNextLevelExperience() - 1);
        });
        // to-do: chest spawns
    }

    public void End(EndReason endReason) {
        if (endReason == EndReason.NONE) return;
        if (gameState == GameStates.ENDED) return;
        PlayerSet plrs = gameSpace.getPlayers();
        endTime = world.getTime();
        gameState = GameStates.ENDED;
        rules(activity);
        plrs.sendMessage(Text.translatable("sabotage.game_end", Text.literal(getPlayerNamesInSet(initialSaboteurs)).formatted(Formatting.RED)));
        if (endReason == EndReason.INNOCENT_WIN) {
            plrs.sendMessage(Text.translatable(
                    "sabotage.game_end.innocents",
                    Text.translatable("sabotage.innocents").formatted(Formatting.GREEN),
                    Text.translatable("sabotage.detectives").formatted(Formatting.DARK_BLUE),
                    Text.translatable("sabotage.saboteurs").formatted(Formatting.RED)
            ));
        } else if (endReason == EndReason.SABOTEUR_WIN) {
            plrs.sendMessage(Text.translatable(
                    "sabotage.game_end.saboteurs",
                    Text.translatable("sabotage.saboteurs").formatted(Formatting.RED),
                    Text.translatable("sabotage.innocents").formatted(Formatting.GREEN)
            ));
        } else if (endReason == EndReason.TIMEOUT) {
            plrs.sendMessage(Text.translatable("sabotage.game_end.none"));
        }
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
            activity.listen(ReplacePlayerChatEvent.EVENT, game::onChat);
            activity.listen(GamePlayerEvents.REMOVE, game::onPlayerRemove);
            activity.listen(GamePlayerEvents.OFFER, game::onOffer);
            activity.listen(GameActivityEvents.DESTROY, game::onDestroy);
            map.setWorld(world);
            map.generateChests();
            PlayerSet plrs = game.gameSpace.getPlayers();
            plrs.showTitle(Text.literal(Integer.toString(game.config.countdownTime())).formatted(Formatting.GOLD), 20);
            plrs.playSound(SoundEvents.BLOCK_NOTE_BLOCK_HARP.value(), SoundCategory.PLAYERS, 1.0F, 2.0F);
            for (ServerPlayerEntity plr : plrs) {
                game.map.spawnEntity(world, plr);
                game.globalSidebar.addPlayer(plr);
                game.playersOldTeams.put(plr, plr.getScoreboardTeam());
            }

            Scoreboard scoreboard = gameSpace.getServer().getScoreboard();
            scoreboard.addTeam("unknown");
            scoreboard.addTeam("saboteur");
            scoreboard.addTeam("innocent");
            scoreboard.addTeam("detective");
        });
    }

    private boolean onChat(ServerPlayerEntity plr, SignedMessage signedMessage, MessageType.Parameters parameters) {
        if (gameState == GameStates.ACTIVE) {
            if (plr.isSpectator()) {
                dead.sendMessage(Text.literal("<" + plr.getName().getString() + "> ").formatted(Formatting.GRAY).append(signedMessage.getContent().copy().formatted(Formatting.RESET)));
            } else {
                detectives.sendMessage(Text.literal("<" + plr.getName().getString() + "> ").formatted(Formatting.YELLOW).append(signedMessage.getContent().copy().formatted(Formatting.RESET)));
                innocents.sendMessage(Text.literal("<" + plr.getName().getString() + "> ").formatted(Formatting.YELLOW).append(signedMessage.getContent().copy().formatted(Formatting.RESET)));
                saboteurs.sendMessage(Text.literal("<" + plr.getName().getString() + "> ").formatted(getRoleColor(getPlayerRole(plr))).append(signedMessage.getContent().copy().formatted(Formatting.RESET)));
                dead.sendMessage(Text.literal("<" + plr.getName().getString() + "> ").formatted(Formatting.YELLOW).append(signedMessage.getContent().copy().formatted(Formatting.RESET)));
            }
            // I have no idea what this is (or what it does), docs said to use it so I'm using it
            SentMessage.of(signedMessage);
            return true;
        }
        return false;
    }

    private ActionResult onDeath(ServerPlayerEntity plr, DamageSource damageSource) {
        // remove player from team
        changeTeam(plr, plr, TeamS2CPacket.Operation.REMOVE);
        for (ServerPlayerEntity player : getAlivePlayers()) {
            changeTeam(plr, player, TeamS2CPacket.Operation.REMOVE);
            changeTeam(player, plr, TeamS2CPacket.Operation.REMOVE);
        }

        if (gameState == GameStates.ENDED) {
            return ActionResult.FAIL;
        }

        Entity entityAttacker = damageSource.getAttacker();
        Roles plrRole = getPlayerRole(plr);
        plr.changeGameMode(GameMode.SPECTATOR);
        if (entityAttacker instanceof ServerPlayerEntity attacker) {
            Roles attackerRole = getPlayerRole(attacker);
            // surely there's a better way to do this..
            switch(attackerRole) {
                case SABOTEUR -> {
                    SaboteurConfig config = this.config.saboteurConfig();
                    switch(plrRole) {
                        case INNOCENT -> {
                            karmaManager.incrementKarma(attacker, config.innocentKarmaAward());
                            attacker.sendMessage(createAttackerKillMessage(plr, config.innocentKarmaAward()));
                        }

                        case DETECTIVE -> {
                            karmaManager.incrementKarma(attacker, config.detectiveKarmaAward());
                            attacker.sendMessage(createAttackerKillMessage(plr, config.detectiveKarmaAward()));
                        }

                        case SABOTEUR -> {
                            karmaManager.decrementKarma(attacker, config.saboteurKarmaPenalty());
                            attacker.sendMessage(createAttackerKillMessage(plr, -config.saboteurKarmaPenalty()));
                        }
                    }
                }

                case DETECTIVE -> {
                    DetectiveConfig config = this.config.detectiveConfig();
                    awardPlayerKill(attacker, plr, plrRole, config.innocentKarmaPenalty(), config.detectiveKarmaPenalty(), config.saboteurKarmaAward());
                }

                case INNOCENT -> {
                    InnocentConfig config = this.config.innocentConfig();
                    awardPlayerKill(attacker, plr, plrRole, config.innocentKarmaPenalty(), config.detectiveKarmaPenalty(), config.saboteurKarmaAward());
                }
            }
        }

        if (plrRole == Roles.SABOTEUR) {
            saboteurs.remove(plr);
        } else if (plrRole == Roles.DETECTIVE) {
            detectives.remove(plr);
        } else if (plrRole == Roles.INNOCENT) {
            innocents.remove(plr);
        }

        EndReason endReason = checkWinCondition();
        if (endReason != EndReason.NONE) {
            End(endReason);
        } else {
            MutablePlayerSet plrs = gameSpace.getPlayers().copy(gameSpace.getServer());
            if (entityAttacker instanceof ServerPlayerEntity attacker) {
                plrs.remove(attacker);
            }

            plrs.sendMessage(Text.translatable("sabotage.kill_message", plr.getName(), plrs.size()).formatted(Formatting.YELLOW));
        }
        dead.add(plr);
        return ActionResult.FAIL;
    }

    private PlayerOfferResult onOffer(PlayerOffer offer) {
        ServerPlayerEntity plr = offer.player();
        return offer.accept(this.world, new Vec3d(0.0, 66.0, 0.0)).and(() -> {
            // player joined after game start, so they're technically dead
            plr.changeGameMode(GameMode.SPECTATOR);
            dead.add(plr);
        });
    }

    private void onDestroy(GameCloseReason gameCloseReason) {
        Sabotage.activeGames.remove(this);
        Scoreboard scoreboard = gameSpace.getServer().getScoreboard();
        scoreboard.removeTeam(scoreboard.getTeam("unknown"));
        scoreboard.removeTeam(scoreboard.getTeam("innocent"));
        scoreboard.removeTeam(scoreboard.getTeam("detective"));
        scoreboard.removeTeam(scoreboard.getTeam("saboteur"));
    }

    private void onPlayerRemove(ServerPlayerEntity plr) {
        // remove player from team
        changeTeam(plr, plr, TeamS2CPacket.Operation.REMOVE);
        for (ServerPlayerEntity player : getAlivePlayers()) {
            changeTeam(plr, player, TeamS2CPacket.Operation.REMOVE);
            changeTeam(player, plr, TeamS2CPacket.Operation.REMOVE);
        }
        Team oldTeam = playersOldTeams.get(plr);
        if (oldTeam != null) {
            plr.networkHandler.sendPacket(TeamS2CPacket.changePlayerTeam(oldTeam, plr.getGameProfile().getName(), TeamS2CPacket.Operation.ADD));
        }
        Roles role = getPlayerRole(plr);
        if (role == Roles.SABOTEUR) {
            saboteurs.remove(plr);
        } else if (role == Roles.DETECTIVE) {
            detectives.remove(plr);
        } else if (role == Roles.INNOCENT) {
            innocents.remove(plr);
        }
        if (gameState != GameStates.ENDED) {
            if (role != Roles.NONE) {
                EndReason endReason = checkWinCondition();
                if (endReason != EndReason.NONE) {
                    End(endReason);
                } else {
                    PlayerSet plrs = getAlivePlayers();
                    plrs.sendMessage(Text.translatable("sabotage.kill_message", plr.getName(), plrs.size() - 1).formatted(Formatting.YELLOW));
                }
            }
        }
    }

    public void onTick() {
        long time = world.getTime();
        taskScheduler.onTick();
        switch(gameState) {
            case COUNTDOWN -> {
                if (time % 20 == 0) {
                    // second has passed
                    PlayerSet plrs = gameSpace.getPlayers();
                    int secondsSinceStart = (int) Math.floor((time / 20) - (startTime / 20));
                    int countdownTime = config.countdownTime();
                    if (secondsSinceStart >= countdownTime) {
                        gameState = GameStates.GRACE_PERIOD;
                        plrs.playSound(SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE);
                        plrs.sendMessage(Text.translatable("sabotage.game_start", config.gracePeriod()).formatted(Formatting.YELLOW));
                    } else {
                        plrs.showTitle(Text.literal(Integer.toString(countdownTime - secondsSinceStart)).formatted(Formatting.GOLD), 20);
                        plrs.playSound(SoundEvents.BLOCK_NOTE_BLOCK_HARP.value(), SoundCategory.PLAYERS, 1.0F, 2.0F);
                    }
                }
                // Make sure players don't move during countdown
                for (ServerPlayerEntity plr : getAlivePlayers()) {
                    Vec3d pos = map.getPlayerSpawns().get(new PlayerRef(plr.getUuid()));
                    // Set X and Y as relative so it will send 0 change when we pass yaw (yaw - yaw = 0) and pitch
                    Set<PositionFlag> flags = ImmutableSet.of(PositionFlag.X_ROT, PositionFlag.Y_ROT);
                    // Teleport without changing the pitch and yaw
                    plr.networkHandler.requestTeleport(pos.getX(), pos.getY(), pos.getZ(), plr.getYaw(), plr.getPitch(), flags);
                }
            }

            case GRACE_PERIOD -> {
                int secondsSinceStart = (int) Math.floor((time / 20) - (startTime / 20)) - config.countdownTime();
                int gracePeriod = config.gracePeriod();
                if (secondsSinceStart >= gracePeriod) {
                    Start();
                } else {
                    int secondsLeft = gracePeriod - secondsSinceStart;
                    this.globalSidebar.setLine(SidebarLine.create(0, Text.translatable("sabotage.sidebar.grace_period." + ((secondsLeft == 1) ? "singular" : "plural"), secondsLeft)));
                }
            }

            case ACTIVE -> {
                if (time % 20 == 0) {
                    // second has passed
                    double timePassed = Math.floor((world.getTime() / 20) - (startTime / 20)) - config.countdownTime() - config.gracePeriod();
                    int timeLimit = config.timeLimit();
                    if (timePassed >= timeLimit) {
                        End(EndReason.TIMEOUT);
                        return;
                    }
                    updateSidebars();
                    double factor = ((timeLimit - timePassed) / timeLimit);
                    getAlivePlayers().forEach(plr -> {
                        plr.setExperiencePoints((int) (plr.getNextLevelExperience() * factor));
                    });
                }
            }

            case ENDED -> {
                if (time % 20 == 0) {
                    // second has passed
                    double timePassed = world.getTime() / 20 - endTime / 20;
                    if (timePassed >= config.endDelay()) {
                        gameSpace.close(GameCloseReason.FINISHED);
                    }
                }
            }
            default -> {
                // unknown state, close game.
                gameSpace.close(GameCloseReason.ERRORED);
            }
        }
    }
}

