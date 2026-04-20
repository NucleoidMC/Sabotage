package me.ellieis.Sabotage.game.utils;

import net.minecraft.world.level.Level;
import xyz.nucleoid.plasmid.api.game.GameSpace;

import java.util.ArrayList;


public class TaskScheduler {
    private final GameSpace gameSpace;
    private final Level level;
    private final ArrayList<Task> tasks = new ArrayList<>();

    public TaskScheduler(GameSpace gameSpace, Level level) {
        this.gameSpace = gameSpace;
        this.level = level;
    }

    public ArrayList<Task> getTasks() {
        return tasks;
    }

    public void addTask(Task task) {
        tasks.add(task);
    }

    public void onTick() {
        long time = level.getGameTime();
        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);
            if (task.executionTime() <= time) {
                task.task().accept(gameSpace);
                tasks.remove(task);
            }
        }
    }

    public void onGameEnd() {
        for (Task task : tasks) {
            task.task().accept(gameSpace);
        }
    }
}
