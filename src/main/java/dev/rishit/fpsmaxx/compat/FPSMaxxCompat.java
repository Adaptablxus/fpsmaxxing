package dev.rishit.fpsmaxx.compat;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.rishit.fpsmaxx.FPSMaxxConstants;

/**
 * Soft compatibility checks. Nothing here loads a class from another mod; the
 * optional integration classes are only touched when the corresponding mod is
 * present.
 */
public final class FPSMaxxCompat {
	private static final Logger LOGGER = LoggerFactory.getLogger(FPSMaxxConstants.MOD_ID);

	public static final boolean SODIUM = isLoaded("sodium");
	public static final boolean IRIS = isLoaded("iris");
	public static final boolean VULKANMOD = isLoaded("vulkanmod");
	public static final boolean DISTANT_HORIZONS = isLoaded("distanthorizons");
	public static final boolean C2ME = isLoaded("c2me");
	public static final boolean LITHIUM = isLoaded("lithium");
	public static final boolean ENTITY_CULLING = isLoaded("entityculling");
	public static final boolean IMMEDIATELY_FAST = isLoaded("immediatelyfast");
	public static final boolean FERRITECORE = isLoaded("ferritecore");
	public static final boolean MODERNFIX = isLoaded("modernfix");
	public static final boolean BOBBY = isLoaded("bobby");
	public static final boolean NVIDIUM = isLoaded("nvidium");
	public static final boolean ASYNC_PARTICLES = isLoaded("asyncparticles");
	public static final boolean PARTICLE_CORE = isLoaded("particle_core");
	public static final boolean MORE_CULLING = isLoaded("moreculling");
	public static final boolean BAD_OPTIMIZATIONS = isLoaded("badoptimizations");
	public static final boolean SODIUM_EXTRA = isLoaded("sodium-extra");
	public static final boolean SERVER_CORE = isLoaded("servercore");

	/**
	 * Fresh &amp; Smooth is a visual-first client pack built around shaders,
	 * animations and a large set of particle mods. Detecting the combination
	 * (rather than a single mod) avoids matching unrelated packs that happen to
	 * include one of them.
	 */
	public static final boolean FRESH_AND_SMOOTH =
		isLoaded("particlerain")
			&& isLoaded("particular")
			&& isLoaded("wakes")
			&& isLoaded("visuality")
			&& isLoaded("bettersmoke")
			&& isLoaded("do_a_barrel_roll")
			&& isLoaded("notenoughanimations");

	private static boolean sodiumTerrainHook;
	private static boolean sodiumFogHook;
	private static boolean sodiumConfigHook;
	private static boolean sodiumCapLogged;
	private static boolean sodiumErrorLogged;

	private FPSMaxxCompat() {
	}

	private static boolean isLoaded(String modId) {
		return FabricLoader.getInstance().isModLoaded(modId);
	}

	public static void markSodiumTerrainHook() {
		if (!sodiumTerrainHook) {
			sodiumTerrainHook = true;
			LOGGER.info("Sodium terrain-distance hook active");
		}
	}

	public static void markSodiumFogHook() {
		if (!sodiumFogHook) {
			sodiumFogHook = true;
			LOGGER.info("Sodium fog hook active");
		}
	}

	public static void markSodiumTerrainCap(int chunks) {
		if (!sodiumCapLogged) {
			sodiumCapLogged = true;
			LOGGER.info("Sodium visible distance capped to {} chunks", chunks);
		}
	}

	public static void markSodiumConfigHook() {
		if (!sodiumConfigHook) {
			sodiumConfigHook = true;
			LOGGER.info("Sodium render-distance slider extended to 64 chunks");
		}
	}

	public static boolean sodiumTerrainHook() {
		return sodiumTerrainHook;
	}

	public static boolean sodiumFogHook() {
		return sodiumFogHook;
	}

	public static boolean sodiumConfigHook() {
		return sodiumConfigHook;
	}

	public static void reportSodiumError(Throwable throwable) {
		if (sodiumErrorLogged) {
			return;
		}
		sodiumErrorLogged = true;
		LOGGER.warn(
			"Sodium integration failed; FPSMaxx will fall back without touching Sodium internals. "
				+ "Set adaptiveMode to \"render\" if the visible-distance governor does not work.",
			throwable
		);
	}

	public static String describe() {
		StringBuilder builder = new StringBuilder();
		append(builder, "Sodium", SODIUM);
		append(builder, "Iris", IRIS);
		append(builder, "VulkanMod", VULKANMOD);
		append(builder, "Distant Horizons", DISTANT_HORIZONS);
		append(builder, "C2ME", C2ME);
		append(builder, "Lithium", LITHIUM);
		append(builder, "EntityCulling", ENTITY_CULLING);
		append(builder, "ImmediatelyFast", IMMEDIATELY_FAST);
		append(builder, "FerriteCore", FERRITECORE);
		append(builder, "ModernFix", MODERNFIX);
		append(builder, "Bobby", BOBBY);
		append(builder, "Nvidium", NVIDIUM);
		append(builder, "AsyncParticles", ASYNC_PARTICLES);
		append(builder, "MoreCulling", MORE_CULLING);
		append(builder, "BadOptimizations", BAD_OPTIMIZATIONS);
		append(builder, "Sodium Extra", SODIUM_EXTRA);
		append(builder, "ServerCore", SERVER_CORE);
		if (FRESH_AND_SMOOTH) {
			append(builder, "Fresh & Smooth profile", true);
		}
		return builder.length() == 0 ? "no known optimization mods" : builder.toString();
	}

	private static void append(StringBuilder builder, String name, boolean present) {
		if (!present) {
			return;
		}
		if (builder.length() > 0) {
			builder.append(", ");
		}
		builder.append(name);
	}
}
