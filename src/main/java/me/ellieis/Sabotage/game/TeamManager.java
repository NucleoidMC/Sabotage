package me.ellieis.Sabotage.game;

import me.ellieis.Sabotage.game.config.SabotageConfig;
import me.ellieis.Sabotage.game.phase.SabotageActive;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.GameSpacePlayers;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.MutablePlayerSet;
import xyz.nucleoid.plasmid.api.game.player.PlayerSet;
import xyz.nucleoid.plasmid.mixin.chat.PlayerListS2CPacketEntryAccessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static me.ellieis.Sabotage.game.custom.SabotageItems.DETECTIVE_SHEARS;


public class TeamManager {

    GameSpace gameSpace;
    GameActivity gameActivity;
    SabotageActive game;
    HashMap<ServerPlayer, Style> oldColors = new HashMap<>();
    HashMap<ServerPlayer, PlayerTeam> oldTeams = new HashMap<>();
    public PlayerTeam sab;
    public PlayerTeam det;
    public PlayerTeam inno;
    public PlayerTeam unknown;
    public PlayerTeam deadTeam;
    public final MutablePlayerSet saboteurs;
    public final MutablePlayerSet detectives;
    public final MutablePlayerSet innocents;
    public final MutablePlayerSet dead;
    public PlayerSet initialSaboteurs;
    private final SabotageConfig config;

    public TeamManager(GameSpace gameSpace, GameActivity gameActivity, SabotageActive game, SabotageConfig config) {
        this.gameSpace = gameSpace;
        this.gameActivity = gameActivity;
        this.config = config;
        gameActivity.listen(GamePlayerEvents.ADD, this::onAddPlayer);
        gameActivity.listen(GamePlayerEvents.REMOVE, this::onRemovePlayer);
        this.game = game;
        this.sab = new PlayerTeam(gameSpace.getServer().getScoreboard(), "sab");
        sab.setColor(ChatFormatting.RED);
        this.det = new PlayerTeam(gameSpace.getServer().getScoreboard(), "det");
        det.setColor(ChatFormatting.BLUE);
        this.inno = new PlayerTeam(gameSpace.getServer().getScoreboard(), "inno");
        inno.setColor(ChatFormatting.GREEN);
        this.unknown = new PlayerTeam(gameSpace.getServer().getScoreboard(), "unknown");
        unknown.setColor(ChatFormatting.YELLOW);
        this.deadTeam = new PlayerTeam(gameSpace.getServer().getScoreboard(), "dead");
        deadTeam.setColor(ChatFormatting.GRAY);
        this.saboteurs = new MutablePlayerSet(gameSpace.getServer());
        this.detectives = new MutablePlayerSet(gameSpace.getServer());
        this.innocents = new MutablePlayerSet(gameSpace.getServer());
        this.dead = new MutablePlayerSet(gameSpace.getServer());
    }

    private void onAddPlayer(ServerPlayer player) {
        var name = player.getTabListDisplayName();
        if (name == null) {
            name = player.getName();
        }
        oldTeams.put(player, player.getTeam());
        oldColors.put(player, name.getStyle());
    }

    private void onRemovePlayer(ServerPlayer player) {
        var name = player.getTabListDisplayName();
        if (name == null) {
            name = player.getName();
        }
        GameSpacePlayers plrs = gameSpace.getPlayers();
        plrs.sendPacket(this.createPlayerListPacket(player, Component.empty().append(name.copy().setStyle(oldColors.get(player)))));
        plrs.forEach((plr) -> {
            playerTeamPacket(deadTeam, player, plr, ClientboundSetPlayerTeamPacket.Action.ADD);
            if (oldTeams.get(plr) != null) {
                player.connection.send(ClientboundSetPlayerTeamPacket.createPlayerPacket(oldTeams.get(plr), plr.getScoreboardName(), ClientboundSetPlayerTeamPacket.Action.ADD));
            } else {
                player.connection.send(ClientboundSetPlayerTeamPacket.createPlayerPacket(deadTeam, plr.getScoreboardName(), ClientboundSetPlayerTeamPacket.Action.REMOVE));
            }
        });

        oldTeams.remove(player);
        oldColors.remove(player);
    }

    private Component formatPlayerName(ServerPlayer player, Component name, boolean isSaboteur) {
        Style style;
        if (isSaboteur) {
            if (saboteurs.contains(player)) {
                style = Style.EMPTY.withColor(ChatFormatting.RED);
            } else if (detectives.contains(player)) {
                style = Style.EMPTY.withColor(ChatFormatting.BLUE);
            } else if (innocents.contains(player)) {
                style = Style.EMPTY.withColor(ChatFormatting.GREEN);
            } else {
                style = Style.EMPTY.withColor(ChatFormatting.GRAY);
            }
        }
        else {
            if (detectives.contains(player)) {
                style = Style.EMPTY.withColor(ChatFormatting.BLUE);
            } else if (dead.contains(player)) {
                style = Style.EMPTY.withColor(ChatFormatting.GRAY);
            } else {
                style = Style.EMPTY.withColor(ChatFormatting.YELLOW);
            }
        }
        return Component.empty().append(name.copy().setStyle(style));
    }
    public ClientboundPlayerInfoUpdatePacket createPlayerListPacket(ServerPlayer player, Component playerName) {
        var packet = new ClientboundPlayerInfoUpdatePacket(net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME, player);

        var entry = packet.entries().get(0);
        var name = player.getTabListDisplayName();
        if (name == null) {
            name = player.getName();
        }
        ((PlayerListS2CPacketEntryAccessor) (Object) entry).setDisplayName(playerName);

        return packet;
    }
    public ClientboundPlayerInfoUpdatePacket updatePlayerName(ServerPlayer player, boolean isSaboteur) {
        var name = player.getTabListDisplayName();
        if (name == null) {
            name = player.getName();
        }
        return this.createPlayerListPacket(player, this.formatPlayerName(player, name, isSaboteur));
    }

