package com.spacerace.core.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;

/**
 * Singleton manager for all game audio.
 * <p>
 * Uses LibGDX {@link Music} for long background tracks (streamed from disk)
 * and {@link Sound} for short effects (loaded fully into memory).
 * <p>
 * Must call {@link #initialize()} once after LibGDX is fully ready
 * (e.g. inside {@code SpaceRaceGame.create()}).
 */
public class AudioManager {

    private static AudioManager instance;

    // ── Music (streamed) ────────────────────────────────────────────────
    private Music menuMusic;
    private Music raceMusic;

    // ── Sound effects (in-memory) ───────────────────────────────────────
    private Sound countdownBeep;
    private Sound fallSound;
    private Sound victoryFanfare;
    private Sound engineLoop;
    private Sound pickupSound;

    // ── Engine loop instance IDs (one per car) ──────────────────────────
    private long engineLoopId1 = -1;
    private long engineLoopId2 = -1;

    // ── Volume constants ────────────────────────────────────────────────
    private static final float MENU_MUSIC_VOLUME = 0.5f;   // 50%
    private static final float RACE_MUSIC_VOLUME = 0.2f;   // 20%
    private static final float ENGINE_VOLUME     = 0.15f;   // subtle
    private static final float SFX_VOLUME        = 0.6f;    // effects

    // ── Singleton access ────────────────────────────────────────────────

    private AudioManager() {
        // private – use getInstance()
    }

    /**
     * Returns the single {@code AudioManager} instance, creating it lazily
     * if necessary.
     */
    public static AudioManager getInstance() {
        if (instance == null) {
            instance = new AudioManager();
        }
        return instance;
    }

    // ── Initialization ──────────────────────────────────────────────────

    /**
     * Loads every audio asset. Call <b>once</b> after {@code Gdx.files} is
     * available (i.e. inside {@code create()}).
     * <p>
     * Each file is loaded inside its own try-catch so that a single missing
     * asset never prevents the rest of the game from running.
     */
    public void initialize() {
        // --- Music ---
        try {
            menuMusic = Gdx.audio.newMusic(Gdx.files.internal("audio/menu_music.wav"));
            menuMusic.setLooping(true);
        } catch (Exception e) {
            System.err.println("[AudioManager] Could not load menu_music.wav: " + e.getMessage());
            menuMusic = null;
        }

        try {
            raceMusic = Gdx.audio.newMusic(Gdx.files.internal("audio/race_music.wav"));
            raceMusic.setLooping(true);
        } catch (Exception e) {
            System.err.println("[AudioManager] Could not load race_music.wav: " + e.getMessage());
            raceMusic = null;
        }

        // --- Sound effects ---
        try {
            engineLoop = Gdx.audio.newSound(Gdx.files.internal("audio/engine_loop.wav"));
        } catch (Exception e) {
            System.err.println("[AudioManager] Could not load engine_loop.wav: " + e.getMessage());
            engineLoop = null;
        }

        try {
            countdownBeep = Gdx.audio.newSound(Gdx.files.internal("audio/countdown_beep.wav"));
        } catch (Exception e) {
            System.err.println("[AudioManager] Could not load countdown_beep.wav: " + e.getMessage());
            countdownBeep = null;
        }

        try {
            fallSound = Gdx.audio.newSound(Gdx.files.internal("audio/fall_sound.wav"));
        } catch (Exception e) {
            System.err.println("[AudioManager] Could not load fall_sound.wav: " + e.getMessage());
            fallSound = null;
        }

        try {
            victoryFanfare = Gdx.audio.newSound(Gdx.files.internal("audio/victory_fanfare.wav"));
        } catch (Exception e) {
            System.err.println("[AudioManager] Could not load victory_fanfare.wav: " + e.getMessage());
            victoryFanfare = null;
        }

        try {
            pickupSound = Gdx.audio.newSound(Gdx.files.internal("audio/pickup_sound.wav"));
        } catch (Exception e) {
            System.err.println("[AudioManager] Could not load pickup_sound.wav: " + e.getMessage());
            pickupSound = null;
        }
    }

    // ── Menu music ──────────────────────────────────────────────────────

    /** Starts the menu background music (looping). */
    public void playMenuMusic() {
        try {
            if (menuMusic != null && !menuMusic.isPlaying()) {
                menuMusic.setVolume(MENU_MUSIC_VOLUME);
                menuMusic.play();
            }
        } catch (Exception e) {
            System.err.println("[AudioManager] Error playing menu music: " + e.getMessage());
        }
    }

    /** Stops the menu background music. */
    public void stopMenuMusic() {
        try {
            if (menuMusic != null && menuMusic.isPlaying()) {
                menuMusic.stop();
            }
        } catch (Exception e) {
            System.err.println("[AudioManager] Error stopping menu music: " + e.getMessage());
        }
    }

    // ── Race music ──────────────────────────────────────────────────────

    /** Starts the race background music (looping). */
    public void playRaceMusic() {
        try {
            if (raceMusic != null && !raceMusic.isPlaying()) {
                raceMusic.setVolume(RACE_MUSIC_VOLUME);
                raceMusic.play();
            }
        } catch (Exception e) {
            System.err.println("[AudioManager] Error playing race music: " + e.getMessage());
        }
    }

