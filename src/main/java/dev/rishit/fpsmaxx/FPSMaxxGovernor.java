package dev.rishit.fpsmaxx;

import dev.rishit.fpsmaxx.compat.FPSMaxxCompat;
import dev.rishit.fpsmaxx.compat.FPSMaxxSodiumCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Util;

/**
 * Measures FPS once per second and shrinks/grows the visible chunk radius to
 * keep the frame rate at the configured target.
 *
 * In the default visible mode, chunks stay loaded at the distance chosen in the
 * video settings and only the rendered radius changes, so the governor never
 * causes chunk loading hits when it moves up or down. The optional render
 * fallback for unsupported renderers changes the real render distance instead.
 */
public final class FPSMaxxGovernor {
	private static final long EVAL_INTERVAL_MS = 1000L;
	private static final long RAMP_COOLDOWN_MS = 7000L;

	private static int visibleDistance = -1;
	private static int maxRenderDistance = -1;
	private static long lastEval;
	private static long rampCooldownUntil;
	private static long settleUntil;
	private static int goodStreak;
	private static ClientLevel lastLevel;
	private static String lastEffectiveMode;
	private static float particleScale = 1.0F;

	private FPSMaxxGovernor() {
	}

	public static void tick(Minecraft client) {
		ClientLevel level = client.level;
		if (level == null || client.player == null) {
			visibleDistance = -1;
			lastLevel = null;
			return;
		}

		FPSMaxxConfig config = FPSMaxxConfig.get();
		lastEffectiveMode = effectiveMode();

		long now = Util.getMillis();

		if (level != lastLevel) {
			lastLevel = level;
			applySimulationDistance(client, config);

			if (renderDistanceMode()) {
				maxRenderDistance = Math.max(
					2,
					Math.min(client.options.renderDistance().get(), FPSMaxxConstants.maxSafeRenderDistance())
				);
			} else {
				maxRenderDistance = -1;
			}

			visibleDistance = config.adaptiveEnabled
				? Math.min(renderDistanceMode() ? maxRenderDistance : currentMaxVisible(client), config.initialVisibleDistance)
				: -1;
			if (renderDistanceMode() && visibleDistance > 0) {
				applyRenderDistance(client, visibleDistance);
			}
			lastEval = now;
			rampCooldownUntil = 0L;
			settleUntil = now + 4000L;
			goodStreak = 0;
			particleScale = 1.0F;
			invalidateSectionGraph();
			return;
		}

		if (!config.adaptiveEnabled) {
			visibleDistance = -1;
			particleScale = 1.0F;
			return;
		}

		// Opening a menu or alt-tabbing makes the vanilla FPS counter collapse.
		// Never let that shrink the world.
		if (client.isPaused()) {
			lastEval = now;
			settleUntil = now + 1000L;
			return;
		}

		if (now - lastEval < EVAL_INTERVAL_MS || now < settleUntil) {
			lastEval = now;
			return;
		}
		lastEval = now;

		int max = renderDistanceMode() && maxRenderDistance > 0
			? maxRenderDistance
			: currentMaxVisible(client);
		if (visibleDistance < 0 || visibleDistance > max) {
			visibleDistance = max;
		}

		int min = Math.min(config.minVisibleDistance, max);
		int fps = client.getFps();
		if (fps <= 0) {
			return;
		}

		int target = effectiveTargetFps(client, config);
		int next = visibleDistance;

		// Particle work is a smaller lever than terrain, but it reacts faster
		// and helps when the CPU is saturated by weather or large farms.
		if (config.adaptiveParticles) {
			if (fps < target - 10) {
				particleScale = Math.max(0.5F, particleScale - 0.1F);
			} else if (fps > target + 10) {
				particleScale = Math.min(1.0F, particleScale + 0.05F);
			}
		} else {
			particleScale = 1.0F;
		}

		if (fps < target - 5) {
			// Drop fast when below target. Compiling newly visible chunk meshes
			// causes a short FPS dip, so wait before trusting FPS again.
			int drop = Math.max(3, visibleDistance / 10);
			if (fps < target / 2) {
				drop = Math.max(drop, visibleDistance / 3);
			}
			next = Math.max(min, visibleDistance - drop);
			rampCooldownUntil = now + RAMP_COOLDOWN_MS;
			settleUntil = now + 3000L;
			goodStreak = 0;
		} else if (now >= rampCooldownUntil && visibleDistance < max) {
			if (fps > target + 8) {
				goodStreak++;
			} else {
				goodStreak = 0;
			}

			// Require two consecutive good measurements. This stops the
			// distance from flapping around a single lucky frame-rate sample.
			if (goodStreak >= 2) {
				int step = fps > target + 25 ? 3 : (fps > target + 15 ? 2 : 1);
				next = Math.min(max, visibleDistance + step);
				settleUntil = now + 2500L;
				goodStreak = 0;
			}
		}

		if (next != visibleDistance) {
			FPSMaxxClient.LOGGER.debug(
				"{} distance {} -> {} chunks (FPS {}, target {})",
				renderDistanceMode() ? "Render" : "Visible",
				visibleDistance,
				next,
				fps,
				target
			);
			if (renderDistanceMode()) {
				applyRenderDistance(client, next);
			}
			visibleDistance = next;
			invalidateSectionGraph();
		}
	}

