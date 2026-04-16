package me.ellieis.Sabotage.game.custom.items;

import eu.pb4.polymer.core.api.item.PolymerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.resources.Identifier;
import xyz.nucleoid.packettweaker.PacketContext;

public class TesterSign extends SignItem implements PolymerItem {

    public TesterSign(Properties settings, Block standingBlock, Block wallBlock) {
        super(standingBlock, wallBlock, settings);
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return Items.OAK_SIGN;
    }

    @Override
    public Identifier getPolymerItemModel(ItemStack stack, PacketContext context) {
        return null;
    }
}
