package me.ellieis.Sabotage.game.phase;

import me.ellieis.Sabotage.game.config.SabotageConfig;
import me.ellieis.Sabotage.game.map.SabotageMapBuilder;
import me.ellieis.Sabotage.game.map.WaitingMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.plasmid.api.game.*;
import xyz.nucleoid.plasmid.api.game.common.GameWaitingLobby;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;
import xyz.nucleoid.plasmid.api.game.player.JoinOffer;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.block.BlockBreakEvent;
import xyz.nucleoid.stimuli.event.block.BlockRandomTickEvent;
import xyz.nucleoid.stimuli.event.entity.EntityDamageEvent;
import xyz.nucleoid.stimuli.event.player.PlayerAttackEntityEvent;

public class SabotageWaiting {
    private final SabotageConfig config;
    private final GameSpace gameSpace;
    private final WaitingMap map;
    private final ServerWorld world;

    public SabotageWaiting(SabotageConfig config, GameSpace gameSpace, WaitingMap map, ServerWorld world) {
        this.config = config;
        this.gameSpace = gameSpace;
        this.map = map;
        this.world = world;
    }
    private static void rules(GameActivity activity) {
        activity.deny(GameRuleType.FALL_DAMAGE);
        activity.deny(GameRuleType.PVP);
        activity.deny(GameRuleType.FLUID_FLOW);
        activity.deny(GameRuleType.USE_BLOCKS);
        activity.deny(GameRuleType.BLOCK_DROPS);
        activity.deny(GameRuleType.BREAK_BLOCKS);
        activity.deny(GameRuleType.CORAL_DEATH);
        activity.deny(GameRuleType.CRAFTING);
        activity.deny(GameRuleType.FIRE_TICK);
        activity.deny(GameRuleType.HUNGER);
        activity.deny(GameRuleType.ICE_MELT);
        activity.deny(GameRuleType.PLACE_BLOCKS);
        activity.deny(GameRuleType.PORTALS);
        activity.listen(EntityDamageEvent.EVENT, (entity, source, amount) -> EventResult.DENY);
    }
    public static GameOpenProcedure Open(GameOpenContext<SabotageConfig> context) {
        SabotageConfig config = context.game().config();
        MinecraftServer server = context.server();
        // set up how the world that this minigame will take place in should be constructed
        WaitingMap map = SabotageMapBuilder.buildWaiting(server, config.waitingLobby(), config);
        RuntimeWorldConfig worldConfig = new RuntimeWorldConfig()
                .setGenerator(map.asChunkGenerator(server));
        return context.openWithWorld(worldConfig, (activity, world) -> {
            SabotageWaiting game = new SabotageWaiting(config, activity.getGameSpace(), map, world);
            GameWaitingLobby.addTo(activity, config.playerConfig());

            rules(activity);
            activity.listen(GamePlayerEvents.OFFER, JoinOffer::accept);
            activity.listen(GamePlayerEvents.ACCEPT, game::acceptPlayer);
            activity.listen(GameActivityEvents.REQUEST_START, game::requestStart);
            activity.listen(PlayerAttackEntityEvent.EVENT, (_plr, _hand, _entity, _result) -> EventResult.DENY);
            activity.listen(BlockRandomTickEvent.EVENT, (_block, _pos, _state) -> EventResult.DENY);
        });
    }

    public GameResult requestStart() {
        SabotageActive.Open(this.gameSpace, this.config);
        gameSpace.getWorlds().remove(this.world);
        return GameResult.ok();
    }
    private JoinAcceptorResult acceptPlayer(JoinAcceptor acceptor) {
        return acceptor.teleport(this.world, Vec3d.ZERO).thenRunForEach(plr -> {
            map.spawnPlayer(world, plr);
            plr.changeGameMode(GameMode.ADVENTURE);
        });
    }
}
