/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.modules.health;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks server TPS (ticks per second) over time.
 *
 * <p>This class monitors server performance by measuring the actual tick rate
 * and maintaining historical data for average, minimum, and maximum TPS calculations.
 * It uses a ring buffer to efficiently store recent TPS samples.
 *
 * <h2>How TPS Tracking Works</h2>
 * <p>The tracker measures the time between ticks to calculate the actual TPS:
 * <ul>
 *   <li>Normal Minecraft runs at 20 TPS (50ms per tick)</li>
 *   <li>When the server falls behind, TPS drops below 20</li>
 *   <li>TPS cannot exceed 20 (server doesn't run faster)</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create tracker with 60 samples (1 minute at 1 sample/second)
 * TPSTracker tracker = new TPSTracker(60);
 *
 * // Start tracking (call once)
 * scheduler.runTaskTimer(() -> tracker.tick(), 20L, 20L);
 *
 * // Get current TPS
 * double currentTps = tracker.getCurrentTps();
 *
 * // Get averages
 * double avgTps = tracker.getAverageTps();
 * double minTps = tracker.getMinTps();
 * double maxTps = tracker.getMaxTps();
 *
 * // Get TPS history
 * double[] history = tracker.getHistory();
 * }</pre>
 *
 * <h2>Integration with Health Monitoring</h2>
 * <pre>{@code
 * ModuleManager modules = ModuleManager.builder(this)
 *     .enableHealthMonitoring(true)
 *     .healthThreshold(18.0)       // Notify modules when TPS < 18
 *     .recoveryThreshold(19.5)     // Notify recovery when TPS > 19.5
 *     .checkInterval(Duration.ofSeconds(5))
 *     .build();
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see HealthContext
 * @see sh.pcx.unified.modules.lifecycle.Healthy
 */
public final class TPSTracker {

    /**
     * The ideal tick duration in nanoseconds (50ms = 20 TPS).
     */
    private static final long TICK_DURATION_NS = 50_000_000L;

    /**
     * Maximum possible TPS (Minecraft cap).
     */
    private static final double MAX_TPS = 20.0;

    private final double[] samples;
    private final AtomicInteger sampleIndex;
    private final AtomicInteger sampleCount;
    private final AtomicLong lastTickTime;
    private volatile double currentTps;

    /**
     * Creates a TPS tracker with the specified sample capacity.
     *
     * <p>The capacity determines how many samples are kept for averaging.
     * For example, 60 samples with 1 sample per second gives a 1-minute window.
     *
     * @param capacity the number of samples to store
     */
    public TPSTracker(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        this.samples = new double[capacity];
        this.sampleIndex = new AtomicInteger(0);
        this.sampleCount = new AtomicInteger(0);
        this.lastTickTime = new AtomicLong(0);
        this.currentTps = MAX_TPS;
        Arrays.fill(samples, MAX_TPS);
    }

    /**
     * Creates a TPS tracker with default capacity (60 samples).
     */
    public TPSTracker() {
        this(60);
    }

    /**
     * Called each tick to update TPS measurements.
     *
     * <p>This method should be called at regular intervals (e.g., every 20 ticks).
     * It calculates the actual TPS based on the time elapsed since the last call.
     */
    public void tick() {
        long now = System.nanoTime();
        long last = lastTickTime.getAndSet(now);

        if (last != 0) {
            long elapsed = now - last;
            // Calculate TPS based on elapsed time
            // Expected: 20 ticks * 50ms = 1000ms for 20L interval
            double tps = (TICK_DURATION_NS * 20.0) / elapsed * 20.0;
            // Clamp to valid range
            tps = Math.min(MAX_TPS, Math.max(0.0, tps));

            this.currentTps = tps;
            recordSample(tps);
        }
    }

    /**
     * Records a TPS sample in the ring buffer.
     *
     * @param tps the TPS value to record
     */
    private void recordSample(double tps) {
        int index = sampleIndex.getAndUpdate(i -> (i + 1) % samples.length);
        samples[index] = tps;
        sampleCount.updateAndGet(c -> Math.min(c + 1, samples.length));
    }

    /**
     * Returns the current (most recent) TPS.
     *
     * @return the current TPS (0.0 to 20.0)
     */
    public double getCurrentTps() {
        return currentTps;
    }

    /**
     * Returns the average TPS over the sample window.
     *
     * @return the average TPS
     */
    public double getAverageTps() {
        int count = sampleCount.get();
        if (count == 0) {
            return MAX_TPS;
        }

        double sum = 0;
        for (int i = 0; i < count; i++) {
            sum += samples[i];
        }
        return sum / count;
    }

    /**
     * Returns the minimum TPS recorded in the sample window.
     *
     * @return the minimum TPS
     */
    public double getMinTps() {
        int count = sampleCount.get();
        if (count == 0) {
            return MAX_TPS;
        }

        double min = MAX_TPS;
        for (int i = 0; i < count; i++) {
            min = Math.min(min, samples[i]);
        }
        return min;
    }

    /**
     * Returns the maximum TPS recorded in the sample window.
     *
     * @return the maximum TPS
     */
    public double getMaxTps() {
        int count = sampleCount.get();
        if (count == 0) {
            return MAX_TPS;
        }

        double max = 0;
        for (int i = 0; i < count; i++) {
            max = Math.max(max, samples[i]);
        }
        return max;
    }

    /**
     * Returns a copy of the TPS history.
     *
     * <p>The array contains the most recent samples in order from oldest to newest.
     *
     * @return a copy of the TPS history
     */
    @NotNull
    public double[] getHistory() {
        int count = sampleCount.get();
        double[] history = new double[count];
        int start = (sampleIndex.get() - count + samples.length) % samples.length;

        for (int i = 0; i < count; i++) {
            history[i] = samples[(start + i) % samples.length];
        }
        return history;
    }

    /**
     * Returns the number of samples currently stored.
     *
     * @return the sample count
     */
    public int getSampleCount() {
        return sampleCount.get();
    }

    /**
     * Returns the capacity (maximum samples) of this tracker.
     *
     * @return the capacity
     */
    public int getCapacity() {
        return samples.length;
    }

    /**
     * Returns whether the server is currently lagging (TPS below 19).
     *
     * @return {@code true} if the server is lagging
     */
    public boolean isLagging() {
        return currentTps < 19.0;
    }

    /**
     * Returns whether the TPS is below the specified threshold.
     *
     * @param threshold the TPS threshold
     * @return {@code true} if current TPS is below threshold
     */
    public boolean isBelowThreshold(double threshold) {
        return currentTps < threshold;
    }

    /**
     * Returns whether the TPS is above the specified threshold.
     *
     * @param threshold the TPS threshold
     * @return {@code true} if current TPS is above threshold
     */
    public boolean isAboveThreshold(double threshold) {
        return currentTps >= threshold;
    }

    /**
     * Resets the tracker, clearing all samples.
     */
    public void reset() {
        Arrays.fill(samples, MAX_TPS);
        sampleIndex.set(0);
        sampleCount.set(0);
        lastTickTime.set(0);
        currentTps = MAX_TPS;
    }

    /**
     * Returns a formatted string with current TPS statistics.
     *
     * @return a formatted TPS summary
     */
    @NotNull
    public String getSummary() {
        return String.format(
                "TPS: %.1f (avg: %.1f, min: %.1f, max: %.1f)",
                currentTps,
                getAverageTps(),
                getMinTps(),
                getMaxTps()
        );
    }

    @Override
    public String toString() {
        return "TPSTracker{" +
                "current=" + String.format("%.1f", currentTps) +
                ", avg=" + String.format("%.1f", getAverageTps()) +
                ", samples=" + sampleCount.get() + "/" + samples.length +
                '}';
    }
}
