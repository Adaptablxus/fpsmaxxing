package dev.rishit.fpsmaxx.neoforge;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import dev.rishit.fpsmaxx.FPSMaxxConfig;
import dev.rishit.fpsmaxx.FPSMaxxConstants;
import dev.rishit.fpsmaxx.FPSMaxxGovernor;
import dev.rishit.fpsmaxx.FPSMaxxPresetScreen;
import dev.rishit.fpsmaxx.FPSMaxxPresets;
import dev.rishit.fpsmaxx.compat.FPSMaxxCompat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * NeoForge client command tree. Mirrors the Fabric command set; the only
 * difference is the source type and how feedback is sent.
 */
public final class FPSMaxxNeoForgeCommands {
	private FPSMaxxNeoForgeCommands() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(
			Commands.literal("fpsmaxx")
				.executes(ctx -> {
					sendStatus(ctx.getSource());
					return 1;
				})
				.then(Commands.literal("status").executes(ctx -> {
					sendStatus(ctx.getSource());
					return 1;
				}))
				.then(Commands.literal("preset").executes(ctx -> {
					Minecraft.getInstance().setScreenAndShow(new FPSMaxxPresetScreen(null));
					return 1;
				}))
				.then(Commands.literal("adaptive")
					.then(Commands.argument("enabled", BoolArgumentType.bool()).executes(ctx -> {
						FPSMaxxConfig config = FPSMaxxConfig.get();
						config.adaptiveEnabled = BoolArgumentType.getBool(ctx, "enabled");
						config.save();
						FPSMaxxGovernor.onAdaptiveToggled();
						feedback(ctx.getSource(), "FPSMaxx adaptive mode "
							+ (config.adaptiveEnabled ? "enabled" : "disabled"));
						return 1;
					})))
				.then(Commands.literal("target")
					.then(Commands.argument("fps", IntegerArgumentType.integer(30, 1000)).executes(ctx -> {
						FPSMaxxConfig config = FPSMaxxConfig.get();
						config.targetFps = IntegerArgumentType.getInteger(ctx, "fps");
						config.save();
						feedback(ctx.getSource(), "FPSMaxx target set to " + config.targetFps + " FPS");
						return 1;
					})))
				.then(Commands.literal("min")
					.then(Commands.argument("chunks", IntegerArgumentType.integer(2, FPSMaxxConstants.MAX_RENDER_DISTANCE)).executes(ctx -> {
						FPSMaxxConfig config = FPSMaxxConfig.get();
						config.minVisibleDistance = IntegerArgumentType.getInteger(ctx, "chunks");
						config.sanitize();
						config.save();
						feedback(ctx.getSource(), "FPSMaxx minimum visible distance set to "
							+ config.minVisibleDistance + " chunks");
						return 1;
					})))
				.then(Commands.literal("render")
					.then(Commands.argument("chunks", IntegerArgumentType.integer(2, FPSMaxxConstants.MAX_RENDER_DISTANCE)).executes(ctx -> {
						Minecraft client = Minecraft.getInstance();
						int chunks = IntegerArgumentType.getInteger(ctx, "chunks");
						client.options.renderDistance().set(chunks);
						client.options.save();
						FPSMaxxGovernor.setMaxRenderDistance(chunks);
						feedback(ctx.getSource(), "FPSMaxx render distance requested: " + chunks
							+ " chunks (effective " + client.options.getEffectiveRenderDistance() + ")");
						return 1;
					})))
				.then(Commands.literal("mode")
					.then(Commands.literal("auto").executes(ctx -> setMode(ctx.getSource(), "auto")))
					.then(Commands.literal("visible").executes(ctx -> setMode(ctx.getSource(), "visible")))
					.then(Commands.literal("render").executes(ctx -> setMode(ctx.getSource(), "render"))))
				.then(Commands.literal("simulation")
					.then(Commands.argument(
						"chunks",
						IntegerArgumentType.integer(
							FPSMaxxConstants.MIN_SIMULATION_DISTANCE,
							FPSMaxxConstants.MAX_SIMULATION_DISTANCE
						)
					).executes(ctx -> {
						Minecraft client = Minecraft.getInstance();
						FPSMaxxConfig config = FPSMaxxConfig.get();
						int requested = IntegerArgumentType.getInteger(ctx, "chunks");
						config.simulationDistance = requested;
						config.forceSimulationDistance = true;
						config.sanitize();
						config.save();
						FPSMaxxGovernor.applySimulationDistance(client, config);
						client.options.save();
						int applied = client.options.simulationDistance().get();
						feedback(ctx.getSource(), "FPSMaxx simulation distance requested: " + requested
							+ " chunks (effective " + applied
							+ ", memory-safe max " + FPSMaxxConstants.maxSafeSimulationDistance() + ")");
						return 1;
					})))
				.then(Commands.literal("entity")
					.then(Commands.argument("blocks", IntegerArgumentType.integer(0, 512)).executes(ctx -> {
						FPSMaxxConfig config = FPSMaxxConfig.get();
						config.entityCullDistance = IntegerArgumentType.getInteger(ctx, "blocks");
						config.save();
						feedback(ctx.getSource(), "FPSMaxx entity cull distance set to "
							+ config.entityCullDistance + " blocks (0 = off)");
						return 1;
					})))
				.then(Commands.literal("blockentity")
					.then(Commands.argument("blocks", IntegerArgumentType.integer(0, 512)).executes(ctx -> {
						FPSMaxxConfig config = FPSMaxxConfig.get();
						config.blockEntityCullDistance = IntegerArgumentType.getInteger(ctx, "blocks");
						config.save();
						feedback(ctx.getSource(), "FPSMaxx block entity cull distance set to "
							+ config.blockEntityCullDistance + " blocks (0 = off)");
						return 1;
					})))
				.then(Commands.literal("particles")
					.then(Commands.argument("blocks", IntegerArgumentType.integer(0, 512)).executes(ctx -> {
						FPSMaxxConfig config = FPSMaxxConfig.get();
						config.particleCullDistance = IntegerArgumentType.getInteger(ctx, "blocks");
						config.save();
						feedback(ctx.getSource(), "FPSMaxx particle cull distance set to "
							+ config.particleCullDistance + " blocks (0 = off)");
						return 1;
					})))
				.then(Commands.literal("maxparticles")
					.then(Commands.argument("count", IntegerArgumentType.integer(0, 20000)).executes(ctx -> {
						FPSMaxxConfig config = FPSMaxxConfig.get();
						config.maxParticles = IntegerArgumentType.getInteger(ctx, "count");
						config.sanitize();
						config.save();
						feedback(ctx.getSource(), "FPSMaxx particle cap set to " + config.maxParticles
							+ " (0 = vanilla limits only)");
						return 1;
					})))
				.then(Commands.literal("lowend").executes(ctx -> {
					feedback(ctx.getSource(), FPSMaxxPresets.applyLowEnd(Minecraft.getInstance()));
					return 1;
				}))
				.then(Commands.literal("potato").executes(ctx -> {
					feedback(ctx.getSource(), FPSMaxxPresets.applyPotato(Minecraft.getInstance()));
					return 1;
				}))
				.then(Commands.literal("freshsmooth").executes(ctx -> {
					feedback(ctx.getSource(), FPSMaxxPresets.applyFreshSmooth(Minecraft.getInstance(), null));
					return 1;
				}))
				.then(Commands.literal("aggressive").executes(ctx -> {
					feedback(ctx.getSource(), FPSMaxxPresets.applyAggressiveLowEnd(Minecraft.getInstance()));
					return 1;
				}))
				.then(Commands.literal("visual")
					.then(Commands.argument("enabled", BoolArgumentType.bool()).executes(ctx -> {
						FPSMaxxConfig config = FPSMaxxConfig.get();
						config.freshSmoothCompatibility = BoolArgumentType.getBool(ctx, "enabled");
						config.save();
						feedback(ctx.getSource(), "FPSMaxx Fresh & Smooth compatibility "
							+ (config.freshSmoothCompatibility ? "enabled" : "disabled")
							+ " (detected: " + (FPSMaxxCompat.FRESH_AND_SMOOTH ? "yes" : "no") + ")");
						return 1;
					})))
				.then(Commands.literal("sectionbudget")
					.then(Commands.argument("sections", IntegerArgumentType.integer(0, 512)).executes(ctx -> {
						FPSMaxxConfig config = FPSMaxxConfig.get();
						config.sectionUpdateBudget = IntegerArgumentType.getInteger(ctx, "sections");
						config.save();
						feedback(ctx.getSource(), "FPSMaxx section update budget set to "
							+ config.sectionUpdateBudget + " per frame (0 = vanilla/unlimited)");
						return 1;
					})))
				.then(Commands.literal("balanced").executes(ctx -> {
					feedback(ctx.getSource(), FPSMaxxPresets.applyBalanced(Minecraft.getInstance()));
					return 1;
				}))
				.then(Commands.literal("reload").executes(ctx -> {
					FPSMaxxConfig.load();
					FPSMaxxGovernor.onAdaptiveToggled();
					FPSMaxxGovernor.applySimulationDistance(Minecraft.getInstance(), FPSMaxxConfig.get());
					feedback(ctx.getSource(), "FPSMaxx config reloaded");
					return 1;
				}))
		);
	}

	private static int setMode(CommandSourceStack source, String mode) {
		FPSMaxxConfig config = FPSMaxxConfig.get();
		config.adaptiveMode = mode;
		config.save();
		FPSMaxxGovernor.onAdaptiveToggled();
		feedback(source, "FPSMaxx adaptive mode set to " + mode
			+ " (effective " + FPSMaxxGovernor.effectiveMode() + ")");
		return 1;
	}

	private static void sendStatus(CommandSourceStack source) {
		FPSMaxxConfig config = FPSMaxxConfig.get();
		Minecraft client = Minecraft.getInstance();
		feedback(source, FPSMaxxGovernor.statusLine(client));
		String sodium = !FPSMaxxCompat.SODIUM
			? "absent"
			: "terrain " + (FPSMaxxCompat.sodiumTerrainHook() ? "hooked" : "no-hook")
				+ ", fog " + (FPSMaxxCompat.sodiumFogHook() ? "hooked" : "no-hook")
				+ ", config " + (FPSMaxxCompat.sodiumConfigHook() ? "hooked" : "no-hook");
		source.sendSystemMessage(Component.literal(
			"mode " + FPSMaxxGovernor.effectiveMode()
				+ " | low-end " + (config.lowEndMode ? "on" : "off")
				+ " | profile " + (FPSMaxxPresets.useFreshSmoothProfile()
					? "Fresh & Smooth safe"
					: config.lowEndMode ? "aggressive" : "balanced")
				+ " | graphics " + (config.graphicsPresetFpsMaxx
					? "FpsMaxx"
					: client.options.graphicsPreset().get().getSerializedName())
				+ " | max slider " + FPSMaxxConstants.MAX_RENDER_DISTANCE
				+ " | min " + config.minVisibleDistance
				+ " | sim " + config.simulationDistance
				+ (config.forceSimulationDistance ? " (forced)" : " (manual)")
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
					: " (vanilla)")
				+ " | sodium " + sodium
		).withStyle(ChatFormatting.GRAY));
	}

	private static void feedback(CommandSourceStack source, String message) {
		source.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.GREEN));
	}
}
