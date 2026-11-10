package dev.rishit.fpsmaxx;

import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.commands.RenderPassDescriptor;
import com.mojang.renderpearl.api.textures.FilterMode;
import dev.rishit.fpsmaxx.compat.FPSMaxxCompat;
import dev.rishit.fpsmaxx.mixin.GameRendererAccessor;
import dev.rishit.fpsmaxx.mixin.SkyRendererAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SkyRenderer;
import org.joml.Vector4f;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Dynamic render resolution for the 3D world.
 *
 * <p>Minecraft renders the world and the GUI into the same main render target.
 * Swapping that target for the whole frame would blur the HUD, so FPSMaxx only
 * swaps it while {@code GameRenderer.renderLevel()} runs. The low-resolution
 * world is then upscaled into the real target before the GUI is drawn, which
 * keeps text and menus at native resolution.</p>
 *
 * <p>The target cache is deliberately small: changing the scale a few times
 * during a session should not allocate a new framebuffer every second. Targets
 * are keyed by size and the least-recently-used one is released when the cache
 * grows past four entries.</p>
 */
public final class FPSMaxxDynamicResolution {
	private static final Vector4f CLEAR_COLOR = new Vector4f(0.0F, 0.0F, 0.0F, 1.0F);
	private static final int MAX_CACHED_TARGETS = 4;
	private static final int MIN_TARGET_WIDTH = 160;
	private static final int MIN_TARGET_HEIGHT = 90;

