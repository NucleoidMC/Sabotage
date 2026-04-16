package me.ellieis.Sabotage.game.custom;

import eu.pb4.polymer.core.api.item.PolymerBlockItem;
import eu.pb4.polymer.core.api.item.PolymerCreativeModeTabUtils;
import me.ellieis.Sabotage.Sabotage;
import me.ellieis.Sabotage.game.custom.items.DetectiveShears;
import me.ellieis.Sabotage.game.custom.items.ShopItem;
import me.ellieis.Sabotage.game.custom.items.TesterSign;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;

public class SabotageItems {
    public static final Item SABOTAGE_CHEST = new PolymerBlockItem(SabotageBlocks.SABOTAGE_CHEST, new Item.Properties().useBlockDescriptionPrefix().setId(ResourceKey.create(Registries.ITEM, Sabotage.identifier("sabotage_chest"))), Items.CHEST);
    public static final Item TESTER_WOOL = new PolymerBlockItem(SabotageBlocks.TESTER_WOOL, new Item.Properties().useBlockDescriptionPrefix().setId(ResourceKey.create(Registries.ITEM, Sabotage.identifier("tester_wool"))), Items.WHITE_WOOL);
    public static final Item TESTER_SIGN = new TesterSign(new Item.Properties().useBlockDescriptionPrefix().setId(ResourceKey.create(Registries.ITEM, Sabotage.identifier("tester_sign"))), SabotageBlocks.TESTER_SIGN, SabotageBlocks.WALL_TESTER_SIGN);
    public static final Item DETECTIVE_SHEARS = new DetectiveShears(new Item.Properties().useItemDescriptionPrefix().setId(ResourceKey.create(Registries.ITEM, Sabotage.identifier("detective_shears"))).durability(101));
    public static final Item SHOP_ITEM = new ShopItem(new Item.Properties().useItemDescriptionPrefix().setId(ResourceKey.create(Registries.ITEM, Sabotage.identifier("shop_item"))).stacksTo(1));

    public static final CreativeModeTab ITEM_GROUP = FabricCreativeModeTab.builder()
            .title(Component.translatable("gameType.sabotage.sabotage"))
            .icon(SABOTAGE_CHEST::getDefaultInstance)
            .displayItems((context, entries) -> {
                entries.accept(SABOTAGE_CHEST);
                entries.accept(TESTER_WOOL);
                entries.accept(TESTER_SIGN);
            })
            .build();
    public static void register() {
        register("sabotage_chest", SABOTAGE_CHEST);
        register("detective_shears", DETECTIVE_SHEARS);
        register("tester_sign", TESTER_SIGN);
        register("tester_wool", TESTER_WOOL);
        register("shop_item", SHOP_ITEM);
        PolymerCreativeModeTabUtils.registerPolymerCreativeModeTab(Sabotage.identifier("general"), ITEM_GROUP);
    }
    private static <T extends Item> T register(String id, T item) {
        return Registry.register(BuiltInRegistries.ITEM, Sabotage.identifier(id), item);
    }
}
