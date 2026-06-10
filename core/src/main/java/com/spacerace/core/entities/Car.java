package com.spacerace.core.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector2;
import com.spacerace.core.audio.AudioManager;

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

    // Smooth collision bounce velocity (decays over time)
    private final Vector2 collisionVelocity = new Vector2();
    private float collisionSpin = 0f;

    // Power-up timers
    private float nitroTimer = 0f;
    private float shieldTimer = 0f;

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
        if (nitroTimer > 0) nitroTimer -= delta;
        if (shieldTimer > 0) shieldTimer -= delta;

        switch (state) {
            case DRIVING:  updateDriving(delta); break;
            case FALLING:  updateFalling(delta); break;
            case RESPAWNING: updateRespawning(delta); break;
        }
    }

    private void updateDriving(float delta) {
        float currentMaxSpeed = (nitroTimer > 0) ? MAX_SPEED * 1.6f : MAX_SPEED;
        float currentAccel = (nitroTimer > 0) ? ACCELERATION * 2f : ACCELERATION;

        if (accelerating) speed += currentAccel * delta;
        if (braking) {
            if (speed > 0) speed -= BRAKE_FORCE * delta;
            else speed -= currentAccel * 0.5f * delta;
        }

        speed *= FRICTION;
        if (Math.abs(speed) < 1f) speed = 0f;
        speed = MathUtils.clamp(speed, -MAX_REVERSE, currentMaxSpeed);

        float speedFraction = Math.abs(speed) / currentMaxSpeed;
        float effectiveTurnRate = TURN_SPEED * Math.max(speedFraction, speed != 0 ? MIN_TURN_SPEED_FACTOR : 0);

        if (turningLeft) rotation += effectiveTurnRate * delta * (speed >= 0 ? 1 : -1);
        if (turningRight) rotation -= effectiveTurnRate * delta * (speed >= 0 ? 1 : -1);
        rotation = ((rotation % 360f) + 360f) % 360f;

        float radians = MathUtils.degreesToRadians * rotation;
        velocity.set(MathUtils.cos(radians) * speed, MathUtils.sin(radians) * speed);
        position.add(velocity.x * delta, velocity.y * delta);

        // Apply smooth collision bounce
        if (collisionVelocity.len2() > 1f) {
            position.add(collisionVelocity.x * delta, collisionVelocity.y * delta);
            // Damping: ~85% reduction over ~0.3 seconds
            float damping = 1f - 6f * delta;
            if (damping < 0f) damping = 0f;
            collisionVelocity.scl(damping);
        } else {
            collisionVelocity.setZero();
        }

        // Apply collision spin
        if (Math.abs(collisionSpin) > 0.5f) {
            rotation += collisionSpin * delta;
            collisionSpin *= (1f - 4f * delta);
            if (Math.abs(collisionSpin) < 0.5f) collisionSpin = 0f;
        }
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
        AudioManager.getInstance().playFallSound();
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

        // Draw Nitro Flame
        if (nitroTimer > 0 && state == PlayerState.DRIVING) {
            renderer.setColor(Color.CYAN);
            float flicker = MathUtils.random(5f, 20f);
            renderer.triangle(-w / 4f, -h / 2f, w / 4f, -h / 2f, 0f, -h / 2f - flicker);
        }

        renderer.identity();
        
        // Draw Shield Aura (un-rotated to stay circular)
        if (shieldTimer > 0 && state == PlayerState.DRIVING) {
            renderer.setColor(new Color(1f, 0.84f, 0f, 0.4f)); // Gold translucent
            float pulse = (float) Math.sin(shieldTimer * 15f) * 3f;
            renderer.circle(position.x, position.y, Math.max(w, h) / 2f + 8f + pulse);
        }

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

    /** Sets the safe respawn position explicitly (used to place car at track center). */
    public void setSafePosition(float x, float y, float rot) {
        lastSafePosition.set(x, y);
        lastSafeRotation = rot;
    }

    public Vector2 getLastSafePosition() {
        return lastSafePosition;
    }

    public void clampToTrack(float trackWidth, float trackHeight) {
        position.x = MathUtils.clamp(position.x, WIDTH / 2f, trackWidth - WIDTH / 2f);
        position.y = MathUtils.clamp(position.y, HEIGHT / 2f, trackHeight - HEIGHT / 2f);
    }

    // ── Collision support ────────────────────────────────────────────────

    public Vector2 getPosition() { return position; }

    public Vector2 getVelocityVector() { return velocity; }

    public void setSpeed(float speed) { this.speed = speed; }

    /**
     * Returns a LibGDX Polygon representing the car's oriented bounding box.
     * Used for SAT collision detection.
     */
    public Polygon getBoundingPolygon() {
        float w = WIDTH;
        float h = HEIGHT;
        // Vertices relative to center (0,0), before rotation
        Polygon poly = new Polygon(new float[]{
            -w / 2f, -h / 2f,
             w / 2f, -h / 2f,
             w / 2f,  h / 2f,
            -w / 2f,  h / 2f
        });
        poly.setPosition(position.x, position.y);
        poly.setRotation(rotation - 90f); // car is drawn with -90 offset
        return poly;
    }

    public void activateNitro() { nitroTimer = 3f; }
    public void activateShield() { shieldTimer = 5f; }
    public boolean hasShield() { return shieldTimer > 0; }

    /**
     * Applies a collision impulse as a smooth velocity bounce
     * plus a small rotation kick.
     */
    public void applyCollisionImpulse(Vector2 pushDirection, float pushStrength, float spinKick) {
        // Small immediate separation to prevent overlap sticking
        position.add(pushDirection.x * 3f, pushDirection.y * 3f);
        // Main bounce as velocity (plays out smoothly over many frames)
        collisionVelocity.add(pushDirection.x * pushStrength, pushDirection.y * pushStrength);
        // Rotation kick for visual realism
        collisionSpin += spinKick;
        
        if (!hasShield()) {
            speed *= 0.6f; // lose some speed unless shielded
        }
    }

    /**
     * Checks for and resolves collision between two cars using SAT via
     * LibGDX's Polygon. Applies smooth velocity bounce instead of
     * instant position teleport.
     */
    public static void resolveCollision(Car a, Car b) {
        if (a.state != PlayerState.DRIVING || b.state != PlayerState.DRIVING) return;

        Polygon polyA = a.getBoundingPolygon();
        Polygon polyB = b.getBoundingPolygon();

        if (!Intersector.overlapConvexPolygons(polyA, polyB)) return;

        // Compute push direction: from A center to B center
        Vector2 diff = new Vector2(b.position.x - a.position.x, b.position.y - a.position.y);
        float dist = diff.len();
        if (dist < 0.001f) {
            diff.set(1f, 0f);
            dist = 1f;
        }
        diff.scl(1f / dist); // normalize

        // Bounce velocity based on combined speed (pixels/second)
        float combinedSpeed = Math.abs(a.speed) + Math.abs(b.speed);
        float bounceStrength = 300f + combinedSpeed * 0.8f; // base + speed-dependent

        // Rotation kick (faster impact = more spin)
        float spinAmount = 60f + combinedSpeed * 0.15f;

        // Apply shields: if one has a shield, they bounce the other away harder and barely move themselves
        float bounceA = bounceStrength;
        float bounceB = bounceStrength;
        
        if (a.hasShield() && !b.hasShield()) {
            bounceA *= 0.2f;
            bounceB *= 2.0f;
        } else if (b.hasShield() && !a.hasShield()) {
            bounceA *= 2.0f;
            bounceB *= 0.2f;
        }

        // Apply to both cars in opposite directions
        a.applyCollisionImpulse(new Vector2(-diff.x, -diff.y), bounceA, -spinAmount);
        b.applyCollisionImpulse(new Vector2(diff.x, diff.y), bounceB, spinAmount);
    }
}
