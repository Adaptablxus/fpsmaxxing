package dev.rishit.fpsmaxx.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.SkyRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * The sky renderer keeps its own reference to the main target. It has to point
 * at the low-resolution target too, otherwise the sky is drawn at native size
 * into the real target while the terrain is drawn into the small one.
 */
@Mixin(SkyRenderer.class)
public interface SkyRendererAccessor {
	@Accessor("renderTarget")
	RenderTarget fpsmaxx$getRenderTarget();

	@Mutable
	@Accessor("renderTarget")
	void fpsmaxx$setRenderTarget(RenderTarget target);
}
