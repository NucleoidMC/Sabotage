package me.ellieis.Sabotage.game.custom.items;

import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import me.ellieis.Sabotage.Sabotage;
import me.ellieis.Sabotage.game.phase.SabotageActive;
import me.ellieis.Sabotage.game.shop.ShopMenu;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import xyz.nucleoid.plasmid.api.game.GameSpaceManager;

public class ShopCartItem extends Item implements PolymerItem {
    public ShopCartItem(Properties settings) {
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
        ShopMenu.showMenu((ServerPlayer) plr, game);
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
