package dev.rishit.fpsmaxx.mixin;

import net.minecraft.server.level.ChunkTaskPriorityQueue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Vanilla sizes this queue from ChunkLevel.MAX_LEVEL (46 priorities), which is
 * enough for a 32 chunk view distance but not for 64: far chunk tasks arrive
 * with priorities in the 50s/60s and crash the queue.
 *
 * Clamping to the lowest supported priority keeps those tasks valid. They are
 * the farthest chunks anyway, so they simply run last.
 */
@Mixin(ChunkTaskPriorityQueue.class)
public abstract class ChunkTaskPriorityQueueMixin {
	@ModifyVariable(
		method = "submit",
		at = @At("HEAD"),
		argsOnly = true,
		index = 4
	)
	private int fpsmaxx$clampSubmittedPriority(int level) {
		return Math.min(level, ChunkTaskPriorityQueue.PRIORITY_LEVEL_COUNT - 1);
	}

	@ModifyVariable(
		method = "resortChunkTasks",
		at = @At("HEAD"),
		argsOnly = true,
		index = 3
	)
	private int fpsmaxx$clampResortedPriority(int newPriority) {
		return Math.min(newPriority, ChunkTaskPriorityQueue.PRIORITY_LEVEL_COUNT - 1);
	}
}
