package me.ellieis.Sabotage.game.custom;

import eu.pb4.polymer.core.api.block.PolymerBlockUtils;
import me.ellieis.Sabotage.Sabotage;
import me.ellieis.Sabotage.game.custom.blocks.*;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

import java.util.function.Function;


public class SabotageBlocks {
    public static final SabotageChest SABOTAGE_CHEST = register("sabotage_chest", SabotageChest::new, AbstractBlock.Settings.copy(Blocks.CHEST).dropsNothing());
    public static final TesterWool TESTER_WOOL = register("tester_wool", TesterWool::new, AbstractBlock.Settings.copy(Blocks.WHITE_WOOL).dropsNothing());
    public static final TesterSign TESTER_SIGN = register("tester_sign", TesterSign::new, AbstractBlock.Settings.copy(Blocks.OAK_SIGN).dropsNothing());
    public static final WallTesterSign WALL_TESTER_SIGN = register("wall_tester_sign", WallTesterSign::new, AbstractBlock.Settings.copy(Blocks.OAK_WALL_SIGN).dropsNothing());

    public static final BlockEntityType<TesterSignBlockEntity> TESTER_SIGN_ENTITY = registerBlockEntity("tester_sign_block_entity", FabricBlockEntityTypeBuilder.create(TesterSignBlockEntity::new, TESTER_SIGN, WALL_TESTER_SIGN).build());
    public static final BlockEntityType<SabotageChestBlockEntity> SABOTAGE_CHEST_ENTITY = registerBlockEntity("sabotage_chest_block_entity", FabricBlockEntityTypeBuilder.create(SabotageChestBlockEntity::new, SABOTAGE_CHEST).build());

    private static <T extends AbstractBlock> T register(String id, Function<AbstractBlock.Settings, T> factory, AbstractBlock.Settings settings) {
        Block block = (Block) factory.apply(settings.registryKey(RegistryKey.of(RegistryKeys.BLOCK, Sabotage.identifier(id))));
        return (T) Registry.register(Registries.BLOCK, Sabotage.identifier(id), block);
    }
    private static <T extends BlockEntity> BlockEntityType<T> registerBlockEntity(String id, BlockEntityType<T> type) {
        Registry.register(Registries.BLOCK_ENTITY_TYPE, Sabotage.identifier(id), type);
        PolymerBlockUtils.registerBlockEntity(type);
        return type;
    }
    public static void register() {}
}
