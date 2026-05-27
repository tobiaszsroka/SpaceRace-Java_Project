package com.spacerace.core.track;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

/**
 * Wraps a Tiled (.tmx) map and provides:
 * - Rendering via OrthogonalTiledMapRenderer
 * - Track boundary collision via the "walls" tile layer
 * - Spawn points and checkpoints parsed from the "objects" layer
 *
 * Expected Tiled layers:
 *   "background" — visual background (stars, nebulae)
 *   "track"      — road surface
 *   "walls"      — collidable barriers (any non-empty tile = wall)
 *   "objects"     — MapObjects: spawn_p1, spawn_p2, checkpoint_N, finish_line
 */
public class TrackMap implements Disposable {

    private final TiledMap map;
    private final OrthogonalTiledMapRenderer renderer;

    private final float mapWidthPx;
    private final float mapHeightPx;
    private final int tileWidth;
    private final int tileHeight;
    private final int mapWidthTiles;
    private final int mapHeightTiles;

    private TiledMapTileLayer wallsLayer;

    public TrackMap(String tmxPath) {
        map = new TmxMapLoader().load(tmxPath);
        renderer = new OrthogonalTiledMapRenderer(map);

        tileWidth = map.getProperties().get("tilewidth", Integer.class);
        tileHeight = map.getProperties().get("tileheight", Integer.class);
        mapWidthTiles = map.getProperties().get("width", Integer.class);
        mapHeightTiles = map.getProperties().get("height", Integer.class);
        mapWidthPx = mapWidthTiles * tileWidth;
        mapHeightPx = mapHeightTiles * tileHeight;

        TiledMapTileLayer layer = (TiledMapTileLayer) map.getLayers().get("walls");
        if (layer != null) {
            wallsLayer = layer;
        }
    }

    public void render(OrthographicCamera camera) {
        renderer.setView(camera);
        renderer.render();
    }

    /**
     * Returns true if the given world-space point is on a wall tile.
     * Used for edge-of-track detection (falling off in Phase 3).
     */
    public boolean isWall(float worldX, float worldY) {
        if (wallsLayer == null) return false;

        int tileX = (int) (worldX / tileWidth);
        int tileY = (int) (worldY / tileHeight);

        if (tileX < 0 || tileX >= mapWidthTiles || tileY < 0 || tileY >= mapHeightTiles) {
            return true; // outside map = wall
        }

        return wallsLayer.getCell(tileX, tileY) != null;
    }

    /**
     * Returns true if the given world-space point is on the track surface.
     * Checks for a non-empty cell on the "track" layer.
     */
    public boolean isOnTrack(float worldX, float worldY) {
        TiledMapTileLayer trackLayer = (TiledMapTileLayer) map.getLayers().get("track");
        if (trackLayer == null) return true; // no track layer = everything is track

        int tileX = (int) (worldX / tileWidth);
        int tileY = (int) (worldY / tileHeight);

        if (tileX < 0 || tileX >= mapWidthTiles || tileY < 0 || tileY >= mapHeightTiles) {
            return false;
        }

        return trackLayer.getCell(tileX, tileY) != null;
    }

    public Vector2 getSpawnPoint(String name) {
        MapLayer objectsLayer = map.getLayers().get("objects");
        if (objectsLayer == null) return new Vector2(mapWidthPx / 4f, mapHeightPx / 2f);

        for (MapObject obj : objectsLayer.getObjects()) {
            if (name.equals(obj.getName()) && obj instanceof RectangleMapObject) {
                Rectangle rect = ((RectangleMapObject) obj).getRectangle();
                return new Vector2(rect.x + rect.width / 2f, rect.y + rect.height / 2f);
            }
        }

        return new Vector2(mapWidthPx / 4f, mapHeightPx / 2f);
    }

    /**
     * Returns all checkpoint objects sorted by name (checkpoint_0, checkpoint_1, ...).
     * Each checkpoint is a Rectangle in world coordinates.
     */
    public Array<Rectangle> getCheckpoints() {
        Array<Rectangle> checkpoints = new Array<>();
        MapLayer objectsLayer = map.getLayers().get("objects");
        if (objectsLayer == null) return checkpoints;

        for (MapObject obj : objectsLayer.getObjects()) {
            if (obj.getName() != null && obj.getName().startsWith("checkpoint") && obj instanceof RectangleMapObject) {
                checkpoints.add(((RectangleMapObject) obj).getRectangle());
            }
        }

        return checkpoints;
    }

    public float getWidthPx() { return mapWidthPx; }
    public float getHeightPx() { return mapHeightPx; }
    public TiledMap getTiledMap() { return map; }

    @Override
    public void dispose() {
        renderer.dispose();
        map.dispose();
    }
}
