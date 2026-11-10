package dev.rishit.fpsmaxx;

import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;

/**
 * Frame-time sampler and benchmark runner.
 *
 * <p>The client calls {@link #recordFrame()} once per rendered frame from the
 * Minecraft mixin. The rolling window powers {@code /fpsmaxx status}; the
 * benchmark keeps its own larger sample set and writes a report to the config
 * directory.</p>
 */
public final class FPSMaxxMetrics {
	private static final int FRAME_WINDOW = 1200;
	private static final int BENCH_MAX_FRAMES = 240_000;
	private static final int BENCH_MAX_SAMPLES = 300;
	private static final long WARMUP_MS = 3000L;
	private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

	private static final long[] frames = new long[FRAME_WINDOW];
	private static int frameIndex;
	private static int frameCount;
	private static long lastFrameNanos;
	private static long lastRecordedFrameNs;
	private static boolean firstFrameLogged;

	private static boolean benchmarkRunning;
	private static long benchmarkWarmupUntil;
	private static long benchmarkEndMs;
	private static int benchmarkDurationSeconds;
	private static long[] benchmarkFrames = new long[0];
	private static int benchmarkFrameCount;
	private static final int[] benchmarkSecondFps = new int[BENCH_MAX_SAMPLES];
	private static final int[] benchmarkVisible = new int[BENCH_MAX_SAMPLES];
	private static final int[] benchmarkSimulation = new int[BENCH_MAX_SAMPLES];
	private static int benchmarkSampleCount;
	private static long lastBenchmarkSampleMs;

	private FPSMaxxMetrics() {
	}

	/**
	 * Called once per rendered frame. The delta between calls is the real frame
	 * interval, including the framerate limiter's sleep, so it matches what the
	 * player experiences better than sampling {@code Minecraft.getFps()} once a
	 * second.
	 */
	public static void recordFrame() {
		long now = System.nanoTime();
		if (lastFrameNanos == 0L) {
			lastFrameNanos = now;
			return;
		}
		long delta = now - lastFrameNanos;
		lastFrameNanos = now;

		Minecraft client = Minecraft.getInstance();
		if (client == null || client.level == null || client.player == null
			|| client.isPaused() || !client.isWindowActive()) {
			return;
		}

		// Ignore the first frame after a pause/alt-tab and absurd outliers.
		if (delta < 100_000L || delta > 2_000_000_000L) {
			return;
		}

		if (!firstFrameLogged) {
			firstFrameLogged = true;
			FPSMaxxBootstrap.LOGGER.info("Frame-time sampler active");
		}

		lastRecordedFrameNs = delta;
		frames[frameIndex] = delta;
		frameIndex = (frameIndex + 1) % FRAME_WINDOW;
		if (frameCount < FRAME_WINDOW) {
			frameCount++;
		}

		if (benchmarkRunning
			&& Util.getMillis() >= benchmarkWarmupUntil
			&& benchmarkFrameCount < benchmarkFrames.length) {
			benchmarkFrames[benchmarkFrameCount++] = delta;
		}
	}

	/** Called once per client tick. */
	public static void tick(Minecraft client) {
		if (!benchmarkRunning) {
			return;
		}

		long now = Util.getMillis();
		if (client.level == null || client.player == null) {
			finishBenchmark(client, "FPSMaxx benchmark cancelled: you left the world.");
			return;
		}
		if (now < benchmarkWarmupUntil) {
			return;
		}

		if (now - lastBenchmarkSampleMs >= 1000L) {
			lastBenchmarkSampleMs = now;
			if (benchmarkSampleCount < BENCH_MAX_SAMPLES) {
				benchmarkSecondFps[benchmarkSampleCount] = Math.max(0, client.getFps());
				int visible = FPSMaxxGovernor.visibleDistanceChunks();
				if (visible <= 0) {
					visible = client.options.getEffectiveRenderDistance();
				}
				benchmarkVisible[benchmarkSampleCount] = Math.max(0, visible);
				benchmarkSimulation[benchmarkSampleCount] = client.options.simulationDistance().get();
				benchmarkSampleCount++;
			}
		}

		if (now >= benchmarkEndMs) {
			finishBenchmark(client, null);
		}
	}