	private static final Map<Long, MainTarget> TARGETS = new LinkedHashMap<>(8, 0.75F, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<Long, MainTarget> eldest) {
			if (size() <= MAX_CACHED_TARGETS) {
				return false;
			}
			eldest.getValue().destroyBuffers();
			return true;
		}
	};

	private static float scale = 1.0F;
	private static MainTarget worldTarget;
	private static RenderTarget originalMain;
	private static SkyRenderer skyRenderer;
	private static RenderTarget originalSky;
	private static boolean active;
	private static boolean failed;
	private static boolean loggedActive;

	private FPSMaxxDynamicResolution() {
	}

	public static float scale() {
		return scale;
	}

	public static void setScale(float value) {
		scale = Math.max(0.5F, Math.min(1.0F, value));
	}

	public static void resetScale() {
		scale = 1.0F;
	}

	public static boolean supported() {
		if (failed || FPSMaxxCompat.VULKANMOD || FPSMaxxCompat.NVIDIUM) {
			return false;
		}
		return !FPSMaxxCompat.irisShaderPackActive();
	}

	public static boolean enabled() {
		return FPSMaxxConfig.get().dynamicResolution && supported() && scale < 0.995F;
	}

	/**
	 * Called at the head of {@code GameRenderer.renderLevel()}. Returns after
	 * swapping the render target; {@link #endFrame(GameRenderer)} restores it.
	 */
	public static void beginFrame(GameRenderer renderer) {
		if (active) {
			endFrame(renderer);
		}
		if (renderer == null || !enabled()) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		if (client.level == null || client.player == null) {
			return;
		}

		try {
			int windowWidth = Math.max(1, client.getWindow().getWidth());
			int windowHeight = Math.max(1, client.getWindow().getHeight());
			int width = Math.max(MIN_TARGET_WIDTH, Math.round(windowWidth * scale));
			int height = Math.max(MIN_TARGET_HEIGHT, Math.round(windowHeight * scale));
			MainTarget target = target(width, height);

			CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
			encoder.clearColorAndDepthTextures(
				target.getColorTexture(),
				CLEAR_COLOR,
				target.getDepthTexture(),
				0.0D
			);

			GameRendererAccessor accessor = (GameRendererAccessor) renderer;
			originalMain = accessor.fpsmaxx$getMainRenderTarget();
			accessor.fpsmaxx$setMainRenderTarget(target);
			worldTarget = target;

			skyRenderer = client.levelRenderer.skyRenderer();
			if (skyRenderer != null) {
				SkyRendererAccessor sky = (SkyRendererAccessor) skyRenderer;
				originalSky = sky.fpsmaxx$getRenderTarget();
				sky.fpsmaxx$setRenderTarget(target);
			} else {
				originalSky = null;
			}

			active = true;
			if (!loggedActive) {
				loggedActive = true;
				FPSMaxxBootstrap.LOGGER.info(
					"FPSMaxx dynamic resolution active: {}% ({}x{} -> {}x{})",
					Math.round(scale * 100.0F),
					windowWidth,
					windowHeight,
					width,
					height
				);
			}
		} catch (Throwable throwable) {
			try {
				restoreTargets(renderer);
			} catch (Throwable ignored) {
				// The native target is already restored if this is the only failure.
			}
			active = false;
			worldTarget = null;
			originalMain = null;
			originalSky = null;
			skyRenderer = null;
			fail(throwable);
		}
	}

	/**
	 * Called at the return of {@code GameRenderer.renderLevel()}. Restores the
	 * native target, then copies the low-resolution colour and depth back with
	 * a linear colour upscale and a nearest depth upscale.
	 */
	public static void endFrame(GameRenderer renderer) {
		if (!active) {
			return;
		}

		try {
			restoreTargets(renderer);
			if (originalMain != null && worldTarget != null) {
				blitColor(worldTarget, originalMain);
				blitDepth(worldTarget, originalMain);
			}
		} catch (Throwable throwable) {
			try {
				restoreTargets(renderer);
			} catch (Throwable ignored) {
				// Nothing else can be done; the feature disables itself below.
			}
			fail(throwable);
		} finally {
			active = false;
			worldTarget = null;
			originalMain = null;
			originalSky = null;
			skyRenderer = null;
		}
	}

	public static void onToggled() {
		resetScale();
	}

	private static MainTarget target(int width, int height) {
		long key = ((long) width << 32) | (height & 0xFFFFFFFFL);
		MainTarget target = TARGETS.get(key);
		if (target == null) {
			target = new MainTarget(width, height);
			TARGETS.put(key, target);
		}
		return target;
	}

	private static void restoreTargets(GameRenderer renderer) {
		if (renderer != null && originalMain != null) {
			((GameRendererAccessor) renderer).fpsmaxx$setMainRenderTarget(originalMain);
		}
		if (skyRenderer != null && originalSky != null) {
			((SkyRendererAccessor) skyRenderer).fpsmaxx$setRenderTarget(originalSky);
		}
	}

	private static void blitColor(RenderTarget source, RenderTarget destination) {
		CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
		try (RenderPass pass = encoder.createRenderPass(
			() -> "FPSMaxx dynamic resolution upscale",
			destination.getColorTextureView(),
			Optional.empty()
		)) {
			pass.setPipeline(RenderSystem.getCompiledPipeline(RenderPipelines.TRACY_BLIT));
			RenderSystem.bindDefaultUniforms(pass);
			pass.setUniform(
				"InSampler",
				source.getColorTextureView(),
				RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)
			);
			pass.draw(3, 1, 0, 0);
		}
	}

	private static void blitDepth(RenderTarget source, RenderTarget destination) {
		if (!source.hasDepth() || !destination.hasDepth()) {
			return;
		}
		CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
		RenderPassDescriptor descriptor = RenderPassDescriptor.builder(
			() -> "FPSMaxx dynamic resolution depth"
		).withDepthAttachment(destination.getDepthTextureView()).build();
		try (RenderPass pass = encoder.createRenderPass(descriptor)) {
			pass.setPipeline(RenderSystem.getCompiledPipeline(RenderPipelines.BLIT_DEPTH));
			RenderSystem.bindDefaultUniforms(pass);
			pass.setUniform(
				"InSampler",
				source.getDepthTextureView(),
				RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST)
			);
			pass.draw(3, 1, 0, 0);
		}
	}

	private static void fail(Throwable throwable) {
		if (failed) {
			return;
		}
		failed = true;
		scale = 1.0F;
		FPSMaxxBootstrap.LOGGER.warn(
			"FPSMaxx dynamic resolution failed; falling back to native resolution",
			throwable
		);
	}
}
