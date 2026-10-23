package dev.rishit.fpsmaxx;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Small JSON config. Loaded lazily so it is available even before the client
 * entrypoint runs (the Options mixin fires very early in startup).
 */
public final class FPSMaxxConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger(FPSMaxxConstants.MOD_ID);
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("fpsmaxx.json");

	private static FPSMaxxConfig instance;

	/** Informational flag set by the low-end preset. */
	public boolean lowEndMode = false;

	public int targetFps = FPSMaxxConstants.DEFAULT_TARGET_FPS;

	public boolean adaptiveEnabled = true;

	/** Distance the governor starts at when entering a world. */
	public int initialVisibleDistance = 32;

	/** Lowest distance the governor is allowed to shrink to. */
	public int minVisibleDistance = 8;

	/**
	 * Simulation distance requested when entering a world. Vanilla's maximum is
	 * 32; the mod keeps it there unless the heap cannot support it.
	 */
	public int simulationDistance = FPSMaxxConstants.MAX_SIMULATION_DISTANCE;

	/** Re-apply {@link #simulationDistance} whenever a world is entered. */
	public boolean forceSimulationDistance = true;

	/**
	 * auto = use the visible-radius governor on vanilla/Sodium and fall back to
	 * changing the real render distance on renderers without a supported hook;
	 * visible = never change the real render distance; render = always change it.
	 */
	public String adaptiveMode = "auto";

	/** Cap Sodium's terrain search distance to the governor radius. */
	public boolean sodiumIntegration = true;

	/** Add a 64-chunk range to Sodium's render-distance video option. */
	public boolean sodiumConfigOverlay = true;

	/**
	 * Match fog to the reduced visible radius. Disable this if a resource pack
	 * or shader pack supplies its own fog and should win.
	 */
	public boolean matchFogToVisibleDistance = true;

	/** Entities farther away than this (in blocks) are not rendered. 0 = off. */
	public int entityCullDistance = 96;

	/** Block entities farther away than this (in blocks) are not rendered. 0 = off. */
	public int blockEntityCullDistance = 96;

	/** Particles farther away than this (in blocks) are not created. 0 = off. */
	public int particleCullDistance = 64;

	/** Maximum live particles. 0 = use vanilla's own limits only. */
	public int maxParticles = 4000;

	/** Let the governor reduce the particle budget when FPS falls below target. */
	public boolean adaptiveParticles = true;

	public static FPSMaxxConfig get() {
		if (instance == null) {
			load();
		}
		return instance;
	}

	public static synchronized void load() {
		boolean fresh = !Files.exists(PATH);
		if (!fresh) {
			try {
				String json = Files.readString(PATH, StandardCharsets.UTF_8);
				FPSMaxxConfig loaded = GSON.fromJson(json, FPSMaxxConfig.class);
				if (loaded != null) {
					instance = loaded;
				}
			} catch (IOException | JsonSyntaxException e) {
				LOGGER.warn("Could not read {}, using defaults", PATH, e);
			}
		}

		if (instance == null) {
			instance = new FPSMaxxConfig();
			fresh = true;
		}

		if (fresh && FPSMaxxConstants.LOW_END_DEVICE) {
			instance.applyLowEndDefaults();
		}

		instance.sanitize();
		instance.save();
	}

	/** Mod-side defaults for a low-end machine. Vanilla graphics are applied by /fpsmaxx lowend. */
	public void applyLowEndDefaults() {
		lowEndMode = true;
		targetFps = FPSMaxxConstants.LOW_END_TARGET_FPS;
		adaptiveEnabled = true;
		initialVisibleDistance = FPSMaxxConstants.LOW_END_INITIAL_VISIBLE_DISTANCE;
		minVisibleDistance = FPSMaxxConstants.LOW_END_MIN_VISIBLE_DISTANCE;
		simulationDistance = FPSMaxxConstants.LOW_END_SIMULATION_DISTANCE;
		forceSimulationDistance = true;
		adaptiveMode = "auto";
		sodiumIntegration = true;
		sodiumConfigOverlay = true;
		matchFogToVisibleDistance = true;
		entityCullDistance = FPSMaxxConstants.LOW_END_ENTITY_CULL_DISTANCE;
		blockEntityCullDistance = FPSMaxxConstants.LOW_END_BLOCK_ENTITY_CULL_DISTANCE;
		particleCullDistance = FPSMaxxConstants.LOW_END_PARTICLE_CULL_DISTANCE;
		maxParticles = FPSMaxxConstants.LOW_END_MAX_PARTICLES;
		adaptiveParticles = true;
	}

	/** Mod-side defaults for a normal PC. Vanilla graphics are left untouched. */
	public void applyBalancedDefaults() {
		lowEndMode = false;
		targetFps = FPSMaxxConstants.BALANCED_TARGET_FPS;
		adaptiveEnabled = true;
		initialVisibleDistance = FPSMaxxConstants.BALANCED_INITIAL_VISIBLE_DISTANCE;
		minVisibleDistance = FPSMaxxConstants.BALANCED_MIN_VISIBLE_DISTANCE;
		simulationDistance = FPSMaxxConstants.MAX_SIMULATION_DISTANCE;
		forceSimulationDistance = true;
		adaptiveMode = "auto";
		sodiumIntegration = true;
		sodiumConfigOverlay = true;
		matchFogToVisibleDistance = true;
		entityCullDistance = FPSMaxxConstants.BALANCED_ENTITY_CULL_DISTANCE;
		blockEntityCullDistance = FPSMaxxConstants.BALANCED_BLOCK_ENTITY_CULL_DISTANCE;
		particleCullDistance = FPSMaxxConstants.BALANCED_PARTICLE_CULL_DISTANCE;
		maxParticles = FPSMaxxConstants.BALANCED_MAX_PARTICLES;
		adaptiveParticles = true;
	}

	public void sanitize() {
		targetFps = clamp(targetFps, 30, 1000);
		initialVisibleDistance = clamp(initialVisibleDistance, 2, FPSMaxxConstants.MAX_RENDER_DISTANCE);
		minVisibleDistance = clamp(minVisibleDistance, 2, initialVisibleDistance);
		simulationDistance = clamp(
			simulationDistance,
			FPSMaxxConstants.MIN_SIMULATION_DISTANCE,
			FPSMaxxConstants.MAX_SIMULATION_DISTANCE
		);
		if (adaptiveMode == null
			|| (!adaptiveMode.equalsIgnoreCase("auto")
				&& !adaptiveMode.equalsIgnoreCase("visible")
				&& !adaptiveMode.equalsIgnoreCase("render"))) {
			adaptiveMode = "auto";
		} else {
			adaptiveMode = adaptiveMode.toLowerCase(Locale.ROOT);
		}
		entityCullDistance = clamp(entityCullDistance, 0, 512);
		blockEntityCullDistance = clamp(blockEntityCullDistance, 0, 512);
		particleCullDistance = clamp(particleCullDistance, 0, 512);
		maxParticles = maxParticles == 0 ? 0 : clamp(maxParticles, 100, 20000);
	}

	public synchronized void save() {
		try {
			Files.createDirectories(PATH.getParent());
			Files.writeString(PATH, GSON.toJson(this), StandardCharsets.UTF_8);
		} catch (IOException e) {
			LOGGER.warn("Could not save {}", PATH, e);
		}
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}
}
