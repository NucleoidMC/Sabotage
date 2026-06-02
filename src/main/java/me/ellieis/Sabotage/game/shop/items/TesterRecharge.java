package me.ellieis.Sabotage.game.shop.items;

import me.ellieis.Sabotage.game.custom.items.DetectiveShears;
import me.ellieis.Sabotage.game.phase.SabotageActive;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class TesterRecharge extends BaseShopItem {
    public TesterRecharge() {
        super("tester_recharge", 10, "sabotage.shop.tester_recharge", "sabotage.shop.tester_recharge.desc");
    }

    @Override
    public boolean onBuy(ServerPlayer player, SabotageActive game) {
        Inventory inventory = player.getInventory();
        int slot = -1;
        for (int i = 0; i < 36; i++) {
            if (inventory.getItem(i).getItem() instanceof DetectiveShears) {
                slot = i;
                break;
            }
        }
        if (slot == -1) {
            player.sendSystemMessage(Component.translatable("sabotage.shop.tester_recharge.fail").withStyle(ChatFormatting.RED), true);
            return false;
        }
        ItemStack stack = inventory.getItem(slot);
        if (stack.getDamageValue() >= 50) {
            stack.setDamageValue(stack.getDamageValue() - 50);
            player.sendSystemMessage(Component.translatable("sabotage.shop.buy_success", Component.literal("(20 Karma)").withStyle(ChatFormatting.BLUE)).withStyle(ChatFormatting.GREEN), true);
            player.sendSystemMessage(Component.translatable("sabotage.shop.tester_recharge.desc"));
        } else {
            player.sendSystemMessage(Component.translatable("sabotage.shop.tester_recharge.full").withStyle(ChatFormatting.RED), true);
            return false;
        }
        return true;
    }
}