	public static boolean startBenchmark(Minecraft client, int seconds) {
		if (benchmarkRunning) {
			FPSMaxxStatus.sendChat(
				client,
				"FPSMaxx benchmark is already running (" + benchmarkStatus() + ").",
				ChatFormatting.YELLOW
			);
			return false;
		}
		if (client.level == null || client.player == null) {
			FPSMaxxStatus.sendChat(client, "Join a world before starting a benchmark.", ChatFormatting.YELLOW);
			return false;
		}

		benchmarkRunning = true;
		benchmarkDurationSeconds = seconds;
		long now = Util.getMillis();
		benchmarkWarmupUntil = now + WARMUP_MS;
		benchmarkEndMs = benchmarkWarmupUntil + seconds * 1000L;
		benchmarkFrames = new long[Math.min(BENCH_MAX_FRAMES, Math.max(600, seconds * 2000))];
		benchmarkFrameCount = 0;
		benchmarkSampleCount = 0;
		lastBenchmarkSampleMs = 0L;

		FPSMaxxStatus.sendChat(
			client,
			"FPSMaxx benchmark started: " + seconds
				+ "s after a 3s warm-up. Keep playing normally; run /fpsmaxx benchmark stop to end early.",
			ChatFormatting.GREEN
		);
		return true;
	}

	public static void stopBenchmark(Minecraft client) {
		if (!benchmarkRunning) {
			FPSMaxxStatus.sendChat(client, "No FPSMaxx benchmark is running.", ChatFormatting.YELLOW);
			return;
		}
		if (Util.getMillis() < benchmarkWarmupUntil || benchmarkFrameCount < 10) {
			benchmarkRunning = false;
			benchmarkFrames = new long[0];
			benchmarkFrameCount = 0;
			benchmarkSampleCount = 0;
			FPSMaxxStatus.sendChat(
				client,
				"FPSMaxx benchmark cancelled before enough frames were collected.",
				ChatFormatting.YELLOW
			);
			return;
		}
		finishBenchmark(client, null);
	}

