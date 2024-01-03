package me.ellieis.Sabotage.game.phase;

import eu.pb4.sidebars.api.lines.SidebarLine;
import me.ellieis.Sabotage.game.SabotageConfig;
import me.ellieis.Sabotage.game.map.SabotageMap;
import me.ellieis.Sabotage.game.map.SabotageMapBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.plasmid.game.*;
import xyz.nucleoid.plasmid.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.game.common.widget.SidebarWidget;
import xyz.nucleoid.plasmid.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.game.player.PlayerOffer;
import xyz.nucleoid.plasmid.game.player.PlayerOfferResult;
import xyz.nucleoid.plasmid.game.rule.GameRuleType;
import xyz.nucleoid.stimuli.Stimuli;

public class SabotageWaiting {
    private final SabotageConfig config;
    private final GameSpace gameSpace;
    private final SabotageMap map;
    private final ServerWorld world;
    private SidebarWidget widget;
    private int playerCount;
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
            game.widget = GlobalWidgets.addTo(activity).addSidebar();
            game.widget.setTitle(
                    Text.translatableWithFallback("gameType.sabotage.sabotage", "Sabotage").formatted(Formatting.GOLD)
            );
            game.widget.setLine(SidebarLine.create(1, Text.translatableWithFallback("sabotage.waiting", "Waiting for players.")));
            // line 0 is reserved for player count in this phase.
            // it's only set once a player joins.
            rules(activity);
            activity.listen(GameActivityEvents.TICK, game::onTick);
            activity.listen(GamePlayerEvents.OFFER, game::onOffer);
            activity.listen(GamePlayerEvents.JOIN, game::onJoin);
            activity.listen(GamePlayerEvents.LEAVE, game::onLeave);
        });
    }

    public GameResult requestStart() {
        // to-do: checks if start should happen
        SabotageActive.Open(this.gameSpace, this.world, this.map, this.config);
        return GameResult.ok();
    }

    private void onTick() {

    }

    private void onJoin(ServerPlayerEntity plr) {
        this.map.spawnEntity(this.world, plr);
        this.widget.addPlayer(plr);
        long playerCount = this.gameSpace.getPlayers().stream().count();
        this.widget.setLine(SidebarLine.create(0,  Text.literal(playerCount + "/" +  this.config.getMaxPlayers() + " Players"  )));
        if (playerCount >= 4) {
            //requestStart();
        } else {
        }
    }

    private void onLeave(ServerPlayerEntity _plr) {
        long playerCount = this.gameSpace.getPlayers().stream().count();
        this.widget.setLine(SidebarLine.create(0,  Text.literal(playerCount + "/" +  this.config.getMaxPlayers() + " Players"  )));
    }
    private PlayerOfferResult onOffer(PlayerOffer offer) {
        ServerPlayerEntity plr = offer.player();
        long playerCount = this.gameSpace.getPlayers().stream().count();
        if (playerCount >= this.config.getMaxPlayers()) {
            return offer.reject(Text.translatableWithFallback("sabotage.full", "Game is full."));
        }
        return offer.accept(this.world, new Vec3d(0.0, 66.0, 0.0)).and(() -> {
            plr.changeGameMode(GameMode.ADVENTURE);
        });
    }
}
