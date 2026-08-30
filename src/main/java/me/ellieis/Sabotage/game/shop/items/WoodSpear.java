package me.ellieis.Sabotage.game.shop.items;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;

public class WoodSpear extends ItemStackShopItem {
    public WoodSpear() {
        super("wooden_spear", 30,"sabotage.shop.wooden_spear", "sabotage.shop.wooden_spear.desc", new ItemStackTemplate(Items.WOODEN_SPEAR, DataComponentPatch.builder().set(DataComponents.DAMAGE, 58).build()));
    }
}
