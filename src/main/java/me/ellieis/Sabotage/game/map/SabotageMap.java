package me.ellieis.Sabotage.game.map;

import me.ellieis.Sabotage.game.config.SabotageConfig;
import me.ellieis.Sabotage.game.custom.blocks.SabotageChest;
import me.ellieis.Sabotage.game.custom.blocks.TesterWool;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.chunk.ChunkGenerator;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.map_templates.TemplateRegion;
import xyz.nucleoid.plasmid.api.game.GameOpenException;
import xyz.nucleoid.plasmid.api.game.level.generator.TemplateChunkGenerator;
import xyz.nucleoid.plasmid.api.util.PlayerRef;


import java.util.*;

import static me.ellieis.Sabotage.game.custom.SabotageBlocks.SABOTAGE_CHEST;

public class SabotageMap {
    private final SabotageConfig config;
    private final MapTemplate template;
    private final List<TemplateRegion> spawns;
    private final TemplateRegion testerCloseRegion;
    private final List<BlockPos> testerWools = new ArrayList<>();
    private final List<ChestInfo> chestSpawns = new ArrayList<>();
    private final Map<PlayerRef, Vec3> playerSpawnPos = new HashMap<>();
    private final int playerCount;
    private ServerLevel level;

    public SabotageMap(MapTemplate template, SabotageConfig config, int playerCount) {
        this.config = config;
        this.template = template;
        this.spawns = template.getMetadata().getRegions("spawn").toList();
        this.testerCloseRegion = template.getMetadata().getFirstRegion("tester_close_region");
        this.playerCount = playerCount;

        if (this.spawns.isEmpty()) {
            throw new GameOpenException(Component.literal("Failed to load spawns, as there aren't any."));
        }

        if (this.testerCloseRegion == null) {
            throw new GameOpenException(Component.literal("Failed to load tester close region, as there isn't any"));
        }

        // generate chest positions from placed SabotageChests
        template.getBounds().forEach(blockPos -> {
            BlockState blockState = template.getBlockState(blockPos);
            Block block = blockState.getBlock();
            if (block instanceof SabotageChest) {
                // note to self: make your positions immutable in forEach loops..
                chestSpawns.add(new ChestInfo(blockPos.immutable(), blockState.getValue(BlockStateProperties.HORIZONTAL_FACING)));
                template.setBlockState(blockPos, Blocks.AIR.defaultBlockState());
            } else if (block instanceof TesterWool) {
                testerWools.add(blockPos.immutable());
            }
        });
    }
    public void setLevel(ServerLevel level) {
        this.level = level;
    }

    public void generateChests() {
        Collections.shuffle(chestSpawns);

        // Make sure that the chest count doesn't go over the amount of chest positions
        int chestCount = Math.min(config.playerChestCount() * playerCount, chestSpawns.size());
        for (ChestInfo chestInfo : chestSpawns) {
            if (chestCount > 0) {
                chestCount--;
                level.setBlockAndUpdate(chestInfo.pos(), SABOTAGE_CHEST.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, chestInfo.direction()));
            } else {
                break;
            }
        }
    }

    public List<BlockPos> getTesterWools() {
        return this.testerWools;
    }
    public MapTemplate getTemplate() {
        return this.template;
    }

    public TemplateRegion getTesterCloseRegion() {
        return this.testerCloseRegion;
    }

    public List<TemplateRegion> getSpawns() {
        return this.spawns;
    }

    public Map<PlayerRef, Vec3> getPlayerSpawns() {
        return this.playerSpawnPos;
    }

    public void spawnPlayer(ServerLevel level, ServerPlayer plr) {
        TemplateRegion spawn = spawns.get(new Random().nextInt(spawns.size()));
        Vec3 pos = spawn.getBounds().centerBottom();
        plr.teleportTo(level, pos.x(), pos.y(), pos.z(), new HashSet<>(), spawn.getData().getFloatOr("Rotation", 0f), 0, true);
        plr.setOnGround(true);
        plr.setDeltaMovement(0,0,0);
        this.playerSpawnPos.put(new PlayerRef(plr.getUUID()), pos);
    }

    public ChunkGenerator asChunkGenerator(MinecraftServer server) {
        return new TemplateChunkGenerator(server, this.template);
    }

    record ChestInfo(BlockPos pos, Direction direction) { }

}
