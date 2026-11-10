package dev.rishit.fpsmaxx;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.device.DeviceInfo;
import com.mojang.renderpearl.api.device.GpuDevice;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Lightweight hardware detection used for first-run defaults and status
 * output. GPU information is read from the render backend once the window
 * exists, so it cannot be queried during early config loading.
 */
public final class FPSMaxxHardware {
	public enum Tier {
		POTATO,
		LOW,
		MID,
		HIGH
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(FPSMaxxConstants.MOD_ID);

	private static boolean detected;
	private static int attempts;
	private static String gpuName = "unknown";
	private static String vendor = "unknown";
	private static String backend = "unknown";
	private static String deviceType = "unknown";
	private static String pendingNotice;
	private static long totalRamMb = -1L;

	private FPSMaxxHardware() {
	}

	/** Called once per client tick until detection succeeds. */
	public static void tick(Minecraft client) {
		if (!detected) {
			detect();
			if (!detected) {
				return;
			}

			FPSMaxxConfig config = FPSMaxxConfig.get();
			config.detectedHardware = tier().name().toLowerCase(Locale.ROOT);
			applyFirstRunDefaults(config);
			config.save();
		}

		if (pendingNotice != null && client.player != null) {
			FPSMaxxStatus.sendChat(client, pendingNotice, ChatFormatting.YELLOW);
			pendingNotice = null;
		}
	}

	private static void detect() {
		if (detected) {
			return;
		}
		attempts++;

		try {
			GpuDevice device = RenderSystem.tryGetDevice();
			if (device == null) {
				if (attempts >= 600) {
					detected = true;
					LOGGER.info("GPU detection unavailable after {} attempts; using CPU/RAM heuristics", attempts);
				}
				return;
			}

			DeviceInfo info = device.getDeviceInfo();
			gpuName = clean(info.name());
			vendor = clean(info.vendorName());
			backend = clean(info.backendName());
			deviceType = info.type() == null ? "unknown" : info.type().name();
			detected = true;
			LOGGER.info(
				"Hardware: GPU {} ({}, {}) | {} CPU threads | {} MB heap | {} MB RAM | tier {}",
				gpuName,
				vendor,
				deviceType,
				Runtime.getRuntime().availableProcessors(),
				Runtime.getRuntime().maxMemory() / (1024L * 1024L),
				totalRamMb(),
				tier()
			);
		} catch (Throwable throwable) {
			detected = true;
			LOGGER.info("GPU detection failed ({}); using CPU/RAM heuristics", throwable.toString());
		}
	}

	private static void applyFirstRunDefaults(FPSMaxxConfig config) {
		if (!FPSMaxxConfig.shouldAutoDetectHardware()) {
			return;
		}
		FPSMaxxConfig.markAutoDetectHandled();

		Tier tier = tier();
		if (!config.autoDetectHardware) {
			LOGGER.info("Hardware auto-profile disabled; detected tier {}", tier);
			return;
		}

		if (tier == Tier.POTATO) {
			config.applyPotatoDefaults();
			pendingNotice = "FPSMaxx detected " + shortGpu() + " (" + tier.name().toLowerCase(Locale.ROOT)
				+ "): potato mod-side profile enabled. Use /fpsmaxx balanced to revert.";
		} else if (tier == Tier.LOW && !config.lowEndMode) {
			config.applyLowEndDefaults();
			pendingNotice = "FPSMaxx detected " + shortGpu() + " (" + tier.name().toLowerCase(Locale.ROOT)
				+ "): low-end mod-side profile enabled. Use /fpsmaxx balanced to revert.";
		}

		if (pendingNotice != null) {
			LOGGER.info(pendingNotice);
		}
	}

	public static boolean isDetected() {
		return detected;
	}

	public static String gpuName() {
		return gpuName;
	}

	public static String shortGpu() {
		if (!detected || gpuName.equals("unknown")) {
			return "this device";
		}
		return gpuName.length() > 40 ? gpuName.substring(0, 37) + "..." : gpuName;
	}

