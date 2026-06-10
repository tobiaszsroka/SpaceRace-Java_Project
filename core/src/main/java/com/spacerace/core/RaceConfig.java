package com.spacerace.core;

import com.spacerace.core.cars.CarCatalog;
import com.spacerace.core.track.TrackCatalog;

/** Player choices passed from the setup screen into the race. */
public final class RaceConfig {

    public final TrackCatalog.Entry track;
    public final CarCatalog.Entry carP1;
    public final CarCatalog.Entry carP2;
    public final int totalLaps;

    public RaceConfig(TrackCatalog.Entry track, CarCatalog.Entry carP1, CarCatalog.Entry carP2) {
        this(track, carP1, carP2, 3);
    }

    public RaceConfig(TrackCatalog.Entry track, CarCatalog.Entry carP1, CarCatalog.Entry carP2, int totalLaps) {
        this.track = track;
        this.carP1 = carP1;
        this.carP2 = carP2;
        this.totalLaps = totalLaps;
    }

    public String getMapPath() {
        return track.getMapPath();
    }
}
