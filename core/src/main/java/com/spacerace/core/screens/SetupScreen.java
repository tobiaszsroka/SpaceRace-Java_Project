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
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.spacerace.core.RaceConfig;
import com.spacerace.core.SpaceRaceGame;
import com.spacerace.core.cars.CarCatalog;
import com.spacerace.core.track.TrackCatalog;
import com.spacerace.core.track.TrackMap;

/** Pre-race setup: choose track and car color for each player. */
public class SetupScreen implements Screen {

    private static final float PREVIEW_W = 26f;
    private static final float PREVIEW_H = 42f;
    private static final float CAR_GAP = 68f;
    private static final float CAR_START_X = 88f;

    private static final float TRACK_LIST_X = 120f;
    private static final float TRACK_LIST_TOP = 498f;
    private static final float TRACK_LINE_H = 34f;

    private static final float P1_CAR_Y = 268f;
    private static final float P2_CAR_Y = 158f;

    private static final Rectangle TRACK_PREVIEW_BOX = new Rectangle(518f, 348f, 262f, 218f);
    private static final Rectangle START_BUTTON = new Rectangle(300f, 44f, 200f, 48f);

    private final SpaceRaceGame game;
    private final SpriteBatch batch;

    private final OrthographicCamera camera;
    private final Viewport viewport;
    private final Vector3 touch = new Vector3();
    private final GlyphLayout layout = new GlyphLayout();

    private final Array<Rectangle> trackHitboxes = new Array<>();
    private final Array<Rectangle> carP1Hitboxes = new Array<>();
    private final Array<Rectangle> carP2Hitboxes = new Array<>();

    private ShapeRenderer shapeRenderer;
    private OrthographicCamera previewCamera;
    private TrackMap previewMap;
    private int loadedPreviewIndex = -1;

    private BitmapFont titleFont;
    private BitmapFont labelFont;
    private BitmapFont itemFont;
    private BitmapFont buttonFont;

    private int selectedTrack;
    private int selectedCarP1;
    private int selectedCarP2;

    public SetupScreen(SpaceRaceGame game) {
        this.game = game;
        this.batch = game.getBatch();

        camera = new OrthographicCamera();
        viewport = new FitViewport(SpaceRaceGame.WORLD_WIDTH, SpaceRaceGame.WORLD_HEIGHT, camera);
        camera.position.set(SpaceRaceGame.WORLD_WIDTH / 2f, SpaceRaceGame.WORLD_HEIGHT / 2f, 0);
        camera.update();
    }

    @Override
    public void show() {
        shapeRenderer = new ShapeRenderer();
        previewCamera = new OrthographicCamera();

        titleFont = new BitmapFont();
        titleFont.setColor(Color.WHITE);
        titleFont.getData().setScale(2.2f);

        labelFont = new BitmapFont();
        labelFont.setColor(Color.LIGHT_GRAY);
        labelFont.getData().setScale(1.05f);

        itemFont = new BitmapFont();
        itemFont.getData().setScale(1.1f);

        buttonFont = new BitmapFont();
        buttonFont.setColor(Color.WHITE);
        buttonFont.getData().setScale(1.6f);

        rebuildHitboxes();
    }

    private void rebuildHitboxes() {
        trackHitboxes.clear();
        carP1Hitboxes.clear();
        carP2Hitboxes.clear();

        for (int i = 0; i < TrackCatalog.TRACKS.length; i++) {
            float y = TRACK_LIST_TOP - i * TRACK_LINE_H;
            trackHitboxes.add(new Rectangle(TRACK_LIST_X - 8f, y - 24f, 360f, 30f));
        }

        for (int i = 0; i < CarCatalog.CARS.length; i++) {
            float x = CAR_START_X + i * CAR_GAP;
            carP1Hitboxes.add(new Rectangle(x - 6f, P1_CAR_Y - 6f, PREVIEW_W + 52f, PREVIEW_H + 40f));
            carP2Hitboxes.add(new Rectangle(x - 6f, P2_CAR_Y - 6f, PREVIEW_W + 52f, PREVIEW_H + 40f));
        }
    }

