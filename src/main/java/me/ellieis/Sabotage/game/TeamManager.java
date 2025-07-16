package me.ellieis.Sabotage.game;

import me.ellieis.Sabotage.game.config.SabotageConfig;
import me.ellieis.Sabotage.game.phase.SabotageActive;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.TeamS2CPacket;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
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
    HashMap<ServerPlayerEntity, Style> oldColors = new HashMap<>();
    HashMap<ServerPlayerEntity, Team> oldTeams = new HashMap<>();
    public Team sab;
    public Team det;
    public Team inno;
    public Team unknown;
    public Team deadTeam;
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
        this.sab = new Team(gameSpace.getServer().getScoreboard(), "sab");
        sab.setColor(Formatting.RED);
        this.det = new Team(gameSpace.getServer().getScoreboard(), "det");
        det.setColor(Formatting.BLUE);
        this.inno = new Team(gameSpace.getServer().getScoreboard(), "inno");
        inno.setColor(Formatting.GREEN);
        this.unknown = new Team(gameSpace.getServer().getScoreboard(), "unknown");
        unknown.setColor(Formatting.YELLOW);
        this.deadTeam = new Team(gameSpace.getServer().getScoreboard(), "dead");
        deadTeam.setColor(Formatting.GRAY);
        this.saboteurs = new MutablePlayerSet(gameSpace.getServer());
        this.detectives = new MutablePlayerSet(gameSpace.getServer());
        this.innocents = new MutablePlayerSet(gameSpace.getServer());
        this.dead = new MutablePlayerSet(gameSpace.getServer());
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
            playerTeamPacket(deadTeam, player, plr, TeamS2CPacket.Operation.ADD);
            if (oldTeams.get(plr) != null) {
                player.networkHandler.sendPacket(TeamS2CPacket.changePlayerTeam(oldTeams.get(plr), plr.getNameForScoreboard(), TeamS2CPacket.Operation.ADD));
            } else {
                player.networkHandler.sendPacket(TeamS2CPacket.changePlayerTeam(deadTeam, plr.getNameForScoreboard(), TeamS2CPacket.Operation.REMOVE));
            }
        });

        oldTeams.remove(player);
        oldColors.remove(player);
    }

    private Text formatPlayerName(ServerPlayerEntity player, Text name, boolean isSaboteur) {
        Style style;
        if (isSaboteur) {
            if (saboteurs.contains(player)) {
                style = Style.EMPTY.withColor(Formatting.RED);
            } else if (detectives.contains(player)) {
                style = Style.EMPTY.withColor(Formatting.BLUE);
            } else if (innocents.contains(player)) {
                style = Style.EMPTY.withColor(Formatting.GREEN);
            } else {
                style = Style.EMPTY.withColor(Formatting.GRAY);
            }
        }
        else {
            if (detectives.contains(player)) {
                style = Style.EMPTY.withColor(Formatting.BLUE);
            } else if (dead.contains(player)) {
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

    public Team getPlayerTeam(ServerPlayerEntity plr, boolean isSab) {
        Roles role = getPlayerRole(plr);
        if (isSab) {
            if (role == Roles.SABOTEUR) {
                return sab;
            } else if (role == Roles.DETECTIVE) {
                return det;
            } else if (role == Roles.INNOCENT) {
                return inno;
            } else {
                return deadTeam;
            }
        } else {
            if (role == Roles.DETECTIVE) {
                return det;
            } else if (role == Roles.NONE) {
                return deadTeam;
            } else {
                return unknown;
            }
        }
    }
    public void setPlayerTeams() {
        PlayerSet plrs = game.getAlivePlayers();
        for (ServerPlayerEntity plr : plrs) {
            Roles role = getPlayerRole(plr);
            for (ServerPlayerEntity otherPlr : plrs) {
                if (role == Roles.INNOCENT || role == Roles.DETECTIVE) {
                    plr.networkHandler.sendPacket(TeamS2CPacket.updateTeam(det, true));
                    plr.networkHandler.sendPacket(TeamS2CPacket.updateTeam(unknown, true));
                    plr.networkHandler.sendPacket(TeamS2CPacket.updateTeam(deadTeam, true));
                    playerTeamPacket(getPlayerTeam(otherPlr, false), plr, otherPlr, TeamS2CPacket.Operation.ADD);
                } else if (role == Roles.SABOTEUR) {
                    plr.networkHandler.sendPacket(TeamS2CPacket.updateTeam(sab, true));
                    plr.networkHandler.sendPacket(TeamS2CPacket.updateTeam(det, true));
                    plr.networkHandler.sendPacket(TeamS2CPacket.updateTeam(inno, true));
                    plr.networkHandler.sendPacket(TeamS2CPacket.updateTeam(unknown, true));
                    plr.networkHandler.sendPacket(TeamS2CPacket.updateTeam(deadTeam, true));
                    playerTeamPacket(getPlayerTeam(otherPlr, true), plr, otherPlr, TeamS2CPacket.Operation.ADD);
                }
            }
        }
    }

    public void pickRoles() {
        PlayerSet plrs = game.getAlivePlayers();
        int playerCount = plrs.size();
        // need to make a new list from .toList to make it mutable
        List<ServerPlayerEntity> plrList = new ArrayList<>(plrs.stream().toList());
        Collections.shuffle(plrList);
        int sabCount = Math.max(playerCount / 3, 1);
        int detCount = playerCount / 8;
        if (detCount < 1 && config.detectiveConfig().forceDetective()) {
            detCount = 1;
        }
        for (ServerPlayerEntity plr : plrList) {
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
        innocents.showTitle(Text.translatable("sabotage.role_reveal", Text.translatable("sabotage.innocent").formatted(Formatting.GREEN)), 10, 80, 10);
        innocents.playSound(SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL);
        detectives.showTitle(Text.translatable("sabotage.role_reveal", Text.translatable("sabotage.detective").formatted(Formatting.BLUE)), 10, 80, 10);
        detectives.playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME);
        saboteurs.showTitle(Text.translatable("sabotage.role_reveal", Text.translatable("sabotage.saboteur").formatted(Formatting.RED)), 10, 80, 10);
        saboteurs.playSound(SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL);
        saboteurs.playSound(SoundEvents.AMBIENT_SOUL_SAND_VALLEY_MOOD.value());

        // role colors
        for (ServerPlayerEntity player: plrs) {
            for (ServerPlayerEntity plr: plrs) {
                player.networkHandler.sendPacket(updatePlayerName(plr, getPlayerRole(player) == Roles.SABOTEUR));
            }
        }
        setPlayerTeams();
        // give detectives their portable tester
        for (ServerPlayerEntity detective : detectives) {
            detective.getInventory().insertStack(new ItemStack(DETECTIVE_SHEARS));
        }

        game.setSidebars();
    }

    public Roles getPlayerRole(ServerPlayerEntity plr) {
        if (innocents.contains(plr)) {
            return Roles.INNOCENT;
        } else if (detectives.contains(plr)) {
            return Roles.DETECTIVE;
        } else if (saboteurs.contains(plr)) {
            return Roles.SABOTEUR;
        }
        return Roles.NONE;
    }

    public static Formatting getRoleColor(Roles role) {
        return (role == Roles.INNOCENT) ? Formatting.GREEN :
                (role == Roles.DETECTIVE) ? Formatting.BLUE :
                        (role == Roles.SABOTEUR) ? Formatting.RED : Formatting.RESET;
    }

}
