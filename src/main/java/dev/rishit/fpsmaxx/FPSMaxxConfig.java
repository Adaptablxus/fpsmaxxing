package dev.rishit.fpsmaxx;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import dev.rishit.fpsmaxx.compat.FPSMaxxCompat;
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

	private static FPSMaxxConfig instance;
	private static boolean freshInstall;
	private static boolean autoDetectHandled;

	private static Path path() {
		return FPSMaxxPlatform.configDir().resolve("fpsmaxx.json");
	}

	/** Informational flag set by the low-end preset. */
	public boolean lowEndMode = false;

	/**
	 * True when the potato profile is the active mod-side profile. The hidden
	 * FpsMaxx preset uses this to restore potato rather than low-end on startup.
	 */
	public boolean potatoMode = false;

	/**
	 * True when the hidden FpsMaxx graphics preset is selected in Video
	 * Settings. It is deliberately separate from vanilla's
	 * {@code graphicsPreset} option: FpsMaxx also changes options that vanilla
	 * presets do not know about, so vanilla reports {@code CUSTOM} while the
	 * slider must still show FpsMaxx.
	 */
	public boolean graphicsPresetFpsMaxx = false;

	/**
	 * Keep the visual identity of the Fresh &amp; Smooth modpack (shaders,
	 * particles, animations and resource packs) while still applying the FPSMaxx
	 * governor, culling and chunk budget. Enabled automatically when the pack is
	 * detected; set to false to force the full aggressive profile.
	 */
	public boolean freshSmoothCompatibility = FPSMaxxCompat.FRESH_AND_SMOOTH;

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
	 * Let the governor lower simulation distance in singleplayer when FPS is
	 * consistently below target. This changes mob AI and ticking, so it is off
	 * by default and on only in the low-end and potato profiles.
	 */
	public boolean adaptiveSimulation = false;

	/** Lowest simulation distance the adaptive governor may choose. */
	public int minSimulationDistance = FPSMaxxConstants.MIN_SIMULATION_DISTANCE;

	/** Apply a low-end or potato mod-side profile automatically on a fresh install. */
	public boolean autoDetectHardware = true;

	/** Last detected hardware tier; informational only. */
	public String detectedHardware = "unknown";

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

	/**
	 * Let the governor shrink entity and block-entity cull distances when FPS is
	 * below target. The configured distances stay as the ceiling and are restored
	 * when the frame rate recovers. This is a rendering-only lever, so it never
	 * changes gameplay or server-side ticking.
	 */
	public boolean adaptiveCulling = false;

	/**
	 * Render the 3D world at a lower internal resolution when FPS is below
	 * target, then upscale it before the GUI is drawn. The HUD stays native.
	 * Disabled automatically when Iris shaders, VulkanMod or Nvidium own the
	 * render pipeline.
	 */
	public boolean dynamicResolution = false;

	/** Lowest dynamic resolution, as a percentage of the window size. */
	public int dynamicResolutionMinPercent = 60;

	/**
	 * Maximum chunk-section mesh rebuilds scheduled in one frame. 0 = vanilla
	 * (unlimited). A budget smooths the frame-time spikes caused by rebuilding
	 * many sections at once; skipped sections are re-queued for the next frame.
	 */
	public int sectionUpdateBudget = 0;

	public static FPSMaxxConfig get() {
		if (instance == null) {
			load();
		}
		return instance;
	}

	public static synchronized void load() {
		Path path = path();
		boolean fresh = !Files.exists(path);
		if (fresh) {
			freshInstall = true;
		}
		if (!fresh) {
			try {
				String json = Files.readString(path, StandardCharsets.UTF_8);
				JsonObject object = JsonParser.parseString(json).getAsJsonObject();
				FPSMaxxConfig loaded = GSON.fromJson(object, FPSMaxxConfig.class);
				if (loaded != null) {
					instance = loaded;
					// 1.5.6/1.5.7 add adaptive culling and dynamic resolution.
					// Configs that were already on a low-end profile keep that
					// intent; balanced/custom configs are left exactly as they
					// were.
					if (!object.has("adaptiveCulling") && loaded.lowEndMode) {
						loaded.adaptiveCulling = true;
					}
					if (!object.has("dynamicResolution") && loaded.lowEndMode) {
						loaded.dynamicResolution = true;
					}
				}
			} catch (IOException | JsonSyntaxException | IllegalStateException e) {
				LOGGER.warn("Could not read {}, using defaults", path, e);
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

	/**
	 * True only for a config file that did not exist when this JVM loaded it.
	 * Hardware detection runs once, after the render device is available, so it
	 * can refine the first-run profile without touching existing installs.
	 */
	public static boolean shouldAutoDetectHardware() {
		return freshInstall && !autoDetectHandled;
	}

	public static void markAutoDetectHandled() {
		autoDetectHandled = true;
	}

	/** Mod-side defaults for a low-end machine. Vanilla graphics are applied by /fpsmaxx lowend. */
	public void applyLowEndDefaults() {
		lowEndMode = true;
		potatoMode = false;
		targetFps = FPSMaxxConstants.LOW_END_TARGET_FPS;
		adaptiveEnabled = true;
		initialVisibleDistance = FPSMaxxConstants.LOW_END_INITIAL_VISIBLE_DISTANCE;
		minVisibleDistance = FPSMaxxConstants.LOW_END_MIN_VISIBLE_DISTANCE;
		simulationDistance = FPSMaxxConstants.LOW_END_SIMULATION_DISTANCE;
		forceSimulationDistance = true;
		adaptiveSimulation = true;
		minSimulationDistance = FPSMaxxConstants.MIN_SIMULATION_DISTANCE;
		adaptiveMode = "auto";
		sodiumIntegration = true;
		sodiumConfigOverlay = true;
		matchFogToVisibleDistance = true;
		entityCullDistance = FPSMaxxConstants.LOW_END_ENTITY_CULL_DISTANCE;
		blockEntityCullDistance = FPSMaxxConstants.LOW_END_BLOCK_ENTITY_CULL_DISTANCE;
		particleCullDistance = FPSMaxxConstants.LOW_END_PARTICLE_CULL_DISTANCE;
		maxParticles = FPSMaxxConstants.LOW_END_MAX_PARTICLES;
		adaptiveParticles = true;
		adaptiveCulling = true;
		dynamicResolution = true;
		dynamicResolutionMinPercent = 60;
		sectionUpdateBudget = FPSMaxxConstants.LOW_END_SECTION_UPDATE_BUDGET;
	}

	/** Mod-side defaults for an extremely weak device. */
	public void applyPotatoDefaults() {
		lowEndMode = true;
		potatoMode = true;
		targetFps = FPSMaxxConstants.POTATO_TARGET_FPS;
		adaptiveEnabled = true;
		initialVisibleDistance = FPSMaxxConstants.POTATO_INITIAL_VISIBLE_DISTANCE;
		minVisibleDistance = FPSMaxxConstants.POTATO_MIN_VISIBLE_DISTANCE;
		simulationDistance = FPSMaxxConstants.POTATO_SIMULATION_DISTANCE;
		forceSimulationDistance = true;
		adaptiveSimulation = true;
		minSimulationDistance = FPSMaxxConstants.MIN_SIMULATION_DISTANCE;
		adaptiveMode = "auto";
		sodiumIntegration = true;
		sodiumConfigOverlay = true;
		matchFogToVisibleDistance = true;
		entityCullDistance = FPSMaxxConstants.POTATO_ENTITY_CULL_DISTANCE;
		blockEntityCullDistance = FPSMaxxConstants.POTATO_BLOCK_ENTITY_CULL_DISTANCE;
		particleCullDistance = FPSMaxxConstants.POTATO_PARTICLE_CULL_DISTANCE;
		maxParticles = FPSMaxxConstants.POTATO_MAX_PARTICLES;
		adaptiveParticles = true;
		adaptiveCulling = true;
		dynamicResolution = true;
		dynamicResolutionMinPercent = 50;
		sectionUpdateBudget = FPSMaxxConstants.POTATO_SECTION_UPDATE_BUDGET;
	}

	/**
	 * Mod-side defaults for the Fresh &amp; Smooth pack. The pack's shaders,
	 * particles and animations are left alone; only the distances, culling and
	 * chunk scheduling are reduced.
	 */
	public void applyFreshSmoothDefaults() {
		lowEndMode = true;
		potatoMode = false;
		targetFps = FPSMaxxConstants.FRESH_SMOOTH_TARGET_FPS;
		adaptiveEnabled = true;
		initialVisibleDistance = FPSMaxxConstants.FRESH_SMOOTH_INITIAL_VISIBLE_DISTANCE;
		minVisibleDistance = FPSMaxxConstants.FRESH_SMOOTH_MIN_VISIBLE_DISTANCE;
		simulationDistance = FPSMaxxConstants.FRESH_SMOOTH_SIMULATION_DISTANCE;
		forceSimulationDistance = true;
		adaptiveSimulation = false;
		minSimulationDistance = FPSMaxxConstants.MIN_SIMULATION_DISTANCE;
		adaptiveMode = "auto";
		sodiumIntegration = true;
		sodiumConfigOverlay = true;
		// Iris shader packs own the fog; matching the visible radius to vanilla
		// fog would fight the pack's atmospheric look.
		matchFogToVisibleDistance = false;
		entityCullDistance = FPSMaxxConstants.FRESH_SMOOTH_ENTITY_CULL_DISTANCE;
		blockEntityCullDistance = FPSMaxxConstants.FRESH_SMOOTH_BLOCK_ENTITY_CULL_DISTANCE;
		particleCullDistance = FPSMaxxConstants.FRESH_SMOOTH_PARTICLE_CULL_DISTANCE;
		maxParticles = FPSMaxxConstants.FRESH_SMOOTH_MAX_PARTICLES;
		adaptiveParticles = true;
		adaptiveCulling = true;
		dynamicResolution = false;
		dynamicResolutionMinPercent = 75;
		sectionUpdateBudget = FPSMaxxConstants.FRESH_SMOOTH_SECTION_UPDATE_BUDGET;
	}

	/** Mod-side defaults for a normal PC. Vanilla graphics are left untouched. */
	public void applyBalancedDefaults() {
		lowEndMode = false;
		potatoMode = false;
		graphicsPresetFpsMaxx = false;
		targetFps = FPSMaxxConstants.BALANCED_TARGET_FPS;
		adaptiveEnabled = true;
		initialVisibleDistance = FPSMaxxConstants.BALANCED_INITIAL_VISIBLE_DISTANCE;
		minVisibleDistance = FPSMaxxConstants.BALANCED_MIN_VISIBLE_DISTANCE;
		simulationDistance = FPSMaxxConstants.MAX_SIMULATION_DISTANCE;
		forceSimulationDistance = true;
		adaptiveSimulation = false;
		minSimulationDistance = FPSMaxxConstants.MIN_SIMULATION_DISTANCE;
		adaptiveMode = "auto";
		sodiumIntegration = true;
		sodiumConfigOverlay = true;
		matchFogToVisibleDistance = true;
		entityCullDistance = FPSMaxxConstants.BALANCED_ENTITY_CULL_DISTANCE;
		blockEntityCullDistance = FPSMaxxConstants.BALANCED_BLOCK_ENTITY_CULL_DISTANCE;
		particleCullDistance = FPSMaxxConstants.BALANCED_PARTICLE_CULL_DISTANCE;
		maxParticles = FPSMaxxConstants.BALANCED_MAX_PARTICLES;
		adaptiveParticles = true;
		adaptiveCulling = false;
		dynamicResolution = false;
		dynamicResolutionMinPercent = 80;
		sectionUpdateBudget = FPSMaxxConstants.BALANCED_SECTION_UPDATE_BUDGET;
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
		minSimulationDistance = clamp(
			minSimulationDistance,
			FPSMaxxConstants.MIN_SIMULATION_DISTANCE,
			simulationDistance
		);
		if (detectedHardware == null || detectedHardware.isBlank()) {
			detectedHardware = "unknown";
		}
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
		dynamicResolutionMinPercent = clamp(dynamicResolutionMinPercent, 50, 100);
		sectionUpdateBudget = clamp(sectionUpdateBudget, 0, 512);
	}

	public synchronized void save() {
		Path path = path();
		try {
			Files.createDirectories(path.getParent());
			Files.writeString(path, GSON.toJson(this), StandardCharsets.UTF_8);
		} catch (IOException e) {
			LOGGER.warn("Could not save {}", path, e);
		}
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}
}
