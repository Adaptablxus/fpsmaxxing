package dev.rishit.fpsmaxx;

import dev.rishit.fpsmaxx.compat.FPSMaxxCompat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared status and chat formatting so the Fabric and NeoForge command trees
 * report exactly the same information.
 */
public final class FPSMaxxStatus {
	private FPSMaxxStatus() {
	}

	public static List<String> lines(Minecraft client) {
		FPSMaxxConfig config = FPSMaxxConfig.get();
		List<String> lines = new ArrayList<>(8);
		lines.add(FPSMaxxGovernor.statusLine(client));

		String sodium = !FPSMaxxCompat.SODIUM
			? "absent"
			: "terrain " + (FPSMaxxCompat.sodiumTerrainHook() ? "hooked" : "no-hook")
				+ ", fog " + (FPSMaxxCompat.sodiumFogHook() ? "hooked" : "no-hook")
				+ ", config " + (FPSMaxxCompat.sodiumConfigHook() ? "hooked" : "no-hook");

		lines.add("mode " + FPSMaxxGovernor.effectiveMode()
			+ " | low-end " + (config.lowEndMode ? "on" : "off")
			+ " | profile " + (FPSMaxxPresets.useFreshSmoothProfile()
				? "Fresh & Smooth safe"
				: config.lowEndMode ? "aggressive" : "balanced")
			+ " | graphics " + (config.graphicsPresetFpsMaxx
				? "FpsMaxx"
				: client.options.graphicsPreset().get().getSerializedName())
			+ " | max slider " + FPSMaxxConstants.MAX_RENDER_DISTANCE
			+ " | min " + config.minVisibleDistance
			+ " | adaptive sim " + (config.adaptiveSimulation ? "on" : "off")
			+ " (floor " + config.minSimulationDistance + ")");

		lines.add("sim " + client.options.simulationDistance().get()
			+ (config.forceSimulationDistance
				? " chunks (request " + config.simulationDistance + ")"
				: " chunks (manual)")
			+ " | entity cull " + config.entityCullDistance
			+ " | block entity cull " + config.blockEntityCullDistance
			+ " | particles " + (FPSMaxxCompat.ASYNC_PARTICLES
				? "AsyncParticles"
				: config.particleCullDistance + " blocks / " + config.maxParticles + " cap"
					+ (config.adaptiveParticles
						? " (" + Math.round(FPSMaxxGovernor.particleScale() * 100.0F) + "%)"
						: ""))
			+ " | section budget " + config.sectionUpdateBudget
			+ (config.sectionUpdateBudget > 0
				? " (now " + FPSMaxxGovernor.sectionUpdateBudget() + ")"
				: " (vanilla)"));

		lines.add("sodium " + sodium);
		lines.add(FPSMaxxHardware.describe());
		lines.add(FPSMaxxMetrics.summaryLine());

		String benchmark = FPSMaxxMetrics.benchmarkStatus();
		if (benchmark != null) {
			lines.add(benchmark);
		}
		return lines;
	}

	public static List<String> hardwareLines(Minecraft client) {
		FPSMaxxConfig config = FPSMaxxConfig.get();
		List<String> lines = new ArrayList<>(4);
		lines.add(FPSMaxxHardware.describe());
		lines.add("auto-detect " + (config.autoDetectHardware ? "on" : "off")
			+ " | detected tier " + config.detectedHardware
			+ " | first-run profile " + (FPSMaxxConfig.shouldAutoDetectHardware() ? "pending" : "applied"));
		lines.add("RAM " + FPSMaxxHardware.totalRamMb() + " MB"
			+ " | heap " + (Runtime.getRuntime().maxMemory() / (1024L * 1024L)) + " MB"
			+ " | CPU threads " + Runtime.getRuntime().availableProcessors()
			+ " | Android " + (FPSMaxxHardware.isAndroid() ? "yes" : "no"));
		return lines;
	}

	public static void sendChat(Minecraft client, String message) {
		sendChat(client, message, ChatFormatting.GREEN);
	}

	public static void sendChat(Minecraft client, String message, ChatFormatting color) {
		Component component = Component.literal(message).withStyle(color);
		if (client.player != null) {
			client.player.sendSystemMessage(component);
		} else if (client.gui != null) {
			client.showDebugChat(component);
		}
	}
}
