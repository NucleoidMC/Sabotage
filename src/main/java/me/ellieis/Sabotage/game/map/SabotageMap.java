package me.ellieis.Sabotage.game.map;

import me.ellieis.Sabotage.game.custom.blocks.SabotageChest;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.map_templates.TemplateRegion;
import xyz.nucleoid.plasmid.game.GameOpenException;
import xyz.nucleoid.plasmid.game.world.generator.TemplateChunkGenerator;
import xyz.nucleoid.plasmid.util.PlayerRef;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class SabotageMap {
    private final MapTemplate template;
    private final List<TemplateRegion> spawns;
    private final Map<PlayerRef, Vec3d> playerSpawnPos = new HashMap<>();

    public SabotageMap(MapTemplate template) {
        this.template = template;
        this.spawns = template.getMetadata().getRegions("spawn").toList();
        if (this.spawns.isEmpty()) {
            throw new GameOpenException(Text.literal("Failed to load spawns"));
        }

        // generate chest positions from placed SabotageChests
        template.getBounds().forEach(blockPos -> {
            Block block = template.getBlockState(blockPos).getBlock();
            if (block instanceof SabotageChest) {
                template.getMetadata().addRegion("chest", BlockBounds.ofBlock(blockPos));
                template.setBlockState(blockPos, Blocks.AIR.getDefaultState());
            }
        });
    }
    public void generateChests() {
        // to-do: generate chests from added chest regions
    }
    public MapTemplate getTemplate() {
        return this.template;
    }

    public List<TemplateRegion> getSpawns() {
        return this.spawns;
    }

    public Map<PlayerRef, Vec3d> getPlayerSpawns() {
        return this.playerSpawnPos;
    }

    public void spawnEntity(Entity entity) {
        TemplateRegion spawn = spawns.get(new Random().nextInt(spawns.size()));
        Vec3d pos = spawn.getBounds().centerBottom();
        entity.teleport(pos.getX(), pos.getY(), pos.getZ());
        entity.setYaw(spawn.getData().getFloat("Rotation"));
    }

    public void spawnEntity(ServerWorld world, ServerPlayerEntity plr) {
        TemplateRegion spawn = spawns.get(new Random().nextInt(spawns.size()));
        Vec3d pos = spawn.getBounds().centerBottom();
        plr.teleport(world, pos.getX(), pos.getY(), pos.getZ(), spawn.getData().getFloat("Rotation"), 0);
        this.playerSpawnPos.put(new PlayerRef(plr.getUuid()), pos);
    }

    public ChunkGenerator asChunkGenerator(MinecraftServer server) {
        return new TemplateChunkGenerator(server, this.template);
    }
}
