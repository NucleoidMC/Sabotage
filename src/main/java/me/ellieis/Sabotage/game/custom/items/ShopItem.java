package me.ellieis.Sabotage.game.custom.items;

import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import me.ellieis.Sabotage.Sabotage;
import me.ellieis.Sabotage.game.phase.SabotageActive;
import net.minecraft.dialog.AfterAction;
import net.minecraft.dialog.DialogCommonData;
import net.minecraft.dialog.body.DialogBody;
import net.minecraft.dialog.body.PlainMessageDialogBody;
import net.minecraft.dialog.type.NoticeDialog;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import xyz.nucleoid.packettweaker.PacketContext;
import xyz.nucleoid.plasmid.api.game.GameSpaceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static me.ellieis.Sabotage.Sabotage.SHOP_BUY_PACKET_ID;

public class ShopItem extends Item implements PolymerItem {
    public ShopItem(Settings settings) {
        super(settings);
    }
    @Override
    public ActionResult use(World world, PlayerEntity plr, Hand hand) {
        if (world.isClient()) {
            return ActionResult.PASS;
        }
        if (!GameSpaceManager.get().inGame(plr)) {
            return ActionResult.PASS;
        }
        boolean isInGame = false;
        for (SabotageActive game : Sabotage.activeGames) {
            if (game.getWorld().equals(world)){
                isInGame = true;
                break;
            }
        }
        if (!isInGame) {
            return ActionResult.PASS;
        }

        var body = new ArrayList<DialogBody>();
        body.add(new PlainMessageDialogBody(Text.translatable("sabotage.shop.desc"), 300));
        body.add(new PlainMessageDialogBody(Text.translatable("sabotage.shop.trapped_chest").styled((style ->
            style.withClickEvent(new ClickEvent.Custom(SHOP_BUY_PACKET_ID, Optional.of(NbtString.of("trapped_chest")))).withColor(Formatting.BLUE)
        )), 300));
        body.add(new PlainMessageDialogBody(Text.translatable("sabotage.shop.tester_bypass").styled((style ->
                style.withClickEvent(new ClickEvent.Custom(SHOP_BUY_PACKET_ID, Optional.of(NbtString.of("tester_bypass")))).withColor(Formatting.BLUE)
        )), 300));
        var dialog = new NoticeDialog(new DialogCommonData(getName(), Optional.empty(), true, false, AfterAction.CLOSE, body, List.of()), NoticeDialog.OK_BUTTON);
        plr.openDialog(RegistryEntry.of(dialog));
        return ActionResult.FAIL;
    }
    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return Items.ENCHANTED_BOOK;
    }

    @Override
    public Identifier getPolymerItemModel(ItemStack stack, PacketContext context) {
        if (PolymerResourcePackUtils.hasMainPack(context)) {
            return Sabotage.identifier("shop_item");
        }
        return null;
    }
}
