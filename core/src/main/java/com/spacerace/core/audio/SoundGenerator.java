package com.spacerace.core.audio;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

/**
 * Procedural sound generator for SpaceRace.
 * Generates WAV audio files programmatically — no external assets needed.
 * All files are 16-bit signed PCM mono at 44100 Hz.
 *
 * Call {@link #generateAll()} early in startup (before LibGDX asset loading)
 * to ensure every required sound file exists in the {@code audio/} directory.
 */
public class SoundGenerator {

    private static final int SAMPLE_RATE = 44100;
    private static final double TWO_PI = 2.0 * Math.PI;
    private static final Random RNG = new Random(42); // deterministic seed

    /* ------------------------------------------------------------------ */
    /*  PUBLIC API                                                        */
    /* ------------------------------------------------------------------ */

    /**
     * Checks for each expected audio file and generates any that are missing.
     * Safe to call multiple times — existing files are never overwritten.
     */
    public static void generateAll() {
        File dir = new File("audio");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        generateIfMissing(dir, "menu_music.wav",      SoundGenerator::generateMenuMusic);
        generateIfMissing(dir, "race_music.wav",       SoundGenerator::generateRaceMusic);
        generateIfMissing(dir, "engine_loop.wav",      SoundGenerator::generateEngineLoop);
        generateIfMissing(dir, "countdown_beep.wav",   SoundGenerator::generateCountdownBeep);
        generateIfMissing(dir, "fall_sound.wav",       SoundGenerator::generateFallSound);
        generateIfMissing(dir, "victory_fanfare.wav",  SoundGenerator::generateVictoryFanfare);
        generateIfMissing(dir, "pickup_sound.wav",     SoundGenerator::generatePickupSound);

        System.out.println("[SoundGenerator] All audio files ready.");
    }

    /* ------------------------------------------------------------------ */
    /*  GENERATOR METHODS                                                 */
    /* ------------------------------------------------------------------ */

