package me.ellieis.Sabotage.game.map;

import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.gen.chunk.ChunkGenerator;

import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.map_templates.TemplateRegion;
import xyz.nucleoid.plasmid.game.world.generator.TemplateChunkGenerator;

public class SabotageMap {
    private final MapTemplate template;
    private final TemplateRegion spawn;

    public SabotageMap(MapTemplate template) {
        this.template = template;
        this.spawn = template.getMetadata().getFirstRegion("spawn");
    }

    public MapTemplate getTemplate() {
        return this.template;
    }

    public TemplateRegion getSpawn() {
        return this.spawn;
    }

    public void spawnEntity(ServerWorld world, Entity entity) {
        float yaw = this.spawn.getData().getFloat("Rotation");
        Vec3d pos = this.spawn.getBounds().center();
        if (entity instanceof ServerPlayerEntity) {
            ServerPlayerEntity plr = (ServerPlayerEntity) entity;
            plr.teleport(world, pos.getX(), pos.getY(), pos.getZ(), yaw, 0);
        } else {
            entity.teleport(pos.getX(), pos.getY(), pos.getZ());
            entity.setYaw(yaw);
        }
    }

    public ChunkGenerator asChunkGenerator(MinecraftServer server) {
        return new TemplateChunkGenerator(server, this.template);
    }
}
