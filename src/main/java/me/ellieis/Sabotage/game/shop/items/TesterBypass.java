package me.ellieis.Sabotage.game.shop.items;

import me.ellieis.Sabotage.game.phase.SabotageActive;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class TesterBypass extends BaseShopItem {
    public TesterBypass() {
        super("sabotage.shop.tester_bypass", 20, "tester_bypass", "sabotage.shop.tester_bypass.desc");
    }

    public boolean onBuy(ServerPlayer player, SabotageActive game) {
        game.testerBypassers.add(player);
        player.sendSystemMessage(Component.translatable("sabotage.shop.buy_success", Component.literal("(20 Karma)").withStyle(ChatFormatting.BLUE)).withStyle(ChatFormatting.GREEN), true);
        player.sendSystemMessage(Component.translatable(this.desc));
        return true;
    }
}
