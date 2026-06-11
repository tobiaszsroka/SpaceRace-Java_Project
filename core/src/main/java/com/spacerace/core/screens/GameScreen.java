package com.spacerace.core.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.spacerace.core.RaceConfig;
import com.spacerace.core.SpaceRaceGame;
import com.spacerace.core.audio.AudioManager;
import com.spacerace.core.entities.Car;
import com.spacerace.core.entities.CollisionEffectManager;
import com.spacerace.core.entities.PowerUpManager;
import com.spacerace.core.track.RaceManager;
import com.spacerace.core.track.TrackMap;
import com.spacerace.core.ui.GameHUD;
import com.spacerace.core.ui.PauseOverlay;
import com.spacerace.core.ui.UiPanel;

public class GameScreen implements Screen {

    private final SpaceRaceGame game;
    private final SpriteBatch batch;
    private final RaceConfig config;

    private OrthographicCamera cameraP1;
    private OrthographicCamera cameraP2;

    private ShapeRenderer shapeRenderer;
    private BitmapFont bigFont;
    private final GlyphLayout finishOverlayLayout = new GlyphLayout();

    private TrackMap trackMap;
    private Car player1;
    private Car player2;
    private RaceManager raceManager;
    private GameHUD hud;
    private PowerUpManager powerUpManager;
    private final CollisionEffectManager collisionEffects = new CollisionEffectManager();

    private boolean paused;
    private PauseOverlay pauseOverlay;

    private Array<Rectangle> debugCheckpoints;

    // Countdown before race starts
    private float countdownTimer = 4f; // 3..2..1..START!
    private boolean countdownFinished = false;
    private int lastCountdownPhase = -1; // track display phase for beep sync
    private boolean raceFinishSoundPlayed = false;

