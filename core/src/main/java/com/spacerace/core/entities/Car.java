package com.spacerace.core.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/**
 * Top-down arcade car with position, rotation, and speed-dependent turning.
 * Position represents the center of the car.
 * Rotation is in degrees CCW from positive X axis (90° = facing up).
 */
public class Car {

    private final Vector2 position;
    private float rotation;
    private float speed;
    private final Vector2 velocity;

    private static final float MAX_SPEED = 300f;
    private static final float MAX_REVERSE = 100f;
    private static final float ACCELERATION = 200f;
    private static final float BRAKE_FORCE = 300f;
    private static final float FRICTION = 0.98f;
    private static final float TURN_SPEED = 180f;
    private static final float MIN_TURN_SPEED_FACTOR = 0.15f;

    public static final float WIDTH = 30f;
    public static final float HEIGHT = 50f;

    private final Color color;

    private boolean accelerating;
    private boolean braking;
    private boolean turningLeft;
    private boolean turningRight;

    public Car(float x, float y, Color color) {
        this.position = new Vector2(x, y);
        this.rotation = 90f;
        this.speed = 0f;
        this.velocity = new Vector2();
        this.color = color;
    }

    public void setAccelerating(boolean value) { this.accelerating = value; }
    public void setBraking(boolean value) { this.braking = value; }
    public void setTurningLeft(boolean value) { this.turningLeft = value; }
    public void setTurningRight(boolean value) { this.turningRight = value; }

    public void update(float delta) {
        if (accelerating) {
            speed += ACCELERATION * delta;
        }
        if (braking) {
            if (speed > 0) {
                speed -= BRAKE_FORCE * delta;
            } else {
                speed -= ACCELERATION * 0.5f * delta;
            }
        }

        speed *= FRICTION;
        if (Math.abs(speed) < 1f) speed = 0f;
        speed = MathUtils.clamp(speed, -MAX_REVERSE, MAX_SPEED);

        float speedFraction = Math.abs(speed) / MAX_SPEED;
        float effectiveTurnRate = TURN_SPEED * Math.max(speedFraction, speed != 0 ? MIN_TURN_SPEED_FACTOR : 0);

        if (turningLeft) {
            rotation += effectiveTurnRate * delta * (speed >= 0 ? 1 : -1);
        }
        if (turningRight) {
            rotation -= effectiveTurnRate * delta * (speed >= 0 ? 1 : -1);
        }
        rotation = ((rotation % 360f) + 360f) % 360f;

        float radians = MathUtils.degreesToRadians * rotation;
        velocity.set(MathUtils.cos(radians) * speed, MathUtils.sin(radians) * speed);
        position.add(velocity.x * delta, velocity.y * delta);
    }

    public void render(ShapeRenderer renderer) {
        renderer.begin(ShapeRenderer.ShapeType.Filled);
        renderer.setColor(color);
        renderer.identity();
        renderer.translate(position.x, position.y, 0);
        renderer.rotate(0, 0, 1, rotation - 90f);
        renderer.rect(-WIDTH / 2f, -HEIGHT / 2f, WIDTH, HEIGHT);

        renderer.setColor(Color.WHITE);
        renderer.triangle(
                -WIDTH / 2f, HEIGHT / 2f,
                 WIDTH / 2f, HEIGHT / 2f,
                 0f, HEIGHT / 2f + 10f
        );
        renderer.identity();
        renderer.end();
    }

    public float getX() { return position.x; }
    public float getY() { return position.y; }
    public float getRotation() { return rotation; }
    public float getSpeed() { return speed; }
    public Color getColor() { return color; }

    public void clampToTrack(float trackWidth, float trackHeight) {
        position.x = MathUtils.clamp(position.x, WIDTH / 2f, trackWidth - WIDTH / 2f);
        position.y = MathUtils.clamp(position.y, HEIGHT / 2f, trackHeight - HEIGHT / 2f);
    }
}
