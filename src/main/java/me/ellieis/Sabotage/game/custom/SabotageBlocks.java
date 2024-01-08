package me.ellieis.Sabotage.game.custom;

import eu.pb4.polymer.core.api.block.PolymerBlockUtils;
import me.ellieis.Sabotage.Sabotage;
import me.ellieis.Sabotage.game.custom.blocks.SabotageChest;
import me.ellieis.Sabotage.game.custom.blocks.SabotageChestBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;


public class SabotageBlocks {
    public static final SabotageChest SABOTAGE_CHEST = new SabotageChest(AbstractBlock.Settings.copy(Blocks.CHEST).dropsNothing(), Blocks.CHEST);

    public static final BlockEntityType<SabotageChestBlockEntity> SABOTAGE_CHEST_ENTITY = FabricBlockEntityTypeBuilder.create(SabotageChestBlockEntity::new, SABOTAGE_CHEST).build(null);
    public static void register() {
        register("sabotage_chest", SABOTAGE_CHEST);

        registerBlockEntity("sabotage_chest_block_entity", SABOTAGE_CHEST_ENTITY);

    }

    private static <T extends Block> T register(String id, T block) {
        return Registry.register(Registries.BLOCK, Sabotage.identifier(id), block);
    }
    private static <T extends BlockEntity> BlockEntityType<T> registerBlockEntity(String id, BlockEntityType<T> type) {
        Registry.register(Registries.BLOCK_ENTITY_TYPE, Sabotage.identifier(id), type);
        PolymerBlockUtils.registerBlockEntity(SABOTAGE_CHEST_ENTITY);
        return type;
    }
}
