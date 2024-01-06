package me.ellieis.Sabotage.game.custom;

import me.ellieis.Sabotage.Sabotage;
import me.ellieis.Sabotage.game.custom.items.SabotageChest;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class SabotageItems {
    public static final Item SABOTAGE_CHEST = new SabotageChest(SabotageBlocks.SABOTAGE_CHEST, new Item.Settings(), Items.CHEST);

    public static void register() {
        register("sabotage_chest", SABOTAGE_CHEST);
    }
    private static <T extends Item> T register(String id, T item) {
        return Registry.register(Registries.ITEM, Sabotage.identifier(id), item);
    }
}
