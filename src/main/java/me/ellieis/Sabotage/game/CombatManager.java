package me.ellieis.Sabotage.game;

import me.ellieis.Sabotage.game.config.DetectiveConfig;
import me.ellieis.Sabotage.game.config.InnocentConfig;
import me.ellieis.Sabotage.game.config.SabotageConfig;
import me.ellieis.Sabotage.game.config.SaboteurConfig;
import me.ellieis.Sabotage.game.phase.SabotageActive;
import me.ellieis.Sabotage.game.statistics.KarmaManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.network.packet.s2c.play.TeamS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.GameSpacePlayers;
import xyz.nucleoid.stimuli.event.EventResult;

import java.util.ArrayList;
import java.util.HashMap;

record CombatLog(ServerPlayerEntity attacker, float damage, long timeOfAttack) {

}

public class CombatManager {
    GameSpace gameSpace;
    KarmaManager karmaManager;
    TeamManager teamManager;
    SabotageConfig config;
    SabotageActive game;
    private final HashMap<ServerPlayerEntity, ArrayList<CombatLog>> playerDamageLog = new HashMap<>();
    public CombatManager(GameSpace gameSpace, TeamManager teamManager, KarmaManager karmaManager, SabotageConfig config, SabotageActive game) {
        this.gameSpace = gameSpace;
        this.teamManager = teamManager;
        this.karmaManager = karmaManager;
        this.config = config;
        this.game = game;
        gameSpace.getPlayers().forEach((plr) -> playerDamageLog.put(plr, new ArrayList<>()));
    }

    public void onDamage(ServerPlayerEntity plr, DamageSource damageSource, float damageAmount) {
        Roles receiverRole = teamManager.getPlayerRole(plr);
        if (receiverRole == Roles.DETECTIVE) {
            Entity attackerEntity = damageSource.getAttacker();
            if (attackerEntity instanceof ServerPlayerEntity attacker) {
                Roles attackerRole = teamManager.getPlayerRole(attacker);
                if (attackerRole != Roles.SABOTEUR) {
                    attacker.sendMessage(Text.translatable("sabotage.damage_detective_message", Text.translatable("sabotage.detective").formatted(Formatting.BLUE)));
                    attacker.playSound(SoundEvents.BLOCK_ANVIL_PLACE, 1, 0.5f);
                }
            }
        }

        Entity attackerEntity = damageSource.getAttacker();
        if (attackerEntity instanceof ServerPlayerEntity attacker) {
            ArrayList<CombatLog> log = playerDamageLog.get(plr);
            if (log != null) {
                log.add(new CombatLog(attacker, damageAmount, gameSpace.getTime()));
            }
        }
    }

