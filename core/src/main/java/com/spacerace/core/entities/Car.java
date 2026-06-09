package com.spacerace.core.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/**
 * Top-down arcade car with state machine: DRIVING → FALLING → RESPAWNING → DRIVING.
 * Position = center of car. Rotation = degrees CCW from +X (90° = up).
 */
public class Car {

    private final Vector2 position;
    private float rotation;
    private float speed;
    private final Vector2 velocity;

    private static final float MAX_SPEED = 350f;
    private static final float MAX_REVERSE = 150f;
    private static final float ACCELERATION = 600f;
    private static final float BRAKE_FORCE = 500f;
    private static final float FRICTION = 0.97f;
    private static final float TURN_SPEED = 260f;
    private static final float MIN_TURN_SPEED_FACTOR = 0.25f;

    public static final float WIDTH = 30f;
    public static final float HEIGHT = 50f;

    private final Color color;

    private boolean accelerating;
    private boolean braking;
    private boolean turningLeft;
    private boolean turningRight;

    private PlayerState state = PlayerState.DRIVING;
    private float stateTimer;

    private static final float FALL_DURATION = 1.2f;
    private static final float RESPAWN_DURATION = 1.5f;
    private float fallingScale = 1f;
    private float fallingSpinSpeed;

    private final Vector2 spawnPoint;
    private float spawnRotation;
    private final Vector2 lastSafePosition;
    private float lastSafeRotation;

    private int currentCheckpoint;
    private int lapsCompleted;

    public Car(float x, float y, float rotation, Color color) {
        this.position = new Vector2(x, y);
        this.spawnPoint = new Vector2(x, y);
        this.lastSafePosition = new Vector2(x, y);
        this.rotation = rotation;
        this.spawnRotation = rotation;
        this.lastSafeRotation = rotation;
        this.speed = 0f;
        this.velocity = new Vector2();
        this.color = color;
    }

    public Car(float x, float y, Color color) {
        this(x, y, 90f, color);
    }

    public void setAccelerating(boolean value) { this.accelerating = value; }
    public void setBraking(boolean value) { this.braking = value; }
    public void setTurningLeft(boolean value) { this.turningLeft = value; }
    public void setTurningRight(boolean value) { this.turningRight = value; }

    public void update(float delta) {
        switch (state) {
            case DRIVING:  updateDriving(delta); break;
            case FALLING:  updateFalling(delta); break;
            case RESPAWNING: updateRespawning(delta); break;
        }
    }

    private void updateDriving(float delta) {
        if (accelerating) speed += ACCELERATION * delta;
        if (braking) {
            if (speed > 0) speed -= BRAKE_FORCE * delta;
            else speed -= ACCELERATION * 0.5f * delta;
        }

        speed *= FRICTION;
        if (Math.abs(speed) < 1f) speed = 0f;
        speed = MathUtils.clamp(speed, -MAX_REVERSE, MAX_SPEED);

        float speedFraction = Math.abs(speed) / MAX_SPEED;
        float effectiveTurnRate = TURN_SPEED * Math.max(speedFraction, speed != 0 ? MIN_TURN_SPEED_FACTOR : 0);

        if (turningLeft) rotation += effectiveTurnRate * delta * (speed >= 0 ? 1 : -1);
        if (turningRight) rotation -= effectiveTurnRate * delta * (speed >= 0 ? 1 : -1);
        rotation = ((rotation % 360f) + 360f) % 360f;

        float radians = MathUtils.degreesToRadians * rotation;
        velocity.set(MathUtils.cos(radians) * speed, MathUtils.sin(radians) * speed);
        position.add(velocity.x * delta, velocity.y * delta);
    }

    private void updateFalling(float delta) {
        stateTimer += delta;
        float progress = Math.min(stateTimer / FALL_DURATION, 1f);
        fallingScale = 1f - progress;
        rotation += fallingSpinSpeed * delta;

        if (progress >= 1f) {
            state = PlayerState.RESPAWNING;
            stateTimer = 0f;
        }
    }

    private void updateRespawning(float delta) {
        stateTimer += delta;
        if (stateTimer >= RESPAWN_DURATION) {
            position.set(lastSafePosition);
            rotation = lastSafeRotation;
            speed = 0f;
            fallingScale = 1f;
            state = PlayerState.DRIVING;
            stateTimer = 0f;
        }
    }

    public void startFalling() {
        if (state != PlayerState.DRIVING) return;
        state = PlayerState.FALLING;
        stateTimer = 0f;
        fallingScale = 1f;
        fallingSpinSpeed = 360f + MathUtils.random(180f);
        speed = 0f;
    }

    public void render(ShapeRenderer renderer) {
        if (state == PlayerState.RESPAWNING) {
            renderRespawnGhost(renderer);
            return;
        }

        float scale = (state == PlayerState.FALLING) ? fallingScale : 1f;
        float w = WIDTH * scale;
        float h = HEIGHT * scale;

        renderer.begin(ShapeRenderer.ShapeType.Filled);
        renderer.setColor(state == PlayerState.FALLING
                ? new Color(color.r, color.g, color.b, Math.max(fallingScale, 0.2f))
                : color);

        renderer.identity();
        renderer.translate(position.x, position.y, 0);
        renderer.rotate(0, 0, 1, rotation - 90f);
        renderer.rect(-w / 2f, -h / 2f, w, h);

        if (scale > 0.3f) {
            renderer.setColor(Color.WHITE);
            renderer.triangle(-w / 2f, h / 2f, w / 2f, h / 2f, 0f, h / 2f + 10f * scale);
        }

        renderer.identity();
        renderer.end();
    }

    private void renderRespawnGhost(ShapeRenderer renderer) {
        boolean visible = ((int) (stateTimer * 8f)) % 2 == 0;
        if (!visible) return;

        renderer.begin(ShapeRenderer.ShapeType.Filled);
        renderer.setColor(color.r, color.g, color.b, 0.4f);
        renderer.identity();
        renderer.translate(lastSafePosition.x, lastSafePosition.y, 0);
        renderer.rotate(0, 0, 1, lastSafeRotation - 90f);
        renderer.rect(-WIDTH / 2f, -HEIGHT / 2f, WIDTH, HEIGHT);
        renderer.identity();
        renderer.end();
    }

    public float getX() { return position.x; }
    public float getY() { return position.y; }
    public float getRotation() { return rotation; }
    public float getSpeed() { return speed; }
    public Color getColor() { return color; }
    public PlayerState getState() { return state; }
    public boolean isDriving() { return state == PlayerState.DRIVING; }

    public int getCurrentCheckpoint() { return currentCheckpoint; }
    public int getLapsCompleted() { return lapsCompleted; }

    public void advanceCheckpoint(int totalCheckpoints) {
        currentCheckpoint++;
        if (currentCheckpoint >= totalCheckpoints) {
            currentCheckpoint = 0;
            lapsCompleted++;
        }
    }

    public void setSpawnPoint(float x, float y, float rotation) {
        spawnPoint.set(x, y);
        spawnRotation = rotation;
    }

    public void updateSafePosition() {
        lastSafePosition.set(position);
        lastSafeRotation = rotation;
    }

    public void clampToTrack(float trackWidth, float trackHeight) {
        position.x = MathUtils.clamp(position.x, WIDTH / 2f, trackWidth - WIDTH / 2f);
        position.y = MathUtils.clamp(position.y, HEIGHT / 2f, trackHeight - HEIGHT / 2f);
    }
}
