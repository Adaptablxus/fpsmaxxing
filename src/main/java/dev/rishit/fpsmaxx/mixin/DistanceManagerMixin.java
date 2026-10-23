package dev.rishit.fpsmaxx.mixin;

import dev.rishit.fpsmaxx.FPSMaxxConstants;
import net.minecraft.server.level.DistanceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * The player ticket tracker is hard-limited to a 32 chunk radius. Widen it to
 * the memory-safe view distance plus a small border, up to 68 for 64 chunks.
 */
@Mixin(DistanceManager.class)
public abstract class DistanceManagerMixin {
	@ModifyArg(
		method = "<init>",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/server/level/DistanceManager$PlayerTicketTracker;<init>(Lnet/minecraft/server/level/DistanceManager;I)V"
		),
		index = 1
	)
	private int fpsmaxx$capPlayerTicketTracker(int vanillaMaximum) {
		return Math.min(
			FPSMaxxConstants.TICKET_RANGE,
			FPSMaxxConstants.maxSafeRenderDistance() + 4
		);
	}
}