    /**
     * Menu music — ~15 s lively, upbeat arcade theme.
     * Bouncy bass, bright arpeggios, and a catchy melody loop
     * at ~132 BPM for an energetic menu feel.
     */
    private static short[] generateMenuMusic() {
        int totalSamples = SAMPLE_RATE * 15;
        double[] buf = new double[totalSamples];

        double bpm = 132.0;
        double beatLen = 60.0 / bpm;

        // Bouncy bass pattern (C major / A minor feel)
        // C3=131, E3=165, G3=196, A2=110, F3=175
        double[] bassPattern = {
            131, 131, 196, 196,  165, 165, 110, 110,
            175, 175, 131, 131,  196, 196, 165, 165,
        };

        // Bright arpeggio pattern (higher octave)
        double[] arpPattern = {
            523, 659, 784, 659,  523, 784, 1047, 784,
            587, 698, 880, 698,  587, 880, 1047, 880,
        };

        // Simple catchy melody (plays on top)
        // each entry: {startBeat, durationBeats, freqHz}
        double[][] melody = {
            {0, 1, 1047},  {1, 0.5, 988},  {1.5, 0.5, 880}, {2, 2, 784},
            {4, 1, 880},   {5, 1, 784},    {6, 1, 659},      {7, 1, 784},
            {8, 1, 1047},  {9, 0.5, 988},  {9.5, 0.5, 880},  {10, 2, 659},
            {12, 1, 784},  {13, 1, 659},   {14, 1, 523},     {15, 1, 659},
            {16, 1, 1047}, {17, 0.5, 988}, {17.5, 0.5, 880}, {18, 2, 784},
            {20, 1, 880},  {21, 1, 784},   {22, 1, 659},     {23, 1, 784},
            {24, 1, 1047}, {25, 0.5, 1175},{25.5, 0.5, 1047},{26, 2, 880},
            {28, 1, 784},  {29, 1, 880},   {30, 2, 1047},
        };

        for (int i = 0; i < totalSamples; i++) {
            double t = (double) i / SAMPLE_RATE;
            double beatPos = t / beatLen;
            int beat = (int) beatPos % bassPattern.length;
            double beatFrac = beatPos - (int) beatPos;

            double sample = 0.0;

            // ===== BASS (bouncy with quick envelope) =====
            double bassFreq = bassPattern[beat];
            double bassEnv = Math.max(0, 1.0 - beatFrac * 2.0);
            bassEnv = Math.sqrt(bassEnv); // rounder decay
            double bass = 0;
            for (int h = 1; h <= 4; h++) {
                bass += (1.0 / h) * Math.sin(TWO_PI * bassFreq * h * t);
            }
            sample += 0.25 * bass * bassEnv;

            // ===== ARPEGGIO (16th notes, bright and sparkly) =====
            double sixteenthPos = beatPos * 4.0;
            int sixteenthIdx = (int) sixteenthPos % arpPattern.length;
            double sixteenthFrac = sixteenthPos - (int) sixteenthPos;
            double arpFreq = arpPattern[sixteenthIdx];
            double arpEnv = Math.max(0, 1.0 - sixteenthFrac * 3.0);
            arpEnv *= arpEnv;
            double arp = 0.6 * Math.sin(TWO_PI * arpFreq * t)
                       + 0.3 * Math.sin(TWO_PI * arpFreq * 2 * t)
                       + 0.1 * Math.sin(TWO_PI * arpFreq * 3 * t);
            sample += 0.12 * arp * arpEnv;

            // ===== MELODY (sine with harmonics) =====
            for (double[] note : melody) {
                double noteStart = note[0] * beatLen;
                double noteDur = note[1] * beatLen;
                double noteFreq = note[2];
                if (t < noteStart || t > noteStart + noteDur + 0.1) continue;

                double noteT = t - noteStart;
                double melEnv;
                if (noteT < 0.02) melEnv = noteT / 0.02;
                else if (noteT > noteDur) melEnv = Math.max(0, 1.0 - (noteT - noteDur) / 0.1);
                else melEnv = 0.8 + 0.2 * Math.exp(-noteT * 3);

                double mel = 0.5 * Math.sin(TWO_PI * noteFreq * t)
                           + 0.25 * Math.sin(TWO_PI * noteFreq * 2 * t)
                           + 0.1 * Math.sin(TWO_PI * noteFreq * 0.5 * t);
                sample += 0.18 * mel * melEnv;
            }

            // ===== LIGHT KICK on beat =====
            if (beatFrac < 0.1) {
                double kickT = beatFrac * beatLen;
                double kickEnv = Math.exp(-kickT * 25.0);
                sample += 0.2 * kickEnv * Math.sin(TWO_PI * 100 * kickT);
            }

            // Loop crossfade
            double fadeIn  = Math.min(1.0, t / 0.1);
            double fadeOut = Math.min(1.0, (15.0 - t) / 0.1);
            sample *= fadeIn * fadeOut;

            buf[i] = sample;
        }

        addSimpleReverb(buf, SAMPLE_RATE, new double[]{0.05, 0.09, 0.15},
                                          new double[]{0.18, 0.12, 0.07});

        return normalize(buf, 0.88);
    }

