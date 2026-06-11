package com.spacerace.core.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Disposable;

public class PauseOverlay implements Disposable {

    private final ShapeRenderer shapeRenderer;
    private final BitmapFont titleFont;
    private final BitmapFont optionFont;
    private final GlyphLayout layout = new GlyphLayout();

    public PauseOverlay() {
        shapeRenderer = new ShapeRenderer();

        titleFont = new BitmapFont();
        titleFont.getData().setScale(3f);
        titleFont.setColor(Color.WHITE);

        optionFont = new BitmapFont();
        optionFont.getData().setScale(1.5f);
        optionFont.setColor(Color.LIGHT_GRAY);
    }

    public void render(SpriteBatch batch) {
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();

        OrthographicCamera camera = new OrthographicCamera(w, h);
        camera.position.set(w / 2f, h / 2f, 0);
        camera.update();

        UiPanel.drawDimOverlay(shapeRenderer, camera, 0.65f);

        UiPanel.TextLine[] lines = {
                new UiPanel.TextLine(titleFont, "PAUSED", Color.WHITE),
                new UiPanel.TextLine(optionFont, "ESC - Resume", Color.LIGHT_GRAY),
                new UiPanel.TextLine(optionFont, "Q - Quit to Menu", Color.LIGHT_GRAY),
        };
        UiPanel.renderMenuPanel(shapeRenderer, batch, camera, layout, lines, 18f);
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
        titleFont.dispose();
        optionFont.dispose();
    }
}
