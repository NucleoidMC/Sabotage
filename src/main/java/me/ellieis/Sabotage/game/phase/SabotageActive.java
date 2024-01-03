package me.ellieis.Sabotage.game.phase;

import me.ellieis.Sabotage.game.SabotageConfig;
import me.ellieis.Sabotage.game.map.SabotageMap;
import net.minecraft.server.world.ServerWorld;
import xyz.nucleoid.plasmid.game.GameActivity;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.game.rule.GameRuleType;

public class SabotageActive {
    private final SabotageConfig config;
    private final GameSpace gameSpace;
    private final SabotageMap map;
    private final ServerWorld world;
    public SabotageActive(SabotageConfig config, GameSpace gameSpace, SabotageMap map, ServerWorld world) {
        this.config = config;
        this.gameSpace = gameSpace;
        this.map = map;
        this.world = world;
    }
    private static void rules(GameActivity activity) {
        activity.allow(GameRuleType.FALL_DAMAGE);
        activity.allow(GameRuleType.INTERACTION);
        activity.allow(GameRuleType.PICKUP_ITEMS);
        activity.allow(GameRuleType.MODIFY_ARMOR);
        activity.allow(GameRuleType.MODIFY_INVENTORY);
        activity.allow(GameRuleType.PVP);
        activity.allow(GameRuleType.THROW_ITEMS);
        activity.deny(GameRuleType.PORTALS);
        activity.deny(GameRuleType.HUNGER);
        activity.deny(GameRuleType.ICE_MELT);
        activity.deny(GameRuleType.PLACE_BLOCKS);
        activity.deny(GameRuleType.FIRE_TICK);
        activity.deny(GameRuleType.BREAK_BLOCKS);
        activity.deny(GameRuleType.CRAFTING);
    }
    public static void Open(GameSpace gameSpace, ServerWorld world, SabotageMap map, SabotageConfig config) {
        gameSpace.setActivity(activity -> {
            SabotageActive game = new SabotageActive(config, gameSpace, map, world);

            rules(activity);
            activity.listen(GameActivityEvents.TICK, game::onTick);
        });
    }

    public void onTick() {

    }
}
