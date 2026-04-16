package me.ellieis.Sabotage.game.custom.items;

import eu.pb4.polymer.core.api.item.PolymerItem;
import me.ellieis.Sabotage.Sabotage;
import me.ellieis.Sabotage.game.phase.SabotageActive;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

public class DetectiveShears extends Item implements PolymerItem {
    public DetectiveShears(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack item, Player plr, LivingEntity entity, InteractionHand hand) {
        Level world = plr.level();
        if (!world.isClientSide()) {
            for (SabotageActive game : Sabotage.activeGames) {
                if (game.getWorld().equals(world) && (item.getMaxDamage() - item.getDamageValue()) > 1) {
                    item.setDamageValue(item.getDamageValue() + 50);
                    game.testEntity((ServerPlayer) plr, entity);
                    break;
                }
            }
        }
        return InteractionResult.FAIL;
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return Items.SHEARS;
    }

    @Override
    public Identifier getPolymerItemModel(ItemStack stack, PacketContext context, HolderLookup.Provider lookup) {
        return null;
    }
}
