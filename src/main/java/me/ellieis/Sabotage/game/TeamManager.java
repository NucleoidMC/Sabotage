package me.ellieis.Sabotage.game;

import me.ellieis.Sabotage.game.phase.SabotageActive;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.TeamS2CPacket;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.mixin.chat.PlayerListS2CPacketEntryAccessor;

import java.util.HashMap;


public class TeamManager {

    GameSpace gameSpace;
    GameActivity gameActivity;
    SabotageActive game;
    HashMap<ServerPlayerEntity, Style> oldColors = new HashMap<>();
    public TeamManager(GameSpace gameSpace, GameActivity gameActivity, SabotageActive game) {
        this.gameSpace = gameSpace;
        this.gameActivity = gameActivity;
        gameActivity.listen(GamePlayerEvents.ADD, this::onAddPlayer);
        gameActivity.listen(GamePlayerEvents.REMOVE, this::onRemovePlayer);
        this.game = game;
    }

    private void onAddPlayer(ServerPlayerEntity player) {
        var name = player.getPlayerListName();
        if (name == null) {
            name = player.getName();
        }
        oldColors.put(player, name.getStyle());
    }

    private void onRemovePlayer(ServerPlayerEntity player) {
        var name = player.getPlayerListName();
        if (name == null) {
            name = player.getName();
        }

        player.getServer().getPlayerManager().sendToAll(this.createPlayerListPacket(player, Text.empty().append(name.copy().setStyle(oldColors.get(player)))));
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
}
