package me.ellieis.Sabotage.game.statistics;

import net.minecraft.server.network.ServerPlayerEntity;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.stats.GameStatisticBundle;

import static me.ellieis.Sabotage.Sabotage.MOD_ID;
import static me.ellieis.Sabotage.game.statistics.SabotagePlayerStatistics.KARMA;

public class KarmaManager {
    private final GameSpace gameSpace;
    private final GameStatisticBundle stats;
    public KarmaManager(GameSpace gameSpace) {
        this.gameSpace = gameSpace;
        stats = gameSpace.getStatistics().bundle(MOD_ID);
    }
    public int getKarma(ServerPlayerEntity plr) {
        return stats.forPlayer(plr).get(KARMA, 20);
    }
    public void setKarma(ServerPlayerEntity plr, int karma) {
        stats.forPlayer(plr).set(KARMA, karma);
        plr.setExperienceLevel(karma);
        if (karma <= 0) {
            plr.kill();
        }
    }
    public void incrementKarma(ServerPlayerEntity plr, int karma) {
        setKarma(plr, getKarma(plr) + karma);
    }
    public void decrementKarma(ServerPlayerEntity plr, int karma) {
        setKarma(plr, getKarma(plr) - karma);
    }
}
