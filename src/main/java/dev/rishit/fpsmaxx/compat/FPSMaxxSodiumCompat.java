package dev.rishit.fpsmaxx.compat;

import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;

/**
 * Only loaded when Sodium is present (guarded by {@link FPSMaxxCompat#SODIUM}).
 */
public final class FPSMaxxSodiumCompat {
	private FPSMaxxSodiumCompat() {
	}

	public static void invalidateTerrainGraph() {
		SodiumWorldRenderer renderer = SodiumWorldRenderer.instanceNullable();
		if (renderer != null) {
			renderer.scheduleTerrainUpdate();
		}
	}
}
