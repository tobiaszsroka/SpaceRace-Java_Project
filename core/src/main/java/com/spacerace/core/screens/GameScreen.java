package com.spacerace.core.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.spacerace.core.SpaceRaceGame;
import com.spacerace.core.entities.Car;
import com.spacerace.core.track.TrackMap;

/**
 * Split-screen gameplay: left half = P1 (WASD), right half = P2 (Arrows).
 * Renders a Tiled map and two independently-controlled cars.
 */
public class GameScreen implements Screen {

    private final SpaceRaceGame game;
    private final SpriteBatch batch;
    private final String mapPath;

    private OrthographicCamera cameraP1;
    private OrthographicCamera cameraP2;

    private ShapeRenderer shapeRenderer;
    private BitmapFont labelFont;

    private TrackMap trackMap;
    private Car player1;
    private Car player2;

    public GameScreen(SpaceRaceGame game, String mapPath) {
        this.game = game;
        this.batch = game.getBatch();
        this.mapPath = mapPath;
    }

    /** Convenience constructor using the default placeholder map. */
    public GameScreen(SpaceRaceGame game) {
        this(game, "maps/track_placeholder.tmx");
    }

    @Override
    public void show() {
        cameraP1 = new OrthographicCamera();
        cameraP2 = new OrthographicCamera();
        shapeRenderer = new ShapeRenderer();

        labelFont = new BitmapFont();
        labelFont.setColor(Color.WHITE);
        labelFont.getData().setScale(1.2f);

        trackMap = new TrackMap(mapPath);

        Vector2 spawnP1 = trackMap.getSpawnPoint("spawn_p1");
        Vector2 spawnP2 = trackMap.getSpawnPoint("spawn_p2");
        player1 = new Car(spawnP1.x, spawnP1.y, Color.CYAN);
        player2 = new Car(spawnP2.x, spawnP2.y, Color.ORANGE);
    }

    @Override
    public void render(float delta) {
        handleInput();

        player1.update(delta);
        player2.update(delta);
        player1.clampToTrack(trackMap.getWidthPx(), trackMap.getHeightPx());
        player2.clampToTrack(trackMap.getWidthPx(), trackMap.getHeightPx());

        updateCamera(cameraP1, player1);
        updateCamera(cameraP2, player2);

        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();
        int halfWidth = screenWidth / 2;

        Gdx.gl.glClearColor(0.02f, 0.02f, 0.08f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // P1 viewport (left half)
        renderViewport(cameraP1, 0, halfWidth, screenHeight, "P1", Color.CYAN);

        // P2 viewport (right half)
        renderViewport(cameraP2, halfWidth, halfWidth, screenHeight, "P2", Color.ORANGE);

        // Full-screen elements
        Gdx.gl.glViewport(0, 0, screenWidth, screenHeight);
        drawDivider(screenWidth, screenHeight, halfWidth);

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new MainMenuScreen(game));
            dispose();
        }
    }

    private void renderViewport(OrthographicCamera camera, int x, int width, int height,
                                String label, Color color) {
        Gdx.gl.glViewport(x, 0, width, height);
        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
        Gdx.gl.glScissor(x, 0, width, height);

        trackMap.render(camera);

        shapeRenderer.setProjectionMatrix(camera.combined);
        player1.render(shapeRenderer);
        player2.render(shapeRenderer);

        renderHUD(label, color, width, height);

        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
    }

    private void handleInput() {
        player1.setAccelerating(Gdx.input.isKeyPressed(Input.Keys.W));
        player1.setBraking(Gdx.input.isKeyPressed(Input.Keys.S));
        player1.setTurningLeft(Gdx.input.isKeyPressed(Input.Keys.A));
        player1.setTurningRight(Gdx.input.isKeyPressed(Input.Keys.D));

        player2.setAccelerating(Gdx.input.isKeyPressed(Input.Keys.UP));
        player2.setBraking(Gdx.input.isKeyPressed(Input.Keys.DOWN));
        player2.setTurningLeft(Gdx.input.isKeyPressed(Input.Keys.LEFT));
        player2.setTurningRight(Gdx.input.isKeyPressed(Input.Keys.RIGHT));
    }

    private void updateCamera(OrthographicCamera camera, Car car) {
        camera.position.set(car.getX(), car.getY(), 0);
        camera.update();
    }

    private void renderHUD(String label, Color color, int vpW, int vpH) {
        OrthographicCamera hudCamera = new OrthographicCamera(vpW, vpH);
        hudCamera.position.set(vpW / 2f, vpH / 2f, 0);
        hudCamera.update();

        batch.setProjectionMatrix(hudCamera.combined);
        batch.begin();
        labelFont.setColor(color);
        labelFont.draw(batch, label + "  [ESC = Menu]", 10f, vpH - 10f);
        batch.end();
    }

    private void drawDivider(int screenWidth, int screenHeight, int halfWidth) {
        OrthographicCamera uiCamera = new OrthographicCamera(screenWidth, screenHeight);
        uiCamera.position.set(screenWidth / 2f, screenHeight / 2f, 0);
        uiCamera.update();

        shapeRenderer.setProjectionMatrix(uiCamera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(halfWidth - 1, 0, 2, screenHeight);
        shapeRenderer.end();
    }

    @Override public void resize(int width, int height) { }
    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { }

    @Override
    public void dispose() {
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (labelFont != null) labelFont.dispose();
        if (trackMap != null) trackMap.dispose();
    }
}
