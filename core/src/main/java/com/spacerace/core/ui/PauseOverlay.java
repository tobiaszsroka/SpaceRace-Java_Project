package com.spacerace.core.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Disposable;

public class PauseOverlay implements Disposable {

    private final ShapeRenderer shapeRenderer;
    private final BitmapFont titleFont;
    private final BitmapFont optionFont;

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

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.7f);
        shapeRenderer.rect(0, 0, w, h);
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        titleFont.draw(batch, "PAUSED", w / 2f - 90f, h / 2f + 60f);
        optionFont.draw(batch, "ESC - Resume", w / 2f - 80f, h / 2f - 20f);
        optionFont.draw(batch, "Q - Quit to Menu", w / 2f - 100f, h / 2f - 60f);
        batch.end();
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
        titleFont.dispose();
        optionFont.dispose();
    }
}
