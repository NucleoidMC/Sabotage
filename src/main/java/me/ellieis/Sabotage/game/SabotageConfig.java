package me.ellieis.Sabotage.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;

public class SabotageConfig {
    private final Identifier map;
    public static final Codec<SabotageConfig> CODEC = RecordCodecBuilder.create(instance -> {
        return instance.group(
                Identifier.CODEC.fieldOf("map").forGetter(SabotageConfig::getMap)
        ).apply(instance, SabotageConfig::new);
    });
    public SabotageConfig(Identifier map) {
        this.map = map;
    }
    public Identifier getMap() {
        return this.map;
    }
}
