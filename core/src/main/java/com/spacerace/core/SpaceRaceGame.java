package com.spacerace.core;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.spacerace.core.screens.MainMenuScreen;

/**
 * Main application entry point. Manages screen transitions
 * and holds the shared SpriteBatch used by all screens.
 */
public class SpaceRaceGame extends Game {

    private SpriteBatch batch;

    public static final float WORLD_WIDTH = 800f;
    public static final float WORLD_HEIGHT = 600f;

    @Override
    public void create() {
        batch = new SpriteBatch();
        setScreen(new MainMenuScreen(this));
    }

    public SpriteBatch getBatch() {
        return batch;
    }

    @Override
    public void dispose() {
        super.dispose();
        if (batch != null) batch.dispose();
    }
}
