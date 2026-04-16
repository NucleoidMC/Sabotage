package me.ellieis.Sabotage.game.custom.items;

import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import me.ellieis.Sabotage.Sabotage;
import me.ellieis.Sabotage.game.Roles;
import me.ellieis.Sabotage.game.phase.SabotageActive;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.dialog.DialogAction;
import net.minecraft.server.dialog.CommonDialogData;
import net.minecraft.server.dialog.body.DialogBody;
import net.minecraft.server.dialog.body.PlainMessage;
import net.minecraft.server.dialog.NoticeDialog;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
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
    private static PlainMessage itemBody(String translationKey, String itemId) {
        return new PlainMessage(Component.translatable(translationKey).withStyle(style -> style.withClickEvent(new ClickEvent.Custom(SHOP_BUY_PACKET_ID, Optional.of(StringTag.valueOf(itemId)))).withColor(ChatFormatting.BLUE)), 300);
    }
    public ShopItem(Properties settings) {
        super(settings);
    }
    @Override
    public InteractionResult use(Level world, Player plr, InteractionHand hand) {
        if (world.isClientSide()) {
            return InteractionResult.PASS;
        }
        if (!GameSpaceManager.get().inGame(plr)) {
            return InteractionResult.PASS;
        }
        SabotageActive game = null;
        for (SabotageActive game2 : Sabotage.activeGames) {
            if (game2.getWorld().equals(world)){
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
        switch (role) {
            case INNOCENT:
                body.add(itemBody("sabotage.shop.wooden_spear", "wooden_spear"));
                body.add(itemBody("sabotage.shop.tracker", "player_tracker"));
                break;
            case DETECTIVE:
                body.add(itemBody("sabotage.shop.tester_recharge", "tester_recharge"));
                body.add(itemBody("sabotage.shop.tracker", "player_tracker"));
                break;
            case SABOTEUR:
                body.add(itemBody("sabotage.shop.trapped_chest", "trapped_chest"));
                body.add(itemBody("sabotage.shop.tester_bypass", "tester_bypass"));
                break;
            case NONE:
            default:
                body.add(new PlainMessage(Component.translatable("sabotage.shop.no_role"), 300));
        }

        var dialog = new NoticeDialog(new CommonDialogData(getName(plr.getItemInHand(hand)), Optional.empty(), true, false, DialogAction.CLOSE, body, List.of()), NoticeDialog.DEFAULT_ACTION);
        plr.openDialog(Holder.direct(dialog));
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
