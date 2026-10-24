package dev.rishit.fpsmaxx.compat;

import dev.rishit.fpsmaxx.FPSMaxxConfig;
import dev.rishit.fpsmaxx.FPSMaxxConstants;
import dev.rishit.fpsmaxx.FPSMaxxPresetScreen;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.ModOptionsBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adds FPSMaxx to Sodium's own video-settings screen.
 *
 * <p>Sodium replaces vanilla's Video Settings screen, so the mixin that puts
 * FpsMaxx on the vanilla graphics-preset slider never runs in a Sodium pack.
 * The config API gives Sodium users an FPSMaxx page with the same five-position
 * slider, plus the 64-chunk render-distance range.</p>
 *
 * <p>This class is only loaded when Sodium invokes its config entrypoint.</p>
 */
public final class FPSMaxxSodiumConfig implements ConfigEntryPoint {
	private static final Logger LOGGER = LoggerFactory.getLogger(FPSMaxxConstants.MOD_ID);
	private static final Identifier RENDER_DISTANCE =
		Identifier.fromNamespaceAndPath("sodium", "general.render_distance");

	@Override
	public void registerConfigLate(ConfigBuilder builder) {
		FPSMaxxConfig config = FPSMaxxConfig.get();
		String version = FabricLoader.getInstance()
			.getModContainer(FPSMaxxConstants.MOD_ID)
			.map(container -> container.getMetadata().getVersion().getFriendlyString())
			.orElse("dev");

		try {
			ModOptionsBuilder modOptions = builder.registerOwnModOptions()
				.setName("FPSMaxx")
				.setVersion(version);

			if (config.sodiumConfigOverlay && !FPSMaxxCompat.C2ME) {
				modOptions.registerOptionOverlay(
					RENDER_DISTANCE,
					builder.createIntegerOption(RENDER_DISTANCE)
						.setRange(2, FPSMaxxConstants.MAX_RENDER_DISTANCE, 1)
				);
				FPSMaxxCompat.markSodiumConfigHook();
			} else if (FPSMaxxCompat.C2ME) {
				LOGGER.info("C2ME detected; leaving Sodium's render-distance slider to C2ME");
			}

			modOptions.addPage(
				builder.createExternalPage()
					.setName(Component.translatable("fpsmaxx.sodium.page"))
					.setScreenConsumer(parent -> Minecraft.getInstance().setScreenAndShow(
						new FPSMaxxPresetScreen(parent)
					))
			);
			LOGGER.info("FPSMaxx preset page registered in Sodium's video settings (v{})", version);
		} catch (Throwable throwable) {
			LOGGER.warn(
				"Could not register FPSMaxx's Sodium config page; use /fpsmaxx freshsmooth or /fpsmaxx lowend",
				throwable
			);
		}
	}
}
