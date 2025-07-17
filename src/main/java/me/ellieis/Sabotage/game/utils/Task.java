package me.ellieis.Sabotage.game.utils;

import xyz.nucleoid.plasmid.api.game.GameSpace;

import java.util.function.Consumer;

public record Task (int executionTime, Consumer<GameSpace> task) {
}