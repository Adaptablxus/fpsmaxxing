package dev.rishit.fpsmaxx;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ParticleStatus;

/**
 * One-shot presets. The mod-side preset only changes FPSMaxx settings; the
 * low-end preset also applies vanilla's Fast graphics preset and a few extra
 * reductions so a weak GPU gets immediate relief.
 */
public final class FPSMaxxPresets {
	private FPSMaxxPresets() {
	}

	public static String applyLowEnd(Minecraft client) {
		FPSMaxxConfig config = FPSMaxxConfig.get();

		int previousRender = client.options.renderDistance().get();
		int previousSimulation = client.options.simulationDistance().get();

		// Fast is vanilla's own low-end profile. Apply it first, then keep the
		// render distance capped and restore a little headroom for the governor.
		client.options.applyGraphicsPreset(GraphicsPreset.FAST);

		int renderDistance = Math.max(
			2,
			Math.min(FPSMaxxConstants.LOW_END_RENDER_DISTANCE, previousRender)
		);
		int simulationDistance = Math.max(
			FPSMaxxConstants.MIN_SIMULATION_DISTANCE,
			Math.min(FPSMaxxConstants.LOW_END_SIMULATION_DISTANCE, previousSimulation)
		);

		client.options.renderDistance().set(renderDistance);
		client.options.simulationDistance().set(simulationDistance);
		client.options.cloudStatus().set(CloudStatus.OFF);
		client.options.particles().set(ParticleStatus.MINIMAL);
		client.options.entityShadows().set(false);
		client.options.ambientOcclusion().set(false);
		client.options.biomeBlendRadius().set(0);
		client.options.weatherRadius().set(5);
		client.options.entityDistanceScaling().set(0.5D);
		client.options.cutoutLeaves().set(true);
		client.options.framerateLimit().set(60);
		client.options.save();

		config.applyLowEndDefaults();
		config.initialVisibleDistance = Math.min(config.initialVisibleDistance, renderDistance);
		config.minVisibleDistance = Math.min(config.minVisibleDistance, config.initialVisibleDistance);
		config.simulationDistance = simulationDistance;
		config.sanitize();
		config.save();

		FPSMaxxGovernor.onAdaptiveToggled();
		FPSMaxxGovernor.applySimulationDistance(client, config);

		return "FPSMaxx low-end preset applied (Fast graphics): render " + renderDistance
			+ " chunks, simulation " + simulationDistance
			+ ", target " + config.targetFps + " FPS"
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