    /**
     * Race music — ~12 s energetic loop at ~140 BPM.
     * Driving bass pulse on the beat, arpeggiated synth melody on top,
     * hi-hat-like noise on off-beats.
     */
    private static short[] generateRaceMusic() {
        int totalSamples = SAMPLE_RATE * 12;
        double[] buf = new double[totalSamples];

        double bpm = 140.0;
        double beatLen = 60.0 / bpm; // ~0.4286 s
        int totalBeats = (int) (12.0 / beatLen); // ~28 beats

        // Bass notes (one per beat, simple 4-bar repeating pattern in A minor)
        // A2=110, C3=131, D3=147, E3=165
        double[] bassPattern = {
            110, 110, 110, 110,   131, 131, 147, 147,
            110, 110, 110, 110,   165, 165, 147, 147,
            110, 110, 110, 110,   131, 131, 147, 147,
            110, 110, 165, 165,   147, 147, 131, 131
        };

        // Arpeggio notes (16th note pattern)
        double[] arpPattern = {
            440, 523.25, 659.25, 523.25,  440, 523.25, 784.0, 659.25,
            440, 523.25, 659.25, 523.25,  440, 659.25, 784.0, 880.0,
        };

        for (int i = 0; i < totalSamples; i++) {
            double t = (double) i / SAMPLE_RATE;
            double beatPos = t / beatLen;
            int beat = (int) beatPos % bassPattern.length;
            double beatFrac = beatPos - (int) beatPos;

            double sample = 0.0;

            // ===== BASS (sawtooth-ish with envelope) =====
            double bassFreq = bassPattern[beat];
            // Sawtooth approximation (first 6 harmonics)
            double bassEnv = Math.max(0, 1.0 - beatFrac * 2.5); // quick decay
            bassEnv = bassEnv * bassEnv; // exponential feel
            double bass = 0;
            for (int h = 1; h <= 6; h++) {
                double amp = 1.0 / h;
                bass += amp * Math.sin(TWO_PI * bassFreq * h * t);
            }
            bass *= 0.35 * bassEnv;
            // Low-pass-ish effect: reduce high harmonics further with envelope
            sample += bass;

            // ===== KICK DRUM (sine burst on every beat) =====
            if (beatFrac < 0.15) {
                double kickT = beatFrac * beatLen;
                double kickFreq = 150.0 * Math.exp(-kickT * 30.0) + 40.0;
                double kickEnv = Math.exp(-kickT * 20.0);
                sample += 0.5 * kickEnv * Math.sin(TWO_PI * kickFreq * kickT);
            }

            // ===== HI-HAT (noise burst on off-beats) =====
            double subBeatFrac = (beatFrac * 2.0) % 1.0;
            boolean offBeat = beatFrac >= 0.5;
            if (offBeat && subBeatFrac < 0.12) {
                double hatEnv = Math.exp(-subBeatFrac * beatLen * 40.0);
                sample += 0.12 * hatEnv * (RNG.nextDouble() * 2 - 1);
            }

            // ===== ARPEGGIO SYNTH (16th note pulses) =====
            double sixteenthPos = beatPos * 4.0; // 16th note position
            int sixteenthIdx = (int) sixteenthPos % arpPattern.length;
            double sixteenthFrac = sixteenthPos - (int) sixteenthPos;
            double arpFreq = arpPattern[sixteenthIdx];
            double arpEnv = Math.max(0, 1.0 - sixteenthFrac * 4.0);
            arpEnv *= arpEnv;
            // Square-ish wave (odd harmonics)
            double arp = 0;
            for (int h = 1; h <= 5; h += 2) {
                arp += (1.0 / h) * Math.sin(TWO_PI * arpFreq * h * t);
            }
            sample += 0.15 * arpEnv * arp;

            // ===== LEAD PAD (sustained for atmosphere) =====
            double padFreq = 220.0; // A3 drone
            double pad = 0.06 * Math.sin(TWO_PI * padFreq * t)
                       + 0.04 * Math.sin(TWO_PI * padFreq * 1.5 * t)  // fifth
                       + 0.03 * Math.sin(TWO_PI * padFreq * 2.0 * t); // octave
            double padLfo = 0.7 + 0.3 * Math.sin(TWO_PI * 0.5 * t);
            sample += pad * padLfo;

            // Loop crossfade (smooth the boundary)
            double fadeIn  = Math.min(1.0, t / 0.05);
            double fadeOut = Math.min(1.0, (12.0 - t) / 0.05);
            sample *= fadeIn * fadeOut;

            buf[i] = sample;
        }

        // Light reverb for fullness
        addSimpleReverb(buf, SAMPLE_RATE, new double[]{0.037, 0.067, 0.113},
                                          new double[]{0.15,  0.10,  0.06});

        return normalize(buf, 0.90);
    }

