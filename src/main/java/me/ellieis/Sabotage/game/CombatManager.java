package me.ellieis.Sabotage.game;

import me.ellieis.Sabotage.game.config.DetectiveConfig;
import me.ellieis.Sabotage.game.config.InnocentConfig;
import me.ellieis.Sabotage.game.config.SabotageConfig;
import me.ellieis.Sabotage.game.config.SaboteurConfig;
import me.ellieis.Sabotage.game.phase.SabotageActive;
import me.ellieis.Sabotage.game.statistics.KarmaManager;
import me.ellieis.Sabotage.mixin.MannequinAccessor;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.world.entity.animal.cow.CowSoundVariants;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.GameType;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.GameSpacePlayers;
import xyz.nucleoid.stimuli.event.EventResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

public class CombatManager {
    GameSpace gameSpace;
    KarmaManager karmaManager;
    TeamManager teamManager;
    SabotageConfig config;
    SabotageActive game;
    private final HashMap<ServerPlayer, BodyData> bodies = new HashMap<>();

    private final HashMap<ServerPlayer, ArrayList<CombatLog>> playerDamageLog = new HashMap<>();

    public CombatManager(GameSpace gameSpace, TeamManager teamManager, KarmaManager karmaManager, SabotageConfig config, SabotageActive game) {
        this.gameSpace = gameSpace;
        this.teamManager = teamManager;
        this.karmaManager = karmaManager;
        this.config = config;
        this.game = game;
        gameSpace.getPlayers().forEach((plr) -> playerDamageLog.put(plr, new ArrayList<>()));
    }

    public void onDamage(ServerPlayer plr, DamageSource damageSource, float damageAmount) {
        Roles receiverRole = teamManager.getPlayerRole(plr);
        if (receiverRole == Roles.DETECTIVE) {
            Entity attackerEntity = damageSource.getEntity();
            if (attackerEntity instanceof ServerPlayer attacker) {
                Roles attackerRole = teamManager.getPlayerRole(attacker);
                if (attackerRole != Roles.SABOTEUR) {
                    attacker.sendSystemMessage(Component.translatable("sabotage.damage_detective_message", Component.translatable("sabotage.detective").withStyle(ChatFormatting.BLUE)));
                    attacker.playSound(SoundEvents.ANVIL_PLACE, 1, 0.5f);
                }
            }
        }

        Entity attackerEntity = damageSource.getEntity();
        if (attackerEntity instanceof ServerPlayer attacker) {
            ArrayList<CombatLog> log = playerDamageLog.get(plr);
            if (log != null) {
                log.add(new CombatLog(attacker, damageAmount, gameSpace.getTime()));
            }
        }
    }

