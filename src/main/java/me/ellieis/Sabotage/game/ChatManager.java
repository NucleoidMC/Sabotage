package me.ellieis.Sabotage.game;

import me.ellieis.Sabotage.game.config.SabotageConfig;
import net.minecraft.world.entity.*;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.portal.TeleportTransition;
import xyz.nucleoid.plasmid.api.game.GameSpace;

import java.util.*;

public class ChatManager {
    GameSpace gameSpace;
    SabotageConfig config;
    TeamManager teamManager;
    HashMap<ServerPlayer, ArrayList<messageInfo>> messages = new HashMap<>();
    public ChatManager(GameSpace gameSpace, SabotageConfig config, TeamManager teamManager) {
        this.gameSpace = gameSpace;
        this.config = config;
        this.teamManager = teamManager;
        for (ServerPlayer player : gameSpace.getPlayers()) {
            messages.put(player, new ArrayList<>());
        }
    }


    public void onChat(ServerPlayer plr, Component message) {
        if (config.proximityTextChat()) {
            proximityTextChat(plr, message);
        } else {
            globalTextChat(plr, message);
        }
    }

    private void globalTextChat(ServerPlayer plr, Component message) {
        if (teamManager.detectives.contains(plr)) {
            teamManager.detectives.sendMessage(Component.literal("<" + plr.getName().getString() + "> ").withStyle(ChatFormatting.BLUE).append(message.copy().withStyle(ChatFormatting.RESET)));
            teamManager.innocents.sendMessage(Component.literal("<" + plr.getName().getString() + "> ").withStyle(ChatFormatting.BLUE).append(message.copy().withStyle(ChatFormatting.RESET)));
            teamManager.saboteurs.sendMessage(Component.literal("<" + plr.getName().getString() + "> ").withStyle(TeamManager.getRoleColor(teamManager.getPlayerRole(plr))).append(message.copy().withStyle(ChatFormatting.RESET)));
            teamManager.dead.sendMessage(Component.literal("<" + plr.getName().getString() + "> ").withStyle(ChatFormatting.BLUE).append(message.copy().withStyle(ChatFormatting.RESET)));
        } else {
            teamManager.detectives.sendMessage(Component.literal("<" + plr.getName().getString() + "> ").withStyle(ChatFormatting.YELLOW).append(message.copy().withStyle(ChatFormatting.RESET)));
            teamManager.innocents.sendMessage(Component.literal("<" + plr.getName().getString() + "> ").withStyle(ChatFormatting.YELLOW).append(message.copy().withStyle(ChatFormatting.RESET)));
            teamManager.saboteurs.sendMessage(Component.literal("<" + plr.getName().getString() + "> ").withStyle(TeamManager.getRoleColor(teamManager.getPlayerRole(plr))).append(message.copy().withStyle(ChatFormatting.RESET)));
            teamManager.dead.sendMessage(Component.literal("<" + plr.getName().getString() + "> ").withStyle(ChatFormatting.YELLOW).append(message.copy().withStyle(ChatFormatting.RESET)));
        }
    }

    private void proximityTextChat(ServerPlayer plr, Component message) {
        Display.TextDisplay entity = new Display.TextDisplay(EntityTypes.TEXT_DISPLAY, plr.level());
        entity.setBillboardConstraints(Display.BillboardConstraints.CENTER);
        entity.setText(message);
        entity.setLineWidth(100);
        entity.setTransformationInterpolationDuration(1);
        plr.level().addFreshEntity(entity);
        ArrayList<messageInfo> entities = messages.get(plr);
        entities.add(new messageInfo(entity,(int) plr.level().getGameTime()));
        int i = 0;
        for (messageInfo info : entities) {
            positionLabel(plr, info, i);
            int textLength = info.entity().getText().getString().length();
            i += (int) Math.floor(textLength / 15);
            i++;
        }
    }
    private void positionLabel(ServerPlayer plr, messageInfo info, int i) {
        Vec3 pos = plr.position();
        info.entity().setPosRotInterpolationDuration(1);
        info.entity().teleport(new TeleportTransition((ServerLevel) plr.level(),
                pos.add(0, 2.5 + (i * 0.25), 0),
                plr.getDeltaMovement(),
                0,
                0,
                TeleportTransition.DO_NOTHING
        ));

        // this is to make the entity movement less blocky.
        gameSpace.getPlayers().sendPacket(
                new ClientboundTeleportEntityPacket(
                        info.entity().getId(),
                        new PositionMoveRotation(
                                info.entity().trackingPosition(),
                                info.entity().getDeltaMovement(),
                                info.entity().getYRot(),
                                info.entity().getXRot()
                        ), Set.of(), false));
    }
    public void onTick() {
        for (Map.Entry<ServerPlayer, ArrayList<messageInfo>> entry: messages.entrySet()) {
            int i = 0;
            ArrayList<messageInfo> messagesToRemove = new ArrayList<>();
            for (messageInfo info: entry.getValue()) {
                ServerPlayer plr = entry.getKey();
                positionLabel(plr, info, i);
                int textLength = info.entity().getText().getString().length();
                if ((plr.level().getGameTime() - info.spawnTime()) >= 100 + textLength * 1.5) {
                    info.entity().remove(Entity.RemovalReason.KILLED);
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

    record messageInfo(Display.TextDisplay entity, int spawnTime) {
    }
}
