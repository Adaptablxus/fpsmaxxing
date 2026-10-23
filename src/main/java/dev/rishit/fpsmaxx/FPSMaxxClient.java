package dev.rishit.fpsmaxx;

import dev.rishit.fpsmaxx.compat.FPSMaxxCompat;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FPSMaxxClient implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger(FPSMaxxConstants.MOD_ID);

	private static KeyMapping toggleKey;

	@Override
	public void onInitializeClient() {
		FPSMaxxConfig.load();
		FPSMaxxCommands.register();

		toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.fpsmaxx.toggle",
			GLFW.GLFW_KEY_F6,
			KeyMapping.Category.MISC
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
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

			FPSMaxxGovernor.tick(client);
		});

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
	}
}
