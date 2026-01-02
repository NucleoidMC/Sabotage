package me.ellieis.Sabotage.game.phase;

import com.google.common.collect.ImmutableSet;
import eu.pb4.sidebars.api.Sidebar;
import me.ellieis.Sabotage.Sabotage;
import me.ellieis.Sabotage.game.*;
import me.ellieis.Sabotage.game.config.SabotageConfig;
import me.ellieis.Sabotage.game.custom.blocks.TesterSign;
import me.ellieis.Sabotage.game.custom.blocks.WallTesterSign;
import me.ellieis.Sabotage.game.custom.items.DetectiveShears;
import me.ellieis.Sabotage.game.map.SabotageMap;
import me.ellieis.Sabotage.game.map.SabotageMapBuilder;
import me.ellieis.Sabotage.game.statistics.KarmaManager;
import me.ellieis.Sabotage.game.utils.Task;
import me.ellieis.Sabotage.game.utils.TaskScheduler;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.MannequinEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SentMessage;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.scoreboard.number.BlankNumberFormat;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.rule.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameCloseReason;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.api.game.common.widget.SidebarWidget;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.*;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;
import xyz.nucleoid.plasmid.api.game.stats.GameStatisticBundle;
import xyz.nucleoid.plasmid.api.util.PlayerRef;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.block.BlockRandomTickEvent;
import xyz.nucleoid.stimuli.event.block.BlockUseEvent;
import xyz.nucleoid.stimuli.event.block.FlowerPotModifyEvent;
import xyz.nucleoid.stimuli.event.entity.EntityUseEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDamageEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;
import xyz.nucleoid.stimuli.event.player.ReplacePlayerChatEvent;
import xyz.nucleoid.stimuli.event.world.ExplosionDetonatedEvent;


import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static me.ellieis.Sabotage.Sabotage.MOD_ID;

public class SabotageActive {

    private final SabotageConfig config;
    public final GameSpace gameSpace;
    private final SabotageMap map;
    private final ServerWorld world;
    public final GameStatisticBundle stats;
    private final KarmaManager karmaManager;
    public final TaskScheduler taskScheduler;
    private boolean isTesterOnCooldown = false;
    private long startTime;
    private long endTime;
    private final GameActivity activity;
    public GameStates gameState = GameStates.LOBBY_WAITING;
    private GlobalWidgets widgets;
    private SidebarWidget globalSidebar;
    public SidebarWidget innocentSidebar;
    public SidebarWidget detectiveSidebar;
    public SidebarWidget saboteurSidebar;
    private final TeamManager teamManager;
    private final ChatManager chatManager;
    private final CombatManager combatManager;
    public SabotageActive(SabotageConfig config, GameSpace gameSpace, SabotageMap map, ServerWorld world, GameActivity activity) {
        this.config = config;
        this.gameSpace = gameSpace;
        this.map = map;
        this.world = world;


        this.stats = gameSpace.getStatistics().bundle(MOD_ID);
        this.activity = activity;
        this.karmaManager = new KarmaManager(stats);
        this.taskScheduler = new TaskScheduler(gameSpace, world);
        this.teamManager = new TeamManager(gameSpace, activity, this, config);
        this.chatManager = new ChatManager(gameSpace, config, teamManager);
        this.combatManager = new CombatManager(gameSpace, teamManager, karmaManager, config, this);
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
        activity.deny(GameRuleType.HUNGER);
        activity.deny(GameRuleType.FIRE_TICK);
        activity.deny(GameRuleType.BREAK_BLOCKS);
        activity.deny(GameRuleType.CRAFTING);
        activity.listen(BlockRandomTickEvent.EVENT, (_block, _pos, _state) -> EventResult.DENY);
        activity.listen(FlowerPotModifyEvent.EVENT, ((_plr, _hand, _result) -> EventResult.DENY));
        activity.listen(BlockUseEvent.EVENT, (plr, _hand, result) -> {
            ServerWorld world = plr.getEntityWorld();
            Block block = world.getBlockState(result.getBlockPos()).getBlock();
            // this is possibly the worst code i've written in my life
            if (block instanceof AnvilBlock || block instanceof AbstractFurnaceBlock ||
                    block instanceof StonecutterBlock || block instanceof ChiseledBookshelfBlock ||
                    block instanceof BarrelBlock || block instanceof BedBlock ||
                    block instanceof GrindstoneBlock || block instanceof CraftingTableBlock
            ) {
               return EventResult.DENY.asActionResult();
            }
            return EventResult.PASS.asActionResult();
        });
    }

