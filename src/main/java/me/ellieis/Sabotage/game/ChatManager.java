package me.ellieis.Sabotage.game;

import me.ellieis.Sabotage.game.config.SabotageConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.network.packet.s2c.play.EntityPassengersSetS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
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
    SabotageConfig config;
    TeamManager teamManager;
    HashMap<ServerPlayerEntity, ArrayList<messageInfo>> messages = new HashMap<>();
    public ChatManager(GameSpace gameSpace, SabotageConfig config, TeamManager teamManager) {
        this.gameSpace = gameSpace;
        this.config = config;
        this.teamManager = teamManager;
        for (ServerPlayerEntity player : gameSpace.getPlayers()) {
            messages.put(player, new ArrayList<>());
        }
    }


    public void onChat(ServerPlayerEntity plr, Text message) {
        if (config.proximityTextChat()) {
            proximityTextChat(plr, message);
        } else {
            globalTextChat(plr, message);
        }
    }

    private void globalTextChat(ServerPlayerEntity plr, Text message) {
        if (teamManager.detectives.contains(plr)) {
            teamManager.detectives.sendMessage(Text.literal("<" + plr.getName().getString() + "> ").formatted(Formatting.BLUE).append(message.copy().formatted(Formatting.RESET)));
            teamManager.innocents.sendMessage(Text.literal("<" + plr.getName().getString() + "> ").formatted(Formatting.BLUE).append(message.copy().formatted(Formatting.RESET)));
            teamManager.saboteurs.sendMessage(Text.literal("<" + plr.getName().getString() + "> ").formatted(TeamManager.getRoleColor(teamManager.getPlayerRole(plr))).append(message.copy().formatted(Formatting.RESET)));
            teamManager.dead.sendMessage(Text.literal("<" + plr.getName().getString() + "> ").formatted(Formatting.BLUE).append(message.copy().formatted(Formatting.RESET)));
        } else {
            teamManager.detectives.sendMessage(Text.literal("<" + plr.getName().getString() + "> ").formatted(Formatting.YELLOW).append(message.copy().formatted(Formatting.RESET)));
            teamManager.innocents.sendMessage(Text.literal("<" + plr.getName().getString() + "> ").formatted(Formatting.YELLOW).append(message.copy().formatted(Formatting.RESET)));
            teamManager.saboteurs.sendMessage(Text.literal("<" + plr.getName().getString() + "> ").formatted(TeamManager.getRoleColor(teamManager.getPlayerRole(plr))).append(message.copy().formatted(Formatting.RESET)));
            teamManager.dead.sendMessage(Text.literal("<" + plr.getName().getString() + "> ").formatted(Formatting.YELLOW).append(message.copy().formatted(Formatting.RESET)));
        }
    }

    private void proximityTextChat(ServerPlayerEntity plr, Text message) {
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
