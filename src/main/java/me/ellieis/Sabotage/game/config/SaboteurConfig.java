package me.ellieis.Sabotage.game.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record SaboteurConfig(int innocentKarmaAward, int detectiveKarmaAward, int saboteurKarmaPenalty){
    public static final Codec<SaboteurConfig> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                Codec.INT.fieldOf("innocent_karma_award").forGetter(SaboteurConfig::innocentKarmaAward),
                Codec.INT.fieldOf("detective_karma_award").forGetter(SaboteurConfig::detectiveKarmaAward),
                Codec.INT.fieldOf("saboteur_karma_penalty").forGetter(SaboteurConfig::saboteurKarmaPenalty)
            ).apply(instance, SaboteurConfig::new)
    );
}
