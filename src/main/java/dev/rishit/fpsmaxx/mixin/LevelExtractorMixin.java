package dev.rishit.fpsmaxx.mixin;

import dev.rishit.fpsmaxx.FPSMaxxConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Entities are expensive to extract and submit. At 64 chunks the server keeps
 * far more of them around, so anything past the configured distance is skipped.
 */
@Mixin(LevelExtractor.class)
public abstract class LevelExtractorMixin {
	@Inject(method = "isEntityVisible", at = @At("RETURN"), cancellable = true)
	private void fpsmaxx$cullDistantEntities(
		Entity entity, Frustum frustum, double camX, double camY, double camZ, CallbackInfoReturnable<Boolean> cir
	) {
		if (!cir.getReturnValue()) {
			return;
		}

		int cullDistance = FPSMaxxConfig.get().entityCullDistance;
		if (cullDistance <= 0) {
			return;
		}

		Entity cameraEntity = Minecraft.getInstance().getCameraEntity();
		if (entity == cameraEntity) {
			return;
		}

		double dx = entity.getX() - camX;
		double dy = entity.getY() - camY;
		double dz = entity.getZ() - camZ;
		double limit = (double) cullDistance * cullDistance;
		if (dx * dx + dy * dy + dz * dz > limit) {
			cir.setReturnValue(false);
		}
	}
}
