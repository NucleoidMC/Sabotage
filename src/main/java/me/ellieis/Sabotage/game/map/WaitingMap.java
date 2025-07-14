package me.ellieis.Sabotage.game.map;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.map_templates.TemplateRegion;
import xyz.nucleoid.plasmid.api.game.GameOpenException;
import xyz.nucleoid.plasmid.api.game.world.generator.TemplateChunkGenerator;

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
            throw new GameOpenException(Text.literal("Failed to load spawns, as there aren't any."));
        }
    }

    public List<TemplateRegion> getSpawns() {
        return this.spawns;
    }

    public void spawnPlayer(ServerWorld world, ServerPlayerEntity plr) {
        TemplateRegion spawn = spawns.get(new Random().nextInt(spawns.size()));
        Vec3d pos = spawn.getBounds().sampleBlock(net.minecraft.util.math.random.Random.create()).toCenterPos();
        plr.teleport(world, pos.getX(), pos.getY(), pos.getZ(), new HashSet<>(), spawn.getData().getFloat("Rotation", 0f), 0, true);
        plr.setOnGround(true);
        plr.setVelocity(0,0,0);
    }

    public ChunkGenerator asChunkGenerator(MinecraftServer server) {
        return new TemplateChunkGenerator(server, this.template);
    }
}
