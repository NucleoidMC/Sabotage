package me.ellieis.Sabotage.game;

public enum GameStates {
    // waiting for players
    LOBBY_WAITING,
    // countdown before grace period
    COUNTDOWN,
    // grace period to collect chests
    // saboteurs / detectives not picked yet
    GRACE_PERIOD,
    // game loop here
    ACTIVE,
    ENDED
}
