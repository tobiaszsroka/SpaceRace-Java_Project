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
import com.spacerace.core.ui.GameHUD;
import com.spacerace.core.ui.PauseOverlay;

public class GameScreen implements Screen {

    private final SpaceRaceGame game;
    private final SpriteBatch batch;
    private final String mapPath;
    private final int totalLaps;

    private OrthographicCamera cameraP1;
    private OrthographicCamera cameraP2;

    private ShapeRenderer shapeRenderer;
    private BitmapFont bigFont;

    private TrackMap trackMap;
    private Car player1;
    private Car player2;
    private RaceManager raceManager;
    private GameHUD hud;

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

        bigFont = new BitmapFont();
        bigFont.getData().setScale(3f);

        pauseOverlay = new PauseOverlay();
        hud = new GameHUD();
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
            if (raceManager.isRaceFinished()) {
                game.setScreen(new MainMenuScreen(game));
                dispose();
                return;
            }
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
            hud.update(delta);
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
        if (trackMap.isOnTrack(car.getX(), car.getY())) {
            car.updateSafePosition();
        } else {
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

        hud.render(batch, width, height, label, color, owner, totalLaps);

        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
    }

    private void handleInput() {
        applyInput(player1, Input.Keys.W, Input.Keys.S, Input.Keys.A, Input.Keys.D);
        applyInput(player2, Input.Keys.UP, Input.Keys.DOWN, Input.Keys.LEFT, Input.Keys.RIGHT);
    }

    private void applyInput(Car car, int gas, int brake, int left, int right) {
        boolean driving = car.isDriving();
        car.setAccelerating(driving && Gdx.input.isKeyPressed(gas));
        car.setBraking(driving && Gdx.input.isKeyPressed(brake));
        car.setTurningLeft(driving && Gdx.input.isKeyPressed(left));
        car.setTurningRight(driving && Gdx.input.isKeyPressed(right));
    }

    private void updateCamera(OrthographicCamera camera, Car car) {
        camera.position.set(car.getX(), car.getY(), 0);
        camera.update();
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
        String timeText = "Time: " + formatTime(hud.getRaceTimer());

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        bigFont.setColor(winner.getColor());
        bigFont.draw(batch, winnerName + " WINS!", screenWidth / 2f - 180f, screenHeight / 2f + 60f);
        bigFont.setColor(Color.WHITE);
        bigFont.draw(batch, timeText, screenWidth / 2f - 140f, screenHeight / 2f);
        BitmapFont smallFont = hud.getFont();
        smallFont.setColor(Color.LIGHT_GRAY);
        smallFont.draw(batch, "ESC - Back to Menu", screenWidth / 2f - 80f, screenHeight / 2f - 60f);
        batch.end();
    }

    private String formatTime(float seconds) {
        int mins = (int) (seconds / 60);
        int secs = (int) (seconds % 60);
        int millis = (int) ((seconds * 100) % 100);
        return String.format("%d:%02d.%02d", mins, secs, millis);
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
        if (bigFont != null) bigFont.dispose();
        if (trackMap != null) trackMap.dispose();
        if (pauseOverlay != null) pauseOverlay.dispose();
        if (hud != null) hud.dispose();
    }
}
