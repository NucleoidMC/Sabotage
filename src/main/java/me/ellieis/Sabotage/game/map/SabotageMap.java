package me.ellieis.Sabotage.game.map;

import me.ellieis.Sabotage.game.config.SabotageConfig;
import me.ellieis.Sabotage.game.custom.blocks.SabotageChest;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.map_templates.TemplateRegion;
import xyz.nucleoid.plasmid.game.GameOpenException;
import xyz.nucleoid.plasmid.game.world.generator.TemplateChunkGenerator;
import xyz.nucleoid.plasmid.util.PlayerRef;

import java.util.*;

import static me.ellieis.Sabotage.game.custom.SabotageBlocks.SABOTAGE_CHEST;
record ChestInfo(BlockPos pos, Direction direction) {

}
public class SabotageMap {
    private final SabotageConfig config;
    private final MapTemplate template;
    private final List<TemplateRegion> spawns;
    private final List<ChestInfo> chestSpawns;
    private final Map<PlayerRef, Vec3d> playerSpawnPos = new HashMap<>();
    private ServerWorld world;

    public SabotageMap(MapTemplate template, SabotageConfig config) {
        this.config = config;
        this.template = template;
        this.spawns = template.getMetadata().getRegions("spawn").toList();
        this.chestSpawns = new ArrayList<>();
        if (this.spawns.isEmpty()) {
            throw new GameOpenException(Text.literal("Failed to load spawns"));
        }

        // generate chest positions from placed SabotageChests
        template.getBounds().forEach(blockPos -> {
            BlockState blockState = template.getBlockState(blockPos);
            Block block = blockState.getBlock();
            if (block instanceof SabotageChest) {
                // note to self: make your positions immutable in forEach loops..
                chestSpawns.add(new ChestInfo(blockPos.toImmutable(), blockState.get(Properties.HORIZONTAL_FACING)));
                template.setBlockState(blockPos, Blocks.AIR.getDefaultState());
            }
        });
    }
    public void setWorld(ServerWorld world) {
        this.world = world;
    }
    public void generateChests() {
        Collections.shuffle(chestSpawns);

        // Make sure that the chest count doesn't go over the amount of chest positions
        int chestCount = Math.min(config.chestCount(), chestSpawns.size());
        for (ChestInfo chestInfo : chestSpawns) {
            if (chestCount > 0) {
                chestCount--;
                world.setBlockState(chestInfo.pos(), SABOTAGE_CHEST.getDefaultState().with(Properties.HORIZONTAL_FACING, chestInfo.direction()));
            } else {
                break;
            }
        }
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
