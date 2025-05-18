package me.ellieis.Sabotage.game;

import me.ellieis.Sabotage.game.phase.SabotageActive;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.TeamS2CPacket;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.apache.logging.log4j.core.jmx.Server;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.GameSpacePlayers;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.PlayerSet;
import xyz.nucleoid.plasmid.mixin.chat.PlayerListS2CPacketEntryAccessor;

import java.util.HashMap;


public class TeamManager {

    GameSpace gameSpace;
    GameActivity gameActivity;
    SabotageActive game;
    HashMap<ServerPlayerEntity, Style> oldColors = new HashMap<>();
    HashMap<ServerPlayerEntity, Team> oldTeams = new HashMap<>();
    public Team sab;
    public Team det;
    public Team inno;
    public Team unknown;
    public Team dead;
    public TeamManager(GameSpace gameSpace, GameActivity gameActivity, SabotageActive game) {
        this.gameSpace = gameSpace;
        this.gameActivity = gameActivity;
        gameActivity.listen(GamePlayerEvents.ADD, this::onAddPlayer);
        gameActivity.listen(GamePlayerEvents.REMOVE, this::onRemovePlayer);
        this.game = game;
        this.sab = new Team(gameSpace.getServer().getScoreboard(), "sab");
        sab.setColor(Formatting.RED);
        this.det = new Team(gameSpace.getServer().getScoreboard(), "det");
        det.setColor(Formatting.BLUE);
        this.inno = new Team(gameSpace.getServer().getScoreboard(), "inno");
        inno.setColor(Formatting.GREEN);
        this.unknown = new Team(gameSpace.getServer().getScoreboard(), "unknown");
        unknown.setColor(Formatting.YELLOW);
        this.dead = new Team(gameSpace.getServer().getScoreboard(), "dead");
        dead.setColor(Formatting.GRAY);
    }

    private void onAddPlayer(ServerPlayerEntity player) {
        var name = player.getPlayerListName();
        if (name == null) {
            name = player.getName();
        }
        oldTeams.put(player, player.getScoreboardTeam());
        oldColors.put(player, name.getStyle());
    }

    private void onRemovePlayer(ServerPlayerEntity player) {
        var name = player.getPlayerListName();
        if (name == null) {
            name = player.getName();
        }
        GameSpacePlayers plrs = gameSpace.getPlayers();
        plrs.sendPacket(this.createPlayerListPacket(player, Text.empty().append(name.copy().setStyle(oldColors.get(player)))));
        plrs.forEach((plr) -> {
            playerTeamPacket(dead, player, plr, TeamS2CPacket.Operation.ADD);
            if (oldTeams.get(plr) != null) {
                player.networkHandler.sendPacket(TeamS2CPacket.changePlayerTeam(oldTeams.get(plr), plr.getNameForScoreboard(), TeamS2CPacket.Operation.ADD));
            } else {
                player.networkHandler.sendPacket(TeamS2CPacket.changePlayerTeam(dead, plr.getNameForScoreboard(), TeamS2CPacket.Operation.REMOVE));
            }
        });

        oldTeams.remove(player);
        oldColors.remove(player);
    }

    private Text formatPlayerName(ServerPlayerEntity player, Text name, boolean isSaboteur) {
        Style style;
        if (isSaboteur) {
            if (game.saboteurs.contains(player)) {
                style = Style.EMPTY.withColor(Formatting.RED);
            } else if (game.detectives.contains(player)) {
                style = Style.EMPTY.withColor(Formatting.BLUE);
            } else if (game.innocents.contains(player)) {
                style = Style.EMPTY.withColor(Formatting.GREEN);
            } else {
                style = Style.EMPTY.withColor(Formatting.GRAY);
            }
        }
        else {
            if (game.detectives.contains(player)) {
                style = Style.EMPTY.withColor(Formatting.BLUE);
            } else if (game.dead.contains(player)) {
                style = Style.EMPTY.withColor(Formatting.GRAY);
            } else {
                style = Style.EMPTY.withColor(Formatting.YELLOW);
            }
        }
        return Text.empty().append(name.copy().setStyle(style));
    }
    public PlayerListS2CPacket createPlayerListPacket(ServerPlayerEntity player, Text playerName) {
        var packet = new PlayerListS2CPacket(PlayerListS2CPacket.Action.UPDATE_DISPLAY_NAME, player);

        var entry = packet.getEntries().get(0);
        var name = player.getPlayerListName();
        if (name == null) {
            name = player.getName();
        }
        ((PlayerListS2CPacketEntryAccessor) (Object) entry).setDisplayName(playerName);

        return packet;
    }
    public PlayerListS2CPacket updatePlayerName(ServerPlayerEntity player, boolean isSaboteur) {
        var name = player.getPlayerListName();
        if (name == null) {
            name = player.getName();
        }
        return this.createPlayerListPacket(player, this.formatPlayerName(player, name, isSaboteur));
    }

