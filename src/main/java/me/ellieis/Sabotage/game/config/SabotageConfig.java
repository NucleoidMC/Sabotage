package me.ellieis.Sabotage.game.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.plasmid.game.common.config.PlayerConfig;

public record SabotageConfig(Identifier map, int time, Identifier dimension, int countdownTime, int gracePeriod, int timeLimit, int endDelay, int chestCount, InnocentConfig innocentConfig, DetectiveConfig detectiveConfig, SaboteurConfig saboteurConfig, PlayerConfig playerConfig) {
    public static final Codec<SabotageConfig> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
                Identifier.CODEC.fieldOf("map").forGetter(SabotageConfig::map),
                Codec.INT.optionalFieldOf("time", 6000).forGetter(SabotageConfig::time),
                Identifier.CODEC.optionalFieldOf("dimension", Fantasy.DEFAULT_DIM_TYPE.getValue()).forGetter(SabotageConfig::dimension),
                Codec.INT.optionalFieldOf("countdown_time", 5).forGetter(SabotageConfig::countdownTime),
                Codec.INT.optionalFieldOf("grace_period", 15).forGetter(SabotageConfig::gracePeriod),
                Codec.INT.optionalFieldOf("time_limit", 1200).forGetter(SabotageConfig::timeLimit),
                Codec.INT.optionalFieldOf("end_delay", 10).forGetter(SabotageConfig::endDelay),
                Codec.INT.fieldOf("chest_count").forGetter(SabotageConfig::chestCount),
                InnocentConfig.CODEC.optionalFieldOf("innocent", new InnocentConfig(20, 100, 20)).forGetter(SabotageConfig::innocentConfig),
                DetectiveConfig.CODEC.optionalFieldOf("detective", new DetectiveConfig(20, 100, 100)).forGetter(SabotageConfig::detectiveConfig),
                SaboteurConfig.CODEC.optionalFieldOf("saboteur", new SaboteurConfig(20, 100, 20)).forGetter(SabotageConfig::saboteurConfig),
                PlayerConfig.CODEC.optionalFieldOf("players", new PlayerConfig(4, 64, 6, new PlayerConfig.Countdown(30, 5))).forGetter(SabotageConfig::playerConfig)
        ).apply(instance, SabotageConfig::new)
    );
}