    @Override
    public void render(float delta) {
        if (handleInput()) {
            return;
        }

        ScreenUtils.clear(0.05f, 0.05f, 0.15f, 1f);

        camera.update();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        titleFont.draw(batch, "PRZYGOTOWANIE WYŚCIGU",
                SpaceRaceGame.WORLD_WIDTH / 2f - 220f,
                SpaceRaceGame.WORLD_HEIGHT - 28f);

        drawTrackSection();
        drawCarSectionHeaders();
        drawCarNameLabels();
        drawTrackPreviewLabels();
        drawStartButton();

        labelFont.setColor(Color.LIGHT_GRAY);
        labelFont.draw(batch, "ESC - powrót   |   1-4 - tor   |   A/D - Gracz 1   |   ← → - Gracz 2",
                88f, 16f);

        batch.end();

        drawTrackPreview();
        drawCarPreviews(P1_CAR_Y, selectedCarP1);
        drawCarPreviews(P2_CAR_Y, selectedCarP2);
    }

    private void drawTrackSection() {
        labelFont.setColor(Color.WHITE);
        labelFont.draw(batch, "Tor:", 80f, TRACK_LIST_TOP + 28f);

        for (int i = 0; i < TrackCatalog.TRACKS.length; i++) {
            TrackCatalog.Entry track = TrackCatalog.TRACKS[i];
            boolean selected = i == selectedTrack;
            float y = TRACK_LIST_TOP - i * TRACK_LINE_H;

            String prefix = selected ? "> " : "  ";
            String line = prefix + "[" + (i + 1) + "] " + track.displayName;
            itemFont.setColor(selected ? new Color(0.2f, 0.95f, 1f, 1f) : new Color(0.65f, 0.65f, 0.75f, 1f));
            itemFont.draw(batch, line, TRACK_LIST_X, y);
        }

        TrackCatalog.Entry selected = TrackCatalog.TRACKS[selectedTrack];
        float descY = TRACK_LIST_TOP - TrackCatalog.TRACKS.length * TRACK_LINE_H - 8f;
        labelFont.setColor(0.75f, 0.8f, 0.9f, 1f);
        labelFont.draw(batch, selected.description, TRACK_LIST_X, descY);
    }

    private void drawCarSectionHeaders() {
        labelFont.setColor(Color.WHITE);
        labelFont.draw(batch, "Gracz 1  (A / D)", 80f, P1_CAR_Y + PREVIEW_H + 34f);
        labelFont.draw(batch, "Gracz 2  (← / →)", 80f, P2_CAR_Y + PREVIEW_H + 34f);
    }

    private void drawCarNameLabels() {
        for (int i = 0; i < CarCatalog.CARS.length; i++) {
            CarCatalog.Entry car = CarCatalog.CARS[i];
            float x = CAR_START_X + i * CAR_GAP;

            itemFont.setColor(i == selectedCarP1 ? brighten(car.color, 0.25f) : Color.LIGHT_GRAY);
            layout.setText(itemFont, car.displayName);
            itemFont.draw(batch, car.displayName,
                    x + PREVIEW_W / 2f - layout.width / 2f, P1_CAR_Y - 14f);

            itemFont.setColor(i == selectedCarP2 ? brighten(car.color, 0.25f) : Color.LIGHT_GRAY);
            layout.setText(itemFont, car.displayName);
            itemFont.draw(batch, car.displayName,
                    x + PREVIEW_W / 2f - layout.width / 2f, P2_CAR_Y - 14f);
        }
    }

    private void drawTrackPreviewLabels() {
        labelFont.setColor(Color.WHITE);
        labelFont.draw(batch, "Podgląd",
                TRACK_PREVIEW_BOX.x, TRACK_PREVIEW_BOX.y + TRACK_PREVIEW_BOX.height + 20f);

        TrackCatalog.Entry track = TrackCatalog.TRACKS[selectedTrack];
        itemFont.setColor(new Color(0.2f, 0.95f, 1f, 1f));
        layout.setText(itemFont, track.displayName);
        itemFont.draw(batch, track.displayName,
                TRACK_PREVIEW_BOX.x + TRACK_PREVIEW_BOX.width / 2f - layout.width / 2f,
                TRACK_PREVIEW_BOX.y + TRACK_PREVIEW_BOX.height - 8f);
    }

