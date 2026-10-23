package dev.rishit.fpsmaxx.compat;

import dev.rishit.fpsmaxx.FPSMaxxConfig;
import dev.rishit.fpsmaxx.FPSMaxxConstants;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adds a 64-chunk range to Sodium's own render-distance slider. Sodium's config
 * screen otherwise hardcodes 2..32 and would hide the distance FPSMaxx unlocks.
 *
 * This class is only loaded when Sodium invokes its config entrypoint.
 */
public final class FPSMaxxSodiumConfig implements ConfigEntryPoint {
	private static final Logger LOGGER = LoggerFactory.getLogger(FPSMaxxConstants.MOD_ID);
	private static final Identifier RENDER_DISTANCE =
		Identifier.fromNamespaceAndPath("sodium", "general.render_distance");

	@Override
	public void registerConfigLate(ConfigBuilder builder) {
		FPSMaxxConfig config = FPSMaxxConfig.get();
		if (!config.sodiumConfigOverlay) {
			return;
		}

		// C2ME already ships its own Sodium render-distance override. Do not
		// fight it; its setting is compatible with FPSMaxx.
		if (FPSMaxxCompat.C2ME) {
			LOGGER.info("C2ME detected; leaving Sodium's render-distance slider to C2ME");
			return;
		}

		try {
			builder.registerOwnModOptions().registerOptionOverlay(
				RENDER_DISTANCE,
				builder.createIntegerOption(RENDER_DISTANCE)
					.setRange(2, FPSMaxxConstants.MAX_RENDER_DISTANCE, 1)
			);
			FPSMaxxCompat.markSodiumConfigHook();
		} catch (Throwable throwable) {
			LOGGER.warn(
				"Could not extend Sodium's render-distance slider; use /fpsmaxx render 64 instead",
				throwable
			);
		}
	}
}