    public void playerTeamPacket(Team team, ServerPlayerEntity plr, ServerPlayerEntity otherPlr, TeamS2CPacket.Operation operation) {
        plr.networkHandler.sendPacket(TeamS2CPacket.changePlayerTeam(team, otherPlr.getNameForScoreboard(), operation));
    }

    public void removePlayerTeam(ServerPlayerEntity plr, boolean broadcast) {
        if (broadcast) {
            for (ServerPlayerEntity otherPlr : gameSpace.getPlayers()) {
                System.out.println(plr.getNameForScoreboard() +  ": " + game.getPlayerRole(plr).toString());
                System.out.println(otherPlr.getNameForScoreboard() +  ": " + game.getPlayerRole(otherPlr).toString());
                playerTeamPacket(getPlayerTeam(plr, game.getPlayerRole(otherPlr) == Roles.SABOTEUR), otherPlr, plr, TeamS2CPacket.Operation.REMOVE);
            };
        } else {
            playerTeamPacket(getPlayerTeam(plr, game.getPlayerRole(plr) == Roles.SABOTEUR), plr, plr, TeamS2CPacket.Operation.REMOVE);
        }
    }
    public Team getPlayerTeam(ServerPlayerEntity plr, boolean isSab) {
        Roles role = game.getPlayerRole(plr);
        if (isSab) {
            if (role == Roles.SABOTEUR) {
                return sab;
            } else if (role == Roles.DETECTIVE) {
                return det;
            } else if (role == Roles.INNOCENT) {
                return inno;
            } else {
                return dead;
            }
        } else {
            if (role == Roles.DETECTIVE) {
                return det;
            } else if (role == Roles.NONE) {
                return dead;
            } else {
                return unknown;
            }
        }
    }
    public void setPlayerTeams() {
        PlayerSet plrs = game.getAlivePlayers();
        for (ServerPlayerEntity plr : plrs) {
            Roles role = game.getPlayerRole(plr);
            for (ServerPlayerEntity otherPlr : plrs) {
                if (role == Roles.INNOCENT || role == Roles.DETECTIVE) {
                    plr.networkHandler.sendPacket(TeamS2CPacket.updateTeam(det, true));
                    plr.networkHandler.sendPacket(TeamS2CPacket.updateTeam(unknown, true));
                    plr.networkHandler.sendPacket(TeamS2CPacket.updateTeam(dead, true));
                    playerTeamPacket(getPlayerTeam(otherPlr, false), plr, otherPlr, TeamS2CPacket.Operation.ADD);
                } else if (role == Roles.SABOTEUR) {
                    plr.networkHandler.sendPacket(TeamS2CPacket.updateTeam(sab, true));
                    plr.networkHandler.sendPacket(TeamS2CPacket.updateTeam(det, true));
                    plr.networkHandler.sendPacket(TeamS2CPacket.updateTeam(inno, true));
                    plr.networkHandler.sendPacket(TeamS2CPacket.updateTeam(unknown, true));
                    plr.networkHandler.sendPacket(TeamS2CPacket.updateTeam(dead, true));
                    playerTeamPacket(getPlayerTeam(otherPlr, true), plr, otherPlr, TeamS2CPacket.Operation.ADD);
                }
            }
        }
    }
}
