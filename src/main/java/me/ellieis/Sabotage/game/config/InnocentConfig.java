package me.ellieis.Sabotage.game.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record InnocentConfig(int innocentKarmaPenalty, int detectiveKarmaPenalty, int saboteurKarmaAward){
    public static final Codec<InnocentConfig> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("innocent_karma_penalty").forGetter(InnocentConfig::innocentKarmaPenalty),
                    Codec.INT.fieldOf("detective_karma_penalty").forGetter(InnocentConfig::detectiveKarmaPenalty),
                    Codec.INT.fieldOf("saboteur_karma_award").forGetter(InnocentConfig::saboteurKarmaAward)
            ).apply(instance, InnocentConfig::new)
    );
}
