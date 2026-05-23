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
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.spacerace.core.SpaceRaceGame;

/**
 * GameScreen — the main gameplay screen with vertical split-screen.
 *
 * Layout:
 * ┌────────────┬────────────┐
 * │  Player 1  │  Player 2  │
 * │  (W/A/S/D) │ (Arrows)   │
 * └────────────┴────────────┘
 *
 * Each player has their own {@link OrthographicCamera} and {@link Viewport}.
 * The screen is split vertically: left half for P1, right half for P2.
 *
 * Key concepts demonstrated:
 *  - Independent cameras that follow each player
 *  - glViewport/glScissor for split-screen clipping
 *  - Shared ShapeRenderer for dummy rendering
 *  - Independent input mapping per player
 */
public class GameScreen implements Screen {

    // ── References ────────────────────────────────────────────────────
    private final SpaceRaceGame game;
    private final SpriteBatch batch;

    // ── Split-screen cameras & viewports ──────────────────────────────
    private OrthographicCamera cameraP1;
    private OrthographicCamera cameraP2;
    private Viewport viewportP1;
    private Viewport viewportP2;

    // ── Rendering ─────────────────────────────────────────────────────
    private ShapeRenderer shapeRenderer;
    private BitmapFont labelFont;

    // ── Player rectangles (dummy car representations) ─────────────────
    // These use world coordinates.
    private static final float CAR_WIDTH  = 30f;
    private static final float CAR_HEIGHT = 50f;
    private static final float CAR_SPEED  = 200f; // pixels per second

    private Rectangle player1;
    private Rectangle player2;

    // ── World / track boundaries ──────────────────────────────────────
    // The "track" is a large area the players can drive around in.
    private static final float TRACK_WIDTH  = 2000f;
    private static final float TRACK_HEIGHT = 2000f;

    // ── Background stars (simple decoration) ──────────────────────────
    private float[] starX;
    private float[] starY;
    private float[] starSize;
    private static final int STAR_COUNT = 150;

    public GameScreen(SpaceRaceGame game) {
        this.game  = game;
        this.batch = game.getBatch();
    }

    @Override
    public void show() {
        // ── Create cameras ────────────────────────────────────────────
        cameraP1 = new OrthographicCamera();
        cameraP2 = new OrthographicCamera();

        // Each viewport covers the full virtual world size.
        // The actual screen region (left/right half) is set in render()
        // via glViewport.
        viewportP1 = new FitViewport(SpaceRaceGame.WORLD_WIDTH, SpaceRaceGame.WORLD_HEIGHT, cameraP1);
        viewportP2 = new FitViewport(SpaceRaceGame.WORLD_WIDTH, SpaceRaceGame.WORLD_HEIGHT, cameraP2);

        // ── Create shape renderer ─────────────────────────────────────
        shapeRenderer = new ShapeRenderer();

        // ── Create label font ─────────────────────────────────────────
        labelFont = new BitmapFont();
        labelFont.setColor(Color.WHITE);
        labelFont.getData().setScale(1.2f);

        // ── Spawn players at different positions ──────────────────────
        // Player 1 starts near the left side of the track
        player1 = new Rectangle(
                TRACK_WIDTH * 0.25f - CAR_WIDTH / 2f,
                TRACK_HEIGHT * 0.5f  - CAR_HEIGHT / 2f,
                CAR_WIDTH, CAR_HEIGHT
        );

        // Player 2 starts near the right side of the track
        player2 = new Rectangle(
                TRACK_WIDTH * 0.75f - CAR_WIDTH / 2f,
                TRACK_HEIGHT * 0.5f  - CAR_HEIGHT / 2f,
                CAR_WIDTH, CAR_HEIGHT
        );

        // ── Generate random star positions ────────────────────────────
        starX    = new float[STAR_COUNT];
        starY    = new float[STAR_COUNT];
        starSize = new float[STAR_COUNT];
        for (int i = 0; i < STAR_COUNT; i++) {
            starX[i]    = MathUtils.random(0f, TRACK_WIDTH);
            starY[i]    = MathUtils.random(0f, TRACK_HEIGHT);
            starSize[i] = MathUtils.random(1f, 3f);
        }
    }

