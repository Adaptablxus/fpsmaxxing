package dev.rishit.fpsmaxx;

import dev.rishit.fpsmaxx.compat.FPSMaxxCompat;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.PrioritizeChunkUpdates;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.server.level.ParticleStatus;

/**
 * One-shot presets. The mod-side presets only change FPSMaxx settings; the
 * low-end and potato presets also apply vanilla's Fast graphics profile and a
 * few extra reductions so a weak GPU gets immediate relief.
 */
public final class FPSMaxxPresets {
	private FPSMaxxPresets() {
	}

	public static String applyLowEnd(Minecraft client) {
		return useFreshSmoothProfile()
			? applyFreshSmooth(client, null)
			: applyPerformancePreset(client, false, null);
	}

	public static String applyPotato(Minecraft client) {
		return applyPerformancePreset(client, true, null);
	}

	/**
	 * The hidden FpsMaxx graphics preset. It is the same profile as
	 * {@code /fpsmaxx lowend}, but it is also applied when the player drags the
	 * graphics-preset slider all the way to the left.
	 *
	 * <p>On Fresh &amp; Smooth this automatically uses the visual-safe profile so
	 * the pack's shaders, particles and animations keep working.</p>
	 */
	public static String applyFpsMaxx(Minecraft client, OptionsSubScreen screen) {
		return useFreshSmoothProfile()
			? applyFreshSmooth(client, screen)
			: applyPerformancePreset(client, false, screen);
	}

	/** Force the aggressive profile even when the Fresh &amp; Smooth pack is detected. */
	public static String applyAggressiveLowEnd(Minecraft client) {
		return applyPerformancePreset(client, false, null);
	}

	public static boolean useFreshSmoothProfile() {
		return FPSMaxxCompat.FRESH_AND_SMOOTH && FPSMaxxConfig.get().freshSmoothCompatibility;
	}

	/**
	 * Visual-safe profile for the Fresh &amp; Smooth pack. It leaves the pack's
	 * graphics, shaders and particle options alone, and only reduces the
	 * distances, framerate cap and chunk scheduling that cause frame-time spikes.
	 */
	public static String applyFreshSmooth(Minecraft client, OptionsSubScreen screen) {
		FPSMaxxConfig config = FPSMaxxConfig.get();

		int previousRender = client.options.renderDistance().get();
		int previousSimulation = client.options.simulationDistance().get();

		int renderDistance = Math.max(
			2,
			Math.min(FPSMaxxConstants.FRESH_SMOOTH_RENDER_DISTANCE, previousRender)
		);
		int simulationDistance = Math.max(
			FPSMaxxConstants.MIN_SIMULATION_DISTANCE,
			Math.min(FPSMaxxConstants.FRESH_SMOOTH_SIMULATION_DISTANCE, previousSimulation)
		);

		client.options.renderDistance().set(renderDistance);
		client.options.simulationDistance().set(simulationDistance);
		client.options.framerateLimit().set(60);
		client.options.chunkSectionFadeInTime().set(0.0D);
		client.options.prioritizeChunkUpdates().set(PrioritizeChunkUpdates.NONE);
		client.options.graphicsPreset().set(GraphicsPreset.CUSTOM);
		client.options.save();

		config.applyFreshSmoothDefaults();
		config.graphicsPresetFpsMaxx = true;
		config.initialVisibleDistance = Math.min(config.initialVisibleDistance, renderDistance);
		config.minVisibleDistance = Math.min(config.minVisibleDistance, config.initialVisibleDistance);
		config.simulationDistance = simulationDistance;
		config.sanitize();
		config.save();

		FPSMaxxGovernor.onAdaptiveToggled();
		FPSMaxxGovernor.applySimulationDistance(client, config);

		if (screen != null) {
			refresh(screen,
				client.options.renderDistance(),
				client.options.simulationDistance(),
				client.options.framerateLimit(),
				client.options.chunkSectionFadeInTime(),
				client.options.prioritizeChunkUpdates()
			);
		}

		return "FPSMaxx Fresh & Smooth profile applied: render " + renderDistance
			+ " chunks, simulation " + simulationDistance
			+ ", target " + config.targetFps + " FPS"
			+ ", section budget " + config.sectionUpdateBudget
			+ ", entity cull " + config.entityCullDistance
			+ ", particles " + (FPSMaxxCompat.ASYNC_PARTICLES
				? "handled by AsyncParticles"
				: config.particleCullDistance + " blocks / " + config.maxParticles + " cap")
			+ ". Shaders, particles and animations were left untouched.";
	}

