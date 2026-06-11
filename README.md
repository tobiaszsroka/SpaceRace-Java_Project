# Space Race 🚀

A fast-paced, 2D local multiplayer (split-screen) racing game built with Java and LibGDX. 
Battle your friends on cosmic tracks, collect power-ups, and enjoy custom procedurally generated audio!

## 🎮 Key Features

- **Split-Screen Multiplayer:** Race head-to-head against a friend on the same keyboard.
- **Advanced Physics:** Custom OBB (Oriented Bounding Box) collision detection using the Separating Axis Theorem. Cars bounce off each other with momentum and spin!
- **Procedural Audio:** No pre-recorded MP3s! The game synthesizes its own engine sounds (with dynamic pitch-shifting), countdown beeps, and victory fanfares mathematically at startup.
- **Power-up System:** Collect randomly spawning items to gain an edge:
  - 🔵 **Nitro Boost:** Massive temporary speed and acceleration increase.
  - 🟡 **Shield:** Become invulnerable to collisions and bounce opponents away with double force.
- **Anti-Cheat Lap System:** A robust checkpoint manager ensures players cannot cut corners or cheat to gain laps.
- **Tiled Map Integration:** Easy to design and load new tracks using `.tmx` maps.

## 🛠️ Technologies Used

- **Language:** Java 17+
- **Framework:** LibGDX (LWJGL3 backend)
- **Math & Physics:** Custom 2D vector math and polygon intersections

## 🚀 How to Run

Make sure you have JDK 17+ installed. Clone the repository and run the Gradle wrapper from the terminal:

```bash
# On Windows
gradlew.bat lwjgl3:run

# On Linux/Mac
./gradlew lwjgl3:run
```

## ⌨️ Controls

- **Player 1:** `W A S D`
- **Player 2:** `Arrow Keys`
- **Pause:** `ESC`
