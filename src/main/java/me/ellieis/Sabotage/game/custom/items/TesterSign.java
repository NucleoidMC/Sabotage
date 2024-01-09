package me.ellieis.Sabotage.game.custom.items;

import eu.pb4.polymer.core.api.item.PolymerItem;
import net.minecraft.block.Block;
import net.minecraft.item.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

public class TesterSign extends SignItem implements PolymerItem {

    public TesterSign(Settings settings, Block standingBlock, Block wallBlock) {
        super(settings, standingBlock, wallBlock);
    }
    public TesterSign(Settings settings, Block standingBlock, Block wallBlock, Direction verticalAttachmentDirection) {
        super(settings, standingBlock, wallBlock, verticalAttachmentDirection);
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        return Items.OAK_SIGN;
    }
}
