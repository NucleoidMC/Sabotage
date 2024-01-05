package me.ellieis.Sabotage.game.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.game.common.config.PlayerConfig;

public record SabotageConfig(Identifier map, int countdownTime, int gracePeriod, int timeLimit, int endDelay, InnocentConfig innocentConfig, DetectiveConfig detectiveConfig, SaboteurConfig saboteurConfig, PlayerConfig playerConfig) {
    public static final Codec<SabotageConfig> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
                Identifier.CODEC.fieldOf("map").forGetter(SabotageConfig::map),
                Codec.INT.fieldOf("countdown_time").forGetter(SabotageConfig::countdownTime),
                Codec.INT.fieldOf("grace_period").forGetter(SabotageConfig::gracePeriod),
                Codec.INT.fieldOf("time_limit").forGetter(SabotageConfig::timeLimit),
                Codec.INT.fieldOf("end_delay").forGetter(SabotageConfig::endDelay),
                InnocentConfig.CODEC.fieldOf("innocent").forGetter(SabotageConfig::innocentConfig),
                DetectiveConfig.CODEC.fieldOf("detective").forGetter(SabotageConfig::detectiveConfig),
                SaboteurConfig.CODEC.fieldOf("saboteur").forGetter(SabotageConfig::saboteurConfig),
                PlayerConfig.CODEC.fieldOf("players").forGetter(SabotageConfig::playerConfig)
        ).apply(instance, SabotageConfig::new)
    );
}
