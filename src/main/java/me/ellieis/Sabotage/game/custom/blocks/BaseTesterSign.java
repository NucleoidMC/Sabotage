package me.ellieis.Sabotage.game.custom.blocks;

import eu.pb4.polymer.core.api.block.PolymerBlockUtils;
import me.ellieis.Sabotage.Sabotage;
import me.ellieis.Sabotage.game.phase.SabotageActive;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class BaseTesterSign  {
    public static void onUse(Level level, Player player, BlockPos pos) {
        if (!level.isClientSide()) {
            ServerPlayer plr = (ServerPlayer) player;

            for (SabotageActive game : Sabotage.activeGames) {
                if (game.getLevel().equals(level)) {
                    if (!game.testEntity(plr, pos.getCenter())) {
                        plr.sendSystemMessage(Component.translatable("sabotage.tester.fail").withStyle(ChatFormatting.YELLOW));
                    }
                    break;
                }
            }
        }
    }

    public static void onPlaced(Level level, BlockPos pos) {
        if (!level.isClientSide()) {
            TesterSignBlockEntity be = (TesterSignBlockEntity) level.getBlockEntity(pos);
            Component[] text = {Component.literal("Click"), Component.literal("this sign"), Component.literal("to start"), Component.literal("test")};
            be.setText(new SignText(text, text, DyeColor.RED, true), true);
            be.setWaxed(true);
        }
    }

    private static CompoundTag createSignTextNbt() {
        CompoundTag main = new CompoundTag();
        ListTag text = new ListTag();
        text.add(StringTag.valueOf("Click this"));
        text.add(StringTag.valueOf("sign to"));
        text.add(StringTag.valueOf("start the"));
        text.add(StringTag.valueOf("test"));
        main.put("messages", text);
        main.putString("color", "red");
        main.putBoolean("has_glowing_text", true);
        return main;
    }
    public static Packet<?> getBlockEntityPacket(BlockPos pos) {
        CompoundTag main = new CompoundTag();
        main.putString("id", "minecraft:sign");
        main.putBoolean("is_waxed", true);
        main.putInt("x", pos.getX());
        main.putInt("y", pos.getY());
        main.putInt("z", pos.getZ());
        CompoundTag front = createSignTextNbt();
        CompoundTag back = createSignTextNbt();
        main.put("front_text", front);
        main.put("back_text", back);
        return PolymerBlockUtils.createBlockEntityPacket(pos, BlockEntityType.SIGN, main);
    }
}
