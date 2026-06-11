package com.spacerace.core.track;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapImageLayer;
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

    private final Texture backdropTexture;
    private final int[] tileLayerIndices;

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

        backdropTexture = findBackdropTexture();
        tileLayerIndices = collectTileLayerIndices();
    }

    private Texture findBackdropTexture() {
        for (MapLayer layer : map.getLayers()) {
            if (layer instanceof TiledMapImageLayer imageLayer) {
                Texture texture = imageLayer.getTextureRegion().getTexture();
                texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
                return texture;
            }
        }
        return null;
    }

    private int[] collectTileLayerIndices() {
        int count = 0;
        for (MapLayer layer : map.getLayers()) {
            if (layer instanceof TiledMapTileLayer) count++;
        }
        int[] indices = new int[count];
        int i = 0;
        for (int layerIndex = 0; layerIndex < map.getLayers().getCount(); layerIndex++) {
            if (map.getLayers().get(layerIndex) instanceof TiledMapTileLayer) {
                indices[i++] = layerIndex;
            }
        }
        return indices;
    }

    /** Repeats the map image layer across the full camera view so edges stay cosmic, not black. */
    public void renderBackdrop(SpriteBatch batch, OrthographicCamera camera) {
        if (backdropTexture == null) return;

        float tw = backdropTexture.getWidth();
        float th = backdropTexture.getHeight();
        float pad = Math.max(tw, th) * 0.25f;
        float left = camera.position.x - camera.viewportWidth / 2f - pad;
        float bottom = camera.position.y - camera.viewportHeight / 2f - pad;
        float width = camera.viewportWidth + pad * 2f;
        float height = camera.viewportHeight + pad * 2f;

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.draw(backdropTexture, left, bottom, width, height, 0f, 0f, width / tw, height / th);
        batch.end();
    }

    public void render(OrthographicCamera camera) {
        renderer.setView(camera);
        renderer.render(tileLayerIndices);
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

    public Rectangle getFinishLine() {
        MapLayer objects = map.getLayers().get("objects");
        if (objects == null) return null;

        for (MapObject obj : objects.getObjects()) {
            if ("finish_line".equals(obj.getName()) && obj instanceof RectangleMapObject) {
                return ((RectangleMapObject) obj).getRectangle();
            }
        }
        return null;
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

    /**
     * Finds the nearest point that is well-centered on the track.
     * Searches in a radius around the given world position and picks the
     * track tile with the most surrounding track neighbors (= most centered).
     */
    public Vector2 findNearestTrackCenter(float worldX, float worldY) {
        if (trackLayer == null) return new Vector2(worldX, worldY);

        int startTx = (int) (worldX / tileWidth);
        int startTy = (int) (worldY / tileHeight);
        int searchRadius = 15;

        float bestX = worldX, bestY = worldY;
        float bestMetric = -Float.MAX_VALUE;

        for (int dy = -searchRadius; dy <= searchRadius; dy++) {
            for (int dx = -searchRadius; dx <= searchRadius; dx++) {
                int tx = startTx + dx;
                int ty = startTy + dy;
                if (tx < 0 || tx >= mapWidthTiles || ty < 0 || ty >= mapHeightTiles) continue;
                if (trackLayer.getCell(tx, ty) == null) continue;

                // Score: count how many tiles in a 7x7 area around this tile are also track
                int score = 0;
                for (int cy = -3; cy <= 3; cy++) {
                    for (int cx = -3; cx <= 3; cx++) {
                        int ntx = tx + cx, nty = ty + cy;
                        if (ntx >= 0 && ntx < mapWidthTiles && nty >= 0 && nty < mapHeightTiles) {
                            if (trackLayer.getCell(ntx, nty) != null) score++;
                        }
                    }
                }

                // We want a high score (centered on track), but we MUST penalize distance heavily
                // so we don't teleport down the track to a wider section.
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                float metric = score - dist * 2.5f; // Penalize 2.5 points per tile of distance

                if (metric > bestMetric) {
                    bestMetric = metric;
                    bestX = (tx + 0.5f) * tileWidth;
                    bestY = (ty + 0.5f) * tileHeight;
                }
            }
        }
        return new Vector2(bestX, bestY);
    }

    public float getWidthPx() { return mapWidthPx; }
    public float getHeightPx() { return mapHeightPx; }

    @Override
    public void dispose() {
        renderer.dispose();
        map.dispose();
    }
}