    @Override
    public void render(float delta) {
        // ── Handle input ──────────────────────────────────────────────
        handleInput(delta);

        // ── Update cameras to follow their respective players ─────────
        updateCamera(cameraP1, player1);
        updateCamera(cameraP2, player2);

        // ── Get actual screen dimensions ──────────────────────────────
        int screenWidth  = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();
        int halfWidth    = screenWidth / 2;

        // ── Clear the entire screen first ─────────────────────────────
        Gdx.gl.glClearColor(0.02f, 0.02f, 0.08f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // ── Render Player 1's viewport (left half) ────────────────────
        Gdx.gl.glViewport(0, 0, halfWidth, screenHeight);
        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
        Gdx.gl.glScissor(0, 0, halfWidth, screenHeight);

        renderWorld(cameraP1, player1, player2);
        renderHUD(cameraP1, "P1", Color.CYAN, 0, 0, halfWidth, screenHeight);

        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);

        // ── Render Player 2's viewport (right half) ───────────────────
        Gdx.gl.glViewport(halfWidth, 0, halfWidth, screenHeight);
        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
        Gdx.gl.glScissor(halfWidth, 0, halfWidth, screenHeight);

        renderWorld(cameraP2, player2, player1);
        renderHUD(cameraP2, "P2", Color.ORANGE, halfWidth, 0, halfWidth, screenHeight);

        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);

        // ── Reset viewport to full screen (important for UI overlays) ─
        Gdx.gl.glViewport(0, 0, screenWidth, screenHeight);

        // ── Draw the divider line between the two halves ──────────────
        drawDivider(screenWidth, screenHeight, halfWidth);

