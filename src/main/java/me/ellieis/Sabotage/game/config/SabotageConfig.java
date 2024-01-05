package me.ellieis.Sabotage.game.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.game.common.config.PlayerConfig;

public class SabotageConfig {
    private final Identifier map;
    private final int countdownTime;
    private final int gracePeriod;
    private final int timeLimit;
    private final InnocentConfig innocentConfig;
    private final DetectiveConfig detectiveConfig;
    private final SaboteurConfig saboteurConfig;
    private final PlayerConfig playerConfig;
    public static final Codec<SabotageConfig> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
                Identifier.CODEC.fieldOf("map").forGetter(SabotageConfig::getMap),
                Codec.INT.fieldOf("countdown_time").forGetter(SabotageConfig::getCountdownTime),
                Codec.INT.fieldOf("grace_period").forGetter(SabotageConfig::getGracePeriod),
                Codec.INT.fieldOf("time_limit").forGetter(SabotageConfig::getTimeLimit),
                InnocentConfig.CODEC.fieldOf("innocent").forGetter(SabotageConfig::getInnocentConfig),
                DetectiveConfig.CODEC.fieldOf("detective").forGetter(SabotageConfig::getDetectiveConfig),
                SaboteurConfig.CODEC.fieldOf("saboteur").forGetter(SabotageConfig::getSaboteurConfig),
                PlayerConfig.CODEC.fieldOf("players").forGetter(SabotageConfig::getPlayerConfig)
        ).apply(instance, SabotageConfig::new)
    );
    public SabotageConfig(Identifier map, int countdownTime, int gracePeriod, int timeLimit, InnocentConfig innocentConfig, DetectiveConfig detectiveConfig, SaboteurConfig saboteurConfig, PlayerConfig playerConfig) {
        this.map = map;
        this.countdownTime = countdownTime;
        this.gracePeriod = gracePeriod;
        this.timeLimit = timeLimit;
        this.innocentConfig = innocentConfig;
        this.detectiveConfig = detectiveConfig;
        this.saboteurConfig = saboteurConfig;
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

    public InnocentConfig getInnocentConfig() {
        return this.innocentConfig;
    }

    public DetectiveConfig getDetectiveConfig() {
        return this.detectiveConfig;
    }

    public SaboteurConfig getSaboteurConfig() {
        return saboteurConfig;
    }

    public PlayerConfig getPlayerConfig() {
        return this.playerConfig;
    }

    public Identifier getMap() {
        return this.map;
    }
}
