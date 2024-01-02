package me.ellieis.Sabotage.game;

import me.ellieis.Sabotage.game.map.SabotageMap;
import me.ellieis.Sabotage.game.map.SabotageMapBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.plasmid.game.GameOpenContext;
import xyz.nucleoid.plasmid.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.game.player.PlayerOffer;
import xyz.nucleoid.plasmid.game.player.PlayerOfferResult;

public class SabotageGame {
    private final SabotageConfig config;
    private final GameSpace gameSpace;
    private final SabotageMap map;
    private final ServerWorld world;
    public SabotageGame(SabotageConfig config, GameSpace gameSpace, SabotageMap map, ServerWorld world) {
        this.config = config;
        this.gameSpace = gameSpace;
        this.map = map;
        this.world = world;
    }
    public static GameOpenProcedure Open(GameOpenContext<SabotageConfig> context) {
        SabotageConfig config = context.game().config();
        MinecraftServer server = context.server();
        // set up how the world that this minigame will take place in should be constructed
        SabotageMap map = SabotageMapBuilder.build(server, new Identifier("sabotage:lobby"));
        RuntimeWorldConfig worldConfig = new RuntimeWorldConfig()
                .setGenerator(map.asChunkGenerator(server))
                .setTimeOfDay(6000);

        return context.openWithWorld(worldConfig, (activity, world) -> {
            SabotageGame game = new SabotageGame(config, activity.getGameSpace(), map, world);
            activity.listen(GamePlayerEvents.OFFER, game::onOffer);
            activity.listen(GamePlayerEvents.JOIN, game::onJoin);
        });
    }

    private void onJoin(ServerPlayerEntity plr) {
        this.map.spawnEntity(this.world, plr);
    }
    private PlayerOfferResult onOffer(PlayerOffer offer) {
        ServerPlayerEntity plr = offer.player();
        return offer.accept(this.world, new Vec3d(0.0, 66.0, 0.0)).and(() -> {
            plr.changeGameMode(GameMode.ADVENTURE);
        });
    }
}
