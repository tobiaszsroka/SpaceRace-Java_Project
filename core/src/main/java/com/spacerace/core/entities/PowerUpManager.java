package com.spacerace.core.entities;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.spacerace.core.audio.AudioManager;
import com.spacerace.core.track.TrackMap;

public class PowerUpManager {
    private final Array<PowerUp> powerUps;
    private final TrackMap trackMap;
    
    private float spawnTimer;
    private static final float SPAWN_INTERVAL = 8f; // spawn a new powerup every 8 seconds
    private static final int MAX_POWERUPS = 5;

    public PowerUpManager(TrackMap trackMap) {
        this.trackMap = trackMap;
        this.powerUps = new Array<>();
        this.spawnTimer = SPAWN_INTERVAL / 2f; // spawn the first one fairly quickly
    }

    public void update(float delta, Car p1, Car p2) {
        // Update animations
        for (PowerUp p : powerUps) {
            p.update(delta);
        }

        // Spawn logic
        if (powerUps.size < MAX_POWERUPS) {
            spawnTimer += delta;
            if (spawnTimer >= SPAWN_INTERVAL) {
                spawnRandomPowerUp();
                spawnTimer = 0f;
            }
        }

        // Collision logic
        checkCollisions(p1);
        checkCollisions(p2);
    }

    private void spawnRandomPowerUp() {
        // Try to find a valid spot on the track
        for (int i = 0; i < 50; i++) { // Max 50 attempts
            float x = MathUtils.random(0, trackMap.getWidthPx());
            float y = MathUtils.random(0, trackMap.getHeightPx());

            // Check if it's solidly on the track (similar logic to deepOnTrack)
            float margin = 20f;
            boolean valid = trackMap.isOnTrack(x, y) &&
                            trackMap.isOnTrack(x + margin, y) &&
                            trackMap.isOnTrack(x - margin, y) &&
                            trackMap.isOnTrack(x, y + margin) &&
                            trackMap.isOnTrack(x, y - margin);
            
            if (valid) {
                PowerUp.Type type = MathUtils.randomBoolean() ? PowerUp.Type.NITRO : PowerUp.Type.SHIELD;
                powerUps.add(new PowerUp(x, y, type));
                return;
            }
        }
    }

    private void checkCollisions(Car car) {
        if (!car.isDriving()) return;

        for (int i = powerUps.size - 1; i >= 0; i--) {
            PowerUp powerUp = powerUps.get(i);
            
            // Simple circle-polygon overlap check (approximating car as a circle for pickup)
            if (car.getPosition().dst(powerUp.getPosition()) < (Car.WIDTH / 2f + PowerUp.RADIUS)) {
                // Picked up!
                applyEffect(car, powerUp.getType());
                powerUps.removeIndex(i);
                
                AudioManager.getInstance().playPickupSound();
            }
        }
    }

    private void applyEffect(Car car, PowerUp.Type type) {
        if (type == PowerUp.Type.NITRO) {
            car.activateNitro();
        } else if (type == PowerUp.Type.SHIELD) {
            car.activateShield();
        }
    }

    public void render(ShapeRenderer renderer) {
        for (PowerUp p : powerUps) {
            p.render(renderer);
        }
    }
}
