package dev.rishit.fpsmaxx;

/**
 * Values that both the client and the integrated server mixins need.
 * Kept free of client-only classes so the server-side mixins never touch
 * a class that does not exist on a dedicated server.
 */
public final class FPSMaxxConstants {
	public static final String MOD_ID = "fpsmaxx";

	/** Hard ceiling for the render-distance slider. Vanilla is 32. */
	public static final int MAX_RENDER_DISTANCE = 64;

	/** Vanilla's simulation-distance maximum. */
	public static final int MAX_SIMULATION_DISTANCE = 32;

	/** Vanilla's normal minimum; debug mode allows 2. */
	public static final int MIN_SIMULATION_DISTANCE = 5;

	/** Default FPS the adaptive governor tries to hold. */
	public static final int DEFAULT_TARGET_FPS = 65;

	/** Extra ticket-tracker range so chunks at exactly 64 are tracked too. */
	public static final int TICKET_RANGE = MAX_RENDER_DISTANCE + 4;

	/**
	 * Loading 64 chunks is memory-hungry: the client stores ~18,000 chunks and
	 * the integrated server keeps another copy. Cap the loaded distance based on
	 * the JVM heap so low-RAM phones get a playable world instead of an OOM.
	 *
	 * Players who know what they are doing can pass
	 * -Dfpsmaxx.ignoreMemoryLimit=true.
	 */
	public static int maxSafeRenderDistance() {
		if (Boolean.getBoolean("fpsmaxx.ignoreMemoryLimit")) {
			return MAX_RENDER_DISTANCE;
		}

		long heapMb = Runtime.getRuntime().maxMemory() / (1024L * 1024L);
		if (heapMb >= 4096L) {
			return 64;
		}
		if (heapMb >= 3072L) {
			return 48;
		}
		if (heapMb >= 2048L) {
			return 32;
		}
		if (heapMb >= 1536L) {
			return 24;
		}
		return 16;
	}

	/**
	 * Simulation distance ticks entities/chunks on the server side. It is not a
	 * rendering distance, so it is capped separately. Keep it at vanilla's 32
	 * when the heap can support it, and shrink it with the memory guard.
	 */
	public static int maxSafeSimulationDistance() {
		return Math.min(MAX_SIMULATION_DISTANCE, Math.max(MIN_SIMULATION_DISTANCE, maxSafeRenderDistance()));
	}

	private FPSMaxxConstants() {
	}
}
