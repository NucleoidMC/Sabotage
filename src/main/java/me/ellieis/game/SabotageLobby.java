package me.ellieis.game;

import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.plasmid.game.GameOpenContext;
import xyz.nucleoid.plasmid.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.game.player.PlayerOffer;
import xyz.nucleoid.plasmid.game.player.PlayerOfferResult;
import xyz.nucleoid.plasmid.game.world.generator.TemplateChunkGenerator;

public class SabotageLobby {
    private final SabotageConfig config;
    private final GameSpace gameSpace;
    private final ServerWorld world;
    public SabotageLobby(SabotageConfig config, GameSpace gameSpace, ServerWorld world) {
        this.config = config;
        this.gameSpace = gameSpace;
        this.world = world;
    }
    public static GameOpenProcedure Open(GameOpenContext<SabotageConfig> context) {
        SabotageConfig config = context.game().config();
        // create a very simple map with a stone block at (0; 64; 0)
        MapTemplate template = MapTemplate.createEmpty();
        template.setBlockState(new BlockPos(0, 64, 0), Blocks.STONE.getDefaultState());

        // create a chunk generator that will generate from this template that we just created
        TemplateChunkGenerator generator = new TemplateChunkGenerator(context.server(), template);

        // set up how the world that this minigame will take place in should be constructed
        RuntimeWorldConfig worldConfig = new RuntimeWorldConfig()
                .setGenerator(generator)
                .setTimeOfDay(6000);

        return context.openWithWorld(worldConfig, (activity, world) -> {
            SabotageLobby game = new SabotageLobby(config, activity.getGameSpace(), world);
            activity.listen(GamePlayerEvents.OFFER, game::onOffer);
        });
    }
    private PlayerOfferResult onOffer(PlayerOffer offer) {
        ServerPlayerEntity plr = offer.player();
        return offer.accept(this.world, new Vec3d(0.0, 66.0, 0.0)).and(() -> {
            plr.changeGameMode(GameMode.ADVENTURE);
        });
    }
}
