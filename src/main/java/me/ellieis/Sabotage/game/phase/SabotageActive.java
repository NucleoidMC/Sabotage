package me.ellieis.Sabotage.game.phase;

import com.google.common.collect.ImmutableSet;
import eu.pb4.sidebars.api.Sidebar;
import me.ellieis.Sabotage.Sabotage;
import me.ellieis.Sabotage.game.*;
import me.ellieis.Sabotage.game.config.SabotageConfig;
import me.ellieis.Sabotage.game.custom.blocks.TesterSign;
import me.ellieis.Sabotage.game.custom.blocks.WallTesterSign;
import me.ellieis.Sabotage.game.custom.items.DetectiveShears;
import me.ellieis.Sabotage.game.custom.items.ShopItem;
import me.ellieis.Sabotage.game.map.SabotageMap;
import me.ellieis.Sabotage.game.map.SabotageMapBuilder;
import me.ellieis.Sabotage.game.statistics.KarmaManager;
import me.ellieis.Sabotage.game.utils.Task;
import me.ellieis.Sabotage.game.utils.TaskScheduler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import net.minecraft.world.entity.Relative;
import net.minecraft.network.protocol.game.ClientboundTrackedWaypointPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.numbers.BlankFormat;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.TrappedChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.waypoints.TrackedWaypoint;
import net.minecraft.world.waypoints.Waypoint;
import org.joml.Vector3i;
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
import xyz.nucleoid.stimuli.event.item.ItemThrowEvent;
import xyz.nucleoid.stimuli.event.player.PlayerC2SPacketEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDamageEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;
import xyz.nucleoid.stimuli.event.player.ReplacePlayerChatEvent;
import xyz.nucleoid.stimuli.event.world.ExplosionDetonatedEvent;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static me.ellieis.Sabotage.Sabotage.MOD_ID;
import static me.ellieis.Sabotage.Sabotage.SHOP_BUY_PACKET_ID;
import static me.ellieis.Sabotage.game.custom.SabotageItems.DETECTIVE_SHEARS;
import static me.ellieis.Sabotage.game.custom.SabotageItems.SHOP_ITEM;

public class SabotageActive {

