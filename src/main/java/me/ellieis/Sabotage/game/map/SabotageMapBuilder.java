package me.ellieis.Sabotage.game.map;

import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.map_templates.MapTemplateSerializer;
import xyz.nucleoid.plasmid.game.GameOpenException;

import java.io.IOException;

public class SabotageMapBuilder {
    public static SabotageMap build(MinecraftServer server, Identifier identifier) {
        try {
            MapTemplate template = MapTemplateSerializer.loadFromResource(server, identifier);
            return new SabotageMap(template);
        } catch(IOException exception) {
            throw new GameOpenException(Text.of("Failed to load map " + identifier), exception);
        }
    }
}
