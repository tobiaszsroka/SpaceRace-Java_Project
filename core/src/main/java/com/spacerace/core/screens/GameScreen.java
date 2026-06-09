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
import com.spacerace.core.track.RaceManager;
import com.spacerace.core.track.TrackMap;
import com.spacerace.core.ui.PauseOverlay;

public class GameScreen implements Screen {

    private final SpaceRaceGame game;
    private final SpriteBatch batch;
    private final String mapPath;
    private final int totalLaps;

    private OrthographicCamera cameraP1;
    private OrthographicCamera cameraP2;

    private ShapeRenderer shapeRenderer;
    private BitmapFont labelFont;
    private BitmapFont bigFont;

    private TrackMap trackMap;
    private Car player1;
    private Car player2;
    private RaceManager raceManager;

    private boolean paused;
    private PauseOverlay pauseOverlay;

    public GameScreen(SpaceRaceGame game, String mapPath, int totalLaps) {
        this.game = game;
        this.batch = game.getBatch();
        this.mapPath = mapPath;
        this.totalLaps = totalLaps;
    }

    public GameScreen(SpaceRaceGame game) {
        this(game, "maps/track_placeholder.tmx", 3);
    }

    @Override
    public void show() {
        cameraP1 = new OrthographicCamera(SpaceRaceGame.WORLD_WIDTH, SpaceRaceGame.WORLD_HEIGHT);
        cameraP2 = new OrthographicCamera(SpaceRaceGame.WORLD_WIDTH, SpaceRaceGame.WORLD_HEIGHT);
        shapeRenderer = new ShapeRenderer();

        labelFont = new BitmapFont();
        labelFont.getData().setScale(1.2f);

        bigFont = new BitmapFont();
        bigFont.getData().setScale(3f);

        pauseOverlay = new PauseOverlay();
        trackMap = new TrackMap(mapPath);

        Vector2 spawnP1 = trackMap.getSpawnPoint("spawn_p1");
        Vector2 spawnP2 = trackMap.getSpawnPoint("spawn_p2");
        float rotP1 = trackMap.getSpawnRotation("spawn_p1");
        float rotP2 = trackMap.getSpawnRotation("spawn_p2");

        player1 = new Car(spawnP1.x, spawnP1.y, rotP1, Color.CYAN);
        player2 = new Car(spawnP2.x, spawnP2.y, rotP2, Color.ORANGE);

        raceManager = new RaceManager(trackMap.getCheckpoints(), totalLaps);
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            paused = !paused;
        }
        if (paused && Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
            game.setScreen(new MainMenuScreen(game));
            dispose();
            return;
        }

        if (!paused && !raceManager.isRaceFinished()) {
            handleInput();
            player1.update(delta);
            player2.update(delta);
            checkTrackBounds(player1);
            checkTrackBounds(player2);
            player1.clampToTrack(trackMap.getWidthPx(), trackMap.getHeightPx());
            player2.clampToTrack(trackMap.getWidthPx(), trackMap.getHeightPx());
            raceManager.update(player1, player2);
        }

        updateCamera(cameraP1, player1);
        updateCamera(cameraP2, player2);

        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();
        int halfWidth = screenWidth / 2;

        Gdx.gl.glClearColor(0.02f, 0.02f, 0.08f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        renderViewport(cameraP1, 0, halfWidth, screenHeight, "P1", Color.CYAN, player1);
        renderViewport(cameraP2, halfWidth, halfWidth, screenHeight, "P2", Color.ORANGE, player2);

        Gdx.gl.glViewport(0, 0, screenWidth, screenHeight);
        drawDivider(screenWidth, screenHeight, halfWidth);

        if (paused) pauseOverlay.render(batch);
        if (raceManager.isRaceFinished()) renderFinishOverlay(screenWidth, screenHeight);
    }

    private void checkTrackBounds(Car car) {
        if (!car.isDriving()) return;
        if (!trackMap.isOnTrack(car.getX(), car.getY())) {
            car.startFalling();
        }
    }

    private void renderViewport(OrthographicCamera camera, int x, int width, int height,
                                String label, Color color, Car owner) {
        Gdx.gl.glViewport(x, 0, width, height);
        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
        Gdx.gl.glScissor(x, 0, width, height);

        trackMap.render(camera);

        shapeRenderer.setProjectionMatrix(camera.combined);
        player1.render(shapeRenderer);
        player2.render(shapeRenderer);

        renderHUD(label, color, width, height, owner);

        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
    }

    private void handleInput() {
        if (player1.isDriving()) {
            player1.setAccelerating(Gdx.input.isKeyPressed(Input.Keys.W));
            player1.setBraking(Gdx.input.isKeyPressed(Input.Keys.S));
            player1.setTurningLeft(Gdx.input.isKeyPressed(Input.Keys.A));
            player1.setTurningRight(Gdx.input.isKeyPressed(Input.Keys.D));
        } else {
            player1.setAccelerating(false);
            player1.setBraking(false);
            player1.setTurningLeft(false);
            player1.setTurningRight(false);
        }

        if (player2.isDriving()) {
            player2.setAccelerating(Gdx.input.isKeyPressed(Input.Keys.UP));
            player2.setBraking(Gdx.input.isKeyPressed(Input.Keys.DOWN));
            player2.setTurningLeft(Gdx.input.isKeyPressed(Input.Keys.LEFT));
            player2.setTurningRight(Gdx.input.isKeyPressed(Input.Keys.RIGHT));
        } else {
            player2.setAccelerating(false);
            player2.setBraking(false);
            player2.setTurningLeft(false);
            player2.setTurningRight(false);
        }
    }

    private void updateCamera(OrthographicCamera camera, Car car) {
        camera.position.set(car.getX(), car.getY(), 0);
        camera.update();
    }

    private void renderHUD(String label, Color color, int vpW, int vpH, Car car) {
        OrthographicCamera hudCamera = new OrthographicCamera(vpW, vpH);
        hudCamera.position.set(vpW / 2f, vpH / 2f, 0);
        hudCamera.update();

        batch.setProjectionMatrix(hudCamera.combined);
        batch.begin();
        labelFont.setColor(color);

        String lapText = "Lap " + (car.getLapsCompleted() + 1) + "/" + totalLaps;
        String speedText = "Speed: " + (int) Math.abs(car.getSpeed());
        labelFont.draw(batch, label + "  " + lapText + "  " + speedText, 10f, vpH - 10f);

        if (!car.isDriving()) {
            labelFont.setColor(Color.RED);
            labelFont.draw(batch, car.getState().name(), 10f, vpH - 30f);
        }

        batch.end();
    }

    private void renderFinishOverlay(int screenWidth, int screenHeight) {
        OrthographicCamera camera = new OrthographicCamera(screenWidth, screenHeight);
        camera.position.set(screenWidth / 2f, screenHeight / 2f, 0);
        camera.update();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.6f);
        shapeRenderer.rect(0, 0, screenWidth, screenHeight);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        Car winner = raceManager.getWinner();
        String winnerName = (winner == player1) ? "PLAYER 1" : "PLAYER 2";

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        bigFont.setColor(winner.getColor());
        bigFont.draw(batch, winnerName + " WINS!", screenWidth / 2f - 180f, screenHeight / 2f + 40f);
        labelFont.setColor(Color.LIGHT_GRAY);
        labelFont.draw(batch, "ESC - Back to Menu", screenWidth / 2f - 100f, screenHeight / 2f - 40f);
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
        if (bigFont != null) bigFont.dispose();
        if (trackMap != null) trackMap.dispose();
        if (pauseOverlay != null) pauseOverlay.dispose();
    }
}
