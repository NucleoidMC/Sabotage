package me.ellieis.Sabotage.game.map;

import me.ellieis.Sabotage.game.config.SabotageConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.map_templates.MapTemplateSerializer;
import xyz.nucleoid.plasmid.api.game.GameOpenException;

import java.io.IOException;

public class SabotageMapBuilder {
    public static SabotageMap buildActive(MinecraftServer server, Identifier identifier, SabotageConfig config, int playerCount) {
        try {
            MapTemplate template = MapTemplateSerializer.loadFromResource(server, identifier);
            return new SabotageMap(template, config, playerCount);
        } catch(IOException exception) {
            throw new GameOpenException(Text.literal("Failed to load map " + identifier), exception);
        }
    }
    public static WaitingMap buildWaiting(MinecraftServer server, Identifier identifier, SabotageConfig config) {
        try {
            MapTemplate template = MapTemplateSerializer.loadFromResource(server, identifier);
            return new WaitingMap(template);
        } catch(IOException exception) {
            throw new GameOpenException(Text.literal("Failed to load waiting lobby " + identifier), exception);
        }
    }
}
