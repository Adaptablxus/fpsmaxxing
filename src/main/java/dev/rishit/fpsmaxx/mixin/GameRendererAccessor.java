package dev.rishit.fpsmaxx.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Allows the dynamic-resolution controller to swap the world render target for
 * the duration of {@code GameRenderer.renderLevel()} and restore it before the
 * GUI is drawn.
 */
@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
	@Accessor("mainRenderTarget")
	RenderTarget fpsmaxx$getMainRenderTarget();

	@Mutable
	@Accessor("mainRenderTarget")
	void fpsmaxx$setMainRenderTarget(RenderTarget target);
}