	private static void finishBenchmark(Minecraft client, String cancelMessage) {
		benchmarkRunning = false;

		long[] sampleFrames = Arrays.copyOf(benchmarkFrames, benchmarkFrameCount);
		int[] sampleSecondFps = Arrays.copyOf(benchmarkSecondFps, benchmarkSampleCount);
		int[] sampleVisible = Arrays.copyOf(benchmarkVisible, benchmarkSampleCount);
		int[] sampleSimulation = Arrays.copyOf(benchmarkSimulation, benchmarkSampleCount);
		benchmarkFrames = new long[0];
		benchmarkFrameCount = 0;
		benchmarkSampleCount = 0;

		if (cancelMessage != null) {
			FPSMaxxStatus.sendChat(client, cancelMessage, ChatFormatting.YELLOW);
			return;
		}
		if (sampleFrames.length < 10) {
			FPSMaxxStatus.sendChat(
				client,
				"FPSMaxx benchmark finished without enough frames; try a longer run.",
				ChatFormatting.YELLOW
			);
			return;
		}

		Arrays.sort(sampleFrames);
		long total = 0L;
		int over33 = 0;
		int over50 = 0;
		for (long frame : sampleFrames) {
			total += frame;
			if (frame > 33_333_333L) {
				over33++;
			}
			if (frame > 50_000_000L) {
				over50++;
			}
		}

		double averageFrameMs = total / (double) sampleFrames.length / 1_000_000.0;
		double averageFps = 1000.0 / Math.max(0.001, averageFrameMs);
		long p99 = sampleFrames[(int) Math.min(sampleFrames.length - 1, Math.ceil(sampleFrames.length * 0.99) - 1)];
		double p99Ms = p99 / 1_000_000.0;
		double lowFps = 1000.0 / Math.max(0.001, p99Ms);
		double worstMs = sampleFrames[sampleFrames.length - 1] / 1_000_000.0;
		double over33Percent = 100.0 * over33 / sampleFrames.length;
		double over50Percent = 100.0 * over50 / sampleFrames.length;

		int worstSecond = Integer.MAX_VALUE;
		for (int fps : sampleSecondFps) {
			if (fps > 0) {
				worstSecond = Math.min(worstSecond, fps);
			}
		}

		int visibleMin = Integer.MAX_VALUE;
		int visibleMax = 0;
		long visibleSum = 0L;
		int visibleCount = 0;
		int simulationMin = Integer.MAX_VALUE;
		int simulationMax = 0;
		long simulationSum = 0L;
		int simulationCount = 0;
		for (int i = 0; i < sampleSecondFps.length; i++) {
			if (sampleVisible[i] > 0) {
				visibleMin = Math.min(visibleMin, sampleVisible[i]);
				visibleMax = Math.max(visibleMax, sampleVisible[i]);
				visibleSum += sampleVisible[i];
				visibleCount++;
			}
			if (sampleSimulation[i] > 0) {
				simulationMin = Math.min(simulationMin, sampleSimulation[i]);
				simulationMax = Math.max(simulationMax, sampleSimulation[i]);
				simulationSum += sampleSimulation[i];
				simulationCount++;
			}
		}

		String visibleRange = range(visibleMin, visibleMax, visibleSum, visibleCount);
		String simulationRange = range(simulationMin, simulationMax, simulationSum, simulationCount);
		String worstSecondText = worstSecond == Integer.MAX_VALUE ? "n/a" : worstSecond + " FPS";
		FPSMaxxConfig config = FPSMaxxConfig.get();

		StringBuilder report = new StringBuilder();
		report.append("FPSMaxx benchmark report\n");
		report.append("Generated: ").append(LocalDateTime.now()).append('\n');
		report.append("Loader: ").append(FPSMaxxPlatform.loader())
			.append(' ').append(FPSMaxxPlatform.modVersion("fpsmaxx"))
			.append(" | Minecraft ").append(SharedConstants.getCurrentVersion().name()).append('\n');
		report.append("Hardware: ").append(FPSMaxxHardware.describe()).append('\n');
		report.append(String.format(
			Locale.ROOT,
			"Measured: %d frames over %d s (+3 s warm-up)%n",
			sampleFrames.length,
			benchmarkDurationSeconds
		));
		report.append(String.format(Locale.ROOT, "Average FPS: %.1f%n", averageFps));
		report.append(String.format(Locale.ROOT, "1%% low FPS: %.1f (p99 frame %.1f ms)%n", lowFps, p99Ms));
		report.append(String.format(Locale.ROOT, "Worst frame: %.1f ms%n", worstMs));
		report.append("Worst 1-second window: ").append(worstSecondText).append('\n');
		report.append(String.format(Locale.ROOT, "Frames over 33.3 ms: %.1f%%%n", over33Percent));
		report.append(String.format(Locale.ROOT, "Frames over 50 ms: %.1f%%%n", over50Percent));
		report.append("Visible radius: ").append(visibleRange).append('\n');
		report.append("Simulation distance: ").append(simulationRange).append('\n');
		report.append("Target: ").append(config.targetFps)
			.append(" FPS | mode ").append(FPSMaxxGovernor.effectiveMode())
			.append(" | adaptive simulation ").append(config.adaptiveSimulation ? "on" : "off")
			.append(" | render request ").append(config.initialVisibleDistance)
			.append(" | simulation request ").append(config.simulationDistance)
			.append('\n');

		Path reportPath = null;
		try {
			reportPath = FPSMaxxPlatform.configDir().resolve(
				"fpsmaxx-benchmark-" + FILE_TIME.format(LocalDateTime.now()) + ".txt"
			);
			Files.createDirectories(reportPath.getParent());
			Files.writeString(reportPath, report.toString(), StandardCharsets.UTF_8);
		} catch (IOException | RuntimeException exception) {
			FPSMaxxBootstrap.LOGGER.warn("Could not write the FPSMaxx benchmark report", exception);
			reportPath = null;
		}

		FPSMaxxBootstrap.LOGGER.info(
			"Benchmark: {} frames, avg {} FPS, 1% low {} FPS, visible {}, simulation {}, report {}",
			sampleFrames.length,
			String.format(Locale.ROOT, "%.1f", averageFps),
			String.format(Locale.ROOT, "%.1f", lowFps),
			visibleRange,
			simulationRange,
			reportPath == null ? "not written" : reportPath
		);

		FPSMaxxStatus.sendChat(
			client,
			String.format(
				Locale.ROOT,
				"FPSMaxx benchmark %ds: avg %.1f FPS | 1%% low %.1f FPS | worst 1s %s",
				benchmarkDurationSeconds,
				averageFps,
				lowFps,
				worstSecondText
			),
			ChatFormatting.AQUA
		);
		FPSMaxxStatus.sendChat(
			client,
			String.format(
				Locale.ROOT,
				"visible %s | sim %s | frames >33ms %.1f%%",
				visibleRange,
				simulationRange,
				over33Percent
			),
			ChatFormatting.GRAY
		);
		if (reportPath != null) {
			FPSMaxxStatus.sendChat(client, "Report: " + reportPath, ChatFormatting.GRAY);
		} else {
			FPSMaxxStatus.sendChat(
				client,
				"Report could not be written; see the game log for the full result.",
				ChatFormatting.YELLOW
			);
		}
	}

