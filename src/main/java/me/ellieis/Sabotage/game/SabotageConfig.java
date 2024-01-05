package me.ellieis.Sabotage.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.game.common.config.PlayerConfig;

public class SabotageConfig {
    private final Identifier map;
    private final int countdownTime;
    private final int gracePeriod;
    private final int timeLimit;
    private final PlayerConfig playerConfig;
    public static final Codec<SabotageConfig> CODEC = RecordCodecBuilder.create(instance -> {
        return instance.group(
                Identifier.CODEC.fieldOf("map").forGetter(SabotageConfig::getMap),
                Codec.INT.fieldOf("countdown_time").forGetter(SabotageConfig::getCountdownTime),
                Codec.INT.fieldOf("grace_period").forGetter(SabotageConfig::getGracePeriod),
                Codec.INT.fieldOf("time_limit").forGetter(SabotageConfig::getTimeLimit),
                PlayerConfig.CODEC.fieldOf("players").forGetter(SabotageConfig::getPlayerConfig)
        ).apply(instance, SabotageConfig::new);
    });
    public SabotageConfig(Identifier map, int countdownTime, int gracePeriod, int timeLimit, PlayerConfig playerConfig) {
        this.map = map;
        this.countdownTime = countdownTime;
        this.gracePeriod = gracePeriod;
        this.timeLimit = timeLimit;
        this.playerConfig = playerConfig;
    }

    public int getCountdownTime() {
        return countdownTime;
    }

    public int getGracePeriod() {
        return gracePeriod;
    }

    public int getTimeLimit() {
        return timeLimit;
    }

    public PlayerConfig getPlayerConfig() {
        return this.playerConfig;
    }

    public Identifier getMap() {
        return this.map;
    }
}
