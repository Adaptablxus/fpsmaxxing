package dev.rishit.fpsmaxx.mixin;

import dev.rishit.fpsmaxx.FPSMaxxGovernor;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkTrackingView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The occlusion graph decides which chunk sections are eligible for rendering.
 * Capping the distance here keeps chunks loaded (so movement never hitches)
 * while only drawing the radius the FPS governor allows.
 */
@Mixin(SectionOcclusionGraph.class)
public abstract class SectionOcclusionGraphMixin {
	@Shadow
	private ViewArea viewArea;

	@Inject(method = "isInViewDistance", at = @At("HEAD"), cancellable = true)
	private void fpsmaxx$limitVisibleDistance(long from, long to, CallbackInfoReturnable<Boolean> cir) {
		int cap = FPSMaxxGovernor.visibleDistanceChunks();
		if (cap <= 0 || this.viewArea == null) {
			return;
		}

		SectionPos center = this.viewArea.getCameraSectionPos();
		if (!ChunkTrackingView.isInViewDistance(
			center.x(), center.z(), cap, SectionPos.x(to), SectionPos.z(to)
		)) {
			cir.setReturnValue(false);
		}
	}
}
