package me.ellieis.Sabotage.game.shop.items;

import me.ellieis.Sabotage.game.phase.SabotageActive;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.CommonButtonData;
import net.minecraft.server.dialog.action.StaticAction;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

import static me.ellieis.Sabotage.Sabotage.SHOP_BUY_PACKET_ID;

public abstract class BaseShopItem {
    public final String itemId;
    public final int price;
    public final String name;
    public final String desc;
    public BaseShopItem(String itemId, int price, String name, String desc) {
        this.itemId = itemId;
        this.price = price;
        this.name = name;
        this.desc = desc;
    }
    public ActionButton getItemBody() {
        return new ActionButton(new CommonButtonData(Component.translatable(this.name), 300), Optional.of(new StaticAction(new ClickEvent.Custom(SHOP_BUY_PACKET_ID, Optional.of(StringTag.valueOf(this.itemId))))));
    }

    public abstract boolean onBuy(ServerPlayer player, SabotageActive game);
}