    public EventResult onDeath(ServerPlayer plr, DamageSource damageSource) {
        Entity entityAttacker = damageSource.getEntity();
        Roles plrRole = teamManager.getPlayerRole(plr);
        plr.setGameMode(GameType.SPECTATOR);
        plr.playSound(SoundEvents.COW_SOUNDS.get(CowSoundVariants.SoundSet.CLASSIC).deathSound().value(), 1, 0.7f);
        createPlayerBody(plr, game.getWorld(), plrRole);
        teamManager.dead.add(plr);
        GameSpacePlayers plrSet = gameSpace.getPlayers();
        plrSet.forEach((otherPlr) -> teamManager.playerTeamPacket(teamManager.deadTeam, otherPlr, plr, ClientboundSetPlayerTeamPacket.Action.ADD));
        if (plrRole == Roles.SABOTEUR) {
            plrSet.forEach((otherPlr) -> {
                Roles role = teamManager.getPlayerRole(otherPlr);
                teamManager.playerTeamPacket((role == Roles.DETECTIVE) ?
                                teamManager.det : (role == Roles.NONE) ?
                                teamManager.deadTeam : teamManager.unknown,
                        plr, otherPlr, ClientboundSetPlayerTeamPacket.Action.ADD);
            });
        }

        if (game.gameState != GameStates.ACTIVE) {
            return EventResult.DENY;
        }
        ArrayList<CombatLog> log = playerDamageLog.get(plr);
        HashMap<ServerPlayer, Float> accumulatedDamage = new HashMap<>();
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
        ServerPlayer finalHit;
        if (entityAttacker instanceof ServerPlayer attacker) {
            finalHit = attacker;
        } else {
            finalHit = null;
        }
        accumulatedDamage.forEach((ServerPlayer attacker, Float damage) -> {
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
                            attacker.playSound(SoundEvents.TRIAL_SPAWNER_OPEN_SHUTTER, 1, 1f);
                            attacker.sendSystemMessage(createAttackerKillMessage(plr, Math.round(config.innocentKarmaAward() * karmaRatio), isAssist));
                        }

                        case DETECTIVE -> {
                            karmaManager.incrementKarma(attacker, config.detectiveKarmaAward());
                            attacker.playSound(SoundEvents.TRIAL_SPAWNER_OPEN_SHUTTER, 1, 1f);
                            attacker.playSound(SoundEvents.TRIAL_SPAWNER_DETECT_PLAYER, 0.5f, 1f);
                            attacker.sendSystemMessage(createAttackerKillMessage(plr, Math.round(config.detectiveKarmaAward() * karmaRatio), isAssist));
                        }

                        case SABOTEUR -> {
                            karmaManager.decrementKarma(attacker, config.saboteurKarmaPenalty());
                            attacker.playSound(SoundEvents.TRIAL_SPAWNER_DETECT_PLAYER, 1, 1f);
                            attacker.sendSystemMessage(createAttackerKillMessage(plr, Math.round(-config.saboteurKarmaPenalty() * karmaRatio), isAssist));
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

    private Component createAttackerKillMessage(ServerPlayer plr, int karma, boolean assist) {
        Roles role = teamManager.getPlayerRole(plr);
        ChatFormatting victimColor = TeamManager.getRoleColor(role);
        if (assist) {
            return Component.translatable("sabotage.kill_message.assist", plr.getName().copy().withStyle(victimColor), Component.literal("(" + karma + " karma)").withStyle((karma >= 0) ? ChatFormatting.GREEN : ChatFormatting.RED)).withStyle(ChatFormatting.YELLOW);
        }
        return Component.translatable(
                "sabotage.kill_message_attacker",
                plr.getName().copy().withStyle(victimColor),
                Component.literal("(" + karma + " karma)").withStyle((karma >= 0) ? ChatFormatting.GREEN : ChatFormatting.RED)).withStyle(ChatFormatting.YELLOW);
    }

    private void awardPlayerKill(ServerPlayer attacker, ServerPlayer plr, Roles plrRole, int innocentKarma, int detectiveKarma, int saboteurKarma, boolean assist) {
        // attacker is confirmed innocent or detective
        switch(plrRole) {
            case INNOCENT -> {
                karmaManager.decrementKarma(attacker, innocentKarma);
                attacker.playSound(SoundEvents.TRIAL_SPAWNER_DETECT_PLAYER, 1, 1);
                attacker.sendSystemMessage(createAttackerKillMessage(plr, -innocentKarma, assist));
            }

            case DETECTIVE -> {
                karmaManager.decrementKarma(attacker, detectiveKarma);
                attacker.playSound(SoundEvents.TRIAL_SPAWNER_DETECT_PLAYER, 1, 1);
                attacker.sendSystemMessage(createAttackerKillMessage(plr, -detectiveKarma, assist));
            }

            case SABOTEUR -> {
                karmaManager.incrementKarma(attacker, saboteurKarma);
                attacker.playSound(SoundEvents.TRIAL_SPAWNER_OPEN_SHUTTER, 1, 1);
                attacker.sendSystemMessage(createAttackerKillMessage(plr, saboteurKarma, assist));
            }
        }
    }

    private void createPlayerBody(ServerPlayer plr, ServerLevel world, Roles plrRole) {
        Mannequin mannequin = new Mannequin(EntityType.MANNEQUIN, world);
        ((MannequinAccessor) mannequin).sabotage$setMannequinProfile(ResolvableProfile.createUnresolved(plr.getUUID()));
        mannequin.setPosRaw(plr.getX(), plr.getY(), plr.getZ());
        mannequin.setPose(Pose.SLEEPING);
        BodyData bodyData = new BodyData(plrRole, mannequin);
        world.addFreshEntity(mannequin);
        bodies.put(plr, bodyData);

    }

    public BodyResult getBodyRole(LivingEntity entity) {
        AtomicReference<Roles> role = new AtomicReference<>(Roles.NONE);
        AtomicReference<ServerPlayer> plrAtom = new AtomicReference<>();
        if (entity instanceof Mannequin) {
            bodies.forEach((plr, bodyData) -> {
                if (bodyData.mannequin().equals(entity)) {
                    role.set(bodyData.role());
                    plrAtom.set(plr);
                }
            });
        }
        return new BodyResult(plrAtom.get(), role.get());
    }

    record CombatLog(ServerPlayer attacker, float damage, long timeOfAttack) {

    }

    record BodyData(Roles role, Mannequin mannequin) {
    }
}