    /**
     * Engine loop — ~0.5 s seamless loop.
     * Low-frequency rumble with harmonics and subtle noise.
     */
    private static short[] generateEngineLoop() {
        double duration = 0.5;
        int totalSamples = (int) (SAMPLE_RATE * duration);
        double[] buf = new double[totalSamples];

        double fundamental = 80.0;
        // We pick a duration that is an exact multiple of the fundamental period
        // so the waveform loops seamlessly.
        double period = 1.0 / fundamental;
        int samplesPerCycle = (int) Math.round(period * SAMPLE_RATE);
        // Adjust total samples to exact multiple of cycle
        int adjustedSamples = (totalSamples / samplesPerCycle) * samplesPerCycle;
        if (adjustedSamples == 0) adjustedSamples = samplesPerCycle;
        buf = new double[adjustedSamples];
        totalSamples = adjustedSamples;

        for (int i = 0; i < totalSamples; i++) {
            double t = (double) i / SAMPLE_RATE;
            double sample = 0.0;

            // Harmonics: fundamental + 2nd, 3rd, 4th with decreasing amplitude
            sample += 0.40 * Math.sin(TWO_PI * fundamental * t);
            sample += 0.25 * Math.sin(TWO_PI * fundamental * 2 * t);
            sample += 0.15 * Math.sin(TWO_PI * fundamental * 3 * t);
            sample += 0.10 * Math.sin(TWO_PI * fundamental * 4 * t);
            sample += 0.05 * Math.sin(TWO_PI * fundamental * 5 * t);

            // Subtle amplitude modulation (engine "chug")
            double chugLfo = 0.85 + 0.15 * Math.sin(TWO_PI * 12.0 * t);
            sample *= chugLfo;

            // Slight noise layer (crackle)
            double noise = (RNG.nextDouble() * 2 - 1) * 0.04;
            sample += noise;

            buf[i] = sample;
        }

        return normalize(buf, 0.80);
    }

    /**
     * Countdown beep — ~0.3 s clean 880 Hz tone with smooth envelope.
     */
    private static short[] generateCountdownBeep() {
        double duration = 0.3;
        int totalSamples = (int) (SAMPLE_RATE * duration);
        double[] buf = new double[totalSamples];

        double freq = 880.0;
        double attackTime  = 0.020; // 20 ms
        double releaseTime = 0.050; // 50 ms

        for (int i = 0; i < totalSamples; i++) {
            double t = (double) i / SAMPLE_RATE;

            // Envelope
            double env;
            if (t < attackTime) {
                env = t / attackTime;
                env = env * env; // smooth curve
            } else if (t > duration - releaseTime) {
                double rel = (duration - t) / releaseTime;
                env = rel * rel;
            } else {
                env = 1.0;
            }

            // Main tone + subtle second harmonic for warmth
            double sample = 0.80 * Math.sin(TWO_PI * freq * t)
                          + 0.12 * Math.sin(TWO_PI * freq * 2 * t)
                          + 0.05 * Math.sin(TWO_PI * freq * 3 * t);

            buf[i] = sample * env;
        }

        return normalize(buf, 0.85);
    }

