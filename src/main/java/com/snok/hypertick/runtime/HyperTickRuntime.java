package com.snok.hypertick.runtime;

import com.snok.hypertick.config.ConfigManager;
import com.snok.hypertick.input.InputBuffer;

/**
 * Central place to keep runtime state: config, buffer, and timing.
 * Keeping this simple makes it easy to reference from client code.
 */
public final class HyperTickRuntime {
	public static final InputBuffer INPUT_BUFFER = new InputBuffer();
	public static ConfigManager.Config CONFIG = ConfigManager.loadOrCreateDefault();
	public static long lastTickEpochMs = System.currentTimeMillis();
	public static boolean debugChatEnabled = false;

	// Simple rate gating per input family to honor CONFIG.buffer_rate
	public static long lastAttackBufferedMs = 0L;
	public static long lastUseBufferedMs = 0L;
	public static long lastInteractBufferedMs = 0L;
	public static long lastBufferedMs = 0L; // For movement/camera inputs
	public static final long[] lastSwapBufferedMs = new long[9];

	private HyperTickRuntime() {}
}


