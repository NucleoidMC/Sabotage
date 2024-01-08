package me.ellieis.Sabotage.game.custom.items;

import eu.pb4.polymer.core.api.item.PolymerItem;
import me.ellieis.Sabotage.Sabotage;
import me.ellieis.Sabotage.game.phase.SabotageActive;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class DetectiveShears extends Item implements PolymerItem {
    public DetectiveShears(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnEntity(ItemStack item, PlayerEntity plr, LivingEntity entity, Hand hand) {
        World world = plr.getEntityWorld();
        if (!world.isClient()) {
            for (SabotageActive game : Sabotage.activeGames) {
                if (game.getWorld().equals(world)) {
                    game.testEntity((ServerPlayerEntity) plr, entity);
                    break;
                }
            }
        }
        return ActionResult.FAIL;
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        return Items.SHEARS;
    }
}
