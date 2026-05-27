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
 * Main menu with title and start prompt. Press SPACE to begin.
 */
public class MainMenuScreen implements Screen {

    private final SpaceRaceGame game;
    private final SpriteBatch batch;

    private final OrthographicCamera camera;
    private final Viewport viewport;

    private BitmapFont titleFont;
    private BitmapFont promptFont;

    public MainMenuScreen(SpaceRaceGame game) {
        this.game = game;
        this.batch = game.getBatch();

        camera = new OrthographicCamera();
        viewport = new FitViewport(SpaceRaceGame.WORLD_WIDTH, SpaceRaceGame.WORLD_HEIGHT, camera);
        camera.position.set(SpaceRaceGame.WORLD_WIDTH / 2f, SpaceRaceGame.WORLD_HEIGHT / 2f, 0);
        camera.update();
    }

    @Override
    public void show() {
        titleFont = new BitmapFont();
        titleFont.setColor(Color.WHITE);
        titleFont.getData().setScale(3f);

        promptFont = new BitmapFont();
        promptFont.setColor(Color.LIGHT_GRAY);
        promptFont.getData().setScale(1.5f);
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            game.setScreen(new GameScreen(game));
            dispose();
            return;
        }

        ScreenUtils.clear(0.05f, 0.05f, 0.15f, 1f);

        camera.update();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        titleFont.draw(batch, "SPACE RACE",
                SpaceRaceGame.WORLD_WIDTH / 2f - 150f,
                SpaceRaceGame.WORLD_HEIGHT * 0.65f);

        promptFont.draw(batch, "Press SPACE to Start",
                SpaceRaceGame.WORLD_WIDTH / 2f - 120f,
                SpaceRaceGame.WORLD_HEIGHT * 0.35f);

        promptFont.draw(batch, "Player 1: W A S D    |    Player 2: Arrow Keys",
                SpaceRaceGame.WORLD_WIDTH / 2f - 250f,
                SpaceRaceGame.WORLD_HEIGHT * 0.20f);

        batch.end();
    }

    @Override public void resize(int width, int height) { viewport.update(width, height); }
    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { }

    @Override
    public void dispose() {
        if (titleFont != null) titleFont.dispose();
        if (promptFont != null) promptFont.dispose();
    }
}
