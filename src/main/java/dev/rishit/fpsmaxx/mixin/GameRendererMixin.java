package dev.rishit.fpsmaxx.mixin;

import dev.rishit.fpsmaxx.FPSMaxxConfig;
import dev.rishit.fpsmaxx.FPSMaxxDynamicResolution;
import dev.rishit.fpsmaxx.FPSMaxxGovernor;
import dev.rishit.fpsmaxx.compat.FPSMaxxCompat;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fog uses the visible radius instead of the loaded radius, so the edge of the
 * governed view distance stays hidden as the governor changes it.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@ModifyArg(
		method = "extractCamera",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/fog/FogRenderer;setupFog(Lnet/minecraft/client/Camera;ILnet/minecraft/client/DeltaTracker;FLnet/minecraft/client/multiplayer/ClientLevel;)Lnet/minecraft/client/renderer/fog/FogData;"
		),
		index = 1
	)
	private int fpsmaxx$fogMatchesVisibleDistance(int vanillaRenderDistance) {
		// Iris shader packs render their own atmospheric fog. Changing the
		// vanilla distance underneath them fights the pack and can produce a
		// visible seam when the governor shrinks the radius.
		if (FPSMaxxCompat.IRIS || !FPSMaxxConfig.get().matchFogToVisibleDistance) {
			return vanillaRenderDistance;
		}
		return FPSMaxxGovernor.fogDistanceChunks(vanillaRenderDistance);
	}

	@Inject(method = "renderLevel", at = @At("HEAD"))
	private void fpsmaxx$beginDynamicResolution(CallbackInfo ci) {
		FPSMaxxDynamicResolution.beginFrame((GameRenderer) (Object) this);
	}

	@Inject(method = "renderLevel", at = @At("RETURN"))
	private void fpsmaxx$endDynamicResolution(CallbackInfo ci) {
		FPSMaxxDynamicResolution.endFrame((GameRenderer) (Object) this);
	}
}
