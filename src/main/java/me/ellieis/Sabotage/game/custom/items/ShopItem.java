package me.ellieis.Sabotage.game.custom.items;

import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import me.ellieis.Sabotage.Sabotage;
import me.ellieis.Sabotage.game.Roles;
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
    private static PlainMessageDialogBody itemBody(String translationKey, String itemId) {
        return new PlainMessageDialogBody(Text.translatable(translationKey).styled(style -> style.withClickEvent(new ClickEvent.Custom(SHOP_BUY_PACKET_ID, Optional.of(NbtString.of(itemId)))).withColor(Formatting.BLUE)), 300);
    }
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
        SabotageActive game = null;
        for (SabotageActive game2 : Sabotage.activeGames) {
            if (game2.getWorld().equals(world)){
                game = game2;
                break;
            }
        }
        if (game == null) {
            return ActionResult.PASS;
        }
        Roles role = game.teamManager.getPlayerRole((ServerPlayerEntity) plr);
        var body = new ArrayList<DialogBody>();
        body.add(new PlainMessageDialogBody(Text.translatable("sabotage.shop.desc"), 300));
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
                body.add(new PlainMessageDialogBody(Text.translatable("sabotage.shop.no_role"), 300));
        }

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