        // ── ESC to return to main menu ────────────────────────────────
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new MainMenuScreen(game));
            dispose();
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  INPUT HANDLING
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Processes input for both players independently.
     *
     * Player 1: W (up), S (down), A (left), D (right)
     * Player 2: Arrow keys
     *
     * @param delta time since last frame in seconds
     */
    private void handleInput(float delta) {
        float distance = CAR_SPEED * delta;

        // ── Player 1 — WASD ───────────────────────────────────────────
        if (Gdx.input.isKeyPressed(Input.Keys.W)) player1.y += distance;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) player1.y -= distance;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) player1.x -= distance;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) player1.x += distance;

        // ── Player 2 — Arrow Keys ─────────────────────────────────────
        if (Gdx.input.isKeyPressed(Input.Keys.UP))    player2.y += distance;
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN))  player2.y -= distance;
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT))  player2.x -= distance;
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) player2.x += distance;

        // ── Clamp to track boundaries ─────────────────────────────────
        clampToTrack(player1);
        clampToTrack(player2);
    }

    /**
     * Prevents a player rectangle from leaving the track area.
     */
    private void clampToTrack(Rectangle player) {
        player.x = MathUtils.clamp(player.x, 0, TRACK_WIDTH  - player.width);
        player.y = MathUtils.clamp(player.y, 0, TRACK_HEIGHT - player.height);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  CAMERA
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Centers the camera on the given player rectangle.
     * The camera follows the center of the player's car.
     */
    private void updateCamera(OrthographicCamera camera, Rectangle target) {
        camera.position.set(
                target.x + target.width  / 2f,
                target.y + target.height / 2f,
                0
        );
        camera.update();
    }

    // ═══════════════════════════════════════════════════════════════════
    //  RENDERING
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Renders the game world from a specific camera's perspective.
     * Draws the background, stars, track boundary, and both players.
     *
     * @param camera      the camera for this viewport
     * @param localPlayer the player "owning" this viewport (drawn in their color)
     * @param otherPlayer the opponent (drawn in their color)
     */
    private void renderWorld(OrthographicCamera camera,
                             Rectangle localPlayer, Rectangle otherPlayer) {

        shapeRenderer.setProjectionMatrix(camera.combined);

        // ── Draw background stars ─────────────────────────────────────
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.7f, 0.7f, 0.8f, 1f);
        for (int i = 0; i < STAR_COUNT; i++) {
            shapeRenderer.circle(starX[i], starY[i], starSize[i]);
        }
        shapeRenderer.end();

        // ── Draw track boundary (outlined rectangle) ──────────────────
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.DARK_GRAY);
        shapeRenderer.rect(0, 0, TRACK_WIDTH, TRACK_HEIGHT);

        // Draw a grid for spatial reference (every 200 units)
        shapeRenderer.setColor(0.1f, 0.1f, 0.2f, 1f);
        for (float gx = 0; gx <= TRACK_WIDTH; gx += 200f) {
            shapeRenderer.line(gx, 0, gx, TRACK_HEIGHT);
        }
        for (float gy = 0; gy <= TRACK_HEIGHT; gy += 200f) {
            shapeRenderer.line(0, gy, TRACK_WIDTH, gy);
        }
        shapeRenderer.end();

        // ── Draw the players ──────────────────────────────────────────
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Determine colors based on which player is "local"
        boolean localIsP1 = (localPlayer == player1);

        // Draw Player 1 (cyan)
        shapeRenderer.setColor(Color.CYAN);
        shapeRenderer.rect(player1.x, player1.y, player1.width, player1.height);

        // Draw Player 2 (orange)
        shapeRenderer.setColor(Color.ORANGE);
        shapeRenderer.rect(player2.x, player2.y, player2.width, player2.height);

        // Draw a small "nose" indicator on each car to show facing direction
        // (for now, all cars face upward)
        shapeRenderer.setColor(Color.WHITE);
        // P1 nose
        shapeRenderer.triangle(
                player1.x, player1.y + player1.height,
                player1.x + player1.width, player1.y + player1.height,
                player1.x + player1.width / 2f, player1.y + player1.height + 10f
        );
        // P2 nose
        shapeRenderer.triangle(
                player2.x, player2.y + player2.height,
                player2.x + player2.width, player2.y + player2.height,
                player2.x + player2.width / 2f, player2.y + player2.height + 10f
        );

        shapeRenderer.end();
    }

    /**
     * Draws a simple HUD overlay for the given viewport.
     * Shows the player label in the top-left corner.
     */
    private void renderHUD(OrthographicCamera worldCamera, String label,
                           Color color, int vpX, int vpY, int vpW, int vpH) {
        // We need a separate camera for the HUD so it doesn't move with the world.
        // For simplicity, we draw using the SpriteBatch with an identity-like projection.
        OrthographicCamera hudCamera = new OrthographicCamera(vpW, vpH);
        hudCamera.position.set(vpW / 2f, vpH / 2f, 0);
        hudCamera.update();

        batch.setProjectionMatrix(hudCamera.combined);
        batch.begin();
        labelFont.setColor(color);
        labelFont.draw(batch, label + "  [ESC = Menu]", 10f, vpH - 10f);
        batch.end();
    }

    /**
     * Draws a vertical divider line between the two player viewports.
     */
    private void drawDivider(int screenWidth, int screenHeight, int halfWidth) {
        // Use an orthographic camera matching screen pixel coords
        OrthographicCamera uiCamera = new OrthographicCamera(screenWidth, screenHeight);
        uiCamera.position.set(screenWidth / 2f, screenHeight / 2f, 0);
        uiCamera.update();

        shapeRenderer.setProjectionMatrix(uiCamera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(halfWidth - 1, 0, 2, screenHeight);
        shapeRenderer.end();
    }

    // ═══════════════════════════════════════════════════════════════════
    //  LIFECYCLE
    // ═══════════════════════════════════════════════════════════════════

    @Override
    public void resize(int width, int height) {
        // We don't use viewport.update() here because we manually
        // set glViewport in render(). But we store the dimensions
        // implicitly through Gdx.graphics.getWidth/Height().
    }

    @Override
    public void pause() { }

    @Override
    public void resume() { }

    @Override
    public void hide() { }

    @Override
    public void dispose() {
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (labelFont     != null) labelFont.dispose();
    }
}
