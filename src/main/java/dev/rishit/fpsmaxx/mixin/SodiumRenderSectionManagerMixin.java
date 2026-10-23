package dev.rishit.fpsmaxx.mixin;

import dev.rishit.fpsmaxx.FPSMaxxConfig;
import dev.rishit.fpsmaxx.FPSMaxxGovernor;
import dev.rishit.fpsmaxx.compat.FPSMaxxCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sodium does not use vanilla's SectionOcclusionGraph. Its chunk culling uses
 * RenderSectionManager#getRenderDistance() (in blocks), so capping that value
 * gives Sodium the same adaptive visible radius without unloading chunks.
 *
 * @Pseudo keeps the mixin harmless when Sodium is not installed.
 */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager")
public abstract class SodiumRenderSectionManagerMixin {
	@Inject(
		method = "getRenderDistance",
		at = @At("RETURN"),
		cancellable = true,
		remap = false,
		require = 0
	)
	private void fpsmaxx$capSodiumVisibleDistance(CallbackInfoReturnable<Float> cir) {
		FPSMaxxCompat.markSodiumTerrainHook();

		FPSMaxxConfig config = FPSMaxxConfig.get();
		if (!config.sodiumIntegration) {
			return;
		}

		int cap = FPSMaxxGovernor.visibleDistanceChunks();
		if (cap <= 0) {
			return;
		}

		float capBlocks = cap * 16.0F;
		if (cir.getReturnValueF() > capBlocks) {
			FPSMaxxCompat.markSodiumTerrainCap(cap);
			cir.setReturnValue(capBlocks);
		}
	}
}