    public EventResult onDeath(ServerPlayerEntity plr, DamageSource damageSource) {
        Entity entityAttacker = damageSource.getAttacker();
        Roles plrRole = teamManager.getPlayerRole(plr);
        plr.changeGameMode(GameMode.SPECTATOR);
        plr.playSound(SoundEvents.ENTITY_COW_DEATH, 1, 0.7f);
        teamManager.dead.add(plr);
        GameSpacePlayers plrSet = gameSpace.getPlayers();
        plrSet.forEach((otherPlr) -> teamManager.playerTeamPacket(teamManager.deadTeam, otherPlr, plr, TeamS2CPacket.Operation.ADD));
        if (plrRole == Roles.SABOTEUR) {
            plrSet.forEach((otherPlr) -> {
                Roles role = teamManager.getPlayerRole(otherPlr);
                teamManager.playerTeamPacket((role == Roles.DETECTIVE) ?
                                teamManager.det : (role == Roles.NONE) ?
                                teamManager.deadTeam : teamManager.unknown,
                        plr, otherPlr, TeamS2CPacket.Operation.ADD);
            });
        }

        if (game.gameState != GameStates.ACTIVE) {
            return EventResult.DENY;
        }
        ArrayList<CombatLog> log = playerDamageLog.get(plr);
        HashMap<ServerPlayerEntity, Float> accumulatedDamage = new HashMap<>();
        if (log != null) {
            // filter damage that was more than a minute ago
            log.stream().filter((combatLog) -> (gameSpace.getTime() - combatLog.timeOfAttack() <= 1200)).forEach((combatLog -> {
                float damage = 0;
                if (accumulatedDamage.containsKey(combatLog.attacker())) {
                    damage = accumulatedDamage.get(combatLog.attacker());
                } else  {
                    accumulatedDamage.put(combatLog.attacker(), damage);
                }
                damage += combatLog.damage();
                accumulatedDamage.replace(combatLog.attacker(), damage);
            }));
        }
        ServerPlayerEntity finalHit;
        if (entityAttacker instanceof ServerPlayerEntity attacker) {
            finalHit = attacker;
        } else {
            finalHit = null;
        }
        accumulatedDamage.forEach((ServerPlayerEntity attacker, Float damage) -> {
            Roles attackerRole = teamManager.getPlayerRole(attacker);
            float karmaRatio = damage / plr.getMaxHealth();
            boolean isAssist = true;
            if (finalHit != null && finalHit == attacker) {
                karmaRatio = 1;
                isAssist = false;
            }
            // surely there's a better way to do this..
            switch(attackerRole) {
                case SABOTEUR -> {
                    SaboteurConfig config = this.config.saboteurConfig();
                    switch(plrRole) {
                        case INNOCENT -> {
                            karmaManager.incrementKarma(attacker, config.innocentKarmaAward());
                            attacker.playSound(SoundEvents.BLOCK_TRIAL_SPAWNER_OPEN_SHUTTER, 1, 1f);
                            attacker.sendMessage(createAttackerKillMessage(plr, Math.round(config.innocentKarmaAward() * karmaRatio), isAssist));
                        }

                        case DETECTIVE -> {
                            karmaManager.incrementKarma(attacker, config.detectiveKarmaAward());
                            attacker.playSound(SoundEvents.BLOCK_TRIAL_SPAWNER_OPEN_SHUTTER, 1, 1f);
                            attacker.playSound(SoundEvents.BLOCK_TRIAL_SPAWNER_DETECT_PLAYER, 0.5f, 1f);
                            attacker.sendMessage(createAttackerKillMessage(plr, Math.round(config.detectiveKarmaAward() * karmaRatio), isAssist));
                        }

                        case SABOTEUR -> {
                            karmaManager.decrementKarma(attacker, config.saboteurKarmaPenalty());
                            attacker.playSound(SoundEvents.BLOCK_TRIAL_SPAWNER_DETECT_PLAYER, 1, 1f);
                            attacker.sendMessage(createAttackerKillMessage(plr, Math.round(-config.saboteurKarmaPenalty() * karmaRatio), isAssist));
                        }
                    }
                }

                case DETECTIVE -> {
                    DetectiveConfig config = this.config.detectiveConfig();
                    awardPlayerKill(attacker, plr, plrRole, Math.round(config.innocentKarmaPenalty() * karmaRatio), Math.round(config.detectiveKarmaPenalty() * karmaRatio), Math.round(config.saboteurKarmaAward() * karmaRatio), isAssist);
                }

                case INNOCENT -> {
                    InnocentConfig config = this.config.innocentConfig();
                    awardPlayerKill(attacker, plr, plrRole, Math.round(config.innocentKarmaPenalty() * karmaRatio), Math.round(config.detectiveKarmaPenalty() * karmaRatio), Math.round(config.saboteurKarmaAward() * karmaRatio), isAssist);
                }
            }
        });

        if (plrRole == Roles.SABOTEUR) {
            teamManager.saboteurs.remove(plr);
            game.saboteurSidebar.removePlayer(plr);
        } else if (plrRole == Roles.DETECTIVE) {
            teamManager.detectives.remove(plr);
            game.detectiveSidebar.removePlayer(plr);
        } else if (plrRole == Roles.INNOCENT) {
            teamManager.innocents.remove(plr);
            game.innocentSidebar.removePlayer(plr);
        }

        return EventResult.PASS;
    }

    private Text createAttackerKillMessage(ServerPlayerEntity plr, int karma, boolean assist) {
        Roles role = teamManager.getPlayerRole(plr);
        Formatting victimColor = TeamManager.getRoleColor(role);
        if (assist) {
            return Text.translatable("sabotage.kill_message.assist", plr.getName().copy().formatted(victimColor), Text.literal("(" + karma + " karma)").formatted((karma >= 0) ? Formatting.GREEN : Formatting.RED)).formatted(Formatting.YELLOW);
        }
        return Text.translatable(
                "sabotage.kill_message_attacker",
                plr.getName().copy().formatted(victimColor),
                Text.literal("(" + karma + " karma)").formatted((karma >= 0) ? Formatting.GREEN : Formatting.RED)).formatted(Formatting.YELLOW);
    }

    private void awardPlayerKill(ServerPlayerEntity attacker, ServerPlayerEntity plr, Roles plrRole, int innocentKarma, int detectiveKarma, int saboteurKarma, boolean assist) {
        // attacker is confirmed innocent or detective
        switch(plrRole) {
            case INNOCENT -> {
                karmaManager.decrementKarma(attacker, innocentKarma);
                attacker.playSound(SoundEvents.BLOCK_TRIAL_SPAWNER_DETECT_PLAYER, 1, 1);
                attacker.sendMessage(createAttackerKillMessage(plr, -innocentKarma, assist));
            }

            case DETECTIVE -> {
                karmaManager.decrementKarma(attacker, detectiveKarma);
                attacker.playSound(SoundEvents.BLOCK_TRIAL_SPAWNER_DETECT_PLAYER, 1, 1);
                attacker.sendMessage(createAttackerKillMessage(plr, -detectiveKarma, assist));
            }

            case SABOTEUR -> {
                karmaManager.incrementKarma(attacker, saboteurKarma);
                attacker.playSound(SoundEvents.BLOCK_TRIAL_SPAWNER_OPEN_SHUTTER, 1, 1);
                attacker.sendMessage(createAttackerKillMessage(plr, saboteurKarma, assist));
            }
        }
    }

}
