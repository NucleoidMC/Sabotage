package me.ellieis.Sabotage.game.phase;

import me.ellieis.Sabotage.game.config.SabotageConfig;
import me.ellieis.Sabotage.game.map.SabotageMapBuilder;
import me.ellieis.Sabotage.game.map.WaitingMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.GameType;
import xyz.nucleoid.fantasy.RuntimeLevelConfig;
import xyz.nucleoid.plasmid.api.game.*;
import xyz.nucleoid.plasmid.api.game.common.GameWaitingLobby;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;
import xyz.nucleoid.plasmid.api.game.player.JoinOffer;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.block.BlockRandomTickEvent;
import xyz.nucleoid.stimuli.event.entity.EntityDamageEvent;
import xyz.nucleoid.stimuli.event.player.PlayerAttackEntityEvent;

public class SabotageWaiting {
    private final SabotageConfig config;
    private final GameSpace gameSpace;
    private final WaitingMap map;
    private final ServerLevel level;

    public SabotageWaiting(SabotageConfig config, GameSpace gameSpace, WaitingMap map, ServerLevel level) {
        this.config = config;
        this.gameSpace = gameSpace;
        this.map = map;
        this.level = level;
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
        // set up how the level that this minigame will take place in should be constructed
        WaitingMap map = SabotageMapBuilder.buildWaiting(server, config.waitingLobby(), config);
        RuntimeLevelConfig levelConfig = new RuntimeLevelConfig()
                .setGenerator(map.asChunkGenerator(server));
        return context.openWithLevel(levelConfig, (activity, level) -> {
            SabotageWaiting game = new SabotageWaiting(config, activity.getGameSpace(), map, level);
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
        gameSpace.getLevels().remove(this.level);
        return GameResult.ok();
    }
    private JoinAcceptorResult acceptPlayer(JoinAcceptor acceptor) {
        return acceptor.teleport(this.level, Vec3.ZERO).thenRunForEach(plr -> {
            map.spawnPlayer(level, plr);
            plr.setGameMode(GameType.ADVENTURE);
        });
    }
}
