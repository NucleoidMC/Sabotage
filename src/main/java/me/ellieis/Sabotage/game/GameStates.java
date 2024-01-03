package me.ellieis.Sabotage.game;

public enum GameStates {
    // waiting for players
    LOBBY_WAITING,
    // grace period to collect chests
    // saboteurs / detectives not picked yet
    GRACE_PERIOD,
    // game loop here
    ACTIVE,
    ENDED
}
