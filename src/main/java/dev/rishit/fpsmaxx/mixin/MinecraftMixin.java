package dev.rishit.fpsmaxx.mixin;

import dev.rishit.fpsmaxx.FPSMaxxMetrics;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Samples every rendered frame for the status readout and the benchmark.
 * HEAD is used instead of RETURN because {@code runTick} has several early
 * exits; a frame is counted exactly once even when one of them fires.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	@Inject(method = "runTick", at = @At("HEAD"), require = 1)
	private void fpsmaxx$recordFrame(boolean renderLevel, CallbackInfo ci) {
		if (renderLevel) {
			FPSMaxxMetrics.recordFrame();
		}
	}
}
