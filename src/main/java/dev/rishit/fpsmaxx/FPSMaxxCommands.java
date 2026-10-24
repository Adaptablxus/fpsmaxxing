package dev.rishit.fpsmaxx;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import dev.rishit.fpsmaxx.compat.FPSMaxxCompat;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * In-game tuning without editing the JSON file by hand.
 * Everything is client-side, so these commands never reach the server.
 */
public final class FPSMaxxCommands {
	private FPSMaxxCommands() {
	}

	public static void register() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
			ClientCommands.literal("fpsmaxx")
				.executes(ctx -> {
					sendStatus(ctx.getSource());
					return 1;
				})
				.then(ClientCommands.literal("status").executes(ctx -> {
					sendStatus(ctx.getSource());
					return 1;
				}))
				.then(ClientCommands.literal("preset").executes(ctx -> {
					Minecraft client = ctx.getSource().getClient();
					client.setScreenAndShow(new FPSMaxxPresetScreen(null));
					return 1;
				}))
				.then(ClientCommands.literal("adaptive")
					.then(ClientCommands.argument("enabled", BoolArgumentType.bool()).executes(ctx -> {
						FPSMaxxConfig config = FPSMaxxConfig.get();
						config.adaptiveEnabled = BoolArgumentType.getBool(ctx, "enabled");
						config.save();
						FPSMaxxGovernor.onAdaptiveToggled();
						ctx.getSource().sendFeedback(Component.literal(
							"FPSMaxx adaptive mode " + (config.adaptiveEnabled ? "enabled" : "disabled")
						).withStyle(ChatFormatting.GREEN));
						return 1;
					})))
				.then(ClientCommands.literal("target")
					.then(ClientCommands.argument("fps", IntegerArgumentType.integer(30, 1000)).executes(ctx -> {
						FPSMaxxConfig config = FPSMaxxConfig.get();
						config.targetFps = IntegerArgumentType.getInteger(ctx, "fps");
						config.save();
						ctx.getSource().sendFeedback(Component.literal(
							"FPSMaxx target set to " + config.targetFps + " FPS"
						).withStyle(ChatFormatting.GREEN));
						return 1;
					})))
				.then(ClientCommands.literal("min")
					.then(ClientCommands.argument("chunks", IntegerArgumentType.integer(2, FPSMaxxConstants.MAX_RENDER_DISTANCE)).executes(ctx -> {
						FPSMaxxConfig config = FPSMaxxConfig.get();
						config.minVisibleDistance = IntegerArgumentType.getInteger(ctx, "chunks");
						config.sanitize();
						config.save();
						ctx.getSource().sendFeedback(Component.literal(
							"FPSMaxx minimum visible distance set to " + config.minVisibleDistance + " chunks"
						).withStyle(ChatFormatting.GREEN));
						return 1;
					})))
				.then(ClientCommands.literal("render")
					.then(ClientCommands.argument("chunks", IntegerArgumentType.integer(2, FPSMaxxConstants.MAX_RENDER_DISTANCE)).executes(ctx -> {
						Minecraft client = ctx.getSource().getClient();
						int chunks = IntegerArgumentType.getInteger(ctx, "chunks");
						client.options.renderDistance().set(chunks);
						client.options.save();
						FPSMaxxGovernor.setMaxRenderDistance(chunks);
						ctx.getSource().sendFeedback(Component.literal(
							"FPSMaxx render distance requested: " + chunks + " chunks (effective "
								+ client.options.getEffectiveRenderDistance() + ")"
						).withStyle(ChatFormatting.GREEN));
						return 1;
					})))
				.then(ClientCommands.literal("mode")
					.then(ClientCommands.literal("auto").executes(ctx -> setMode(ctx.getSource(), "auto")))
					.then(ClientCommands.literal("visible").executes(ctx -> setMode(ctx.getSource(), "visible")))
					.then(ClientCommands.literal("render").executes(ctx -> setMode(ctx.getSource(), "render"))))
				.then(ClientCommands.literal("simulation")
					.then(ClientCommands.argument(
						"chunks",
						IntegerArgumentType.integer(
							FPSMaxxConstants.MIN_SIMULATION_DISTANCE,
							FPSMaxxConstants.MAX_SIMULATION_DISTANCE
						)
					).executes(ctx -> {
						Minecraft client = ctx.getSource().getClient();
						FPSMaxxConfig config = FPSMaxxConfig.get();
						int requested = IntegerArgumentType.getInteger(ctx, "chunks");
						config.simulationDistance = requested;
						config.forceSimulationDistance = true;
						config.sanitize();
						config.save();
						FPSMaxxGovernor.applySimulationDistance(client, config);
						client.options.save();
						int applied = client.options.simulationDistance().get();
						ctx.getSource().sendFeedback(Component.literal(
							"FPSMaxx simulation distance requested: " + requested
								+ " chunks (effective " + applied
								+ ", memory-safe max " + FPSMaxxConstants.maxSafeSimulationDistance() + ")"
						).withStyle(ChatFormatting.GREEN));
						return 1;
					})))
				.then(ClientCommands.literal("entity")
					.then(ClientCommands.argument("blocks", IntegerArgumentType.integer(0, 512)).executes(ctx -> {
						FPSMaxxConfig config = FPSMaxxConfig.get();
						config.entityCullDistance = IntegerArgumentType.getInteger(ctx, "blocks");
						config.save();
						ctx.getSource().sendFeedback(Component.literal(
							"FPSMaxx entity cull distance set to " + config.entityCullDistance + " blocks (0 = off)"
						).withStyle(ChatFormatting.GREEN));
						return 1;
					})))
				.then(ClientCommands.literal("blockentity")
					.then(ClientCommands.argument("blocks", IntegerArgumentType.integer(0, 512)).executes(ctx -> {
						FPSMaxxConfig config = FPSMaxxConfig.get();
						config.blockEntityCullDistance = IntegerArgumentType.getInteger(ctx, "blocks");
						config.save();
						ctx.getSource().sendFeedback(Component.literal(
							"FPSMaxx block entity cull distance set to " + config.blockEntityCullDistance + " blocks (0 = off)"
						).withStyle(ChatFormatting.GREEN));
						return 1;
					})))
				.then(ClientCommands.literal("particles")
					.then(ClientCommands.argument("blocks", IntegerArgumentType.integer(0, 512)).executes(ctx -> {
						FPSMaxxConfig config = FPSMaxxConfig.get();
						config.particleCullDistance = IntegerArgumentType.getInteger(ctx, "blocks");
						config.save();
						ctx.getSource().sendFeedback(Component.literal(
							"FPSMaxx particle cull distance set to " + config.particleCullDistance + " blocks (0 = off)"
						).withStyle(ChatFormatting.GREEN));
						return 1;
					})))
				.then(ClientCommands.literal("maxparticles")
					.then(ClientCommands.argument("count", IntegerArgumentType.integer(0, 20000)).executes(ctx -> {
						FPSMaxxConfig config = FPSMaxxConfig.get();
						config.maxParticles = IntegerArgumentType.getInteger(ctx, "count");
						config.sanitize();
						config.save();
						ctx.getSource().sendFeedback(Component.literal(
							"FPSMaxx particle cap set to " + config.maxParticles + " (0 = vanilla limits only)"
						).withStyle(ChatFormatting.GREEN));
						return 1;
					})))
				.then(ClientCommands.literal("lowend").executes(ctx -> {
					ctx.getSource().sendFeedback(Component.literal(
						FPSMaxxPresets.applyLowEnd(ctx.getSource().getClient())
					).withStyle(ChatFormatting.GREEN));
					return 1;
				}))
				.then(ClientCommands.literal("potato").executes(ctx -> {
					ctx.getSource().sendFeedback(Component.literal(
						FPSMaxxPresets.applyPotato(ctx.getSource().getClient())
					).withStyle(ChatFormatting.GREEN));
					return 1;
				}))
				.then(ClientCommands.literal("freshsmooth").executes(ctx -> {
					ctx.getSource().sendFeedback(Component.literal(
						FPSMaxxPresets.applyFreshSmooth(ctx.getSource().getClient(), null)
					).withStyle(ChatFormatting.GREEN));
					return 1;
				}))
				.then(ClientCommands.literal("aggressive").executes(ctx -> {
					ctx.getSource().sendFeedback(Component.literal(
						FPSMaxxPresets.applyAggressiveLowEnd(ctx.getSource().getClient())
					).withStyle(ChatFormatting.GREEN));
					return 1;
				}))
				.then(ClientCommands.literal("visual")
					.then(ClientCommands.argument("enabled", BoolArgumentType.bool()).executes(ctx -> {
						FPSMaxxConfig config = FPSMaxxConfig.get();
						config.freshSmoothCompatibility = BoolArgumentType.getBool(ctx, "enabled");
						config.save();
						ctx.getSource().sendFeedback(Component.literal(
							"FPSMaxx Fresh & Smooth compatibility "
								+ (config.freshSmoothCompatibility ? "enabled" : "disabled")
								+ " (detected: " + (FPSMaxxCompat.FRESH_AND_SMOOTH ? "yes" : "no") + ")"
						).withStyle(ChatFormatting.GREEN));
						return 1;
					})))
				.then(ClientCommands.literal("sectionbudget")
					.then(ClientCommands.argument("sections", IntegerArgumentType.integer(0, 512)).executes(ctx -> {
						FPSMaxxConfig config = FPSMaxxConfig.get();
						config.sectionUpdateBudget = IntegerArgumentType.getInteger(ctx, "sections");
						config.save();
						ctx.getSource().sendFeedback(Component.literal(
							"FPSMaxx section update budget set to " + config.sectionUpdateBudget
								+ " per frame (0 = vanilla/unlimited)"
						).withStyle(ChatFormatting.GREEN));
						return 1;
					})))
				.then(ClientCommands.literal("balanced").executes(ctx -> {
					ctx.getSource().sendFeedback(Component.literal(
						FPSMaxxPresets.applyBalanced(ctx.getSource().getClient())
					).withStyle(ChatFormatting.GREEN));
					return 1;
				}))
				.then(ClientCommands.literal("reload").executes(ctx -> {
					FPSMaxxConfig.load();
					FPSMaxxGovernor.onAdaptiveToggled();
					FPSMaxxGovernor.applySimulationDistance(ctx.getSource().getClient(), FPSMaxxConfig.get());
					ctx.getSource().sendFeedback(Component.literal("FPSMaxx config reloaded").withStyle(ChatFormatting.GREEN));
					return 1;
				}))
		));
	}

	private static int setMode(
		net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource source,
		String mode
	) {
		FPSMaxxConfig config = FPSMaxxConfig.get();
		config.adaptiveMode = mode;
		config.save();
		FPSMaxxGovernor.onAdaptiveToggled();
		source.sendFeedback(Component.literal(
			"FPSMaxx adaptive mode set to " + mode
				+ " (effective " + FPSMaxxGovernor.effectiveMode() + ")"
		).withStyle(ChatFormatting.GREEN));
		return 1;
	}

	private static void sendStatus(net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource source) {
		FPSMaxxConfig config = FPSMaxxConfig.get();
		source.sendFeedback(Component.literal(FPSMaxxGovernor.statusLine(source.getClient())).withStyle(ChatFormatting.GREEN));
		String sodium = !FPSMaxxCompat.SODIUM
			? "absent"
			: "terrain " + (FPSMaxxCompat.sodiumTerrainHook() ? "hooked" : "no-hook")
				+ ", fog " + (FPSMaxxCompat.sodiumFogHook() ? "hooked" : "no-hook")
				+ ", config " + (FPSMaxxCompat.sodiumConfigHook() ? "hooked" : "no-hook");
		source.sendFeedback(Component.literal(
			"mode " + FPSMaxxGovernor.effectiveMode()
				+ " | low-end " + (config.lowEndMode ? "on" : "off")
				+ " | profile " + (FPSMaxxPresets.useFreshSmoothProfile()
					? "Fresh & Smooth safe"
					: config.lowEndMode ? "aggressive" : "balanced")
				+ " | graphics " + (config.graphicsPresetFpsMaxx
					? "FpsMaxx"
					: source.getClient().options.graphicsPreset().get().getSerializedName())
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
}
