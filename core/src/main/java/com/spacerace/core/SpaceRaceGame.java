package com.spacerace.core;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.spacerace.core.screens.MainMenuScreen;

/**
 * SpaceRaceGame — the main application entry point.
 *
 * Extends {@link Game} to leverage built-in screen management.
 * A shared {@link SpriteBatch} is created here and passed to all screens
 * so we avoid creating multiple batches (which is expensive).
 */
public class SpaceRaceGame extends Game {

    /**
     * Shared SpriteBatch used across all screens.
     * Created once in {@link #create()} and disposed in {@link #dispose()}.
     */
    private SpriteBatch batch;

    // ── Virtual resolution constants ──────────────────────────────────
    // These define the logical world size that cameras will use.
    // Each player's viewport will show this much of the world.
    public static final float WORLD_WIDTH  = 800f;
    public static final float WORLD_HEIGHT = 600f;

    @Override
    public void create() {
        batch = new SpriteBatch();

        // Start the game on the Main Menu screen
        setScreen(new MainMenuScreen(this));
    }

    /**
     * Returns the shared SpriteBatch instance.
     * Screens should use this instead of creating their own.
     */
    public SpriteBatch getBatch() {
        return batch;
    }

    @Override
    public void dispose() {
        // Dispose the current screen (if any)
        super.dispose();

        // Clean up the shared batch
        if (batch != null) {
            batch.dispose();
        }
    }
}
