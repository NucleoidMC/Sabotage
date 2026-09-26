package me.ellieis.Sabotage.game.custom.items;

import eu.pb4.polymer.core.api.item.PolymerItem;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.resources.Identifier;

public class TesterSign extends StandingAndWallBlockItem implements PolymerItem {

    public TesterSign(Item.Properties settings, Block standingBlock, Block wallBlock) {
        super(standingBlock, wallBlock, Direction.DOWN, settings);
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return Items.OAK_SIGN;
    }

    @Override
    public Identifier getPolymerItemModel(ItemStack stack, PacketContext context, HolderLookup.Provider lookup) {
        return null;
    }
}