    private void drawStartButton() {
        boolean hover = isMouseOver(START_BUTTON);
        buttonFont.setColor(hover ? Color.YELLOW : Color.WHITE);
        layout.setText(buttonFont, "START");
        buttonFont.draw(batch, "START",
                START_BUTTON.x + START_BUTTON.width / 2f - layout.width / 2f,
                START_BUTTON.y + START_BUTTON.height / 2f + layout.height / 2f - 4f);
    }

    private void drawCarPreviews(float baseY, int selectedIndex) {
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        for (int i = 0; i < CarCatalog.CARS.length; i++) {
            CarCatalog.Entry car = CarCatalog.CARS[i];
            boolean selected = i == selectedIndex;
            float x = CAR_START_X + i * CAR_GAP;
            float y = baseY;

            if (selected) {
                Color fill = darken(car.color, 0.28f);
                fill.a = 0.75f;
                shapeRenderer.setColor(fill);
                shapeRenderer.rect(x - 8f, y - 8f, PREVIEW_W + 16f, PREVIEW_H + 16f);
            }

            shapeRenderer.setColor(car.color);
            shapeRenderer.rect(x, y, PREVIEW_W, PREVIEW_H);
            shapeRenderer.setColor(Color.WHITE);
            shapeRenderer.triangle(
                    x, y + PREVIEW_H,
                    x + PREVIEW_W, y + PREVIEW_H,
                    x + PREVIEW_W / 2f, y + PREVIEW_H + 8f);
        }

        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        for (int i = 0; i < CarCatalog.CARS.length; i++) {
            if (i != selectedIndex) continue;

            CarCatalog.Entry car = CarCatalog.CARS[i];
            float x = CAR_START_X + i * CAR_GAP;
            float y = baseY;

            shapeRenderer.setColor(brighten(car.color, 0.4f));
            shapeRenderer.rect(x - 8f, y - 8f, PREVIEW_W + 16f, PREVIEW_H + 16f);
        }
        shapeRenderer.end();
    }

    private Color brighten(Color color, float amount) {
        return new Color(
                Math.min(color.r + amount, 1f),
                Math.min(color.g + amount, 1f),
                Math.min(color.b + amount, 1f),
                1f);
    }

    private Color darken(Color color, float factor) {
        return new Color(color.r * factor, color.g * factor, color.b * factor, 1f);
    }

    private void ensurePreviewLoaded() {
        if (loadedPreviewIndex == selectedTrack && previewMap != null) {
            return;
        }
        if (previewMap != null) {
            previewMap.dispose();
            previewMap = null;
        }
        previewMap = new TrackMap(TrackCatalog.TRACKS[selectedTrack].getMapPath());
        loadedPreviewIndex = selectedTrack;
        updatePreviewCamera();
    }

    private void updatePreviewCamera() {
        float mapW = previewMap.getWidthPx();
        float mapH = previewMap.getHeightPx();
        float boxAspect = TRACK_PREVIEW_BOX.width / TRACK_PREVIEW_BOX.height;
        float padding = 1.06f;
        float viewW;
        float viewH;

        if (mapW / mapH > boxAspect) {
            viewW = mapW * padding;
            viewH = viewW / boxAspect;
        } else {
            viewH = mapH * padding;
            viewW = viewH * boxAspect;
        }

        previewCamera.setToOrtho(false, viewW, viewH);
        previewCamera.position.set(mapW / 2f, mapH / 2f, 0);
        previewCamera.update();
    }

