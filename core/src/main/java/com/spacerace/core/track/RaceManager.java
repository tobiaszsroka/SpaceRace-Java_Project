package com.spacerace.core.track;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.spacerace.core.entities.Car;

public class RaceManager {

    private final Array<Rectangle> checkpoints;
    private final int totalLaps;
    private boolean raceFinished;
    private Car winner;

    public RaceManager(Array<Rectangle> checkpoints, int totalLaps) {
        this.checkpoints = checkpoints;
        this.totalLaps = totalLaps;
    }

    public void update(Car... cars) {
        if (raceFinished) return;

        for (Car car : cars) {
            if (!car.isDriving()) continue;
            if (checkpoints.size == 0) continue;

            int nextCheckpoint = car.getCurrentCheckpoint();
            Rectangle zone = checkpoints.get(nextCheckpoint);

            if (zone.contains(car.getX(), car.getY())) {
                car.advanceCheckpoint(checkpoints.size);

                if (car.getLapsCompleted() >= totalLaps) {
                    raceFinished = true;
                    winner = car;
                }
            }
        }
    }

    public boolean isRaceFinished() { return raceFinished; }
    public Car getWinner() { return winner; }
    public int getTotalLaps() { return totalLaps; }
}
