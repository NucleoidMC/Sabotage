package me.ellieis.Sabotage.game;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import xyz.nucleoid.plasmid.api.game.GameSpace;

import java.util.*;

class messageInfo {
    DisplayEntity.TextDisplayEntity entity;
    int spawnTime;
    public messageInfo(DisplayEntity.TextDisplayEntity entity, int spawnTime) {
        this.entity = entity;
        this.spawnTime = spawnTime;
    }
}
public class ChatManager {
    GameSpace gameSpace;
    HashMap<ServerPlayerEntity, ArrayList<messageInfo>> messages = new HashMap<>();
    public ChatManager(GameSpace gameSpace) {
        this.gameSpace = gameSpace;
        for (ServerPlayerEntity player : gameSpace.getPlayers()) {
            messages.put(player, new ArrayList<>());
        }
    }


    public void onChat(ServerPlayerEntity plr, Text message) {
        DisplayEntity.TextDisplayEntity entity = new DisplayEntity.TextDisplayEntity(EntityType.TEXT_DISPLAY, plr.getWorld());
        entity.setBillboardMode(DisplayEntity.BillboardMode.CENTER);
        entity.setText(message);
        entity.setLineWidth(100);
        entity.setInterpolationDuration(1);
        plr.getWorld().spawnEntity(entity);
        ArrayList<messageInfo> entities = messages.get(plr);
        entities.add(new messageInfo(entity,(int) plr.getWorld().getTime()));
        int i = 0;
        for (messageInfo info : entities) {
            positionLabel(plr, info, i);
            int textLength = info.entity.getText().getString().length();
            i += (int) Math.floor(textLength / 15);
            i++;
        }
    }
    private void positionLabel(ServerPlayerEntity plr, messageInfo info, int i) {
        Vec3d pos = plr.getPos();
        info.entity.setTeleportDuration(1);
        info.entity.teleportTo(new TeleportTarget((ServerWorld) plr.getWorld(),
                pos.add(0, 2.5 + (i * 0.25), 0),
                plr.getVelocity(),
                0,
                0,
                TeleportTarget.NO_OP
        ));
    }
    public void onTick() {
        for (Map.Entry<ServerPlayerEntity, ArrayList<messageInfo>> entry: messages.entrySet()) {
            int i = 0;
            ArrayList<messageInfo> messagesToRemove = new ArrayList<>();
            for (messageInfo info: entry.getValue()) {
                ServerPlayerEntity plr = entry.getKey();
                positionLabel(plr, info, i);
                int textLength = info.entity.getText().getString().length();
                if ((plr.getWorld().getTime() - info.spawnTime) >= 100 + textLength * 1.5) {
                    info.entity.remove(Entity.RemovalReason.KILLED);
                    messagesToRemove.add(info);
                }
                i += (int) Math.floor(textLength / 15);
                i++;
            }
            for (messageInfo info: messagesToRemove) {
                entry.getValue().remove(info);
            }
        }
    }
}
