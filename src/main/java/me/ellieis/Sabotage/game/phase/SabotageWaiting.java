package me.ellieis.Sabotage.game.phase;

import me.ellieis.Sabotage.game.config.SabotageConfig;
import me.ellieis.Sabotage.game.map.SabotageMap;
import me.ellieis.Sabotage.game.map.SabotageMapBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;

import xyz.nucleoid.plasmid.game.GameActivity;
import xyz.nucleoid.plasmid.game.GameOpenContext;
import xyz.nucleoid.plasmid.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.GameResult;
import xyz.nucleoid.plasmid.game.common.GameWaitingLobby;
import xyz.nucleoid.plasmid.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.game.player.PlayerOffer;
import xyz.nucleoid.plasmid.game.player.PlayerOfferResult;
import xyz.nucleoid.plasmid.game.rule.GameRuleType;

public class SabotageWaiting {
    private final SabotageConfig config;
    private final GameSpace gameSpace;
    private final SabotageMap map;
    private final ServerWorld world;

    public SabotageWaiting(SabotageConfig config, GameSpace gameSpace, SabotageMap map, ServerWorld world) {
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
    }
    public static GameOpenProcedure Open(GameOpenContext<SabotageConfig> context) {
        SabotageConfig config = context.game().config();
        MinecraftServer server = context.server();
        // set up how the world that this minigame will take place in should be constructed
        SabotageMap map = SabotageMapBuilder.build(server, config.getMap());
        RuntimeWorldConfig worldConfig = new RuntimeWorldConfig()
                .setGenerator(map.asChunkGenerator(server))
                .setTimeOfDay(6000);

        return context.openWithWorld(worldConfig, (activity, world) -> {
            SabotageWaiting game = new SabotageWaiting(config, activity.getGameSpace(), map, world);
            GameWaitingLobby.addTo(activity, config.getPlayerConfig());

            rules(activity);
            activity.listen(GamePlayerEvents.OFFER, game::onOffer);
            activity.listen(GameActivityEvents.REQUEST_START, game::requestStart);
        });
    }

    public GameResult requestStart() {
        SabotageActive.Open(this.gameSpace, this.world, this.map, this.config);
        return GameResult.ok();
    }

    private PlayerOfferResult onOffer(PlayerOffer offer) {
        ServerPlayerEntity plr = offer.player();
        return offer.accept(this.world, new Vec3d(0.0, 66.0, 0.0)).and(() -> {
            plr.changeGameMode(GameMode.ADVENTURE);
        });
    }
}
