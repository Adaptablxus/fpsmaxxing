package dev.rishit.fpsmaxx;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.Minecraft;
import net.minecraft.client.PrioritizeChunkUpdates;
import net.minecraft.client.TextureFilteringMethod;
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
		return applyPerformancePreset(client, false);
	}

	public static String applyPotato(Minecraft client) {
		return applyPerformancePreset(client, true);
	}

	private static String applyPerformancePreset(Minecraft client, boolean potato) {
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
		client.options.save();

		if (potato) {
			config.applyPotatoDefaults();
		} else {
			config.applyLowEndDefaults();
		}
		config.initialVisibleDistance = Math.min(config.initialVisibleDistance, renderDistance);
		config.minVisibleDistance = Math.min(config.minVisibleDistance, config.initialVisibleDistance);
		config.simulationDistance = simulationDistance;
		config.sanitize();
		config.save();

		FPSMaxxGovernor.onAdaptiveToggled();
		FPSMaxxGovernor.applySimulationDistance(client, config);

		String preset = potato ? "potato" : "low-end";
		return "FPSMaxx " + preset + " preset applied (Fast graphics): render " + renderDistance
			+ " chunks, simulation " + simulationDistance
			+ ", target " + config.targetFps + " FPS"
			+ ", section budget " + config.sectionUpdateBudget
			+ ", particles " + config.particleCullDistance + " blocks / "
			+ config.maxParticles + " cap";
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