    public GameScreen(SpaceRaceGame game, RaceConfig config) {
        this.game = game;
        this.batch = game.getBatch();
        this.config = config;
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
        trackMap = new TrackMap(config.getMapPath());

        Vector2 spawnP1 = trackMap.getSpawnPoint("spawn_p1");
        Vector2 spawnP2 = trackMap.getSpawnPoint("spawn_p2");
        float rotP1 = trackMap.getSpawnRotation("spawn_p1");
        float rotP2 = trackMap.getSpawnRotation("spawn_p2");

        player1 = new Car(spawnP1.x, spawnP1.y, rotP1, config.carP1);
        player2 = new Car(spawnP2.x, spawnP2.y, rotP2, config.carP2);

        debugCheckpoints = trackMap.getCheckpoints();
        raceManager = new RaceManager(debugCheckpoints, config.totalLaps);
        powerUpManager = new PowerUpManager(trackMap);

        AudioManager.getInstance().playRaceMusic();
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

        // Countdown logic
        if (!countdownFinished) {
            countdownTimer -= delta;

            // Determine which phase the display is in (synced with renderCountdownOverlay)
            int displayPhase;
            if (countdownTimer > 3f) displayPhase = 3;
            else if (countdownTimer > 2f) displayPhase = 2;
            else if (countdownTimer > 1f) displayPhase = 1;
            else displayPhase = 0; // START!

            // Play beep when the displayed phase changes
            if (displayPhase != lastCountdownPhase) {
                AudioManager.getInstance().playCountdownBeep();
                lastCountdownPhase = displayPhase;
            }

            if (countdownTimer <= 0f) {
                countdownFinished = true;
                AudioManager.getInstance().startEngineSound();
            }
        }

        if (!paused && !raceManager.isRaceFinished() && countdownFinished) {
            handleInput();
            player1.update(delta);
            player2.update(delta);
            Car.resolveCollision(player1, player2, collisionEffects);
            collisionEffects.update(delta);
            checkTrackBounds(player1);
            checkTrackBounds(player2);
            player1.clampToTrack(trackMap.getWidthPx(), trackMap.getHeightPx());
            player2.clampToTrack(trackMap.getWidthPx(), trackMap.getHeightPx());
            raceManager.update(player1, player2);
            powerUpManager.update(delta, player1, player2);
            hud.update(delta);

            // Update engine sound pitch based on car speeds
            AudioManager.getInstance().updateEngineSound(
                player1.getSpeed(), player2.getSpeed(), 350f);
        }

        // Handle race finish audio (play once)
        if (raceManager.isRaceFinished() && !raceFinishSoundPlayed) {
            raceFinishSoundPlayed = true;
            AudioManager.getInstance().stopEngineSound();
            AudioManager.getInstance().stopRaceMusic();
            AudioManager.getInstance().playVictoryFanfare();
        }

        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();
        int halfWidth = screenWidth / 2;

        updateCamera(cameraP1, player1, halfWidth, screenHeight);
        updateCamera(cameraP2, player2, halfWidth, screenHeight);

        Gdx.gl.glClearColor(0.04f, 0.05f, 0.14f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        renderViewport(cameraP1, 0, halfWidth, screenHeight, player1);
        renderViewport(cameraP2, halfWidth, halfWidth, screenHeight, player2);

        Gdx.gl.glViewport(0, 0, screenWidth, screenHeight);
        drawDivider(screenWidth, screenHeight, halfWidth);

        if (countdownFinished && !raceManager.isRaceFinished()) {
            hud.renderCenterTimer(batch, screenWidth, screenHeight);
        }

        if (!countdownFinished) renderCountdownOverlay(screenWidth, screenHeight);
        if (paused) pauseOverlay.render(batch);
        if (raceManager.isRaceFinished()) renderFinishOverlay(screenWidth, screenHeight);
    }

    private void checkTrackBounds(Car car) {
        if (!car.isDriving()) return;
        if (trackMap.isOnTrack(car.getX(), car.getY())) {
            car.updateSafePosition();
        } else {
            // Find the nearest center of the road for respawn, starting from the edge where they fell off
            // (using lastSafePosition instead of current out-of-bounds position)
            Vector2 safeEdge = car.getLastSafePosition();
            Vector2 center = trackMap.findNearestTrackCenter(safeEdge.x, safeEdge.y);
            car.setSafePosition(center.x, center.y, car.getRotation());
            car.startFalling();
        }
    }

    private void renderViewport(OrthographicCamera camera, int x, int width, int height, Car owner) {
        Gdx.gl.glViewport(x, 0, width, height);
        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
        Gdx.gl.glScissor(x, 0, width, height);

        trackMap.renderBackdrop(batch, camera);
        trackMap.render(camera);

        shapeRenderer.setProjectionMatrix(camera.combined);
        powerUpManager.render(shapeRenderer);
        player1.renderNitro(shapeRenderer);
        player2.renderNitro(shapeRenderer);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        player1.renderSprite(batch);
        player2.renderSprite(batch);
        batch.end();

        player1.renderShield(shapeRenderer);
        player2.renderShield(shapeRenderer);
        collisionEffects.render(batch, shapeRenderer, camera);

        renderStartFinishLine(camera);

        hud.renderPlayer(batch, width, height, owner, config.totalLaps);

        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
    }

    private void renderStartFinishLine(OrthographicCamera camera) {
        Rectangle finish = trackMap.getFinishLine();
        if (finish == null) return;

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        int size = 16;
        float x = finish.x;
        float y = finish.y;
        float w = finish.width;
        float h = finish.height;

        for (float py = y; py < y + h; py += size) {
            for (float px = x; px < x + w; px += size) {
                float tileW = Math.min(size, x + w - px);
                float tileH = Math.min(size, y + h - py);
                boolean isWhite = ((int) ((px - x) / size) + (int) ((py - y) / size)) % 2 == 0;
                shapeRenderer.setColor(isWhite ? Color.WHITE : Color.BLACK);
                shapeRenderer.rect(px, py, tileW, tileH);
            }
        }
        shapeRenderer.end();
    }

    private void renderCountdownOverlay(int screenWidth, int screenHeight) {
        String countdownText;
        Color textColor;
        if (countdownTimer > 3f) {
            countdownText = "3";
            textColor = Color.RED;
        } else if (countdownTimer > 2f) {
            countdownText = "2";
            textColor = Color.YELLOW;
        } else if (countdownTimer > 1f) {
            countdownText = "1";
            textColor = Color.GREEN;
        } else {
            countdownText = "START!";
            textColor = Color.WHITE;
        }

        hud.renderCountdown(batch, screenWidth, screenHeight, countdownText, textColor);
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

    /**
     * Matches camera aspect ratio to the on-screen viewport so movement speed
     * looks the same in every direction (no vertical/horizontal stretch).
     */
    private void updateCamera(OrthographicCamera camera, Car car, int viewportWidth, int viewportHeight) {
        float viewHeight = SpaceRaceGame.WORLD_HEIGHT;
        float viewWidth = viewHeight * viewportWidth / viewportHeight;
        camera.setToOrtho(false, viewWidth, viewHeight);
        camera.position.set(car.getX(), car.getY(), 0);
        camera.update();
    }

    private void renderFinishOverlay(int screenWidth, int screenHeight) {
        OrthographicCamera camera = new OrthographicCamera(screenWidth, screenHeight);
        camera.position.set(screenWidth / 2f, screenHeight / 2f, 0);
        camera.update();

        UiPanel.drawDimOverlay(shapeRenderer, camera, 0.65f);

        Car winner = raceManager.getWinner();
        String winnerName = (winner == player1) ? "PLAYER 1" : "PLAYER 2";
        String timeText = "Time: " + formatTime(hud.getRaceTimer());
        BitmapFont smallFont = hud.getFont();

        UiPanel.TextLine[] lines = {
                new UiPanel.TextLine(bigFont, winnerName + " WINS!", winner.getColor()),
                new UiPanel.TextLine(bigFont, timeText, Color.WHITE),
                new UiPanel.TextLine(smallFont, "ESC - Back to Menu", Color.LIGHT_GRAY),
        };
        UiPanel.renderMenuPanel(shapeRenderer, batch, camera, finishOverlayLayout, lines, 16f);
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
        AudioManager.getInstance().stopRaceMusic();
        AudioManager.getInstance().stopEngineSound();
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (bigFont != null) bigFont.dispose();
        if (trackMap != null) trackMap.dispose();
        if (pauseOverlay != null) pauseOverlay.dispose();
        if (hud != null) hud.dispose();
        collisionEffects.dispose();
    }
}
