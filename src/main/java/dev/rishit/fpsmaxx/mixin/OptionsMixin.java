package dev.rishit.fpsmaxx.mixin;

import dev.rishit.fpsmaxx.FPSMaxxConstants;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Vanilla caps the render-distance option at 32 chunks and, on low-memory
 * devices, the simulation-distance option at 16. Only the maximum of each
 * IntRange is changed, so the original option instances (and their
 * "graphics preset -> custom" listeners) stay intact.
 *
 * Ordinal 3 is the render-distance range and ordinal 4 is the simulation
 * distance range in Options' constructor for 26.2.
 */
@Mixin(Options.class)
public abstract class OptionsMixin {
	@ModifyArg(
		method = "<init>",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/OptionInstance$IntRange;<init>(IIZ)V",
			ordinal = 3
		),
		index = 1
	)
	private int fpsmaxx$widenRenderDistanceLimit(int vanillaMaximum) {
		return FPSMaxxConstants.MAX_RENDER_DISTANCE;
	}

	/**
	 * On low-memory devices vanilla caps simulation distance at 16. The mod keeps
	 * the requested 32 available; the memory guard applies the safe value when a
	 * world is entered.
	 */
	@ModifyArg(
		method = "<init>",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/OptionInstance$IntRange;<init>(IIZ)V",
			ordinal = 4
		),
		index = 1
	)
	private int fpsmaxx$widenSimulationDistanceLimit(int vanillaMaximum) {
		return FPSMaxxConstants.MAX_SIMULATION_DISTANCE;
	}
}
