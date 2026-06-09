package com.spacerace.core.track;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
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
 * Wraps a Tiled (.tmx) map. Expected layers:
 *   "background", "track", "walls" (tile layers)
 *   "objects" — spawn_p1, spawn_p2, checkpoint_0..N (MapObjects)
 *
 * Spawn objects support a custom "rotation" property (degrees, LibGDX convention).
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

    private final TiledMapTileLayer wallsLayer;
    private final TiledMapTileLayer trackLayer;

    public TrackMap(String tmxPath) {
        map = new TmxMapLoader().load(tmxPath);
        renderer = new OrthogonalTiledMapRenderer(map);

        tileWidth = map.getProperties().get("tilewidth", Integer.class);
        tileHeight = map.getProperties().get("tileheight", Integer.class);
        mapWidthTiles = map.getProperties().get("width", Integer.class);
        mapHeightTiles = map.getProperties().get("height", Integer.class);
        mapWidthPx = mapWidthTiles * tileWidth;
        mapHeightPx = mapHeightTiles * tileHeight;

        wallsLayer = (TiledMapTileLayer) map.getLayers().get("walls");
        trackLayer = (TiledMapTileLayer) map.getLayers().get("track");
    }

    public void render(OrthographicCamera camera) {
        renderer.setView(camera);
        renderer.render();
    }

    public boolean isWall(float worldX, float worldY) {
        if (wallsLayer == null) return false;
        int tx = (int) (worldX / tileWidth);
        int ty = (int) (worldY / tileHeight);
        if (tx < 0 || tx >= mapWidthTiles || ty < 0 || ty >= mapHeightTiles) return true;
        return wallsLayer.getCell(tx, ty) != null;
    }

    public boolean isOnTrack(float worldX, float worldY) {
        if (trackLayer == null) return true;
        int tx = (int) (worldX / tileWidth);
        int ty = (int) (worldY / tileHeight);
        if (tx < 0 || tx >= mapWidthTiles || ty < 0 || ty >= mapHeightTiles) return false;
        return trackLayer.getCell(tx, ty) != null;
    }

    public Vector2 getSpawnPoint(String name) {
        MapLayer objects = map.getLayers().get("objects");
        if (objects == null) return new Vector2(mapWidthPx / 4f, mapHeightPx / 2f);

        for (MapObject obj : objects.getObjects()) {
            if (name.equals(obj.getName()) && obj instanceof RectangleMapObject) {
                Rectangle rect = ((RectangleMapObject) obj).getRectangle();
                return new Vector2(rect.x + rect.width / 2f, rect.y + rect.height / 2f);
            }
        }
        return new Vector2(mapWidthPx / 4f, mapHeightPx / 2f);
    }

    /** Returns spawn rotation in degrees (LibGDX convention). Defaults to 90 (facing up). */
    public float getSpawnRotation(String name) {
        MapLayer objects = map.getLayers().get("objects");
        if (objects == null) return 90f;

        for (MapObject obj : objects.getObjects()) {
            if (name.equals(obj.getName())) {
                MapProperties props = obj.getProperties();
                if (props.containsKey("rotation")) {
                    return props.get("rotation", Float.class);
                }
            }
        }
        return 90f;
    }

    public Array<Rectangle> getCheckpoints() {
        Array<Rectangle> checkpoints = new Array<>();
        MapLayer objects = map.getLayers().get("objects");
        if (objects == null) return checkpoints;

        // Collect and sort by name (checkpoint_0, checkpoint_1, ...)
        Array<MapObject> sorted = new Array<>();
        for (MapObject obj : objects.getObjects()) {
            if (obj.getName() != null && obj.getName().startsWith("checkpoint") && obj instanceof RectangleMapObject) {
                sorted.add(obj);
            }
        }
        sorted.sort((a, b) -> a.getName().compareTo(b.getName()));

        for (MapObject obj : sorted) {
            checkpoints.add(((RectangleMapObject) obj).getRectangle());
        }
        return checkpoints;
    }

    public float getWidthPx() { return mapWidthPx; }
    public float getHeightPx() { return mapHeightPx; }

    @Override
    public void dispose() {
        renderer.dispose();
        map.dispose();
    }
}