    private void drawTrackPreview() {
        ensurePreviewLoaded();

        int screenW = Gdx.graphics.getWidth();
        int screenH = Gdx.graphics.getHeight();

        Vector3 bottomLeft = new Vector3(TRACK_PREVIEW_BOX.x, TRACK_PREVIEW_BOX.y, 0);
        Vector3 topRight = new Vector3(
                TRACK_PREVIEW_BOX.x + TRACK_PREVIEW_BOX.width,
                TRACK_PREVIEW_BOX.y + TRACK_PREVIEW_BOX.height,
                0);
        viewport.project(bottomLeft);
        viewport.project(topRight);

        int x = Math.round(Math.min(bottomLeft.x, topRight.x));
        int y = Math.round(Math.min(bottomLeft.y, topRight.y));
        int w = Math.round(Math.abs(topRight.x - bottomLeft.x));
        int h = Math.round(Math.abs(topRight.y - bottomLeft.y));

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.02f, 0.02f, 0.08f, 1f);
        shapeRenderer.rect(TRACK_PREVIEW_BOX.x, TRACK_PREVIEW_BOX.y,
                TRACK_PREVIEW_BOX.width, TRACK_PREVIEW_BOX.height);
        shapeRenderer.end();

        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
        Gdx.gl.glScissor(x, y, w, h);
        Gdx.gl.glViewport(x, y, w, h);
        previewMap.render(previewCamera);
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
        Gdx.gl.glViewport(0, 0, screenW, screenH);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.35f, 0.65f, 1f, 1f);
        shapeRenderer.rect(TRACK_PREVIEW_BOX.x, TRACK_PREVIEW_BOX.y,
                TRACK_PREVIEW_BOX.width, TRACK_PREVIEW_BOX.height);
        shapeRenderer.end();
    }

    private boolean handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new MainMenuScreen(game));
            dispose();
            return true;
        }

        for (int i = 0; i < TrackCatalog.TRACKS.length; i++) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + i)) {
                selectedTrack = i;
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.A)) {
            selectedCarP1 = (selectedCarP1 - 1 + CarCatalog.CARS.length) % CarCatalog.CARS.length;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.D)) {
            selectedCarP1 = (selectedCarP1 + 1) % CarCatalog.CARS.length;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) {
            selectedCarP2 = (selectedCarP2 - 1 + CarCatalog.CARS.length) % CarCatalog.CARS.length;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
            selectedCarP2 = (selectedCarP2 + 1) % CarCatalog.CARS.length;
        }

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            for (int i = 0; i < trackHitboxes.size; i++) {
                if (isMouseOver(trackHitboxes.get(i))) {
                    selectedTrack = i;
                }
            }
            for (int i = 0; i < carP1Hitboxes.size; i++) {
                if (isMouseOver(carP1Hitboxes.get(i))) {
                    selectedCarP1 = i;
                }
            }
            for (int i = 0; i < carP2Hitboxes.size; i++) {
                if (isMouseOver(carP2Hitboxes.get(i))) {
                    selectedCarP2 = i;
                }
            }
        }

        boolean start = Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
                || Gdx.input.isKeyJustPressed(Input.Keys.ENTER);

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && isMouseOver(START_BUTTON)) {
            start = true;
        }

        if (start) {
            RaceConfig config = new RaceConfig(
                    TrackCatalog.TRACKS[selectedTrack],
                    CarCatalog.CARS[selectedCarP1],
                    CarCatalog.CARS[selectedCarP2]
            );
            game.setScreen(new GameScreen(game, config));
            dispose();
            return true;
        }

        return false;
    }

    private boolean isMouseOver(Rectangle rect) {
        touch.set(Gdx.input.getX(), Gdx.input.getY(), 0);
        viewport.unproject(touch);
        return rect.contains(touch.x, touch.y);
    }

    @Override public void resize(int width, int height) { viewport.update(width, height); }
    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { }

    @Override
    public void dispose() {
        if (previewMap != null) previewMap.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (titleFont != null) titleFont.dispose();
        if (labelFont != null) labelFont.dispose();
        if (itemFont != null) itemFont.dispose();
        if (buttonFont != null) buttonFont.dispose();
    }
}
