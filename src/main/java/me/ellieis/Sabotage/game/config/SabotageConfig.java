package me.ellieis.Sabotage.game.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.ellieis.Sabotage.Sabotage;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.plasmid.api.game.common.config.PlayerLimiterConfig;
import xyz.nucleoid.plasmid.api.game.common.config.WaitingLobbyConfig;

import java.util.OptionalInt;

public record SabotageConfig(Identifier map, Identifier waitingLobby, boolean proximityTextChat, int time, Identifier dimension, int countdownTime, int gracePeriod, int timeLimit, int endDelay, int startingKarma, int playerChestCount, InnocentConfig innocentConfig, DetectiveConfig detectiveConfig, SaboteurConfig saboteurConfig, WaitingLobbyConfig playerConfig) {
    public static final MapCodec<SabotageConfig> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
                Identifier.CODEC.fieldOf("map").forGetter(SabotageConfig::map),
                Identifier.CODEC.optionalFieldOf("waiting_lobby", Sabotage.identifier("lobby")).forGetter(SabotageConfig::waitingLobby),
                Codec.BOOL.optionalFieldOf("proximity_text_chat", false).forGetter(SabotageConfig::proximityTextChat),
                Codec.INT.optionalFieldOf("time", 6000).forGetter(SabotageConfig::time),
                Identifier.CODEC.optionalFieldOf("dimension", Fantasy.DEFAULT_DIM_TYPE.identifier()).forGetter(SabotageConfig::dimension),
                Codec.INT.optionalFieldOf("countdown_time", 5).forGetter(SabotageConfig::countdownTime),
                Codec.INT.optionalFieldOf("grace_period", 15).forGetter(SabotageConfig::gracePeriod),
                Codec.INT.optionalFieldOf("time_limit", 1200).forGetter(SabotageConfig::timeLimit),
                Codec.INT.optionalFieldOf("end_delay", 10).forGetter(SabotageConfig::endDelay),
                Codec.INT.optionalFieldOf("starting_karma", 20).forGetter(SabotageConfig::startingKarma),
                Codec.INT.optionalFieldOf("player_chest_count", 15).forGetter(SabotageConfig::playerChestCount),
                InnocentConfig.CODEC.optionalFieldOf("innocent", new InnocentConfig(20, 100, 40)).forGetter(SabotageConfig::innocentConfig),
                DetectiveConfig.CODEC.optionalFieldOf("detective", new DetectiveConfig(20, 100, 80, false)).forGetter(SabotageConfig::detectiveConfig),
                SaboteurConfig.CODEC.optionalFieldOf("saboteur", new SaboteurConfig(20, 80, 40)).forGetter(SabotageConfig::saboteurConfig),
                WaitingLobbyConfig.CODEC.optionalFieldOf("players", new WaitingLobbyConfig(new PlayerLimiterConfig(OptionalInt.empty(), true), (FabricLoader.getInstance().isDevelopmentEnvironment() ? 1 : 4), 6, new WaitingLobbyConfig.Countdown(30, 5))).forGetter(SabotageConfig::playerConfig)
        ).apply(instance, SabotageConfig::new)
    );

}
