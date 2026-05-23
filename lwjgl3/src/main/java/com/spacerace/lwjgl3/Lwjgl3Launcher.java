package com.spacerace.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.spacerace.core.SpaceRaceGame;

/**
 * Lwjgl3Launcher — desktop entry point for the SpaceRace game.
 *
 * Configures the LWJGL3 backend (window size, title, VSync)
 * and launches the {@link SpaceRaceGame} instance.
 */
public class Lwjgl3Launcher {

    public static void main(String[] args) {
        // Prevent new windows from appearing behind existing ones (macOS/Linux)
        if (StartupHelper.startNewJvmIfRequired()) return;

        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();

        // ── Window setup ──────────────────────────────────────────────
        config.setTitle("Space Race");
        config.setWindowedMode(1280, 720);   // 16:9 split → 640×720 per player
        config.setResizable(true);

        // ── Performance ───────────────────────────────────────────────
        config.useVsync(true);
        config.setForegroundFPS(60);

        // ── Window icon (optional — add icon files to assets/) ────────
        // config.setWindowIcon("icon128.png", "icon64.png", "icon32.png", "icon16.png");

        new Lwjgl3Application(new SpaceRaceGame(), config);
    }
}
