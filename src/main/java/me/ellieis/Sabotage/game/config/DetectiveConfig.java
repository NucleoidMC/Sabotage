package me.ellieis.Sabotage.game.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record DetectiveConfig(int innocentKarmaPenalty, int detectiveKarmaPenalty, int saboteurKarmaAward){
    public static final Codec<DetectiveConfig> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("innocent_karma_penalty").forGetter(DetectiveConfig::innocentKarmaPenalty),
                    Codec.INT.fieldOf("detective_karma_penalty").forGetter(DetectiveConfig::detectiveKarmaPenalty),
                    Codec.INT.fieldOf("saboteur_karma_award").forGetter(DetectiveConfig::saboteurKarmaAward)
            ).apply(instance, DetectiveConfig::new)
    );
}
