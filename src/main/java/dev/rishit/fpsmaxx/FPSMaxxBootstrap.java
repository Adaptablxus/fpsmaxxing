package dev.rishit.fpsmaxx;

import dev.rishit.fpsmaxx.compat.FPSMaxxCompat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loader-neutral client startup and tick logic.
 *
 * <p>Fabric and NeoForge entrypoints only provide their loader's registration
 * APIs (key mapping, commands, tick event) and delegate the actual work here.</p>
 */
public final class FPSMaxxBootstrap {
	public static final Logger LOGGER = LoggerFactory.getLogger(FPSMaxxConstants.MOD_ID);

	private FPSMaxxBootstrap() {
	}

	public static void init() {
		FPSMaxxConfig.load();

		int safeDistance = FPSMaxxConstants.maxSafeRenderDistance();
		int safeSimulation = FPSMaxxConstants.maxSafeSimulationDistance();
		long heapMb = Runtime.getRuntime().maxMemory() / (1024L * 1024L);
		LOGGER.info(
			"FPSMaxx ready: render distance limit {} chunks (memory-safe {}), simulation {} chunks (memory-safe {}), target {} FPS, adaptive={}",
			FPSMaxxConstants.MAX_RENDER_DISTANCE,
			safeDistance,
			FPSMaxxConstants.MAX_SIMULATION_DISTANCE,
			safeSimulation,
			FPSMaxxConfig.get().targetFps,
			FPSMaxxConfig.get().adaptiveEnabled
		);
		LOGGER.info(
			"FPSMaxx low-end mode {}: target {} FPS, particle cull {} blocks, particle cap {} (adaptive {}), entity cull {} blocks, block-entity cull {} blocks, section budget {}",
			FPSMaxxConfig.get().lowEndMode ? "ON" : "OFF",
			FPSMaxxConfig.get().targetFps,
			FPSMaxxConfig.get().particleCullDistance,
			FPSMaxxConfig.get().maxParticles,
			FPSMaxxConfig.get().adaptiveParticles,
			FPSMaxxConfig.get().entityCullDistance,
			FPSMaxxConfig.get().blockEntityCullDistance,
			FPSMaxxConfig.get().sectionUpdateBudget
		);
		if (safeDistance < FPSMaxxConstants.MAX_RENDER_DISTANCE) {
			LOGGER.warn(
				"FPSMaxx: {} MB of heap can only load {} chunks safely. Allocate 4 GB or more to the instance for 64 chunks.",
				heapMb,
				safeDistance
			);
		}
		if (safeSimulation < FPSMaxxConfig.get().simulationDistance) {
			LOGGER.warn(
				"FPSMaxx: {} MB of heap can only simulate {} chunks safely; requested {} will be capped.",
				heapMb,
				safeSimulation,
				FPSMaxxConfig.get().simulationDistance
			);
		}
		LOGGER.info(
			"FPSMaxx compatibility: {} | adaptive mode {} (effective {})",
			FPSMaxxCompat.describe(),
			FPSMaxxConfig.get().adaptiveMode,
			FPSMaxxGovernor.effectiveMode()
		);
		if (FPSMaxxCompat.FRESH_AND_SMOOTH) {
			LOGGER.info(
				"FPSMaxx: Fresh & Smooth detected; visual-safe profile {} (shaders, particles and animations preserved)",
				FPSMaxxConfig.get().freshSmoothCompatibility ? "enabled" : "disabled (aggressive profile)"
			);
		}
		if (FPSMaxxCompat.ASYNC_PARTICLES || FPSMaxxCompat.PARTICLE_CORE) {
			LOGGER.info("FPSMaxx: dedicated particle optimizer detected; FPSMaxx particle culling is deferred to it");
		}
	}

	public static void onClientTick(Minecraft client, KeyMapping toggleKey) {
		if (toggleKey != null) {
			while (toggleKey.consumeClick()) {
				FPSMaxxConfig config = FPSMaxxConfig.get();
				config.adaptiveEnabled = !config.adaptiveEnabled;
				config.save();
				FPSMaxxGovernor.onAdaptiveToggled();

				if (client.player != null) {
					client.player.sendSystemMessage(
						Component.literal(FPSMaxxGovernor.statusLine(client)).withStyle(
							config.adaptiveEnabled ? ChatFormatting.GREEN : ChatFormatting.YELLOW
						)
					);
				}
			}
		}

		FPSMaxxGovernor.tick(client);
	}
}