    /** Stops the race background music. */
    public void stopRaceMusic() {
        try {
            if (raceMusic != null && raceMusic.isPlaying()) {
                raceMusic.stop();
            }
        } catch (Exception e) {
            System.err.println("[AudioManager] Error stopping race music: " + e.getMessage());
        }
    }

    // ── One-shot sound effects ──────────────────────────────────────────

    /** Plays a single countdown beep. */
    public void playCountdownBeep() {
        try {
            if (countdownBeep != null) {
                countdownBeep.play(SFX_VOLUME);
            }
        } catch (Exception e) {
            System.err.println("[AudioManager] Error playing countdown beep: " + e.getMessage());
        }
    }

    /** Plays the victory fanfare once. */
    public void playVictoryFanfare() {
        try {
            if (victoryFanfare != null) {
                victoryFanfare.play(SFX_VOLUME);
            }
        } catch (Exception e) {
            System.err.println("[AudioManager] Error playing victory fanfare: " + e.getMessage());
        }
    }

    /** Plays the fall-off-track sound once. */
    public void playFallSound() {
        try {
            if (fallSound != null) {
                fallSound.play(SFX_VOLUME);
            }
        } catch (Exception e) {
            System.err.println("[AudioManager] Error playing fall sound: " + e.getMessage());
        }
    }

    /** Plays the pickup sound once. */
    public void playPickupSound() {
        try {
            if (pickupSound != null) {
                pickupSound.play(SFX_VOLUME);
            }
        } catch (Exception e) {
            System.err.println("[AudioManager] Error playing pickup sound: " + e.getMessage());
        }
    }

    // ── Engine loop (two independent instances) ─────────────────────────

    /**
     * Starts two independent engine-loop instances – one per car.
     * Each loops at the base {@link #ENGINE_VOLUME}.
     */
    public void startEngineSound() {
        try {
            if (engineLoop != null) {
                engineLoopId1 = engineLoop.loop(ENGINE_VOLUME);
                engineLoopId2 = engineLoop.loop(ENGINE_VOLUME);
            }
        } catch (Exception e) {
            System.err.println("[AudioManager] Error starting engine sound: " + e.getMessage());
        }
    }

    /**
     * Updates pitch and volume for each engine-loop instance based on the
     * current speed of each car.
     * <p>
     * Pitch ranges from <b>0.6</b> (idle) to <b>1.8</b> (full speed).
     * Volume scales slightly with speed so faster cars sound louder.
     *
     * @param speed1   absolute speed of car 1
     * @param speed2   absolute speed of car 2
     * @param maxSpeed the maximum possible speed (used to normalise)
     */
    public void updateEngineSound(float speed1, float speed2, float maxSpeed) {
        try {
            if (engineLoop == null || maxSpeed == 0) return;

            // Car 1
            if (engineLoopId1 != -1) {
                float ratio1 = Math.min(Math.abs(speed1) / maxSpeed, 1.0f);
                float pitch1 = 0.6f + ratio1 * 1.2f;
                float vol1   = ENGINE_VOLUME + ratio1 * 0.1f;
                engineLoop.setPitch(engineLoopId1, pitch1);
                engineLoop.setVolume(engineLoopId1, vol1);
            }

            // Car 2
            if (engineLoopId2 != -1) {
                float ratio2 = Math.min(Math.abs(speed2) / maxSpeed, 1.0f);
                float pitch2 = 0.6f + ratio2 * 1.2f;
                float vol2   = ENGINE_VOLUME + ratio2 * 0.1f;
                engineLoop.setPitch(engineLoopId2, pitch2);
                engineLoop.setVolume(engineLoopId2, vol2);
            }
        } catch (Exception e) {
            System.err.println("[AudioManager] Error updating engine sound: " + e.getMessage());
        }
    }

    /** Stops both engine-loop instances and resets their IDs. */
    public void stopEngineSound() {
        try {
            if (engineLoop != null) {
                engineLoop.stop();
            }
        } catch (Exception e) {
            System.err.println("[AudioManager] Error stopping engine sound: " + e.getMessage());
        } finally {
            engineLoopId1 = -1;
            engineLoopId2 = -1;
        }
    }

    // ── Cleanup ─────────────────────────────────────────────────────────

    /**
     * Disposes every loaded audio resource. Call when the game is shutting
     * down (e.g. inside {@code SpaceRaceGame.dispose()}).
     */
    public void dispose() {
        try { if (menuMusic      != null) menuMusic.dispose();      } catch (Exception e) { /* ignore */ }
        try { if (raceMusic      != null) raceMusic.dispose();      } catch (Exception e) { /* ignore */ }
        try { if (countdownBeep  != null) countdownBeep.dispose();  } catch (Exception e) { /* ignore */ }
        try { if (fallSound      != null) fallSound.dispose();      } catch (Exception e) { /* ignore */ }
        try { if (victoryFanfare != null) victoryFanfare.dispose(); } catch (Exception e) { /* ignore */ }
        try { if (pickupSound    != null) pickupSound.dispose();    } catch (Exception e) { /* ignore */ }
        try { if (engineLoop     != null) engineLoop.dispose();     } catch (Exception e) { /* ignore */ }

        menuMusic      = null;
        raceMusic      = null;
        countdownBeep  = null;
        fallSound      = null;
        victoryFanfare = null;
        pickupSound    = null;
        engineLoop     = null;

        instance = null;
    }
}
