package me.ellieis.Sabotage.game.shop.items;

import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;

public class TrappedChest extends  ItemStackShopItem {
    public TrappedChest() {
        super("trapped_chest", 20, "sabotage.shop.trapped_chest", "sabotage.shop.trapped_chest.desc", new ItemStackTemplate(Items.TRAPPED_CHEST));
    }
}
