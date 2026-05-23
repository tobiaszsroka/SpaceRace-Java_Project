package com.spacerace.core.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.spacerace.core.SpaceRaceGame;

/**
 * MainMenuScreen — the first screen the player sees.
 *
 * Displays a title and a prompt to start the game.
 * Pressing SPACE transitions to the {@link GameScreen}.
 */
public class MainMenuScreen implements Screen {

    // ── References ────────────────────────────────────────────────────
    private final SpaceRaceGame game;
    private final SpriteBatch batch;

    // ── Camera & Viewport ─────────────────────────────────────────────
    private final OrthographicCamera camera;
    private final Viewport viewport;

    // ── Font for menu text ────────────────────────────────────────────
    private BitmapFont titleFont;
    private BitmapFont promptFont;

    public MainMenuScreen(SpaceRaceGame game) {
        this.game  = game;
        this.batch = game.getBatch();

        // Camera centered on the virtual world
        camera = new OrthographicCamera();
        viewport = new FitViewport(SpaceRaceGame.WORLD_WIDTH, SpaceRaceGame.WORLD_HEIGHT, camera);
        camera.position.set(SpaceRaceGame.WORLD_WIDTH / 2f, SpaceRaceGame.WORLD_HEIGHT / 2f, 0);
        camera.update();
    }

    @Override
    public void show() {
        // Create fonts (LibGDX default bitmap font)
        titleFont  = new BitmapFont();
        titleFont.setColor(Color.WHITE);
        titleFont.getData().setScale(3f);

        promptFont = new BitmapFont();
        promptFont.setColor(Color.LIGHT_GRAY);
        promptFont.getData().setScale(1.5f);
    }

    @Override
    public void render(float delta) {
        // ── Input ─────────────────────────────────────────────────────
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            game.setScreen(new GameScreen(game));
            dispose(); // clean up menu resources
            return;
        }

        // ── Clear screen ──────────────────────────────────────────────
        ScreenUtils.clear(0.05f, 0.05f, 0.15f, 1f); // dark space blue

        // ── Draw ──────────────────────────────────────────────────────
        camera.update();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();

        // Title — centered horizontally, upper-third vertically
        titleFont.draw(batch,
                "SPACE RACE",
                SpaceRaceGame.WORLD_WIDTH / 2f - 150f,
                SpaceRaceGame.WORLD_HEIGHT * 0.65f);

        // Prompt — centered, lower-third
        promptFont.draw(batch,
                "Press SPACE to Start",
                SpaceRaceGame.WORLD_WIDTH / 2f - 120f,
                SpaceRaceGame.WORLD_HEIGHT * 0.35f);

        // Controls info
        promptFont.draw(batch,
                "Player 1: W A S D    |    Player 2: Arrow Keys",
                SpaceRaceGame.WORLD_WIDTH / 2f - 250f,
                SpaceRaceGame.WORLD_HEIGHT * 0.20f);

        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }

    @Override
    public void pause() { }

    @Override
    public void resume() { }

    @Override
    public void hide() { }

    @Override
    public void dispose() {
        if (titleFont  != null) titleFont.dispose();
        if (promptFont != null) promptFont.dispose();
    }
}