    private final SabotageConfig config;
    public final GameSpace gameSpace;
    private final SabotageMap map;
    private final ServerLevel world;
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
    public final TeamManager teamManager;
    private final ChatManager chatManager;
    private final CombatManager combatManager;
    private final HashMap<BlockPos, ServerPlayer> trappedChests = new HashMap<>();
    public final ArrayList<ServerPlayer> testerBypassers = new ArrayList<>();
    public final ArrayList<ServerPlayer> playersWithTracker = new ArrayList<>();
    public SabotageActive(SabotageConfig config, GameSpace gameSpace, SabotageMap map, ServerLevel world, GameActivity activity) {
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

    public ServerLevel getWorld() {
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
            ServerLevel world = plr.level();
            Block block = world.getBlockState(result.getBlockPos()).getBlock();
            // this is possibly the worst code i've written in my life
            if (block instanceof AnvilBlock || block instanceof AbstractFurnaceBlock ||
                    block instanceof StonecutterBlock || block instanceof ChiseledBookShelfBlock ||
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
        for (ServerPlayer plr : plrs) {
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
        long timeLeft = (long) Math.abs(Math.floor((world.getGameTime() / 20) - (startTime / 20)) - config.countdownTime() - config.gracePeriod() - config.timeLimit());
        long minutes = timeLeft / 60;
        String seconds;
        if (timeLeft % 60 > 10) {
            seconds = Long.toString(timeLeft % 60);
        } else {
            seconds = "0" + timeLeft % 60;
        }

        // saboteurs
        saboteurSidebar.set(content -> {
            content.add(CommonComponents.EMPTY);
            content.add(Component.translatable("sabotage.sidebar.role", Component.translatable("sabotage.saboteur").withStyle(ChatFormatting.RED)));
            content.add(Component.translatable("sabotage.sidebar.role.desc", Component.translatable("sabotage.innocents").withStyle(ChatFormatting.GREEN)));
            content.add(CommonComponents.EMPTY);
            content.add(Component.translatable("sabotage.sidebar.time_left", minutes, seconds));
        });

        // detectives
        detectiveSidebar.set(content -> {
            content.add(CommonComponents.EMPTY);
            content.add(Component.translatable("sabotage.sidebar.role", Component.translatable("sabotage.detective").withStyle(ChatFormatting.BLUE)));
            content.add(Component.translatable("sabotage.sidebar.role.desc", Component.translatable("sabotage.saboteurs").withStyle(ChatFormatting.RED)));
            content.add(CommonComponents.EMPTY);
            content.add(Component.translatable("sabotage.sidebar.time_left", minutes, seconds));
        });

        // innocents
        innocentSidebar.set(content -> {
            content.add(CommonComponents.EMPTY);
            content.add(Component.translatable("sabotage.sidebar.role", Component.translatable("sabotage.innocent").withStyle(ChatFormatting.GREEN)));
            content.add(Component.translatable("sabotage.sidebar.role.desc", Component.translatable("sabotage.saboteurs").withStyle(ChatFormatting.RED)));
            content.add(CommonComponents.EMPTY);
            content.add(Component.translatable("sabotage.sidebar.time_left", minutes, seconds));
        });

        // dead
        globalSidebar.set(content -> {
            content.add(CommonComponents.EMPTY);
            content.add(Component.translatable("sabotage.sidebar.dead"));
            content.add(Component.translatable("sabotage.sidebar.dead.desc"));
            content.add(CommonComponents.EMPTY);
            content.add(Component.translatable("sabotage.sidebar.time_left", minutes, seconds));
        });


    }

    public void setSidebars() {
        MutableComponent title = Component.translatable("gameType.sabotage.sabotage").withStyle(ChatFormatting.GOLD);
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

        globalSidebar.setDefaultNumberFormat(BlankFormat.INSTANCE);
        innocentSidebar.setDefaultNumberFormat(BlankFormat.INSTANCE);
        detectiveSidebar.setDefaultNumberFormat(BlankFormat.INSTANCE);
        saboteurSidebar.setDefaultNumberFormat(BlankFormat.INSTANCE);

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
            world.setBlockAndUpdate(testerWool, wool.defaultBlockState());
        }
        taskScheduler.addTask(new Task((int) (world.getGameTime() + 200), (gameSpace) -> {
            for (BlockPos testerWool : map.getTesterWools()) {
                world.setBlockAndUpdate(testerWool, Blocks.WHITE_WOOL.defaultBlockState());
            }
            isTesterOnCooldown = false;
        }));

    }

    private void applyTestingEffects(ServerPlayer plr, boolean localSoundEffects) {
        plr.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100));
        plr.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200));
        plr.level().sendParticles(ParticleTypes.ANGRY_VILLAGER, plr.getX(), plr.getY(), plr.getZ(), 10, 0.5, 0.5 ,0.5, 1);
        if (localSoundEffects) {
            plr.playSound(SoundEvents.ARMOR_EQUIP_IRON.value(), 1, 0.5f);
            for (int i = 1; i <= 20; i++) {
                int finalI = i;
                taskScheduler.addTask(new Task((int) (world.getGameTime() + (10 * i)), (gameSpace) -> {
                    plr.playSound(
                            SoundEvents.NOTE_BLOCK_BASS.value(),
                            1.0f,
                            (float) Math.min((0.5 + (0.05 * finalI)), 1.4));
                }));
            }
        }
    }

    // portable tester on dead bodies
    public void testBody(ServerPlayer plr, LivingEntity entity) {
        BodyResult result = combatManager.getBodyRole(entity);
        if (result.plr() != null) {
            applyTestingEffects(plr, true);
            int revealTime = (int) world.getGameTime() + 200;
            Consumer<GameSpace> func = (gameSpace) -> {
                gameSpace.getPlayers().sendMessage(
                        Component.translatable("sabotage.detective_shears_reveal",
                                result.plr().getName(),
                                Component.translatable("sabotage." + (
                                        (result.role() == Roles.SABOTEUR) ? "saboteur" :
                                                (result.role() == Roles.DETECTIVE) ? "detective" : "innocent")
                                ).withStyle(TeamManager.getRoleColor(result.role()))));
            };
            taskScheduler.addTask(new Task(revealTime, func));
        }
    }

    // portable tester only
    public void testEntity(ServerPlayer plr, LivingEntity entity) {
        if (plr.isSpectator() || entity.isSpectator()) return;
        if (plr.hasEffect(MobEffects.SLOWNESS) || entity.hasEffect(MobEffects.SLOWNESS)) {
            // either player is already testing or being tested, abort
            return;
        }

        Roles role = teamManager.getPlayerRole(plr);
        if (role == Roles.DETECTIVE) {
            if (entity.isAlwaysTicking()) {
                ItemCooldowns manager = plr.getCooldowns();
                ItemStack heldStack = plr.getMainHandItem();
                if (!manager.isOnCooldown(heldStack)) {
                    manager.addCooldown(heldStack, 300);
                    final ServerPlayer playerEntity = (ServerPlayer) entity;
                    applyTestingEffects(plr, true);
                    applyTestingEffects(playerEntity, true);
                    int revealTime = (int) world.getGameTime() + 200;
                    Consumer<GameSpace> func = (gameSpace) -> {
                        Roles plrRole = teamManager.getPlayerRole(playerEntity);
                        gameSpace.getPlayers().sendMessage(
                                Component.translatable("sabotage.detective_shears_reveal",
                                        playerEntity.getName(),
                                        Component.translatable("sabotage." + (
                                                (plrRole == Roles.SABOTEUR) ? "saboteur" :
                                                        (plrRole == Roles.DETECTIVE) ? "detective" : "innocent")
                                        ).withStyle(TeamManager.getRoleColor(plrRole))));
                    };
                    taskScheduler.addTask(new Task(revealTime, func));
                }

            }
        }
    }

    // tester only
    public boolean testEntity(ServerPlayer plr, Vec3 pos) {
        if (plr.isSpectator()) return true;
        if (gameState == GameStates.ACTIVE) {
            if (isTesterOnCooldown) {
                return false;
            }
            isTesterOnCooldown = true;
            plr.randomTeleport(pos.x(), pos.y(), pos.z(), true);
            applyTestingEffects(plr, false);
            for (BlockPos blockPos : map.getTesterCloseRegion().getBounds()) {
                world.setBlockAndUpdate(blockPos, Blocks.IRON_BARS.defaultBlockState());
            }
            world.playSound(null, plr.blockPosition(), SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1, 0.5f);
            gameSpace.getPlayers().sendMessage(Component.translatable("sabotage.tester.message", plr.getName(), 10).withStyle(ChatFormatting.YELLOW));
            int revealTime = (int) world.getGameTime() + 200;
            Consumer<GameSpace> reminder = (gameSpace) -> {
                gameSpace.getPlayers().sendMessage(Component.translatable("sabotage.tester.message", plr.getName(), 5).withStyle(ChatFormatting.YELLOW));
            };
            Consumer<GameSpace> reveal = (gameSpace) -> {
                Roles plrRole = teamManager.getPlayerRole(plr);
                if (testerBypassers.contains(plr)) {
                    plrRole = Roles.INNOCENT;
                    testerBypassers.remove(plr);
                }
                // isTesterOnCooldown is changed in this method
                changeTesterWool(plrRole);
                for (BlockPos blockPos : map.getTesterCloseRegion().getBounds()) {
                    world.setBlockAndUpdate(blockPos, Blocks.AIR.defaultBlockState());
                }
                world.playSound(null, plr.blockPosition(), SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1, 0.5f);
            };

            // sounds for testing
            for (int i = 1; i <= 20; i++) {
                int finalI = i;
                taskScheduler.addTask(new Task((int) (world.getGameTime() + (10 * i)), (gameSpace) -> {
                    world.playSound(null,
                            pos.x(),
                            pos.y(),
                            pos.z(),
                            SoundEvents.NOTE_BLOCK_BASS.value(),
                            SoundSource.BLOCKS,
                            1.0f,
                            (float) Math.min((0.5 + (0.05 * finalI)), 1.4));
                }));
            }
            taskScheduler.addTask(new Task(revealTime - 100, reminder));
            taskScheduler.addTask(new Task(revealTime, reveal));
        }
        return true;
    }

    public void updateWaypoints() {
        for (ServerPlayer plr2 : playersWithTracker) {
            for (ServerPlayer plr : getAlivePlayers()) {
                Vector3i pos = plr.blockPosition().toMutable();
                plr2.connection.send(ClientboundTrackedWaypointPacket.updateWaypointPosition(plr.getUUID(), Waypoint.Icon.NULL, new Vec3i(pos.x, pos.y, pos.z)));
            }
        }
    }

    public void Start() {
        gameState = GameStates.ACTIVE;
        teamManager.pickRoles();
        gameStartedRules(activity);
        getAlivePlayers().forEach(plr -> {
            karmaManager.setKarma(plr, config.startingKarma());
            plr.setExperiencePoints(plr.getXpNeededForNextLevel() - 1);
        });
    }

    public void End(EndReason endReason) {
        if (endReason == EndReason.NONE) return;
        if (gameState == GameStates.ENDED) return;
        PlayerSet plrs = gameSpace.getPlayers();
        taskScheduler.onGameEnd();
        endTime = world.getGameTime();
        gameState = GameStates.ENDED;
        rules(activity);
        plrs.sendMessage(Component.translatable("sabotage.game_end", Component.literal(getPlayerNamesInSet(teamManager.initialSaboteurs)).withStyle(ChatFormatting.RED)));
        if (endReason == EndReason.INNOCENT_WIN) {
            plrs.sendMessage(Component.translatable(
                    "sabotage.game_end.innocents",
                    Component.translatable("sabotage.innocents").withStyle(ChatFormatting.GREEN),
                    Component.translatable("sabotage.detectives").withStyle(ChatFormatting.BLUE),
                    Component.translatable("sabotage.saboteurs").withStyle(ChatFormatting.RED)
            ));
        } else if (endReason == EndReason.SABOTEUR_WIN) {
            plrs.sendMessage(Component.translatable(
                    "sabotage.game_end.saboteurs",
                    Component.translatable("sabotage.saboteurs").withStyle(ChatFormatting.RED),
                    Component.translatable("sabotage.innocents").withStyle(ChatFormatting.GREEN)
            ));
        } else if (endReason == EndReason.TIMEOUT) {
            plrs.sendMessage(Component.translatable("sabotage.game_end.none"));
        }
        plrs.playSound(SoundEvents.PLAYER_LEVELUP);
    }
    public static void Open(GameSpace gameSpace, SabotageConfig config) {
        SabotageMap map = SabotageMapBuilder.buildActive(gameSpace.getServer(), config.map(), config, gameSpace.getPlayers().participants().size());
        RuntimeWorldConfig worldConfig = new RuntimeWorldConfig()
                .setGenerator(map.asChunkGenerator(gameSpace.getServer()))
                .setDimensionType(ResourceKey.create(Registries.DIMENSION_TYPE, config.dimension()))
                .setTimeOfDay(config.time());
        ServerLevel world = gameSpace.getWorlds().add(worldConfig);
        gameSpace.setActivity(activity -> {
            SabotageActive game = new SabotageActive(config, gameSpace, map, world, activity);
            game.startTime = world.getGameTime();
            game.widgets = GlobalWidgets.addTo(activity);
            game.globalSidebar = game.widgets.addSidebar(Component.translatable("gameType.sabotage.sabotage").withStyle(ChatFormatting.GOLD));
            game.globalSidebar.setPriority(Sidebar.Priority.LOW);
            game.globalSidebar.addLines(Component.translatable("sabotage.sidebar.countdown"));
            world.getGameRules().set(GameRules.LOCATOR_BAR, false, gameSpace.getServer());
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
            activity.listen(ItemThrowEvent.EVENT, (plr, slot, stack) -> {
                if (stack.getItem() instanceof ShopItem || stack.getItem() instanceof DetectiveShears) {
                    return EventResult.DENY;
                } else {
                    return EventResult.PASS;
                }
            });
            activity.listen(PlayerC2SPacketEvent.EVENT, (plr, msg) -> {
                if (msg instanceof ServerboundCustomClickActionPacket(
                        net.minecraft.resources.Identifier id, java.util.Optional<net.minecraft.nbt.Tag> payload
                )) {
                    if (id.equals(SHOP_BUY_PACKET_ID)) {
                        game.onShopBuy(plr, payload.get().asString().orElseThrow());
                    }
                }
                return EventResult.PASS;
            });
            map.setWorld(world);
            map.generateChests();
            PlayerSet plrs = game.gameSpace.getPlayers();

            for (ServerPlayer plr : plrs) {
                game.map.spawnPlayer(world, plr);
                game.globalSidebar.addPlayer(plr);
                plr.getInventory().setItem(8, new ItemStack(SHOP_ITEM));
                plr.setSharedFlagOnFire(false);
                plr.setRemainingFireTicks(0);
            }

        });
    }

    private void onPlayerJoin(ServerPlayer plr) {
    }

    private EventResult onEntityUse(ServerPlayer plr, Entity entity, InteractionHand hand, EntityHitResult entityHitResult) {
        ItemStack stack = plr.getItemInHand(hand);
        if (stack.getItem() instanceof DetectiveShears) {
            if (!plr.getCooldowns().isOnCooldown(stack)) {
                if (entity instanceof Mannequin mannequin) {
                    plr.getCooldowns().addCooldown(stack, 300);
                    testBody(plr, mannequin);
                }
            }
        }
        return EventResult.PASS;
    }

    private EventResult onDamage(ServerPlayer plr, DamageSource damageSource, float damageAmount) {
        combatManager.onDamage(plr, damageSource,damageAmount);
        return EventResult.PASS;
    }

    private InteractionResult onBlockUse(ServerPlayer player, InteractionHand hand, BlockHitResult blockHitResult) {
        ItemStack item = player.getItemInHand(hand);
        // TNT ignites on place
        if (item.is(Items.TNT)) {
            BlockPos pos = blockHitResult.getBlockPos().relative(blockHitResult.getDirection());;
            Level world = player.level();
            if (world.getBlockState(pos).getBlock() == Blocks.AIR) {
                world.addFreshEntity(new PrimedTnt(world, pos.getX(), pos.getY(), pos.getZ(), player));
                item.shrink(1);
                world.playSound(null, pos, SoundEvents.TNT_PRIMED, SoundSource.BLOCKS);
                return InteractionResult.CONSUME;
            }
        }
        if (item.is(Items.TRAPPED_CHEST)) {
            BlockPos placementPos = blockHitResult.getBlockPos().relative(blockHitResult.getDirection());
            trappedChests.put(placementPos, player);
            item.shrink(1);
            world.setBlockAndUpdate(placementPos, Blocks.TRAPPED_CHEST.getStateForPlacement(new BlockPlaceContext(player, hand, item, blockHitResult)));
            return InteractionResult.SUCCESS;
        }
        // trapped chests explode when interacted
        BlockPos pos = blockHitResult.getBlockPos();
        Vec3 centerPos = pos.getCenter();
        if (world.getBlockState(pos).getBlock() instanceof TrappedChestBlock) {
            world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            world.explode(trappedChests.get(pos), centerPos.x(), centerPos.y(), centerPos.z(), 4, Level.ExplosionInteraction.TNT);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }


    private EventResult onExplosion(Explosion explosion, List<BlockPos> explodedBlocks) {
        int i = 0;
        for (BlockPos blockPos : explodedBlocks) {
            Block block = explosion.level().getBlockState(blockPos).getBlock();
            if (block instanceof TesterSign || block instanceof WallTesterSign) {
                LivingEntity entity = explosion.getIndirectSourceEntity();
                world.playSound(explosion.getDirectSourceEntity(),
                        blockPos,
                        SoundEvents.ANVIL_DESTROY,
                        SoundSource.BLOCKS,
                        1.5f,
                        0.85f);
                if (entity != null && entity.isAlwaysTicking()) {
                    ServerPlayer player = (ServerPlayer) entity;
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
                    player.sendSystemMessage(Component.translatable("sabotage.tester.blew_up", Component.literal("(" + karma + " karma)").withStyle((karma >= 0) ? ChatFormatting.GREEN : ChatFormatting.RED)));

                }
            } else {
                final BlockPos blockPos1 = blockPos;
                final BlockState blockState = world.getBlockState(blockPos);
                if (blockState.getBlock() == Blocks.AIR) {
                    continue;
                }
                i++;
                taskScheduler.addTask(new Task((int) world.getGameTime() + (((100 + (int) (Math.random() *  200)) + (20 * i))),
                        (_gameSpace) -> {
                            world.setBlockAndUpdate(blockPos1, blockState);
                        }
                        ));
            }
        }
        return EventResult.ALLOW;
    }

    private boolean onChat(ServerPlayer plr, PlayerChatMessage signedMessage, ChatType.Bound parameters) {
        if (gameState == GameStates.ACTIVE) {
            if (teamManager.getPlayerRole(plr) != Roles.NONE) {
                chatManager.onChat(plr, signedMessage.decoratedContent());
            }
            if (plr.isSpectator()) {
                teamManager.dead.sendMessage(Component.literal("<" + plr.getName().getString() + "> ").withStyle(ChatFormatting.GRAY).append(signedMessage.decoratedContent().copy().withStyle(ChatFormatting.RESET)));
            }
            // I have no idea what this is (or what it does), docs said to use it so I'm using it
            OutgoingChatMessage.create(signedMessage);
            return true;
        }
        return false;
    }
    private EventResult onDeath(ServerPlayer plr, DamageSource damageSource) {
        for (ServerPlayer plr2 : playersWithTracker) {
            plr2.connection.send(ClientboundTrackedWaypointPacket.removeWaypoint(plr.getUUID()));
        }
        Entity entityAttacker = damageSource.getEntity();
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
            if (entityAttacker instanceof ServerPlayer attacker) {
                plrs.remove(attacker);
            }

            plrs.sendMessage(Component.translatable("sabotage.kill_message", plr.getName(), getAlivePlayers().size()).withStyle(ChatFormatting.YELLOW));
        }
        return EventResult.DENY;
    }

    private void onShopBuy(ServerPlayer plr, String item) {
        Roles role = teamManager.getPlayerRole(plr);
        switch (role) {
            case SABOTEUR:
                switch (item) {
                    case "trapped_chest":
                        if (karmaManager.getKarma(plr) > 20) {
                            plr.getInventory().add(new ItemStack(Items.TRAPPED_CHEST));
                            karmaManager.decrementKarma(plr, 20);
                            plr.sendSystemMessage(Component.translatable("sabotage.shop.trapped_chest.desc"));
                            plr.displayClientMessage(Component.translatable("sabotage.shop.buy_success", Component.literal("(20 Karma)").withStyle(ChatFormatting.BLUE)).withStyle(ChatFormatting.GREEN), true);
                        } else {
                            plr.displayClientMessage(Component.translatable("sabotage.shop.buy_fail").withStyle(ChatFormatting.RED), true);
                        }
                        break;
                    case "tester_bypass":
                        if (karmaManager.getKarma(plr) > 20) {
                            testerBypassers.add(plr);
                            karmaManager.decrementKarma(plr, 20);
                            plr.sendSystemMessage(Component.translatable("sabotage.shop.tester_bypass.desc"));
                            plr.displayClientMessage(Component.translatable("sabotage.shop.buy_success", Component.literal("(20 Karma)").withStyle(ChatFormatting.BLUE)).withStyle(ChatFormatting.GREEN), true);
                        } else {
                            plr.displayClientMessage(Component.translatable("sabotage.shop.buy_fail").withStyle(ChatFormatting.RED), true);
                        }
                        break;
                }
                break;
            case DETECTIVE:
                switch (item) {
                    case "tester_recharge":
                        if (karmaManager.getKarma(plr) > 10) {
                            Inventory inventory = plr.getInventory();
                            int slot = -1;
                            for (int i = 0; i < 36; i++) {
                                if (inventory.getItem(i).getItem() instanceof DetectiveShears) {
                                    slot = i;
                                    break;
                                }
                            }
                            if (slot == -1) {
                                plr.displayClientMessage(Component.translatable("sabotage.shop.tester_recharge.fail").withStyle(ChatFormatting.RED), true);
                                break;
                            }
                            ItemStack stack = inventory.getItem(slot);
                            if (stack.getDamageValue() >= 50) {
                                stack.setDamageValue(stack.getDamageValue() - 50);
                                plr.displayClientMessage(Component.translatable("sabotage.shop.buy_success", Component.literal("(20 Karma)").withStyle(ChatFormatting.BLUE)).withStyle(ChatFormatting.GREEN), true);
                                plr.sendSystemMessage(Component.translatable("sabotage.shop.tester_recharge.desc"));
                                karmaManager.decrementKarma(plr, 10);
                            } else {
                                plr.displayClientMessage(Component.translatable("sabotage.shop.tester_recharge.full").withStyle(ChatFormatting.RED), true);
                            }
                        } else {
                            plr.displayClientMessage(Component.translatable("sabotage.shop.buy_fail").withStyle(ChatFormatting.RED), true);
                        }
                        break;
                    case "player_tracker":
                        if (karmaManager.getKarma(plr) > 20) {
                            playersWithTracker.add(plr);
                            for (ServerPlayer alive : getAlivePlayers()) {
                                Vector3i pos = alive.blockPosition().toMutable();
                                plr.connection.send(ClientboundTrackedWaypointPacket.addWaypointPosition(alive.getUUID(), Waypoint.Icon.NULL, new Vec3i(pos.x, pos.y, pos.z)));
                            }
                            plr.displayClientMessage(Component.translatable("sabotage.shop.buy_success", Component.literal("(20 Karma)").withStyle(ChatFormatting.BLUE)).withStyle(ChatFormatting.GREEN), true);
                            plr.sendSystemMessage(Component.translatable("sabotage.shop.tracker.desc"));
                            karmaManager.decrementKarma(plr, 20);
                        } else {
                            plr.displayClientMessage(Component.translatable("sabotage.shop.buy_fail").withStyle(ChatFormatting.RED), true);
                        }
                        break;
                }
                break;
            case INNOCENT:
                switch (item) {
                    case "wooden_spear":
                        if (karmaManager.getKarma(plr) > 30) {
                            ItemStack spear = new ItemStack(Items.WOODEN_SPEAR);
                            spear.setDamageValue(spear.getMaxDamage() - 1);
                            plr.displayClientMessage(Component.translatable("sabotage.shop.buy_success", Component.literal("(30 Karma)").withStyle(ChatFormatting.BLUE)).withStyle(ChatFormatting.GREEN), true);
                            plr.sendSystemMessage(Component.translatable("sabotage.shop.wooden_spear.desc"));
                            plr.getInventory().add(spear);
                            karmaManager.decrementKarma(plr, 30);
                        } else {
                            plr.displayClientMessage(Component.translatable("sabotage.shop.buy_fail").withStyle(ChatFormatting.RED), true);
                        }
                        break;
                    case "player_tracker":
                        if (karmaManager.getKarma(plr) > 20) {
                            playersWithTracker.add(plr);
                            for (ServerPlayer alive : getAlivePlayers()) {
                                Vector3i pos = alive.blockPosition().toMutable();
                                plr.connection.send(ClientboundTrackedWaypointPacket.addWaypointPosition(alive.getUUID(), Waypoint.Icon.NULL, new Vec3i(pos.x, pos.y, pos.z)));
                            }
                            plr.displayClientMessage(Component.translatable("sabotage.shop.buy_success", Component.literal("(20 Karma)").withStyle(ChatFormatting.BLUE)).withStyle(ChatFormatting.GREEN), true);
                            plr.sendSystemMessage(Component.translatable("sabotage.shop.tracker.desc"));
                            karmaManager.decrementKarma(plr, 20);
                        } else {
                            plr.displayClientMessage(Component.translatable("sabotage.shop.buy_fail").withStyle(ChatFormatting.RED), true);
                        }
                        break;
                }
                break;
        }
    }

    private JoinAcceptorResult onAccept(JoinAcceptor acceptor) {
        return acceptor.teleport(this.world, new Vec3(0, 66, 0)).thenRunForEach((plr) -> {
            // player joined after game start, so they're technically dead
            plr.setGameMode(GameType.SPECTATOR);
            globalSidebar.addPlayer(plr);
            teamManager.dead.add(plr);
        });
    }

    private void onDestroy(GameCloseReason gameCloseReason) {
        Sabotage.activeGames.remove(this);
    }

    private void onPlayerRemove(ServerPlayer plr) {
        for (ServerPlayer plr2 : playersWithTracker) {
            plr2.connection.send(ClientboundTrackedWaypointPacket.removeWaypoint(plr.getUUID()));
        }
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
        plr.setGameMode(GameType.SPECTATOR);
        if (gameState != GameStates.ENDED) {
            if (role != Roles.NONE) {
                EndReason endReason = checkWinCondition();
                if (endReason != EndReason.NONE) {
                    End(endReason);
                } else {
                    PlayerSet plrs = getAlivePlayers();
                    plrs.sendMessage(Component.translatable("sabotage.kill_message", plr.getName(), plrs.size() - 1).withStyle(ChatFormatting.YELLOW));
                }
            }
        }
    }
    private void preventPlayerMovement() {
        // Make sure players don't move during countdown
        for (ServerPlayer plr : getAlivePlayers()) {
            Vec3 pos = map.getPlayerSpawns().get(new PlayerRef(plr.getUUID()));
            // Set X and Y as relative so it will send 0 change when we pass yaw (yaw - yaw = 0) and pitch
            Set<Relative> flags = ImmutableSet.of(Relative.X_ROT, Relative.Y_ROT);

            // Teleport without changing the pitch and yaw
            plr.teleportTo(plr.level(), pos.x(), pos.y(), pos.z(), flags, 0, 0, false);
        }
    }
    public void onTick(PlayerSet plrs) {
        long time = world.getGameTime();
        taskScheduler.onTick();
        chatManager.onTick();
        switch(gameState) {
            // 3 second buffer period for players to load
            case LOBBY_WAITING -> {
                plrs.sendActionBar(Component.translatable("sabotage.waiting"));

                if (time % 20 == 0) {
                    for (ServerPlayer plr : plrs) {
                        plr.forceAddEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false), plr);
                    }

                    int secondsSinceStart = (int) Math.floor((time / 20) - (startTime / 20));
                    if (secondsSinceStart >= 3) {
                        startTime = time;
                        gameState = GameStates.COUNTDOWN;
                        plrs.sendActionBar(Component.literal(""), 0, 0, 0);
                        plrs.showTitle(Component.literal(Integer.toString(config.countdownTime())).withStyle(ChatFormatting.GOLD), 20);
                        plrs.playSound(SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.PLAYERS, 1.0F, 2.0F);
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
                        plrs.playSound(SoundEvents.RESPAWN_ANCHOR_CHARGE);
                        plrs.sendMessage(Component.translatable("sabotage.game_start", config.gracePeriod()).withStyle(ChatFormatting.YELLOW));
                    } else {
                        for (ServerPlayer plr : plrs) {
                            plr.forceAddEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false), plr);
                        }
                        plrs.showTitle(Component.literal(Integer.toString(countdownTime - secondsSinceStart)).withStyle(ChatFormatting.GOLD), 20);
                        plrs.playSound(SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.PLAYERS, 1.0F, 2.0F);
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
                    this.globalSidebar.set(content -> content.add(Component.translatable("sabotage.sidebar.grace_period." + ((secondsLeft == 1) ? "singular" : "plural"), secondsLeft)));
                }
            }

            case ACTIVE -> {
                if (time % 20 == 0) {
                    // second has passed
                    double timePassed = Math.floor((world.getGameTime() / 20) - (startTime / 20)) - config.countdownTime() - config.gracePeriod();
                    int timeLimit = config.timeLimit();
                    if (timePassed >= timeLimit) {
                        End(EndReason.TIMEOUT);
                        return;
                    }
                    updateSidebars();
                    updateWaypoints();
                    double factor = ((timeLimit - timePassed) / timeLimit);
                    getAlivePlayers().forEach(plr -> {
                        if (!playersWithTracker.contains(plr)) {
                            plr.setExperiencePoints((int) (plr.getXpNeededForNextLevel() * factor));
                        }
                    });
                }
            }

            case ENDED -> {
                if (time % 20 == 0) {
                    // second has passed
                    double timePassed = world.getGameTime() / 20 - endTime / 20;
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