    private static String getPlayerNamesInSet(PlayerSet plrs) {
        if (plrs == null) {
            return "";
        }
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

    public PlayerSet getAlivePlayers() {
        MutablePlayerSet plrs = gameSpace.getPlayers().participants().copy(gameSpace.getServer());
        plrs.forEach(plr -> {
            if (plr.isSpectator() || teamManager.dead.contains(plr)) {
                plrs.remove(plr);
            }
        });
        return plrs;
    }

    public void updateSidebars() {
        long timeLeft = (long) Math.abs(Math.floor((world.getTime() / 20) - (startTime / 20)) - config.countdownTime() - config.gracePeriod() - config.timeLimit());
        long minutes = timeLeft / 60;
        String seconds;
        if (timeLeft % 60 > 10) {
            seconds = Long.toString(timeLeft % 60);
        } else {
            seconds = "0" + timeLeft % 60;
        }

        // saboteurs
        saboteurSidebar.set(content -> {
            content.add(ScreenTexts.EMPTY);
            content.add(Text.translatable("sabotage.sidebar.role", Text.translatable("sabotage.saboteur").formatted(Formatting.RED)));
            content.add(Text.translatable("sabotage.sidebar.role.desc", Text.translatable("sabotage.innocents").formatted(Formatting.GREEN)));
            content.add(ScreenTexts.EMPTY);
            content.add(Text.translatable("sabotage.sidebar.time_left", minutes, seconds));
        });

        // detectives
        detectiveSidebar.set(content -> {
            content.add(ScreenTexts.EMPTY);
            content.add(Text.translatable("sabotage.sidebar.role", Text.translatable("sabotage.detective").formatted(Formatting.BLUE)));
            content.add(Text.translatable("sabotage.sidebar.role.desc", Text.translatable("sabotage.saboteurs").formatted(Formatting.RED)));
            content.add(ScreenTexts.EMPTY);
            content.add(Text.translatable("sabotage.sidebar.time_left", minutes, seconds));
        });

        // innocents
        innocentSidebar.set(content -> {
            content.add(ScreenTexts.EMPTY);
            content.add(Text.translatable("sabotage.sidebar.role", Text.translatable("sabotage.innocent").formatted(Formatting.GREEN)));
            content.add(Text.translatable("sabotage.sidebar.role.desc", Text.translatable("sabotage.saboteurs").formatted(Formatting.RED)));
            content.add(ScreenTexts.EMPTY);
            content.add(Text.translatable("sabotage.sidebar.time_left", minutes, seconds));
        });

        // dead
        globalSidebar.set(content -> {
            content.add(ScreenTexts.EMPTY);
            content.add(Text.translatable("sabotage.sidebar.dead"));
            content.add(Text.translatable("sabotage.sidebar.dead.desc"));
            content.add(ScreenTexts.EMPTY);
            content.add(Text.translatable("sabotage.sidebar.time_left", minutes, seconds));
        });
    }

    public void setSidebars() {
        MutableText title = Text.translatable("gameType.sabotage.sabotage").formatted(Formatting.GOLD);
        saboteurSidebar = widgets.addSidebar(title);
        saboteurSidebar.setPriority(Sidebar.Priority.MEDIUM);
        detectiveSidebar = widgets.addSidebar(title);
        detectiveSidebar.setPriority(Sidebar.Priority.MEDIUM);
        innocentSidebar = widgets.addSidebar(title);
        innocentSidebar.setPriority(Sidebar.Priority.MEDIUM);

        updateSidebars();
        gameSpace.getPlayers().forEach(plr -> {
            saboteurSidebar.removePlayer(plr);
            detectiveSidebar.removePlayer(plr);
            innocentSidebar.removePlayer(plr);
        });

        globalSidebar.setDefaultNumberFormat(BlankNumberFormat.INSTANCE);
        innocentSidebar.setDefaultNumberFormat(BlankNumberFormat.INSTANCE);
        detectiveSidebar.setDefaultNumberFormat(BlankNumberFormat.INSTANCE);
        saboteurSidebar.setDefaultNumberFormat(BlankNumberFormat.INSTANCE);

        teamManager.saboteurs.forEach(plr -> saboteurSidebar.addPlayer(plr));
        teamManager.detectives.forEach(plr -> detectiveSidebar.addPlayer(plr));
        teamManager.innocents.forEach(plr -> innocentSidebar.addPlayer(plr));
    }

    public EndReason checkWinCondition() {
        if (teamManager.saboteurs.isEmpty()) {
            return EndReason.INNOCENT_WIN;
        } else if (teamManager.innocents.isEmpty() && teamManager.detectives.isEmpty()) {
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

    private void applyTestingEffects(ServerPlayerEntity plr, boolean localSoundEffects) {
        plr.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 100));
        plr.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 200));
        plr.getEntityWorld().spawnParticles(ParticleTypes.ANGRY_VILLAGER, plr.getX(), plr.getY(), plr.getZ(), 10, 0.5, 0.5 ,0.5, 1);
        if (localSoundEffects) {
            plr.playSound(SoundEvents.ITEM_ARMOR_EQUIP_IRON.value(), 1, 0.5f);
            for (int i = 1; i <= 20; i++) {
                int finalI = i;
                taskScheduler.addTask(new Task((int) (world.getTime() + (10 * i)), (gameSpace) -> {
                    plr.playSound(
                            SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(),
                            1.0f,
                            (float) Math.min((0.5 + (0.05 * finalI)), 1.4));
                }));
            }
        }
    }

    // portable tester on dead bodies
    public void testBody(ServerPlayerEntity plr, LivingEntity entity) {
        BodyResult result = combatManager.getBodyRole(entity);
        if (result.plr() != null) {
            applyTestingEffects(plr, true);
            int revealTime = (int) world.getTime() + 200;
            Consumer<GameSpace> func = (gameSpace) -> {
                gameSpace.getPlayers().sendMessage(
                        Text.translatable("sabotage.detective_shears_reveal",
                                result.plr().getName(),
                                Text.translatable("sabotage." + (
                                        (result.role() == Roles.SABOTEUR) ? "saboteur" :
                                                (result.role() == Roles.DETECTIVE) ? "detective" : "innocent")
                                ).formatted(TeamManager.getRoleColor(result.role()))));
            };
            taskScheduler.addTask(new Task(revealTime, func));
        }
    }

    // portable tester only
    public void testEntity(ServerPlayerEntity plr, LivingEntity entity) {
        if (plr.isSpectator() || entity.isSpectator()) return;
        if (plr.hasStatusEffect(StatusEffects.SLOWNESS) || entity.hasStatusEffect(StatusEffects.SLOWNESS)) {
            // either player is already testing or being tested, abort
            return;
        }

        Roles role = teamManager.getPlayerRole(plr);
        if (role == Roles.DETECTIVE) {
            if (entity.isPlayer()) {
                ItemCooldownManager manager = plr.getItemCooldownManager();
                ItemStack heldStack = plr.getMainHandStack();
                if (!manager.isCoolingDown(heldStack)) {
                    manager.set(heldStack, 300);
                    final ServerPlayerEntity playerEntity = (ServerPlayerEntity) entity;
                    applyTestingEffects(plr, true);
                    applyTestingEffects(playerEntity, true);
                    int revealTime = (int) world.getTime() + 200;
                    Consumer<GameSpace> func = (gameSpace) -> {
                        Roles plrRole = teamManager.getPlayerRole(playerEntity);
                        gameSpace.getPlayers().sendMessage(
                                Text.translatable("sabotage.detective_shears_reveal",
                                        playerEntity.getName(),
                                        Text.translatable("sabotage." + (
                                                (plrRole == Roles.SABOTEUR) ? "saboteur" :
                                                        (plrRole == Roles.DETECTIVE) ? "detective" : "innocent")
                                        ).formatted(TeamManager.getRoleColor(plrRole))));
                    };
                    taskScheduler.addTask(new Task(revealTime, func));
                }

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
            plr.teleport(pos.getX(), pos.getY(), pos.getZ(), true);
            applyTestingEffects(plr, false);
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
                Roles plrRole = teamManager.getPlayerRole(plr);
                // isTesterOnCooldown is changed in this method
                changeTesterWool(plrRole);
                for (BlockPos blockPos : map.getTesterCloseRegion().getBounds()) {
                    world.setBlockState(blockPos, Blocks.AIR.getDefaultState());
                }
                world.playSound(null, plr.getBlockPos(), SoundEvents.BLOCK_IRON_DOOR_OPEN, SoundCategory.BLOCKS, 1, 0.5f);
            };

            // sounds for testing
            for (int i = 1; i <= 20; i++) {
                int finalI = i;
                taskScheduler.addTask(new Task((int) (world.getTime() + (10 * i)), (gameSpace) -> {
                    world.playSound(null,
                            pos.getX(),
                            pos.getY(),
                            pos.getZ(),
                            SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(),
                            SoundCategory.BLOCKS,
                            1.0f,
                            (float) Math.min((0.5 + (0.05 * finalI)), 1.4));
                }));
            }
            taskScheduler.addTask(new Task(revealTime - 100, reminder));
            taskScheduler.addTask(new Task(revealTime, reveal));
        }
        return true;
    }

    public void Start() {
        gameState = GameStates.ACTIVE;
        teamManager.pickRoles();
        gameStartedRules(activity);
        getAlivePlayers().forEach(plr -> {
            karmaManager.setKarma(plr, 20);
            plr.setExperiencePoints(plr.getNextLevelExperience() - 1);
        });
    }

    public void End(EndReason endReason) {
        if (endReason == EndReason.NONE) return;
        if (gameState == GameStates.ENDED) return;
        PlayerSet plrs = gameSpace.getPlayers();
        taskScheduler.onGameEnd();
        endTime = world.getTime();
        gameState = GameStates.ENDED;
        rules(activity);
        plrs.sendMessage(Text.translatable("sabotage.game_end", Text.literal(getPlayerNamesInSet(teamManager.initialSaboteurs)).formatted(Formatting.RED)));
        if (endReason == EndReason.INNOCENT_WIN) {
            plrs.sendMessage(Text.translatable(
                    "sabotage.game_end.innocents",
                    Text.translatable("sabotage.innocents").formatted(Formatting.GREEN),
                    Text.translatable("sabotage.detectives").formatted(Formatting.BLUE),
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
        plrs.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP);
    }
    public static void Open(GameSpace gameSpace, SabotageConfig config) {
        SabotageMap map = SabotageMapBuilder.buildActive(gameSpace.getServer(), config.map(), config, gameSpace.getPlayers().participants().size());
        RuntimeWorldConfig worldConfig = new RuntimeWorldConfig()
                .setGenerator(map.asChunkGenerator(gameSpace.getServer()))
                .setDimensionType(RegistryKey.of(RegistryKeys.DIMENSION_TYPE, config.dimension()))
                .setTimeOfDay(config.time());
        ServerWorld world = gameSpace.getWorlds().add(worldConfig);
        gameSpace.setActivity(activity -> {
            SabotageActive game = new SabotageActive(config, gameSpace, map, world, activity);
            game.startTime = world.getTime();
            game.widgets = GlobalWidgets.addTo(activity);
            game.globalSidebar = game.widgets.addSidebar(Text.translatable("gameType.sabotage.sabotage").formatted(Formatting.GOLD));
            game.globalSidebar.setPriority(Sidebar.Priority.LOW);
            game.globalSidebar.addLines(Text.translatable("sabotage.sidebar.countdown"));
            world.getGameRules().setValue(GameRules.LOCATOR_BAR, false, gameSpace.getServer());
            rules(activity);
            activity.listen(GameActivityEvents.TICK, () -> game.onTick(gameSpace.getPlayers()));
            activity.listen(PlayerDeathEvent.EVENT, game::onDeath);
            activity.listen(ReplacePlayerChatEvent.EVENT, game::onChat);
            activity.listen(GamePlayerEvents.JOIN, game::onPlayerJoin);
            activity.listen(GamePlayerEvents.REMOVE, game::onPlayerRemove);
            activity.listen(GamePlayerEvents.OFFER, JoinOffer::accept);
            activity.listen(GamePlayerEvents.ACCEPT, game::onAccept);
            activity.listen(GameActivityEvents.DESTROY, game::onDestroy);
            activity.listen(BlockUseEvent.EVENT, game::onBlockUse);
            activity.listen(ExplosionDetonatedEvent.EVENT, game::onExplosion);
            activity.listen(PlayerDamageEvent.EVENT, game::onDamage);
            activity.listen(EntityUseEvent.EVENT, game::onEntityUse);
            map.setWorld(world);
            map.generateChests();
            PlayerSet plrs = game.gameSpace.getPlayers();

            for (ServerPlayerEntity plr : plrs) {
                game.map.spawnPlayer(world, plr);
                game.globalSidebar.addPlayer(plr);
                plr.setOnFire(false);
                plr.setFireTicks(0);
            }

        });
    }

    private void onPlayerJoin(ServerPlayerEntity plr) {
    }

    private EventResult onEntityUse(ServerPlayerEntity plr, Entity entity, Hand hand, EntityHitResult entityHitResult) {
        ItemStack stack = plr.getStackInHand(hand);
        if (stack.getItem() instanceof DetectiveShears shears) {
            if (!plr.getItemCooldownManager().isCoolingDown(stack)) {
                if (entity instanceof MannequinEntity mannequin) {
                    plr.getItemCooldownManager().set(stack, 300);
                    testBody(plr, mannequin);
                }
            }
        }
        return EventResult.PASS;
    }

    private EventResult onDamage(ServerPlayerEntity plr, DamageSource damageSource, float damageAmount) {
        combatManager.onDamage(plr, damageSource,damageAmount);
        return EventResult.PASS;
    }

    private ActionResult onBlockUse(ServerPlayerEntity player, Hand hand, BlockHitResult blockHitResult) {
        ItemStack item = player.getStackInHand(hand);
        // TNT ignites on place
        if (item.isOf(Items.TNT)) {
            BlockPos pos = blockHitResult.getBlockPos().offset(blockHitResult.getSide());;
            World world = player.getEntityWorld();
            if (world.getBlockState(pos).getBlock() == Blocks.AIR) {
                world.spawnEntity(new TntEntity(world, pos.getX(), pos.getY(), pos.getZ(), player));
                item.decrement(1);
                world.playSound(null, pos, SoundEvents.ENTITY_TNT_PRIMED, SoundCategory.BLOCKS);
                return ActionResult.CONSUME;
            }
        }

        return ActionResult.PASS;
    }


    private EventResult onExplosion(Explosion explosion, List<BlockPos> explodedBlocks) {
        int i = 0;
        for (BlockPos blockPos : explodedBlocks) {
            Block block = explosion.getWorld().getBlockState(blockPos).getBlock();
            if (block instanceof TesterSign || block instanceof WallTesterSign) {
                LivingEntity entity = explosion.getCausingEntity();
                world.playSound(explosion.getEntity(),
                        blockPos,
                        SoundEvents.BLOCK_ANVIL_DESTROY,
                        SoundCategory.BLOCKS,
                        1.5f,
                        0.85f);
                if (entity != null && entity.isPlayer()) {
                    ServerPlayerEntity player = (ServerPlayerEntity) entity;
                    Roles playerRole = teamManager.getPlayerRole(player);
                    int karma;
                    if (playerRole == Roles.SABOTEUR) {
                        karma = config.saboteurConfig().detectiveKarmaAward();
                        karmaManager.incrementKarma(player, karma);
                    } else if (playerRole != Roles.NONE) {
                        karma = -config.innocentConfig().detectiveKarmaPenalty();
                        karmaManager.decrementKarma(player, -karma);
                    } else {
                        karma = 0;
                    }
                    player.sendMessage(Text.translatable("sabotage.tester.blew_up", Text.literal("(" + karma + " karma)").formatted((karma >= 0) ? Formatting.GREEN : Formatting.RED)));

                }
            } else {
                final BlockPos blockPos1 = blockPos;
                final BlockState blockState = world.getBlockState(blockPos);
                if (blockState.getBlock() == Blocks.AIR) {
                    continue;
                }
                i++;
                taskScheduler.addTask(new Task((int) world.getTime() + (((100 + (int) (Math.random() *  200)) + (20 * i))),
                        (_gameSpace) -> {
                            world.setBlockState(blockPos1, blockState);
                        }
                        ));
            }
        }
        return EventResult.ALLOW;
    }

    private boolean onChat(ServerPlayerEntity plr, SignedMessage signedMessage, MessageType.Parameters parameters) {
        if (gameState == GameStates.ACTIVE) {
            if (teamManager.getPlayerRole(plr) != Roles.NONE) {
                chatManager.onChat(plr, signedMessage.getContent());
            }
            if (plr.isSpectator()) {
                teamManager.dead.sendMessage(Text.literal("<" + plr.getName().getString() + "> ").formatted(Formatting.GRAY).append(signedMessage.getContent().copy().formatted(Formatting.RESET)));
            }
            // I have no idea what this is (or what it does), docs said to use it so I'm using it
            SentMessage.of(signedMessage);
            return true;
        }
        return false;
    }
    private EventResult onDeath(ServerPlayerEntity plr, DamageSource damageSource) {
        Entity entityAttacker = damageSource.getAttacker();
        EventResult result = combatManager.onDeath(plr, damageSource);

        if (result != EventResult.PASS) {
            return result;
        }
        plr.getInventory().dropAll();
        EndReason endReason = checkWinCondition();
        if (endReason != EndReason.NONE) {
            End(endReason);
        } else {
            MutablePlayerSet plrs = gameSpace.getPlayers().copy(gameSpace.getServer());
            if (entityAttacker instanceof ServerPlayerEntity attacker) {
                plrs.remove(attacker);
            }

            plrs.sendMessage(Text.translatable("sabotage.kill_message", plr.getName(), getAlivePlayers().size()).formatted(Formatting.YELLOW));
        }
        return EventResult.DENY;
    }

    private JoinAcceptorResult onAccept(JoinAcceptor acceptor) {
        return acceptor.teleport(this.world, new Vec3d(0, 66, 0)).thenRunForEach((plr) -> {
            // player joined after game start, so they're technically dead
            plr.changeGameMode(GameMode.SPECTATOR);
            globalSidebar.addPlayer(plr);
            teamManager.dead.add(plr);
        });
    }

    private void onDestroy(GameCloseReason gameCloseReason) {
        Sabotage.activeGames.remove(this);
    }

    private void onPlayerRemove(ServerPlayerEntity plr) {
        Roles role = teamManager.getPlayerRole(plr);
        if (role == Roles.SABOTEUR) {
            teamManager.saboteurs.remove(plr);
            saboteurSidebar.removePlayer(plr);
        } else if (role == Roles.DETECTIVE) {
            teamManager.detectives.remove(plr);
            detectiveSidebar.removePlayer(plr);
        } else if (role == Roles.INNOCENT) {
            teamManager.innocents.remove(plr);
            innocentSidebar.removePlayer(plr);
        } else {
            teamManager.dead.remove(plr);
        }
        globalSidebar.removePlayer(plr);
        //combatManager.onPlayerLeave(plr);
        // get around alive check by doing this
        plr.changeGameMode(GameMode.SPECTATOR);
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
    private void preventPlayerMovement() {
        // Make sure players don't move during countdown
        for (ServerPlayerEntity plr : getAlivePlayers()) {
            Vec3d pos = map.getPlayerSpawns().get(new PlayerRef(plr.getUuid()));
            // Set X and Y as relative so it will send 0 change when we pass yaw (yaw - yaw = 0) and pitch
            Set<PositionFlag> flags = ImmutableSet.of(PositionFlag.X_ROT, PositionFlag.Y_ROT);

            // Teleport without changing the pitch and yaw
            plr.teleport(plr.getEntityWorld(), pos.getX(), pos.getY(), pos.getZ(), flags, 0, 0, false);
        }
    }
    public void onTick(PlayerSet plrs) {
        long time = world.getTime();
        taskScheduler.onTick();
        chatManager.onTick();
        switch(gameState) {
            // 3 second buffer period for players to load
            case LOBBY_WAITING -> {
                plrs.sendActionBar(Text.translatable("sabotage.waiting"));

                if (time % 20 == 0) {
                    for (ServerPlayerEntity plr : plrs) {
                        plr.setStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 40, 0, false, false), plr);
                    }

                    int secondsSinceStart = (int) Math.floor((time / 20) - (startTime / 20));
                    if (secondsSinceStart >= 3) {
                        startTime = time;
                        gameState = GameStates.COUNTDOWN;
                        plrs.sendActionBar(Text.literal(""), 0, 0, 0);
                        plrs.showTitle(Text.literal(Integer.toString(config.countdownTime())).formatted(Formatting.GOLD), 20);
                        plrs.playSound(SoundEvents.BLOCK_NOTE_BLOCK_HARP.value(), SoundCategory.PLAYERS, 1.0F, 2.0F);
                    }
                }

                preventPlayerMovement();
            }
            case COUNTDOWN -> {
                if (time % 20 == 0) {
                    // second has passed
                    int secondsSinceStart = (int) Math.floor((time / 20) - (startTime / 20));
                    int countdownTime = config.countdownTime();
                    if (secondsSinceStart >= countdownTime) {
                        gameState = GameStates.GRACE_PERIOD;
                        plrs.playSound(SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE);
                        plrs.sendMessage(Text.translatable("sabotage.game_start", config.gracePeriod()).formatted(Formatting.YELLOW));
                    } else {
                        for (ServerPlayerEntity plr : plrs) {
                            plr.setStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 40, 0, false, false), plr);
                        }
                        plrs.showTitle(Text.literal(Integer.toString(countdownTime - secondsSinceStart)).formatted(Formatting.GOLD), 20);
                        plrs.playSound(SoundEvents.BLOCK_NOTE_BLOCK_HARP.value(), SoundCategory.PLAYERS, 1.0F, 2.0F);
                    }
                }

                preventPlayerMovement();
            }

            case GRACE_PERIOD -> {
                int secondsSinceStart = (int) Math.floor((time / 20) - (startTime / 20)) - config.countdownTime();
                int gracePeriod = config.gracePeriod();
                if (secondsSinceStart >= gracePeriod) {
                    Start();
                } else {
                    int secondsLeft = gracePeriod - secondsSinceStart;
                    this.globalSidebar.set(content -> content.add(Text.translatable("sabotage.sidebar.grace_period." + ((secondsLeft == 1) ? "singular" : "plural"), secondsLeft)));
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

