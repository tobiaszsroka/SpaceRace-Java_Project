package com.spacerace.core.cars;

import com.badlogic.gdx.graphics.Color;

/** Selectable car color variants for the setup screen. */
public final class CarCatalog {

    public static final class Entry {
        public final String displayName;
        public final Color color;

        public Entry(String displayName, Color color) {
            this.displayName = displayName;
            this.color = color;
        }
    }

    public static final Entry[] CARS = {
            new Entry("Cyan", Color.CYAN),
            new Entry("Orange", Color.ORANGE),
            new Entry("Red", Color.RED),
            new Entry("Green", Color.GREEN),
            new Entry("Yellow", Color.YELLOW),
            new Entry("Magenta", Color.MAGENTA),
    };

    private CarCatalog() {}
}
