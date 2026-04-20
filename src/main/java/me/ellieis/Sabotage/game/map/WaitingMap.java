package me.ellieis.Sabotage.game.map;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.chunk.ChunkGenerator;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.map_templates.TemplateRegion;
import xyz.nucleoid.plasmid.api.game.GameOpenException;
import xyz.nucleoid.plasmid.api.game.level.generator.TemplateChunkGenerator;

import java.util.HashSet;
import java.util.List;
import java.util.Random;

public class WaitingMap {
    private final List<TemplateRegion> spawns;
    private final MapTemplate template;

    public WaitingMap(MapTemplate template) {
        this.spawns = template.getMetadata().getRegions("spawn").toList();
        this.template = template;
        if (this.spawns.isEmpty()) {
            throw new GameOpenException(Component.literal("Failed to load spawns, as there aren't any."));
        }
    }

    public List<TemplateRegion> getSpawns() {
        return this.spawns;
    }

    public void spawnPlayer(ServerLevel level, ServerPlayer plr) {
        TemplateRegion spawn = spawns.get(new Random().nextInt(spawns.size()));
        Vec3 pos = spawn.getBounds().centerBottom();
        plr.teleportTo(level, pos.x(), pos.y(), pos.z(), new HashSet<>(), spawn.getData().getFloatOr("Rotation", 0f), 0, true);
        plr.setOnGround(true);
        plr.setDeltaMovement(0,0,0);
    }

    public ChunkGenerator asChunkGenerator(MinecraftServer server) {
        return new TemplateChunkGenerator(server, this.template);
    }
}
