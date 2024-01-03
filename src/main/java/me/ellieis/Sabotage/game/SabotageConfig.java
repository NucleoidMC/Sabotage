package me.ellieis.Sabotage.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.game.common.config.PlayerConfig;

public class SabotageConfig {
    private final Identifier map;
    private final PlayerConfig playerConfig;
    public static final Codec<SabotageConfig> CODEC = RecordCodecBuilder.create(instance -> {
        return instance.group(
                Identifier.CODEC.fieldOf("map").forGetter(SabotageConfig::getMap),
                PlayerConfig.CODEC.fieldOf("players").forGetter(SabotageConfig::getPlayerConfig)
        ).apply(instance, SabotageConfig::new);
    });
    public SabotageConfig(Identifier map, PlayerConfig playerConfig) {
        this.map = map;
        this.playerConfig = playerConfig;
    }

    public PlayerConfig getPlayerConfig() {
        return this.playerConfig;
    }

    public Identifier getMap() {
        return this.map;
    }
}