	/**
	 * Pins simulation distance to the configured value (32 by default) on world
	 * join. Simulation distance is not a rendering distance, so it is not part
	 * of the visible-radius governor: changing it changes mob AI and ticking.
	 *
	 * @return the distance that was applied (or the current distance when
	 *         forcing is disabled)
	 */
	public static int applySimulationDistance(Minecraft client, FPSMaxxConfig config) {
		if (!config.forceSimulationDistance) {
			return client.options.simulationDistance().get();
		}

		int target = Math.min(config.simulationDistance, FPSMaxxConstants.maxSafeSimulationDistance());
		target = Math.max(FPSMaxxConstants.MIN_SIMULATION_DISTANCE, target);
		if (client.options.simulationDistance().get() != target) {
			client.options.simulationDistance().set(target);
			client.options.save();
			FPSMaxxClient.LOGGER.info("Simulation distance set to {} chunks", target);
		}
		return target;
	}

	private static int currentMaxVisible(Minecraft client) {
		int max = client.options.getEffectiveRenderDistance();
		max = Math.min(max, FPSMaxxConstants.MAX_RENDER_DISTANCE);
		return Math.max(2, max);
	}

	private static void applyRenderDistance(Minecraft client, int distance) {
		int clamped = Math.max(2, Math.min(distance, FPSMaxxConstants.MAX_RENDER_DISTANCE));
		if (client.options.renderDistance().get() != clamped) {
			client.options.renderDistance().set(clamped);
			client.options.save();
		}
	}

	/** Called by /fpsmaxx render so render-mode fallback grows back to the request. */
	public static void setMaxRenderDistance(int distance) {
		maxRenderDistance = Math.max(
			2,
			Math.min(distance, FPSMaxxConstants.maxSafeRenderDistance())
		);
	}

	public static String effectiveMode() {
		FPSMaxxConfig config = FPSMaxxConfig.get();
		String configured = config.adaptiveMode == null ? "auto" : config.adaptiveMode;
		if ("render".equalsIgnoreCase(configured)) {
			return "render";
		}
		if ("visible".equalsIgnoreCase(configured)) {
			return "visible";
		}
		if (FPSMaxxCompat.VULKANMOD && !FPSMaxxCompat.SODIUM) {
			return "render";
		}
		if (FPSMaxxCompat.SODIUM && !config.sodiumIntegration) {
			return "render";
		}
		return "visible";
	}

	public static boolean renderDistanceMode() {
		return "render".equals(effectiveMode());
	}

