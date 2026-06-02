package me.ellieis.Sabotage.game.shop.items;

import me.ellieis.Sabotage.game.phase.SabotageActive;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundTrackedWaypointPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.waypoints.Waypoint;
import org.joml.Vector3i;

public class PlayerTracker extends BaseShopItem {
    public PlayerTracker() {
        super("player_tracker", 20, "sabotage.shop.tracker", "sabotage.shop.tracker.desc");
    }

    @Override
    public boolean onBuy(ServerPlayer player, SabotageActive game) {
        game.playersWithTracker.add(player);
        for (ServerPlayer alive : game.getAlivePlayers()) {
            Vector3i pos = alive.blockPosition().toMutable();
            player.connection.send(ClientboundTrackedWaypointPacket.addWaypointPosition(alive.getUUID(), Waypoint.Icon.NULL, new Vec3i(pos.x, pos.y, pos.z)));
        }
        player.sendSystemMessage(Component.translatable("sabotage.shop.buy_success", Component.literal("(20 Karma)").withStyle(ChatFormatting.BLUE)).withStyle(ChatFormatting.GREEN), true);
        player.sendSystemMessage(Component.translatable(this.desc));
        return true;
    }
}
