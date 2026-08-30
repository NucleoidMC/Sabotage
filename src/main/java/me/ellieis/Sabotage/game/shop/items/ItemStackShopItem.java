package me.ellieis.Sabotage.game.shop.items;

import me.ellieis.Sabotage.game.phase.SabotageActive;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStackTemplate;

public class ItemStackShopItem extends BaseShopItem {
    public final ItemStackTemplate item;
    public ItemStackShopItem(String itemId, int price, String name, String desc, ItemStackTemplate item) {
        super(itemId, price, name, desc);
        this.item = item;
    }

    public boolean onBuy(ServerPlayer player, SabotageActive game) {
        player.getInventory().add(item.create());
        player.sendSystemMessage(Component.translatable("sabotage.shop.buy_success", Component.literal("(20 Karma)").withStyle(ChatFormatting.BLUE)).withStyle(ChatFormatting.GREEN), true);
        player.sendSystemMessage(Component.translatable(desc));
        return true;
    }
}
