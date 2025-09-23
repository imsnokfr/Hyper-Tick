# Hyper Tick

Client-side Fabric mod to buffer high-frequency inputs and resolve them cleanly on the server's 20 TPS tick, reducing fumbled swaps and improving combo consistency.

## Install
- Build: `./gradlew.bat build`
- Output JAR: `build/libs/hyper-tick-<version>.jar`
- Put the JAR into your `.minecraft/mods` with Fabric Loader and Fabric API installed (MC 1.21).

## Use in-game
- Press 1–9 to select hotbar slots; Hyper Tick buffers SWAP and applies the chosen slot on tick.
- Left click (ATTACK) and right click (USE) are aligned to tick via safe key injection.
- Logs will show: `HyperTick chose input type=... slot=... ts=...` when a buffered action resolves.

## Config
- File: `config/hyper-tick.json`
- Example:
  ```json
  {
    "mode": "FIRST",
    "buffer_rate": 1000,
    "priority_slots": [2,1,0]
  }
  ```
- `mode`: `FIRST` (earliest wins) or `LAST` (latest wins).
- `priority_slots`: When multiple SWAPs land in a tick, this order is preferred.
- Hot reload: editing and saving the file will be picked up automatically; a log line confirms reload.

## Notes
- Inputs are ignored while a GUI is open or the player is dead.
- Mod is client-only; no server changes.

## Build and run (dev)
- Build: `./gradlew.bat build`
- Run client: `./gradlew.bat runClient`

## Roadmap
- Optional INTERACT capture
- Combo validation pass and README tips
- In-game settings UI (future)
