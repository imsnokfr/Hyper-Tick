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

	private HyperTickRuntime() {}
}