	public static Tier tier() {
		int cores = Runtime.getRuntime().availableProcessors();
		long heapMb = Runtime.getRuntime().maxMemory() / (1024L * 1024L);
		long ramMb = totalRamMb();
		String gpu = (gpuName + " " + vendor).toLowerCase(Locale.ROOT);
		String type = deviceType.toLowerCase(Locale.ROOT);

		if (type.equals("cpu")
			|| containsAny(gpu, "llvmpipe", "softpipe", "swiftshader", "software rasterizer", "mesa offscreen")) {
			return Tier.POTATO;
		}

		if (isAndroid()) {
			if (cores <= 4 || ramMb <= 3072L || heapMb <= 1024L) {
				return Tier.POTATO;
			}
			return Tier.LOW;
		}

		if (cores <= 2 || heapMb <= 1024L) {
			return Tier.POTATO;
		}
		if (type.equals("virtual")) {
			return Tier.LOW;
		}
		if (type.equals("integrated")) {
			if (ramMb <= 8192L || heapMb <= 4096L || cores <= 4) {
				return Tier.POTATO;
			}
			if (cores <= 6) {
				return Tier.LOW;
			}
			return Tier.MID;
		}
		if (type.equals("discrete")) {
			if (containsAny(gpu, "rtx 40", "rtx 50", "rx 7", "rx 9", "arc b", "arc a7")) {
				return Tier.HIGH;
			}
			return Tier.MID;
		}

		if (cores <= 4 || ramMb <= 4096L || heapMb <= 2048L) {
			return Tier.LOW;
		}
		return Tier.MID;
	}

	public static boolean isAndroid() {
		String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		String javaVendor = System.getProperty("java.vendor", "").toLowerCase(Locale.ROOT);
		return os.contains("android")
			|| javaVendor.contains("android")
			|| System.getenv("ANDROID_ROOT") != null
			|| System.getenv("ANDROID_DATA") != null;
	}

	public static long totalRamMb() {
		if (totalRamMb > 0L) {
			return totalRamMb;
		}
		try {
			Object bean = ManagementFactory.getOperatingSystemMXBean();
			Class<?> clazz = Class.forName("com.sun.management.OperatingSystemMXBean");
			if (clazz.isInstance(bean)) {
				Method method = clazz.getMethod("getTotalMemorySize");
				totalRamMb = ((Number) method.invoke(bean)).longValue() / (1024L * 1024L);
				if (totalRamMb > 0L) {
					return totalRamMb;
				}
			}
		} catch (Throwable ignored) {
			// Fall back to the JVM heap below.
		}
		totalRamMb = Runtime.getRuntime().maxMemory() / (1024L * 1024L);
		return totalRamMb;
	}

	public static String describe() {
		String tier = tier().name().toLowerCase(Locale.ROOT);
		if (!detected) {
			return "GPU detection pending | tier " + tier
				+ " | " + Runtime.getRuntime().availableProcessors() + " threads | " + ramText();
		}
		return "GPU " + gpuName + " (" + deviceType.toLowerCase(Locale.ROOT) + ", " + backend + ")"
			+ " | tier " + tier
			+ " | " + Runtime.getRuntime().availableProcessors() + " threads | " + ramText();
	}

	private static String ramText() {
		long ram = totalRamMb();
		return ram >= 1024L ? (ram / 1024L) + " GB RAM" : ram + " MB RAM";
	}

	private static boolean containsAny(String value, String... needles) {
		for (String needle : needles) {
			if (value.contains(needle)) {
				return true;
			}
		}
		return false;
	}

	private static String clean(String value) {
		if (value == null || value.isBlank()) {
			return "unknown";
		}
		String cleaned = value.replace('\n', ' ').replace('\r', ' ').trim();
		return cleaned.length() > 80 ? cleaned.substring(0, 77) + "..." : cleaned;
	}
}