	private static String range(int min, int max, long sum, int count) {
		if (count == 0 || min == Integer.MAX_VALUE) {
			return "n/a";
		}
		return String.format(Locale.ROOT, "%d-%d chunks (avg %.1f)", min, max, sum / (double) count);
	}

	/** Short rolling-window summary for {@code /fpsmaxx status}. */
	public static String summaryLine() {
		if (frameCount < 30) {
			return "frame -- ms | avg -- FPS | 1% low -- FPS (warming up)";
		}

		long[] copy = new long[frameCount];
		System.arraycopy(frames, 0, copy, 0, frameCount);
		long total = 0L;
		for (long frame : copy) {
			total += frame;
		}
		Arrays.sort(copy);
		long p99 = copy[(int) Math.min(copy.length - 1, Math.ceil(copy.length * 0.99) - 1)];
		double averageFrameMs = total / (double) copy.length / 1_000_000.0;
		double p99Ms = p99 / 1_000_000.0;
		double worstMs = copy[copy.length - 1] / 1_000_000.0;
		return String.format(
			Locale.ROOT,
			"frame %.1f ms | avg %.1f FPS | 1%% low %.1f FPS | worst %.1f ms",
			lastRecordedFrameNs / 1_000_000.0,
			1000.0 / Math.max(0.001, averageFrameMs),
			1000.0 / Math.max(0.001, p99Ms),
			worstMs
		);
	}

	public static boolean isBenchmarkRunning() {
		return benchmarkRunning;
	}

	public static String benchmarkStatus() {
		if (!benchmarkRunning) {
			return null;
		}
		long now = Util.getMillis();
		if (now < benchmarkWarmupUntil) {
			long seconds = Math.max(1L, (benchmarkWarmupUntil - now + 999L) / 1000L);
			return "benchmark warming up (" + seconds + "s)";
		}
		long secondsLeft = Math.max(0L, (benchmarkEndMs - now + 999L) / 1000L);
		return "benchmark " + secondsLeft + "s left";
	}
}