    /**
     * Fall sound — ~1.2 s descending pitch sweep with fade-out.
     * Exponential pitch drop from 600 Hz to 50 Hz, layered with
     * noise and a resonant overtone for a dramatic "falling into void" feel.
     */
    private static short[] generateFallSound() {
        double duration = 1.2;
        int totalSamples = (int) (SAMPLE_RATE * duration);
        double[] buf = new double[totalSamples];

        double startFreq = 600.0;
        double endFreq   = 50.0;

        double phase  = 0.0;
        double phase2 = 0.0;
        double phase3 = 0.0;

        for (int i = 0; i < totalSamples; i++) {
            double t = (double) i / SAMPLE_RATE;
            double progress = t / duration;

            // Exponential frequency sweep
            double freq = startFreq * Math.pow(endFreq / startFreq, progress);

            // Phase accumulation for smooth sweep (avoids clicks)
            double dt = 1.0 / SAMPLE_RATE;
            phase  += TWO_PI * freq * dt;
            phase2 += TWO_PI * freq * 1.5 * dt; // fifth
            phase3 += TWO_PI * freq * 2.0 * dt; // octave

            // Volume fade-out (exponential)
            double env = Math.pow(1.0 - progress, 2.0);

            // Main tone
            double sample = 0.55 * Math.sin(phase);
            // Resonant overtone
            sample += 0.20 * Math.sin(phase2) * (1.0 - progress);
            // Octave shimmer
            sample += 0.10 * Math.sin(phase3) * Math.max(0, 1.0 - progress * 2);

            // Wind noise (increasing as we "fall")
            double noise = (RNG.nextDouble() * 2 - 1) * 0.08 * progress;
            sample += noise;

            // Subtle tremolo
            double tremolo = 0.85 + 0.15 * Math.sin(TWO_PI * 8.0 * t);
            sample *= tremolo;

            buf[i] = sample * env;
        }

        // Subtle reverb tail
        addSimpleReverb(buf, SAMPLE_RATE, new double[]{0.05, 0.11, 0.17},
                                          new double[]{0.20, 0.12, 0.07});

        return normalize(buf, 0.85);
    }

    /**
     * Victory fanfare — ~3 s exciting, triumphant celebration.
     * Rapid ascending flourish into a big power chord,
     * with layered harmonics, trills, and a dramatic crescendo.
     */
    private static short[] generateVictoryFanfare() {
        double duration = 3.0;
        int totalSamples = (int) (SAMPLE_RATE * duration);
        double[] buf = new double[totalSamples];

        // Phase 1 (0.0-0.8s): Rapid ascending flourish — quick notes building excitement
        double[][] flourish = {
            {0.00, 0.10, 523.25},  // C5
            {0.10, 0.10, 587.33},  // D5
            {0.20, 0.10, 659.25},  // E5
            {0.30, 0.10, 698.46},  // F5
            {0.40, 0.10, 783.99},  // G5
            {0.50, 0.10, 880.00},  // A5
            {0.60, 0.10, 987.77},  // B5
            {0.70, 0.12, 1046.50}, // C6 (slightly longer to bridge)
        };

        // Phase 2 (0.85-3.0s): Big triumphant power chord held
        // C major chord across multiple octaves for maximum impact
        double chordStart = 0.85;
        double[] chordFreqs = {
            261.63,  // C4
            329.63,  // E4
            392.00,  // G4
            523.25,  // C5
            659.25,  // E5
            783.99,  // G5
            1046.50, // C6
        };

        for (int i = 0; i < totalSamples; i++) {
            double t = (double) i / SAMPLE_RATE;
            double sample = 0.0;

            // ===== FLOURISH NOTES =====
            for (double[] note : flourish) {
                double ns = note[0], nd = note[1], nf = note[2];
                if (t < ns || t > ns + nd + 0.05) continue;

                double nt = t - ns;
                double env;
                if (nt < 0.008) env = nt / 0.008;
                else if (nt > nd) {
                    env = Math.max(0, 1.0 - (nt - nd) / 0.05);
                    env *= env;
                } else {
                    env = 1.0;
                }

                // Bright brass-like tone (fundamental + harmonics)
                double tone = 0.35 * Math.sin(TWO_PI * nf * t)
                            + 0.25 * Math.sin(TWO_PI * nf * 2 * t)
                            + 0.15 * Math.sin(TWO_PI * nf * 3 * t)
                            + 0.08 * Math.sin(TWO_PI * nf * 4 * t);
                // Octave below for weight
                tone += 0.15 * Math.sin(TWO_PI * nf * 0.5 * t);

                sample += tone * env;
            }

            // ===== BIG POWER CHORD =====
            if (t >= chordStart) {
                double ct = t - chordStart;
                double chordDur = duration - chordStart;

                // Attack envelope: quick swell
                double chordEnv;
                if (ct < 0.05) chordEnv = ct / 0.05;
                else {
                    // Slow natural decay
                    chordEnv = 1.0 - 0.3 * (ct / chordDur);
                }
                // End fade
                double endFade = Math.min(1.0, (duration - t) / 0.4);
                chordEnv *= endFade;

                double chord = 0;
                for (int c = 0; c < chordFreqs.length; c++) {
                    double cf = chordFreqs[c];
                    double vol = (c < 4) ? 0.12 : 0.08; // lower notes louder
                    // Slight vibrato for life
                    double vib = 1.0 + 0.003 * Math.sin(TWO_PI * 5.5 * ct + c);
                    chord += vol * Math.sin(TWO_PI * cf * vib * t);
                    // Add harmonics for richness
                    chord += vol * 0.4 * Math.sin(TWO_PI * cf * 2 * vib * t);
                }

                // Celebratory trill on top (rapid alternation between G5 and A5)
                if (ct > 0.3 && ct < 1.5) {
                    double trillFreq = ((int)(ct * 12) % 2 == 0) ? 783.99 : 880.0;
                    double trillEnv = 0.15 * Math.max(0, 1.0 - (ct - 0.3) / 1.2);
                    chord += trillEnv * Math.sin(TWO_PI * trillFreq * t);
                }

                sample += chord * chordEnv;
            }

            // ===== CYMBAL CRASH at chord entrance =====
            if (t >= chordStart && t < chordStart + 0.8) {
                double ct = t - chordStart;
                double crashEnv = Math.exp(-ct * 5.0);
                sample += 0.12 * crashEnv * (RNG.nextDouble() * 2 - 1);
            }

            buf[i] = sample;
        }

        // Rich reverb for a grand feel
        addSimpleReverb(buf, SAMPLE_RATE, new double[]{0.035, 0.071, 0.113, 0.179, 0.251},
                                          new double[]{0.28,  0.22,  0.17,  0.12,  0.08});

        return normalize(buf, 0.90);
    }

