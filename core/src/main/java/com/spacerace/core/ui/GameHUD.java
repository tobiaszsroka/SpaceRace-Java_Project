package com.spacerace.core.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;
import com.spacerace.core.entities.Car;

public class GameHUD implements Disposable {

    private final BitmapFont font;
    private final OrthographicCamera camera;
    private float raceTimer;

    public GameHUD() {
        font = new BitmapFont();
        font.getData().setScale(1.3f);
        camera = new OrthographicCamera();
    }

    public void update(float delta) {
        raceTimer += delta;
    }

    public void render(SpriteBatch batch, int vpW, int vpH,
                       String label, Color color, Car car, int totalLaps) {
        camera.setToOrtho(false, vpW, vpH);
        camera.update();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        float x = 10f;
        float y = vpH - 12f;
        float lineHeight = 22f;

        font.setColor(color);
        font.draw(batch, label, x, y);

        font.setColor(Color.WHITE);
        y -= lineHeight;
        font.draw(batch, "Lap " + Math.min(car.getLapsCompleted() + 1, totalLaps) + "/" + totalLaps, x, y);

        y -= lineHeight;
        font.draw(batch, "Speed: " + (int) Math.abs(car.getSpeed()), x, y);

        y -= lineHeight;
        font.draw(batch, formatTime(raceTimer), x, y);

        if (!car.isDriving()) {
            font.setColor(Color.RED);
            y -= lineHeight;
            font.draw(batch, car.getState().name(), x, y);
        }

        batch.end();
    }

    public float getRaceTimer() { return raceTimer; }
    public BitmapFont getFont() { return font; }

    private String formatTime(float seconds) {
        int mins = (int) (seconds / 60);
        int secs = (int) (seconds % 60);
        int millis = (int) ((seconds * 100) % 100);
        return String.format("%d:%02d.%02d", mins, secs, millis);
    }

    @Override
    public void dispose() {
        font.dispose();
    }
}
