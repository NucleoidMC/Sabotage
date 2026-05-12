package me.ellieis.Sabotage.game.custom.items;

import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import me.ellieis.Sabotage.Sabotage;
import me.ellieis.Sabotage.game.Roles;
import me.ellieis.Sabotage.game.phase.SabotageActive;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.dialog.*;
import net.minecraft.server.dialog.action.Action;
import net.minecraft.server.dialog.action.StaticAction;
import net.minecraft.server.dialog.body.DialogBody;
import net.minecraft.server.dialog.body.PlainMessage;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.StringTag;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import xyz.nucleoid.plasmid.api.game.GameSpaceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static me.ellieis.Sabotage.Sabotage.SHOP_BUY_PACKET_ID;

public class ShopItem extends Item implements PolymerItem {
    private static ActionButton itemBody(String translationKey, String itemId) {
        return new ActionButton(new CommonButtonData(Component.translatable(translationKey), 300), Optional.of(new StaticAction(new ClickEvent.Custom(SHOP_BUY_PACKET_ID, Optional.of(StringTag.valueOf(itemId))))));
    }
    public ShopItem(Properties settings) {
        super(settings);
    }
    @Override
    public InteractionResult use(Level level, Player plr, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.PASS;
        }
        if (!GameSpaceManager.get().inGame(plr)) {
            return InteractionResult.PASS;
        }
        SabotageActive game = null;
        for (SabotageActive game2 : Sabotage.activeGames) {
            if (game2.getLevel().equals(level)){
                game = game2;
                break;
            }
        }
        if (game == null) {
            return InteractionResult.PASS;
        }
        Roles role = game.teamManager.getPlayerRole((ServerPlayer) plr);
        var body = new ArrayList<DialogBody>();
        body.add(new PlainMessage(Component.translatable("sabotage.shop.desc"), 300));
        ArrayList<ActionButton> actions = new ArrayList<>();
        switch (role) {
            case INNOCENT:
                actions.add(itemBody("sabotage.shop.wooden_spear", "wooden_spear"));
                actions.add(itemBody("sabotage.shop.tracker", "player_tracker"));
                break;
            case DETECTIVE:
                actions.add(itemBody("sabotage.shop.tester_recharge", "tester_recharge"));
                actions.add(itemBody("sabotage.shop.tracker", "player_tracker"));
                break;
            case SABOTEUR:
                actions.add(itemBody("sabotage.shop.trapped_chest", "trapped_chest"));
                actions.add(itemBody("sabotage.shop.tester_bypass", "tester_bypass"));
                break;
            case NONE:
            default:
                body.add(new PlainMessage(Component.translatable("sabotage.shop.no_role"), 300));
        }
        if (role.equals(Roles.NONE)) {
            var dialog = new NoticeDialog(new CommonDialogData(getName(plr.getItemInHand(hand)), Optional.empty(), true, false, DialogAction.CLOSE, body, List.of()), NoticeDialog.DEFAULT_ACTION);
            plr.openDialog(Holder.direct(dialog));
        } else {
            var dialog = new MultiActionDialog(new CommonDialogData(getName(plr.getItemInHand(hand)), Optional.empty(), true, false, DialogAction.CLOSE, body, List.of()), actions, Optional.empty(), 1);
            plr.openDialog(Holder.direct(dialog));
        }
        return InteractionResult.FAIL;
    }
    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return Items.ENCHANTED_BOOK;
    }

    @Override
    public Identifier getPolymerItemModel(ItemStack stack, PacketContext context, HolderLookup.Provider lookup) {
        if (PolymerResourcePackUtils.hasMainPack(context)) {
            return Sabotage.identifier("shop_item");
        }
        return null;
    }
}