    /**
     * Pickup sound — ~0.25 s high-pitched "bli-ping!"
     */
    private static short[] generatePickupSound() {
        int totalSamples = (int) (0.25 * SAMPLE_RATE);
        double[] buf = new double[totalSamples];
        
        for (int i = 0; i < totalSamples; i++) {
            double t = (double) i / SAMPLE_RATE;
            // Sweep frequency up very fast
            double freq = 800.0 + 2000.0 * (t / 0.25);
            double envelope = (1.0 - (t / 0.25)); // quick fade out
            buf[i] = Math.sin(TWO_PI * freq * t) * envelope * 0.4;
        }
        
        return normalize(buf, 0.85);
    }

    /* ------------------------------------------------------------------ */
    /*  DSP UTILITIES                                                     */
    /* ------------------------------------------------------------------ */

    /**
     * Adds a simple multi-tap delay (comb-filter reverb approximation) in-place.
     *
     * @param buf        audio buffer (modified in-place)
     * @param sampleRate sample rate
     * @param delays     delay times in seconds
     * @param gains      gain for each delay tap (0–1)
     */
    private static void addSimpleReverb(double[] buf, int sampleRate,
                                        double[] delays, double[] gains) {
        double[] original = buf.clone();
        for (int tap = 0; tap < delays.length; tap++) {
            int delaySamples = (int) (delays[tap] * sampleRate);
            double gain = gains[tap];
            for (int i = delaySamples; i < buf.length; i++) {
                buf[i] += original[i - delaySamples] * gain;
            }
        }
    }

