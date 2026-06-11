package com.spacerace.core.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/** Centered menu-style square panel (pause, victory, HUD timer). */
public final class UiPanel {

    public static final float PANEL_MIN_SIZE = 200f;

    private static final Color PANEL_BG = new Color(0.04f, 0.05f, 0.12f, 0.92f);
    private static final Color PANEL_BORDER = new Color(0.75f, 0.82f, 1f, 1f);

    public static final class TextLine {
        public final BitmapFont font;
        public final String text;
        public final Color color;

        public TextLine(BitmapFont font, String text, Color color) {
            this.font = font;
            this.text = text;
            this.color = color;
        }
    }

    private UiPanel() {}

    public static void drawDimOverlay(ShapeRenderer shapes, OrthographicCamera camera, float alpha) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, alpha);
        shapes.rect(0, 0, camera.viewportWidth, camera.viewportHeight);
        shapes.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    public static void renderMenuPanel(ShapeRenderer shapes, SpriteBatch batch,
                                       OrthographicCamera camera, GlyphLayout layout,
                                       TextLine[] lines, float lineGap) {
        float contentW = 0f;
        float contentH = 0f;
        for (int i = 0; i < lines.length; i++) {
            layout.setText(lines[i].font, lines[i].text);
            contentW = Math.max(contentW, layout.width);
            contentH += layout.height;
            if (i > 0) contentH += lineGap;
        }

        float pad = 32f;
        float boxSize = Math.max(Math.max(contentW, contentH) + pad * 2f, PANEL_MIN_SIZE);
        float screenW = camera.viewportWidth;
        float screenH = camera.viewportHeight;
        float boxX = screenW / 2f - boxSize / 2f;
        float boxY = screenH / 2f - boxSize / 2f;

        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(PANEL_BG);
        shapes.rect(boxX, boxY, boxSize, boxSize);
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(2f);
        shapes.setColor(PANEL_BORDER);
        shapes.rect(boxX, boxY, boxSize, boxSize);
        shapes.end();
        Gdx.gl.glLineWidth(1f);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        float y = screenH / 2f + contentH / 2f;
        for (int i = 0; i < lines.length; i++) {
            TextLine line = lines[i];
            layout.setText(line.font, line.text);
            y -= layout.height;
            line.font.setColor(line.color);
            line.font.draw(batch, line.text, screenW / 2f - layout.width / 2f, y);
            if (i < lines.length - 1) {
                y -= lineGap;
            }
        }

        batch.end();
    }
}