    public void playerTeamPacket(PlayerTeam team, ServerPlayer plr, ServerPlayer otherPlr, ClientboundSetPlayerTeamPacket.Action operation) {
        plr.connection.send(ClientboundSetPlayerTeamPacket.createPlayerPacket(team, otherPlr.getScoreboardName(), operation));
    }

    public PlayerTeam getPlayerTeam(ServerPlayer plr, boolean isSab) {
        Role role = getPlayerRole(plr);
        if (isSab) {
            if (role == Role.SABOTEUR) {
                return sab;
            } else if (role == Role.DETECTIVE) {
                return det;
            } else if (role == Role.INNOCENT) {
                return inno;
            } else {
                return deadTeam;
            }
        } else {
            if (role == Role.DETECTIVE) {
                return det;
            } else if (role == Role.NONE) {
                return deadTeam;
            } else {
                return unknown;
            }
        }
    }
    public void setPlayerTeams() {
        PlayerSet plrs = game.getAlivePlayers();
        for (ServerPlayer plr : plrs) {
            Role role = getPlayerRole(plr);
            for (ServerPlayer otherPlr : plrs) {
                if (role == Role.INNOCENT || role == Role.DETECTIVE) {
                    plr.connection.send(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(det, true));
                    plr.connection.send(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(unknown, true));
                    plr.connection.send(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(deadTeam, true));
                    playerTeamPacket(getPlayerTeam(otherPlr, false), plr, otherPlr, ClientboundSetPlayerTeamPacket.Action.ADD);
                } else if (role == Role.SABOTEUR) {
                    plr.connection.send(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(sab, true));
                    plr.connection.send(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(det, true));
                    plr.connection.send(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(inno, true));
                    plr.connection.send(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(unknown, true));
                    plr.connection.send(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(deadTeam, true));
                    playerTeamPacket(getPlayerTeam(otherPlr, true), plr, otherPlr, ClientboundSetPlayerTeamPacket.Action.ADD);
                }
            }
        }
    }

    public void pickRoles() {
        PlayerSet plrs = game.getAlivePlayers();
        int playerCount = plrs.size();
        // need to make a new list from .toList to make it mutable
        List<ServerPlayer> plrList = new ArrayList<>(plrs.stream().toList());
        Collections.shuffle(plrList);
        int sabCount = Math.max(playerCount / 3, 1);
        int detCount = playerCount / 8;
        if (detCount < 1 && config.detectiveConfig().forceDetective()) {
            detCount = 1;
        }
        for (ServerPlayer plr : plrList) {
            if (detCount >= 1) {
                detectives.add(plr);
                detCount--;
            } else if (sabCount >= 1) {
                saboteurs.add(plr);
                sabCount--;
            } else {
                innocents.add(plr);
            }
        }
        initialSaboteurs = saboteurs.copy(gameSpace.getServer());
        innocents.showTitle(Component.translatable("sabotage.role_reveal", Component.translatable("sabotage.innocent").withStyle(ChatFormatting.GREEN)), 10, 80, 10);
        innocents.playSound(SoundEvents.ILLUSIONER_CAST_SPELL);
        detectives.showTitle(Component.translatable("sabotage.role_reveal", Component.translatable("sabotage.detective").withStyle(ChatFormatting.BLUE)), 10, 80, 10);
        detectives.playSound(SoundEvents.AMETHYST_BLOCK_CHIME);
        saboteurs.showTitle(Component.translatable("sabotage.role_reveal", Component.translatable("sabotage.saboteur").withStyle(ChatFormatting.RED)), 10, 80, 10);
        saboteurs.playSound(SoundEvents.ILLUSIONER_CAST_SPELL);
        saboteurs.playSound(SoundEvents.AMBIENT_SOUL_SAND_VALLEY_MOOD.value());

        // role colors
        for (ServerPlayer player: plrs) {
            for (ServerPlayer plr: plrs) {
                player.connection.send(updatePlayerName(plr, getPlayerRole(player) == Role.SABOTEUR));
            }
        }
        setPlayerTeams();
        // give detectives their portable tester
        for (ServerPlayer detective : detectives) {
            detective.getInventory().add(new ItemStack(DETECTIVE_SHEARS));
        }

        game.setSidebars();
    }

    public Role getPlayerRole(ServerPlayer plr) {
        if (innocents.contains(plr)) {
            return Role.INNOCENT;
        } else if (detectives.contains(plr)) {
            return Role.DETECTIVE;
        } else if (saboteurs.contains(plr)) {
            return Role.SABOTEUR;
        }
        return Role.NONE;
    }

    public static ChatFormatting getRoleColor(Role role) {
        return (role == Role.INNOCENT) ? ChatFormatting.GREEN :
                (role == Role.DETECTIVE) ? ChatFormatting.BLUE :
                        (role == Role.SABOTEUR) ? ChatFormatting.RED : ChatFormatting.RESET;
    }

}
