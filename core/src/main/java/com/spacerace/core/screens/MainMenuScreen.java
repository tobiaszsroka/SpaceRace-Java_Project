package com.spacerace.core.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.spacerace.core.SpaceRaceGame;
import com.spacerace.core.audio.AudioManager;

/** Main menu – click or press PLAY to open the setup screen. */
public class MainMenuScreen implements Screen {

    private final SpaceRaceGame game;
    private final SpriteBatch batch;

    private final OrthographicCamera camera;
    private final Viewport viewport;
    private final Rectangle playButton = new Rectangle(300f, 255f, 200f, 56f);
    private final Vector3 touch = new Vector3();
    private final GlyphLayout layout = new GlyphLayout();

    private BitmapFont titleFont;
    private BitmapFont promptFont;
    private BitmapFont buttonFont;

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
        promptFont.getData().setScale(1.2f);

        buttonFont = new BitmapFont();
        buttonFont.setColor(Color.WHITE);
        buttonFont.getData().setScale(2f);

        AudioManager.getInstance().playMenuMusic();
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

        titleFont.draw(batch, "SPACE RACE",
                SpaceRaceGame.WORLD_WIDTH / 2f - 150f,
                SpaceRaceGame.WORLD_HEIGHT * 0.72f);

        boolean hover = isMouseOver(playButton);
        buttonFont.setColor(hover ? Color.YELLOW : Color.WHITE);
        layout.setText(buttonFont, "PLAY");
        buttonFont.draw(batch, "PLAY",
                playButton.x + playButton.width / 2f - layout.width / 2f,
                playButton.y + playButton.height / 2f + layout.height / 2f - 4f);

        promptFont.setColor(Color.LIGHT_GRAY);
        promptFont.draw(batch, "Click or press SPACE / ENTER",
                SpaceRaceGame.WORLD_WIDTH / 2f - 145f,
                playButton.y - 24f);

        promptFont.draw(batch, "Player 1: W A S D    |    Player 2: Arrow Keys",
                SpaceRaceGame.WORLD_WIDTH / 2f - 210f,
                SpaceRaceGame.WORLD_HEIGHT * 0.12f);

        batch.end();
    }

    private boolean handleInput() {
        boolean play = Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
                || Gdx.input.isKeyJustPressed(Input.Keys.ENTER);

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && isMouseOver(playButton)) {
            play = true;
        }

        if (play) {
            game.setScreen(new SetupScreen(game));
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
        AudioManager.getInstance().stopMenuMusic();
        if (titleFont != null) titleFont.dispose();
        if (promptFont != null) promptFont.dispose();
        if (buttonFont != null) buttonFont.dispose();
    }
}
