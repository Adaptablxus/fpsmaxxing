package dev.rishit.fpsmaxx.mixin;

import dev.rishit.fpsmaxx.FPSMaxxConfig;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Chests, signs, banners and friends are render-heavy. Skip extracting ones
 * that are far outside the visible area. Globally rendered block entities
 * (beacon beams and similar) are left alone.
 */
@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {
	@Shadow
	private Vec3 cameraPos;

	@Inject(method = "tryExtractRenderState", at = @At("HEAD"), cancellable = true)
	private void fpsmaxx$cullDistantBlockEntities(
		BlockEntity blockEntity,
		float partialTicks,
		ModelFeatureRenderer.CrumblingOverlay breakProgress,
		boolean isGloballyRendered,
		CallbackInfoReturnable<BlockEntityRenderState> cir
	) {
		if (isGloballyRendered || this.cameraPos == null) {
			return;
		}

		int cullDistance = FPSMaxxConfig.get().blockEntityCullDistance;
		if (cullDistance <= 0) {
			return;
		}

		Vec3 pos = Vec3.atCenterOf(blockEntity.getBlockPos());
		if (pos.distanceToSqr(this.cameraPos) > (double) cullDistance * cullDistance) {
			cir.setReturnValue(null);
		}
	}
}
