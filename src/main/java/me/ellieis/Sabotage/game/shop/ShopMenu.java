package me.ellieis.Sabotage.game.shop;

import me.ellieis.Sabotage.game.Role;
import me.ellieis.Sabotage.game.phase.SabotageActive;
import me.ellieis.Sabotage.game.shop.items.BaseShopItem;
import me.ellieis.Sabotage.game.shop.items.ShopItems;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.dialog.*;
import net.minecraft.server.dialog.body.DialogBody;
import net.minecraft.server.dialog.body.PlainMessage;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ShopMenu {
    public static void register() {
        ShopItems.register();
    }
    public static ArrayList<BaseShopItem> getShopItemsForRole(Role role) {
        ArrayList<BaseShopItem> items = new ArrayList<>();
        switch (role) {
            case INNOCENT:
                items.add(ShopItems.WOODEN_SPEAR);
                items.add(ShopItems.PLAYER_TRACKER);
                break;
            case DETECTIVE:
                items.add(ShopItems.TESTER_RECHARGE);
                items.add(ShopItems.PLAYER_TRACKER);
                break;
            case SABOTEUR:
                items.add(ShopItems.TRAPPED_CHEST);
                items.add(ShopItems.TESTER_BYPASS);
                break;
        }
        return items;
    }
    public static ArrayList<BaseShopItem> getShopItemsForPlayer(ServerPlayer player, SabotageActive game) {
        return getShopItemsForRole(game.teamManager.getPlayerRole(player));
    }
    public static void showMenu(ServerPlayer player, SabotageActive game) {
        Role role = game.teamManager.getPlayerRole(player);
        var body = new ArrayList<DialogBody>();
        body.add(new PlainMessage(Component.translatable("sabotage.shop.desc"), 300));
        if (role == Role.NONE) {
            body.add(new PlainMessage(Component.translatable("sabotage.shop.no_role"), 300));
            NoticeDialog dialog = new NoticeDialog(new CommonDialogData(Component.translatable("sabotage.shop.name"), Optional.empty(), true, false, DialogAction.CLOSE, body, List.of()), NoticeDialog.DEFAULT_ACTION);
            player.openDialog(Holder.direct(dialog));
            return;
        }
        ArrayList<ActionButton> actions = new ArrayList<>();
        ArrayList<BaseShopItem> items = getShopItemsForRole(role);
        for (BaseShopItem item : items) {
            actions.add(item.getItemBody());
        }
        MultiActionDialog dialog = new MultiActionDialog(new CommonDialogData(Component.translatable("sabotage.shop.name"), Optional.empty(), true, false, DialogAction.CLOSE, body, List.of()), actions, Optional.empty(), 1);
        player.openDialog(Holder.direct(dialog));
    }

    public static BaseShopItem getItemFromId(String id) {
        for (BaseShopItem item : ShopItems.ITEMS) {
            if (item.itemId.equals(id)) {
                return item;
            }
        }
        return null;
    }
}
