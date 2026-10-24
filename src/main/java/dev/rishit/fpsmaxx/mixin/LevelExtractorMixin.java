package dev.rishit.fpsmaxx.mixin;

import dev.rishit.fpsmaxx.FPSMaxxConfig;
import dev.rishit.fpsmaxx.FPSMaxxGovernor;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.SectionUpdateTracker;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.level.SectionUpdateRenderState;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Comparator;
import java.util.List;

/**
 * Entities are expensive to extract and submit. At 64 chunks the server keeps
 * far more of them around, so anything past the configured distance is skipped.
 *
 * The same extractor also builds the list of dirty chunk sections for this
 * frame. Capping that list smooths the short freeze that happens when a large
 * batch of sections becomes dirty at once. Skipped sections are marked dirty
 * again and retried next frame, nearest first.
 */
@Mixin(LevelExtractor.class)
public abstract class LevelExtractorMixin {
	@Shadow
	private SectionUpdateTracker sectionUpdateTracker;

	@Shadow
	private LevelRenderState levelRenderState;

	@Inject(method = "extract", at = @At("TAIL"), require = 1)
	private void fpsmaxx$budgetSectionUpdates(
		DeltaTracker deltaTracker,
		Camera camera,
		float partialTick,
		CallbackInfo ci
	) {
		int budget = FPSMaxxGovernor.sectionUpdateBudget();
		if (budget <= 0 || this.sectionUpdateTracker == null || this.levelRenderState == null) {
			return;
		}

		List<SectionUpdateRenderState> states = this.levelRenderState.sectionUpdateRenderStates;
		int size = states.size();
		if (size <= budget) {
			return;
		}

		Vec3 cameraPos = camera.position();
		states.sort(Comparator.comparingDouble(state -> fpsmaxx$distanceSq(state.sectionNode(), cameraPos)));

		for (int i = budget; i < size; i++) {
			SectionUpdateRenderState state = states.get(i);
			long node = state.sectionNode();
			this.sectionUpdateTracker.setDirty(
				SectionPos.x(node),
				SectionPos.y(node),
				SectionPos.z(node),
				state.playerChanged()
			);
		}
		states.subList(budget, size).clear();
	}

	private static double fpsmaxx$distanceSq(long node, Vec3 cameraPos) {
		double dx = SectionPos.x(node) * 16.0D + 8.0D - cameraPos.x;
		double dy = SectionPos.y(node) * 16.0D + 8.0D - cameraPos.y;
		double dz = SectionPos.z(node) * 16.0D + 8.0D - cameraPos.z;
		return dx * dx + dy * dy + dz * dz;
	}

	@Inject(method = "isEntityVisible", at = @At("RETURN"), cancellable = true)
	private void fpsmaxx$cullDistantEntities(
		Entity entity,
		Frustum frustum,
		double camX,
		double camY,
		double camZ,
		float partialTick,
		long entityId,
		CallbackInfoReturnable<Boolean> cir
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
