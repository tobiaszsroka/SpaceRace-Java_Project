package com.spacerace.core.track;

/** Available race tracks selectable from the setup screen. */
public final class TrackCatalog {

    public static final class Entry {
        public final String fileName;
        public final String displayName;
        public final String description;

        public Entry(String fileName, String displayName, String description) {
            this.fileName = fileName;
            this.displayName = displayName;
            this.description = description;
        }

        public String getMapPath() {
            return "maps/" + fileName;
        }
    }

    public static final Entry[] TRACKS = {
            new Entry(
                    "track_placeholder.tmx",
                    "Placeholder",
                    "Domyślny tor testowy zespołu"
            ),
            new Entry(
                    "track_space.tmx",
                    "Space Loop",
                    "Zamknięta pętla stadionowa"
            ),
            new Entry(
                    "track_switchback.tmx",
                    "Switchback Canyon",
                    "Tor z serpentynami i chicane"
            ),
            new Entry(
                    "track_figure_eight.tmx",
                    "Orbit Eight",
                    "Ósemka z metą na prostej startowej"
            ),
    };

    private TrackCatalog() {}
}
