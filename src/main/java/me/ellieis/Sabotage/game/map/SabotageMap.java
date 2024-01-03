package me.ellieis.Sabotage.game.map;

import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.gen.chunk.ChunkGenerator;

import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.map_templates.TemplateRegion;
import xyz.nucleoid.plasmid.game.GameOpenException;
import xyz.nucleoid.plasmid.game.world.generator.TemplateChunkGenerator;
import xyz.nucleoid.plasmid.util.PlayerRef;

import java.util.*;
import java.util.stream.Stream;

public class SabotageMap {
    private final MapTemplate template;
    private final Stream<TemplateRegion> spawns;
    private final Map<PlayerRef, Vec3d> playerSpawnPos = new HashMap<>();

    public SabotageMap(MapTemplate template) {
        this.template = template;
        this.spawns = template.getMetadata().getRegions("spawn");
        if (this.spawns == null) {
            throw new GameOpenException(Text.literal("Failed to load spawns"));
        }
    }

    public MapTemplate getTemplate() {
        return this.template;
    }

    public Stream<TemplateRegion> getSpawns() {
        return this.spawns;
    }

    public Map<PlayerRef, Vec3d> getPlayerSpawns() {
        return this.playerSpawnPos;
    }

    public void spawnEntity(Entity entity) {
        List<TemplateRegion> spawnList = spawns.toList();
        TemplateRegion spawn = spawnList.get(new Random().nextInt(spawnList.size()));
        Vec3d pos = spawn.getBounds().centerBottom();
        entity.teleport(pos.getX(), pos.getY(), pos.getZ());
        entity.setYaw(spawn.getData().getFloat("Rotation"));
    }

    public void spawnEntity(ServerWorld world, ServerPlayerEntity plr) {
        List<TemplateRegion> spawnList = spawns.toList();
        TemplateRegion spawn = spawnList.get(new Random().nextInt(spawnList.size()));
        Vec3d pos = spawn.getBounds().centerBottom();
        plr.teleport(world, pos.getX(), pos.getY(), pos.getZ(), spawn.getData().getFloat("Rotation"), 0);
        this.playerSpawnPos.put(new PlayerRef(plr.getUuid()), pos);
    }

    public ChunkGenerator asChunkGenerator(MinecraftServer server) {
        return new TemplateChunkGenerator(server, this.template);
    }
}
