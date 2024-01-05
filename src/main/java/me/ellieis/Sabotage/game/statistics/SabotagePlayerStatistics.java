package me.ellieis.Sabotage.game.statistics;

import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.game.stats.StatisticKey;

import static me.ellieis.Sabotage.Sabotage.MOD_ID;

public class SabotagePlayerStatistics {
    public static final StatisticKey<Integer> KARMA = StatisticKey.intKey(new Identifier(MOD_ID, "karma"));
    public static final StatisticKey<Integer> CHESTS_OPENED = StatisticKey.intKey(new Identifier(MOD_ID, "chests_opened"));
}
