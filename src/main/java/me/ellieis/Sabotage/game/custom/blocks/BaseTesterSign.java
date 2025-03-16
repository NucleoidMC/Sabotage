package me.ellieis.Sabotage.game.custom.blocks;

import eu.pb4.polymer.core.api.block.PolymerBlockUtils;
import me.ellieis.Sabotage.Sabotage;
import me.ellieis.Sabotage.game.phase.SabotageActive;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.SignText;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BaseTesterSign  {
    public static void onUse(World world, PlayerEntity player, BlockPos pos) {
        if (!world.isClient()) {
            ServerPlayerEntity plr = (ServerPlayerEntity) player;

            for (SabotageActive game : Sabotage.activeGames) {
                if (game.getWorld().equals(world)) {
                    if (!game.testEntity(plr, pos.toCenterPos())) {
                        plr.sendMessage(Text.translatable("sabotage.tester.fail").formatted(Formatting.YELLOW));
                    }
                    break;
                }
            }
        }
    }

    public static void onPlaced(World world, BlockPos pos) {
        if (!world.isClient()) {
            TesterSignBlockEntity be = (TesterSignBlockEntity) world.getBlockEntity(pos);
            Text[] text = {Text.literal("Click"), Text.literal("this sign"), Text.literal("to start"), Text.literal("test")};
            be.setText(new SignText(text, text, DyeColor.RED, true), true);
            be.setWaxed(true);
        }
    }

    private static NbtCompound createSignTextNbt() {
        NbtCompound main = new NbtCompound();
        NbtList text = new NbtList();
        text.add(NbtString.of("'Click this'"));
        text.add(NbtString.of("'sign to'"));
        text.add(NbtString.of("'start the'"));
        text.add(NbtString.of("test"));
        main.put("messages", text);
        main.putString("color", "red");
        main.putBoolean("has_glowing_text", true);
        return main;
    }
    public static Packet<?> getBlockEntityPacket(BlockPos pos) {
        NbtCompound main = new NbtCompound();
        main.putString("id", "minecraft:sign");
        main.putBoolean("is_waxed", true);
        main.putInt("x", pos.getX());
        main.putInt("y", pos.getY());
        main.putInt("z", pos.getZ());
        NbtCompound front = createSignTextNbt();
        NbtCompound back = createSignTextNbt();
        main.put("front_text", front);
        main.put("back_text", back);
        return PolymerBlockUtils.createBlockEntityPacket(pos, BlockEntityType.SIGN, main);
    }
}
