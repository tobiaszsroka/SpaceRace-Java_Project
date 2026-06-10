package com.spacerace.core.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Circle;

public class PowerUp {
    public enum Type {
        NITRO, SHIELD
    }

    private final Vector2 position;
    private final Type type;
    private float animationTime;

    public static final float RADIUS = 15f; // size of the powerup pickup

    public PowerUp(float x, float y, Type type) {
        this.position = new Vector2(x, y);
        this.type = type;
        this.animationTime = 0f;
    }

    public void update(float delta) {
        animationTime += delta;
    }

    public void render(ShapeRenderer renderer) {
        // Floating/pulsing effect
        float pulse = (float) Math.sin(animationTime * 4f) * 2f;
        
        renderer.begin(ShapeRenderer.ShapeType.Filled);
        
        if (type == Type.NITRO) {
            // Draw Blue Triangle
            renderer.setColor(Color.CYAN);
            renderer.triangle(
                position.x - 10f, position.y - 10f + pulse,
                position.x + 10f, position.y - 10f + pulse,
                position.x, position.y + 12f + pulse
            );
        } else if (type == Type.SHIELD) {
            // Draw Golden Circle
            renderer.setColor(Color.GOLD);
            renderer.circle(position.x, position.y + pulse, 12f);
            
            // Inner white dot for shine
            renderer.setColor(Color.WHITE);
            renderer.circle(position.x - 3f, position.y + 3f + pulse, 3f);
        }

        renderer.end();
    }

    public Vector2 getPosition() {
        return position;
    }

    public Type getType() {
        return type;
    }

    /** Helper for collision check with cars */
    public Circle getHitbox() {
        return new Circle(position.x, position.y, RADIUS);
    }
}