	private static String applyPerformancePreset(
		Minecraft client,
		boolean potato,
		OptionsSubScreen screen
	) {
		FPSMaxxConfig config = FPSMaxxConfig.get();

		int previousRender = client.options.renderDistance().get();
		int previousSimulation = client.options.simulationDistance().get();

		// Fast is vanilla's own low-end profile. Apply it first, then reduce the
		// remaining costs that Fast still leaves on the table.
		client.options.applyGraphicsPreset(GraphicsPreset.FAST);

		int requestedRender = potato
			? FPSMaxxConstants.POTATO_RENDER_DISTANCE
			: FPSMaxxConstants.LOW_END_RENDER_DISTANCE;
		int requestedSimulation = potato
			? FPSMaxxConstants.POTATO_SIMULATION_DISTANCE
			: FPSMaxxConstants.LOW_END_SIMULATION_DISTANCE;

		int renderDistance = Math.max(2, Math.min(requestedRender, previousRender));
		int simulationDistance = Math.max(
			FPSMaxxConstants.MIN_SIMULATION_DISTANCE,
			Math.min(requestedSimulation, previousSimulation)
		);

		client.options.renderDistance().set(renderDistance);
		client.options.simulationDistance().set(simulationDistance);
		client.options.cloudStatus().set(CloudStatus.OFF);
		client.options.cloudRange().set(2);
		client.options.particles().set(ParticleStatus.MINIMAL);
		client.options.weatherRadius().set(3);
		client.options.entityShadows().set(false);
		client.options.ambientOcclusion().set(false);
		client.options.biomeBlendRadius().set(0);
		client.options.entityDistanceScaling().set(0.5D);
		client.options.cutoutLeaves().set(false);
		client.options.vignette().set(false);
		client.options.improvedTransparency().set(false);
		client.options.chunkSectionFadeInTime().set(0.0D);
		client.options.prioritizeChunkUpdates().set(PrioritizeChunkUpdates.NONE);
		client.options.mipmapLevels().set(0);
		client.options.maxAnisotropyBit().set(1);
		client.options.textureFiltering().set(TextureFilteringMethod.NONE);
		client.options.framerateLimit().set(60);

		// The extra reductions above make vanilla's preset value "Custom". Keep
		// that value in sync; the visible slider is driven by the FpsMaxx flag.
		client.options.graphicsPreset().set(GraphicsPreset.CUSTOM);
		client.options.save();

		if (potato) {
			config.applyPotatoDefaults();
		} else {
			config.applyLowEndDefaults();
		}
		config.graphicsPresetFpsMaxx = true;
		config.initialVisibleDistance = Math.min(config.initialVisibleDistance, renderDistance);
		config.minVisibleDistance = Math.min(config.minVisibleDistance, config.initialVisibleDistance);
		config.simulationDistance = simulationDistance;
		config.sanitize();
		config.save();

		FPSMaxxGovernor.onAdaptiveToggled();
		FPSMaxxGovernor.applySimulationDistance(client, config);

		if (screen != null) {
			refresh(screen,
				client.options.renderDistance(),
				client.options.simulationDistance(),
				client.options.cloudStatus(),
				client.options.cloudRange(),
				client.options.particles(),
				client.options.weatherRadius(),
				client.options.entityShadows(),
				client.options.ambientOcclusion(),
				client.options.biomeBlendRadius(),
				client.options.entityDistanceScaling(),
				client.options.cutoutLeaves(),
				client.options.vignette(),
				client.options.improvedTransparency(),
				client.options.chunkSectionFadeInTime(),
				client.options.prioritizeChunkUpdates(),
				client.options.mipmapLevels(),
				client.options.maxAnisotropyBit(),
				client.options.textureFiltering(),
				client.options.framerateLimit()
			);
		}

		String preset = potato ? "potato" : "low-end";
		return "FPSMaxx " + preset + " preset applied (Fast graphics): render " + renderDistance
			+ " chunks, simulation " + simulationDistance
			+ ", target " + config.targetFps + " FPS"
			+ ", section budget " + config.sectionUpdateBudget
			+ ", particles " + config.particleCullDistance + " blocks / "
			+ config.maxParticles + " cap";
	}

	/**
	 * Called when the hidden FpsMaxx position is left for a vanilla preset.
	 * The aggressive mod-side profile is removed, but the vanilla preset keeps
	 * control of the simulation distance the player just selected.
	 */
	public static void resetModProfile(Minecraft client) {
		FPSMaxxConfig config = FPSMaxxConfig.get();
		config.applyBalancedDefaults();
		config.simulationDistance = client.options.simulationDistance().get();
		config.forceSimulationDistance = false;
		config.sanitize();
		config.save();

		FPSMaxxGovernor.onAdaptiveToggled();
	}

	private static void refresh(OptionsSubScreen screen, OptionInstance<?>... options) {
		for (OptionInstance<?> option : options) {
			screen.resetOption(option);
		}
	}

	public static String applyBalanced(Minecraft client) {
		FPSMaxxConfig config = FPSMaxxConfig.get();
		config.applyBalancedDefaults();
		config.sanitize();
		config.save();

		FPSMaxxGovernor.onAdaptiveToggled();
		FPSMaxxGovernor.applySimulationDistance(client, config);

		return "FPSMaxx balanced preset: target " + config.targetFps
			+ " FPS, simulation " + client.options.simulationDistance().get()
			+ ", particles " + config.particleCullDistance + " blocks / "
			+ config.maxParticles + " cap. Vanilla graphics were not changed.";
	}
}