	/**
	 * VSync is a hard cap. With it on, aiming at 65 on a 60 Hz panel would make
	 * the governor shrink the world forever while the FPS counter never moves.
	 */
	public static int effectiveTargetFps(Minecraft client, FPSMaxxConfig config) {
		int target = config.targetFps;
		if (client.options.enableVsync().get()) {
			int refresh = client.getWindow().getRefreshRate();
			if (refresh <= 0) {
				refresh = 60;
			}
			if (refresh < target) {
				target = refresh;
			}
		}
		return Math.max(30, target);
	}

	private static void invalidateSectionGraph() {
		Minecraft client = Minecraft.getInstance();
		if (client.levelRenderer != null && client.levelRenderer.sectionOcclusionGraph() != null) {
			client.levelRenderer.sectionOcclusionGraph().invalidate();
		}

		if (FPSMaxxCompat.SODIUM) {
			try {
				FPSMaxxSodiumCompat.invalidateTerrainGraph();
			} catch (Throwable throwable) {
				FPSMaxxCompat.reportSodiumError(throwable);
			}
		}
	}

	/**
	 * @return visible radius in chunks, or -1 when the governor is off or when
	 *         the render-distance fallback is in use.
	 */
	public static int visibleDistanceChunks() {
		if (!FPSMaxxConfig.get().adaptiveEnabled || renderDistanceMode()) {
			return -1;
		}
		return visibleDistance;
	}

	/** 1.0 = full particle budget, 0.5 = the low-end floor. */
	public static float particleScale() {
		return particleScale;
	}

	/**
	 * Section mesh rebuilds are bursty: a single frame can otherwise schedule
	 * hundreds of compiles and cause a visible stutter. When the FPS is already
	 * below target, halve the budget so the frame rate recovers faster.
	 */
	public static int sectionUpdateBudget() {
		FPSMaxxConfig config = FPSMaxxConfig.get();
		int budget = config.sectionUpdateBudget;
		if (budget <= 0) {
			return 0;
		}

		Minecraft client = Minecraft.getInstance();
		if (client.level == null || client.player == null || client.getFps() <= 0) {
			return budget;
		}

		if (client.getFps() < effectiveTargetFps(client, config) - 10) {
			return Math.max(1, budget / 2);
		}
		return budget;
	}

	/**
	 * Fog must match the reduced visible radius or the world edge becomes visible.
	 */
	public static int fogDistanceChunks(int vanillaEffectiveDistance) {
		int visible = visibleDistanceChunks();
		return visible > 0 ? Math.min(vanillaEffectiveDistance, visible) : vanillaEffectiveDistance;
	}

	public static void onAdaptiveToggled() {
		if ("render".equals(lastEffectiveMode) && maxRenderDistance > 0) {
			applyRenderDistance(Minecraft.getInstance(), maxRenderDistance);
		}

		visibleDistance = -1;
		maxRenderDistance = -1;
		lastEffectiveMode = null;
		lastLevel = null;
		lastEval = 0L;
		rampCooldownUntil = 0L;
		settleUntil = 0L;
		goodStreak = 0;
		particleScale = 1.0F;
		invalidateSectionGraph();
	}

	public static String statusLine(Minecraft client) {
		FPSMaxxConfig config = FPSMaxxConfig.get();
		int shown = config.adaptiveEnabled ? Math.max(visibleDistance, 0) : -1;
		String mode = effectiveMode();
		boolean renderMode = "render".equals(mode);
		String distanceText = renderMode
			? "render " + client.options.renderDistance().get() + " chunks"
			: "visible " + (shown > 0 ? shown + " chunks" : "vanilla");
		int target = effectiveTargetFps(client, config);
		String targetText = target == config.targetFps
			? target + " FPS"
			: target + " FPS (VSync cap; requested " + config.targetFps + ")";
		return "FPSMaxx: adaptive " + (config.adaptiveEnabled ? "ON" : "OFF")
			+ " | mode " + mode
			+ " | " + distanceText
			+ " | sim " + client.options.simulationDistance().get() + " chunks"
			+ " | target " + targetText
			+ " | current " + client.getFps() + " FPS";
	}
}
