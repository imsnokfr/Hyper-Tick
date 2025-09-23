📄 Product Requirements Document (PRD) — TickLock Mod
1. Goal

Create a Minecraft Fabric client-side mod that makes input handling (especially hotbar swaps, crystal PvP combos, and shield disables) more consistent by implementing a high-frequency input buffer and aligning actions with the server’s 20 TPS tick system.

The mod should preserve the player’s intended input sequence while preventing accidental drops/skips caused by Minecraft’s “last input per tick wins” rule.

2. Key Features

High-Frequency Input Buffer

Capture player inputs (hotbar swaps, item use, attack, interact) at a higher resolution (e.g. 500–1000Hz).

Store inputs in a rolling buffer until the next tick is processed.

Tick Downsampling

At each server tick (20 TPS), flush the buffer into a clean output.

Rules:

Default = first input per tick wins (instead of last).

Optionally allow priority mode (e.g. certain slots/items are favored).

Hotbar Priority Handling

Allow custom slot priority (e.g. Axe > Sword > Crystal).

If multiple swaps land in one tick, resolve them using the priority rules.

Combo Consistency

Ensure common PvP sequences (crystal → punch → totem, axe → sword, anchor → glowstone) execute reliably.

Reduce “fumbled swaps” caused by tick timing randomness.

Config Options

Toggle between First-input mode and Last-input mode.

Define slot priority order in config/ticklock.json.

Adjustable buffer rate (default 1000Hz).

Safety

Client-only mod (no server-side changes).

No automated actions — only preserves real player inputs.


4. Success Criteria

Player can consistently perform shield disables and crystal PvP combos without random drops.

Input timing feels smoother at any FPS (60 → 240+).

Mod runs without server crashes, since all logic is client-side.

Config file allows customization without recompiling.

⚙️ Technical Specifications
1. Environment

Minecraft: 1.21 and later more

Loader: Fabric

Language: Java (Yarn mappings)


2. Input Capture

Hook into:

KeyBinding.onKeyPressed → for hotbar swaps, attack, use, interact.

ClientTickEvents.END_CLIENT_TICK → to poll buffer flush.

Optional: Screen.onKeyPressed → if you want it to work in GUIs.

Store each input as:

class BufferedInput {
    long timestamp;   // exact system time in ms
    int slot;         // hotbar slot (0-8)
    InputType type;   // ATTACK, USE, SWAP, PLACE, etc.
}

3. Buffer System

Buffer rate: 1000Hz (1ms resolution).

Flush: every 50ms (server tick).

Resolution Rule:

Default: first input wins.

Optional: slot priority list.

Pseudocode:

onTick() {
    List<BufferedInput> inputs = buffer.collectSinceLastTick();
    if (inputs.isEmpty()) return;
    
    BufferedInput chosen = pickByMode(inputs); 
    executeInput(chosen);
}

4. Config

File: config/ticklock.json

Example:

{
  "mode": "FIRST", 
  "buffer_rate": 1000,
  "priority_slots": [2, 1, 9, 8] 
}

5. Edge Cases

If no input is present in a tick → do nothing.

If multiple identical inputs → only execute once.

If player is dead or in GUI → ignore buffer.

6. Future Enhancements

GUI to adjust slot priorities in-game.

Debug overlay showing inputs vs outputs.

Per-item priority (instead of per-slot).