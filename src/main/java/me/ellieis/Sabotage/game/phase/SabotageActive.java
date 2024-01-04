package me.ellieis.Sabotage.game.phase;

import com.google.common.collect.ImmutableSet;
import me.ellieis.Sabotage.game.SabotageConfig;
import me.ellieis.Sabotage.game.map.SabotageMap;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import xyz.nucleoid.plasmid.game.GameActivity;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.game.common.widget.SidebarWidget;
import xyz.nucleoid.plasmid.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.game.player.MutablePlayerSet;
import xyz.nucleoid.plasmid.game.player.PlayerSet;
import xyz.nucleoid.plasmid.game.rule.GameRuleType;
import xyz.nucleoid.plasmid.util.PlayerRef;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Set;

public class SabotageActive {
    private final SabotageConfig config;
    private final GameSpace gameSpace;
    private final SabotageMap map;
    private final ServerWorld world;
    private boolean gameStarted = false;
    private boolean countdown = false;
    private long startTime;
    private GameActivity activity;
    private final MutablePlayerSet saboteurs;
    private final MutablePlayerSet detectives;
    private final MutablePlayerSet innocents;
    private GlobalWidgets widgets;
    private SidebarWidget globalSidebar;
    private SidebarWidget innocentSidebar;
    private SidebarWidget detectiveSidebar;
    private SidebarWidget saboteurSidebar;
    public SabotageActive(SabotageConfig config, GameSpace gameSpace, SabotageMap map, ServerWorld world) {
        this.config = config;
        this.gameSpace = gameSpace;
        this.map = map;
        this.world = world;

        this.saboteurs = new MutablePlayerSet(gameSpace.getServer());
        this.detectives = new MutablePlayerSet(gameSpace.getServer());
        this.innocents = new MutablePlayerSet(gameSpace.getServer());
    }
    private static void gameStartedRules(GameActivity activity) {
        activity.allow(GameRuleType.FALL_DAMAGE);
        activity.allow(GameRuleType.PVP);
    }
    private static void rules(GameActivity activity) {
        activity.deny(GameRuleType.FALL_DAMAGE);
        activity.allow(GameRuleType.INTERACTION);
        activity.allow(GameRuleType.PICKUP_ITEMS);
        activity.allow(GameRuleType.MODIFY_ARMOR);
        activity.allow(GameRuleType.MODIFY_INVENTORY);
        activity.deny(GameRuleType.PVP);
        activity.allow(GameRuleType.THROW_ITEMS);
        activity.deny(GameRuleType.PORTALS);
        activity.deny(GameRuleType.HUNGER);
        activity.deny(GameRuleType.ICE_MELT);
        activity.deny(GameRuleType.PLACE_BLOCKS);
        activity.deny(GameRuleType.FIRE_TICK);
        activity.deny(GameRuleType.BREAK_BLOCKS);
        activity.deny(GameRuleType.CRAFTING);
    }

    public void pickRoles() {
        PlayerSet plrs = this.gameSpace.getPlayers();
        int playerCount = plrs.size();
        // need to make a new list from .toList to make it mutable
        List<ServerPlayerEntity> plrList = new ArrayList<>(plrs.stream().toList());
        Collections.shuffle(plrList);
        int sabCount = playerCount / 3;
        if (sabCount < 1) {
            sabCount = 1;
        }
        int detCount = playerCount / 8;

        for (ServerPlayerEntity plr : plrList) {
            if (detCount >= 1) {
                this.detectives.add(plr);
                detCount--;
            } else if (sabCount >= 1) {
                this.saboteurs.add(plr);
                sabCount--;
            } else {
                this.innocents.add(plr);
            }
        }
        this.innocents.showTitle(Text.translatable("sabotage.role_reveal", Text.translatable("sabotage.innocent").formatted(Formatting.GREEN)), 10, 80, 10);
        this.innocents.playSound(SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL);
        this.detectives.showTitle(Text.translatable("sabotage.role_reveal", Text.translatable("sabotage.detective").formatted(Formatting.DARK_BLUE)), 10, 80, 10);
        this.detectives.playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME);
        this.saboteurs.showTitle(Text.translatable("sabotage.role_reveal", Text.translatable("sabotage.saboteur").formatted(Formatting.RED)), 10, 80, 10);
        this.saboteurs.playSound(SoundEvents.AMBIENT_SOUL_SAND_VALLEY_MOOD.value());
    }
    public void Start() {
        this.gameStarted = true;
        pickRoles();
        gameStartedRules(this.activity);
    }
    public static void Open(GameSpace gameSpace, ServerWorld world, SabotageMap map, SabotageConfig config) {
        gameSpace.setActivity(activity -> {
            SabotageActive game = new SabotageActive(config, gameSpace, map, world);
            game.startTime = world.getTime();
            game.countdown = true;
            game.widgets = GlobalWidgets.addTo(activity);
            game.globalSidebar = game.widgets.addSidebar(Text.translatable("gameType.sabotage.sabotage"));
            rules(activity);
            activity.listen(GameActivityEvents.TICK, game::onTick);

            PlayerSet plrs = game.gameSpace.getPlayers();

            plrs.showTitle(Text.literal(Integer.toString(game.config.getCountdownTime())).formatted(Formatting.GOLD), 20);
            plrs.playSound(SoundEvents.BLOCK_NOTE_BLOCK_HARP.value(), SoundCategory.PLAYERS, 1.0F, 2.0F);
            plrs.forEach(plr -> {
                game.map.spawnEntity(world, plr);
                game.globalSidebar.addPlayer(plr);
            });

        });
    }

    public void onTick() {
        long time = this.world.getTime();
        if (!gameStarted) {
            if (this.countdown) {
                if (time % 20 == 0) {
                    // second has passed
                    PlayerSet plrs = this.gameSpace.getPlayers();
                    int secondsSinceStart = (int) Math.floor((time / 20) - (this.startTime / 20));
                    int countdownTime = this.config.getCountdownTime();
                    if (secondsSinceStart >= countdownTime) {
                        this.countdown = false;
                        plrs.playSound(SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE);
                        plrs.sendMessage(Text.translatable("sabotage.game_start", this.config.getGracePeriod()).formatted(Formatting.GOLD));
                    } else {
                        plrs.showTitle(Text.literal(Integer.toString(countdownTime - secondsSinceStart)).formatted(Formatting.GOLD), 20);
                        plrs.playSound(SoundEvents.BLOCK_NOTE_BLOCK_HARP.value(), SoundCategory.PLAYERS, 1.0F, 2.0F);
                    }
                }
                // Make sure players don't move during countdown
                for (ServerPlayerEntity plr : gameSpace.getPlayers()) {
                    Vec3d pos = this.map.getPlayerSpawns().get(new PlayerRef(plr.getUuid()));
                    // Set X and Y as relative so it will send 0 change when we pass yaw (yaw - yaw = 0) and pitch
                    Set<PositionFlag> flags = ImmutableSet.of(PositionFlag.X_ROT, PositionFlag.Y_ROT);
                    // Teleport without changing the pitch and yaw
                    plr.networkHandler.requestTeleport(pos.getX(), pos.getY(), pos.getZ(), plr.getYaw(), plr.getPitch(), flags);
                }
            } else {
                // to-do: implement grace period
                int secondsSinceStart = (int) Math.floor((time / 20) - (this.startTime / 20)) - this.config.getCountdownTime();
                if (secondsSinceStart >= this.config.getGracePeriod()) {
                    Start();
                }
            }
        } else {
            // to-do: implement game loop
        }
    }
}