    /**
     * Normalizes a double buffer to the target peak level and converts to 16-bit shorts.
     */
    private static short[] normalize(double[] buf, double targetPeak) {
        double max = 0;
        for (double v : buf) {
            double abs = Math.abs(v);
            if (abs > max) max = abs;
        }

        double scale = (max > 0) ? (targetPeak * 32767.0 / max) : 1.0;
        short[] out = new short[buf.length];
        for (int i = 0; i < buf.length; i++) {
            long val = Math.round(buf[i] * scale);
            if (val > 32767)  val = 32767;
            if (val < -32767) val = -32767;
            out[i] = (short) val;
        }
        return out;
    }

    /* ------------------------------------------------------------------ */
    /*  WAV FILE I/O                                                      */
    /* ------------------------------------------------------------------ */

    /**
     * Creates a complete WAV file byte array from 16-bit PCM samples.
     */
    private static byte[] createWavBytes(short[] samples, int sampleRate) {
        int dataSize = samples.length * 2;
        byte[] wav = new byte[44 + dataSize];
        // RIFF header
        wav[0] = 'R'; wav[1] = 'I'; wav[2] = 'F'; wav[3] = 'F';
        writeInt(wav, 4, 36 + dataSize);
        wav[8] = 'W'; wav[9] = 'A'; wav[10] = 'V'; wav[11] = 'E';
        // fmt chunk
        wav[12] = 'f'; wav[13] = 'm'; wav[14] = 't'; wav[15] = ' ';
        writeInt(wav, 16, 16);            // chunk size
        writeShort(wav, 20, (short) 1);   // PCM format
        writeShort(wav, 22, (short) 1);   // mono
        writeInt(wav, 24, sampleRate);     // sample rate
        writeInt(wav, 28, sampleRate * 2); // byte rate
        writeShort(wav, 32, (short) 2);   // block align
        writeShort(wav, 34, (short) 16);  // bits per sample
        // data chunk
        wav[36] = 'd'; wav[37] = 'a'; wav[38] = 't'; wav[39] = 'a';
        writeInt(wav, 40, dataSize);
        for (int i = 0; i < samples.length; i++) {
            writeShort(wav, 44 + i * 2, samples[i]);
        }
        return wav;
    }

    /** Writes a 32-bit little-endian integer into a byte array. */
    private static void writeInt(byte[] buf, int offset, int value) {
        buf[offset]     = (byte) (value & 0xFF);
        buf[offset + 1] = (byte) ((value >> 8) & 0xFF);
        buf[offset + 2] = (byte) ((value >> 16) & 0xFF);
        buf[offset + 3] = (byte) ((value >> 24) & 0xFF);
    }

    /** Writes a 16-bit little-endian short into a byte array. */
    private static void writeShort(byte[] buf, int offset, short value) {
        buf[offset]     = (byte) (value & 0xFF);
        buf[offset + 1] = (byte) ((value >> 8) & 0xFF);
    }

    /* ------------------------------------------------------------------ */
    /*  FILE HELPERS                                                      */
    /* ------------------------------------------------------------------ */

    @FunctionalInterface
    private interface SampleGenerator {
        short[] generate();
    }

    /**
     * Generates and writes a WAV file only if it does not already exist.
     */
    private static void generateIfMissing(File dir, String filename, SampleGenerator gen) {
        File file = new File(dir, filename);
        if (file.exists()) {
            System.out.println("[SoundGenerator] " + filename + " already exists, skipping.");
            return;
        }

        System.out.println("[SoundGenerator] Generating " + filename + "...");
        try {
            short[] samples = gen.generate();
            byte[] wav = createWavBytes(samples, SAMPLE_RATE);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(wav);
            }
            double durationSec = (double) samples.length / SAMPLE_RATE;
            System.out.printf("[SoundGenerator] %s written (%.2f s, %d bytes)%n",
                              filename, durationSec, wav.length);
        } catch (IOException e) {
            System.err.println("[SoundGenerator] Failed to write " + filename + ": " + e.getMessage());
        }
    }
}
