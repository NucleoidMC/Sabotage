package me.ellieis.Sabotage.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;

public class SabotageConfig {
    private final Identifier map;
    private final int maxPlayers;
    private final int playersRequired;
    public static final Codec<SabotageConfig> CODEC = RecordCodecBuilder.create(instance -> {
        return instance.group(
                Identifier.CODEC.fieldOf("map").forGetter(SabotageConfig::getMap),
                Codec.INT.fieldOf("max_players").forGetter(SabotageConfig::getMaxPlayers),
                Codec.INT.fieldOf("players_required").forGetter(SabotageConfig::getPlayersRequired)
        ).apply(instance, SabotageConfig::new);
    });
    public SabotageConfig(Identifier map, int maxPlayers, int playersRequired) {
        this.map = map;
        this.maxPlayers = maxPlayers;
        this.playersRequired = playersRequired;
    }

    public int getPlayersRequired() {
        return this.playersRequired;
    }

    public int getMaxPlayers() {
        return this.maxPlayers;
    }

    public Identifier getMap() {
        return this.map;
    }
}
