package sh.pcx.unified.util.math;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility class providing common mathematical operations for game development.
 *
 * <p>This class includes functions for clamping, interpolation, mapping values,
 * random number generation, and percentage calculations commonly used in
 * Minecraft plugin development.</p>
 *
 * <h2>Clamping Values:</h2>
 * <pre>{@code
 * // Ensure health stays within valid bounds
 * double health = MathUtils.clamp(currentHealth, 0.0, 20.0);
 *
 * // Clamp integer values
 * int level = MathUtils.clamp(playerLevel, 1, 100);
 * }</pre>
 *
 * <h2>Interpolation:</h2>
 * <pre>{@code
 * // Smoothly transition between positions
 * double x = MathUtils.lerp(startX, endX, progress);
 *
 * // Smooth step for eased transitions
 * double smoothed = MathUtils.smoothStep(startValue, endValue, t);
 *
 * // Inverse lerp to get progress
 * double progress = MathUtils.inverseLerp(0, 100, currentValue);
 * }</pre>
 *
 * <h2>Random Numbers:</h2>
 * <pre>{@code
 * // Random integer in range [min, max]
 * int damage = MathUtils.randomInt(5, 15);
 *
 * // Random double in range [min, max)
 * double chance = MathUtils.randomDouble(0.0, 1.0);
 *
 * // Check probability
 * if (MathUtils.chance(0.25)) {
 *     // 25% chance to execute
 *     dropRareItem();
 * }
 * }</pre>
 *
 * @author Supatuck
 * @since 1.0.0
 */
public final class MathUtils {

    /** The value of PI. */
    public static final double PI = Math.PI;

    /** The value of 2 * PI. */
    public static final double TWO_PI = 2.0 * Math.PI;

    /** The value of PI / 2. */
    public static final double HALF_PI = Math.PI / 2.0;

    /** Degrees to radians conversion factor. */
    public static final double DEG_TO_RAD = Math.PI / 180.0;

    /** Radians to degrees conversion factor. */
    public static final double RAD_TO_DEG = 180.0 / Math.PI;

    /** A small value for floating-point comparisons. */
    public static final double EPSILON = 1e-6;

    /** Float version of epsilon. */
    public static final float EPSILON_F = 1e-6f;

    private MathUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // ==================== Clamping ====================

    /**
     * Clamps an integer value between a minimum and maximum.
     *
     * @param value the value to clamp
     * @param min the minimum allowed value
     * @param max the maximum allowed value
     * @return the clamped value
     *
     * <h3>Example:</h3>
     * <pre>{@code
     * int clamped = MathUtils.clamp(150, 0, 100);
     * // clamped = 100
     * }</pre>
     */
    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Clamps a long value between a minimum and maximum.
     *
     * @param value the value to clamp
     * @param min the minimum allowed value
     * @param max the maximum allowed value
     * @return the clamped value
     */
    public static long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Clamps a float value between a minimum and maximum.
     *
     * @param value the value to clamp
     * @param min the minimum allowed value
     * @param max the maximum allowed value
     * @return the clamped value
     */
    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Clamps a double value between a minimum and maximum.
     *
     * @param value the value to clamp
     * @param min the minimum allowed value
     * @param max the maximum allowed value
     * @return the clamped value
     *
     * <h3>Example:</h3>
     * <pre>{@code
     * double clamped = MathUtils.clamp(1.5, 0.0, 1.0);
     * // clamped = 1.0
     * }</pre>
     */
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Clamps a value between 0 and 1.
     *
     * @param value the value to clamp
     * @return the clamped value in range [0, 1]
     */
    public static double clamp01(double value) {
        return clamp(value, 0.0, 1.0);
    }

    /**
     * Clamps a float value between 0 and 1.
     *
     * @param value the value to clamp
     * @return the clamped value in range [0, 1]
     */
    public static float clamp01(float value) {
        return clamp(value, 0.0f, 1.0f);
    }

    // ==================== Interpolation ====================

    /**
     * Linearly interpolates between two values.
     *
     * @param start the start value
     * @param end the end value
     * @param t the interpolation factor (0 to 1)
     * @return the interpolated value
     *
     * <h3>Example:</h3>
     * <pre>{@code
     * double mid = MathUtils.lerp(0.0, 100.0, 0.5);
     * // mid = 50.0
     * }</pre>
     */
    public static double lerp(double start, double end, double t) {
        return start + (end - start) * t;
    }

    /**
     * Linearly interpolates between two float values.
     *
     * @param start the start value
     * @param end the end value
     * @param t the interpolation factor (0 to 1)
     * @return the interpolated value
     */
    public static float lerp(float start, float end, float t) {
        return start + (end - start) * t;
    }

    /**
     * Clamped linear interpolation (t is clamped to [0, 1]).
     *
     * @param start the start value
     * @param end the end value
     * @param t the interpolation factor
     * @return the interpolated value with t clamped
     */
    public static double lerpClamped(double start, double end, double t) {
        return lerp(start, end, clamp01(t));
    }

    /**
     * Calculates the inverse lerp (finds t given a value in range).
     *
     * @param start the start of the range
     * @param end the end of the range
     * @param value the value within the range
     * @return the interpolation factor t where lerp(start, end, t) = value
     *
     * <h3>Example:</h3>
     * <pre>{@code
     * double t = MathUtils.inverseLerp(0.0, 100.0, 25.0);
     * // t = 0.25
     * }</pre>
     */
    public static double inverseLerp(double start, double end, double value) {
        if (Math.abs(end - start) < EPSILON) {
            return 0.0;
        }
        return (value - start) / (end - start);
    }

    /**
     * Smooth step interpolation with ease-in and ease-out.
     *
     * @param start the start value
     * @param end the end value
     * @param t the interpolation factor (0 to 1)
     * @return the smoothly interpolated value
     *
     * <h3>Example:</h3>
     * <pre>{@code
     * // Creates a smooth S-curve transition
     * double smooth = MathUtils.smoothStep(0.0, 100.0, progress);
     * }</pre>
     */
    public static double smoothStep(double start, double end, double t) {
        t = clamp01(t);
        t = t * t * (3.0 - 2.0 * t);
        return lerp(start, end, t);
    }

    /**
     * Smoother step interpolation (even smoother than smoothStep).
     *
     * @param start the start value
     * @param end the end value
     * @param t the interpolation factor (0 to 1)
     * @return the smoothly interpolated value
     */
    public static double smootherStep(double start, double end, double t) {
        t = clamp01(t);
        t = t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
        return lerp(start, end, t);
    }

    // ==================== Mapping ====================

    /**
     * Maps a value from one range to another.
     *
     * @param value the value to map
     * @param fromMin the minimum of the input range
     * @param fromMax the maximum of the input range
     * @param toMin the minimum of the output range
     * @param toMax the maximum of the output range
     * @return the mapped value
     *
     * <h3>Example:</h3>
     * <pre>{@code
     * // Map a health value (0-100) to a color intensity (0-255)
     * double intensity = MathUtils.map(health, 0, 100, 0, 255);
     * }</pre>
     */
    public static double map(double value, double fromMin, double fromMax, double toMin, double toMax) {
        double t = inverseLerp(fromMin, fromMax, value);
        return lerp(toMin, toMax, t);
    }

    /**
     * Maps a value from one range to another with clamping.
     *
     * @param value the value to map
     * @param fromMin the minimum of the input range
     * @param fromMax the maximum of the input range
     * @param toMin the minimum of the output range
     * @param toMax the maximum of the output range
     * @return the mapped and clamped value
     */
    public static double mapClamped(double value, double fromMin, double fromMax, double toMin, double toMax) {
        double t = clamp01(inverseLerp(fromMin, fromMax, value));
        return lerp(toMin, toMax, t);
    }

    /**
     * Maps a value to a 0-1 range (normalizes).
     *
     * @param value the value to normalize
     * @param min the minimum of the input range
     * @param max the maximum of the input range
     * @return the normalized value in range [0, 1]
     */
    public static double normalize(double value, double min, double max) {
        return inverseLerp(min, max, value);
    }

    /**
     * Maps a 0-1 value to a specified range (denormalizes).
     *
     * @param normalized the normalized value (0 to 1)
     * @param min the minimum of the output range
     * @param max the maximum of the output range
     * @return the denormalized value
     */
    public static double denormalize(double normalized, double min, double max) {
        return lerp(min, max, normalized);
    }

    // ==================== Random ====================

    /**
     * Returns a random integer between min and max (inclusive).
     *
     * @param min the minimum value (inclusive)
     * @param max the maximum value (inclusive)
     * @return a random integer in the range [min, max]
     *
     * <h3>Example:</h3>
     * <pre>{@code
     * int damage = MathUtils.randomInt(10, 20);
     * // damage is between 10 and 20 inclusive
     * }</pre>
     */
    public static int randomInt(int min, int max) {
        if (min > max) {
            int temp = min;
            min = max;
            max = temp;
        }
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    /**
     * Returns a random integer between 0 and max (inclusive).
     *
     * @param max the maximum value (inclusive)
     * @return a random integer in the range [0, max]
     */
    public static int randomInt(int max) {
        return randomInt(0, max);
    }

    /**
     * Returns a random long between min and max (inclusive).
     *
     * @param min the minimum value (inclusive)
     * @param max the maximum value (inclusive)
     * @return a random long in the range [min, max]
     */
    public static long randomLong(long min, long max) {
        if (min > max) {
            long temp = min;
            min = max;
            max = temp;
        }
        return ThreadLocalRandom.current().nextLong(min, max + 1);
    }

    /**
     * Returns a random double between min and max.
     *
     * @param min the minimum value (inclusive)
     * @param max the maximum value (exclusive)
     * @return a random double in the range [min, max)
     *
     * <h3>Example:</h3>
     * <pre>{@code
     * double multiplier = MathUtils.randomDouble(0.8, 1.2);
     * int finalDamage = (int) (baseDamage * multiplier);
     * }</pre>
     */
    public static double randomDouble(double min, double max) {
        if (min > max) {
            double temp = min;
            min = max;
            max = temp;
        }
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    /**
     * Returns a random double between 0 and max.
     *
     * @param max the maximum value (exclusive)
     * @return a random double in the range [0, max)
     */
    public static double randomDouble(double max) {
        return randomDouble(0.0, max);
    }

    /**
     * Returns a random double between 0 and 1.
     *
     * @return a random double in the range [0, 1)
     */
    public static double random() {
        return ThreadLocalRandom.current().nextDouble();
    }

    /**
     * Returns a random float between min and max.
     *
     * @param min the minimum value (inclusive)
     * @param max the maximum value (exclusive)
     * @return a random float in the range [min, max)
     */
    public static float randomFloat(float min, float max) {
        if (min > max) {
            float temp = min;
            min = max;
            max = temp;
        }
        return min + ThreadLocalRandom.current().nextFloat() * (max - min);
    }

    /**
     * Returns a random boolean.
     *
     * @return true or false with 50% probability each
     */
    public static boolean randomBoolean() {
        return ThreadLocalRandom.current().nextBoolean();
    }

    /**
     * Returns true with the specified probability.
     *
     * @param probability the probability (0.0 to 1.0)
     * @return true with the given probability
     *
     * <h3>Example:</h3>
     * <pre>{@code
     * // 10% chance for critical hit
     * if (MathUtils.chance(0.10)) {
     *     damage *= 2;
     * }
     * }</pre>
     */
    public static boolean chance(double probability) {
        return random() < probability;
    }

    /**
     * Returns true with the specified percentage chance.
     *
     * @param percentChance the chance as a percentage (0 to 100)
     * @return true with the given percentage chance
     *
     * <h3>Example:</h3>
     * <pre>{@code
     * // 25% chance to drop item
     * if (MathUtils.chancePercent(25)) {
     *     dropItem();
     * }
     * }</pre>
     */
    public static boolean chancePercent(double percentChance) {
        return chance(percentChance / 100.0);
    }

    /**
     * Returns a random Gaussian (normally distributed) value.
     *
     * @param mean the mean of the distribution
     * @param stdDev the standard deviation
     * @return a random value from the normal distribution
     */
    public static double randomGaussian(double mean, double stdDev) {
        return mean + ThreadLocalRandom.current().nextGaussian() * stdDev;
    }

    // ==================== Percentage ====================

    /**
     * Calculates the percentage of a value relative to a total.
     *
     * @param value the current value
     * @param total the total value
     * @return the percentage (0 to 100)
     *
     * <h3>Example:</h3>
     * <pre>{@code
     * double healthPercent = MathUtils.percentage(currentHealth, maxHealth);
     * // e.g., 75.0 for 15/20 health
     * }</pre>
     */
    public static double percentage(double value, double total) {
        if (total == 0) {
            return 0.0;
        }
        return (value / total) * 100.0;
    }

    /**
     * Calculates a value from a percentage of a total.
     *
     * @param percent the percentage (0 to 100)
     * @param total the total value
     * @return the calculated value
     *
     * <h3>Example:</h3>
     * <pre>{@code
     * double healing = MathUtils.percentageOf(25, maxHealth);
     * // Heal 25% of max health
     * }</pre>
     */
    public static double percentageOf(double percent, double total) {
        return (percent / 100.0) * total;
    }

    /**
     * Returns the fractional representation of a percentage.
     *
     * @param percent the percentage (0 to 100)
     * @return the fraction (0 to 1)
     */
    public static double percentToFraction(double percent) {
        return percent / 100.0;
    }

    /**
     * Returns the percentage representation of a fraction.
     *
     * @param fraction the fraction (0 to 1)
     * @return the percentage (0 to 100)
     */
    public static double fractionToPercent(double fraction) {
        return fraction * 100.0;
    }

    // ==================== Rounding ====================

    /**
     * Rounds a value to the specified number of decimal places.
     *
     * @param value the value to round
     * @param places the number of decimal places
     * @return the rounded value
     *
     * <h3>Example:</h3>
     * <pre>{@code
     * double rounded = MathUtils.round(3.14159, 2);
     * // rounded = 3.14
     * }</pre>
     */
    public static double round(double value, int places) {
        if (places < 0) {
            throw new IllegalArgumentException("Decimal places cannot be negative");
        }
        double factor = Math.pow(10, places);
        return Math.round(value * factor) / factor;
    }

    /**
     * Rounds a value down to the nearest multiple.
     *
     * @param value the value to round
     * @param multiple the multiple to round to
     * @return the rounded value
     *
     * <h3>Example:</h3>
     * <pre>{@code
     * int slot = MathUtils.floorToMultiple(17, 9);
     * // slot = 9
     * }</pre>
     */
    public static int floorToMultiple(int value, int multiple) {
        return (value / multiple) * multiple;
    }

    /**
     * Rounds a value up to the nearest multiple.
     *
     * @param value the value to round
     * @param multiple the multiple to round to
     * @return the rounded value
     */
    public static int ceilToMultiple(int value, int multiple) {
        return ((value + multiple - 1) / multiple) * multiple;
    }

    // ==================== Comparison ====================

    /**
     * Checks if two double values are approximately equal.
     *
     * @param a the first value
     * @param b the second value
     * @return true if the values are approximately equal
     */
    public static boolean approximately(double a, double b) {
        return Math.abs(a - b) < EPSILON;
    }

    /**
     * Checks if two double values are approximately equal within a tolerance.
     *
     * @param a the first value
     * @param b the second value
     * @param tolerance the maximum allowed difference
     * @return true if the values are within the tolerance
     */
    public static boolean approximately(double a, double b, double tolerance) {
        return Math.abs(a - b) < tolerance;
    }

    /**
     * Checks if two float values are approximately equal.
     *
     * @param a the first value
     * @param b the second value
     * @return true if the values are approximately equal
     */
    public static boolean approximately(float a, float b) {
        return Math.abs(a - b) < EPSILON_F;
    }

    // ==================== Angles ====================

    /**
     * Converts degrees to radians.
     *
     * @param degrees the angle in degrees
     * @return the angle in radians
     */
    public static double toRadians(double degrees) {
        return degrees * DEG_TO_RAD;
    }

    /**
     * Converts radians to degrees.
     *
     * @param radians the angle in radians
     * @return the angle in degrees
     */
    public static double toDegrees(double radians) {
        return radians * RAD_TO_DEG;
    }

    /**
     * Normalizes an angle to the range [0, 360).
     *
     * @param degrees the angle in degrees
     * @return the normalized angle
     */
    public static double normalizeAngle(double degrees) {
        degrees = degrees % 360.0;
        if (degrees < 0) {
            degrees += 360.0;
        }
        return degrees;
    }

    /**
     * Calculates the shortest angular distance between two angles.
     *
     * @param from the starting angle in degrees
     * @param to the target angle in degrees
     * @return the shortest angular distance (-180 to 180)
     */
    public static double angleDifference(double from, double to) {
        double diff = normalizeAngle(to - from);
        if (diff > 180.0) {
            diff -= 360.0;
        }
        return diff;
    }

    /**
     * Linearly interpolates between two angles, taking the shortest path.
     *
     * @param from the starting angle in degrees
     * @param to the target angle in degrees
     * @param t the interpolation factor (0 to 1)
     * @return the interpolated angle
     */
    public static double lerpAngle(double from, double to, double t) {
        double diff = angleDifference(from, to);
        return normalizeAngle(from + diff * clamp01(t));
    }

    // ==================== Misc ====================

    /**
     * Returns the sign of a number.
     *
     * @param value the value to check
     * @return -1, 0, or 1 depending on the sign
     */
    public static int sign(double value) {
        if (value > 0) return 1;
        if (value < 0) return -1;
        return 0;
    }

    /**
     * Returns the sign of an integer.
     *
     * @param value the value to check
     * @return -1, 0, or 1 depending on the sign
     */
    public static int sign(int value) {
        if (value > 0) return 1;
        if (value < 0) return -1;
        return 0;
    }

    /**
     * Wraps a value to a range [min, max).
     *
     * @param value the value to wrap
     * @param min the minimum of the range
     * @param max the maximum of the range
     * @return the wrapped value
     */
    public static double wrap(double value, double min, double max) {
        double range = max - min;
        return min + ((((value - min) % range) + range) % range);
    }

    /**
     * Wraps an integer to a range [0, max).
     *
     * @param value the value to wrap
     * @param max the maximum of the range (exclusive)
     * @return the wrapped value
     */
    public static int wrap(int value, int max) {
        return ((value % max) + max) % max;
    }

    /**
     * Calculates the distance between two points in 2D.
     *
     * @param x1 the x-coordinate of the first point
     * @param y1 the y-coordinate of the first point
     * @param x2 the x-coordinate of the second point
     * @param y2 the y-coordinate of the second point
     * @return the distance between the points
     */
    public static double distance(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Calculates the squared distance between two points in 2D.
     *
     * <p>This is faster than distance() when you only need to compare distances.</p>
     *
     * @param x1 the x-coordinate of the first point
     * @param y1 the y-coordinate of the first point
     * @param x2 the x-coordinate of the second point
     * @param y2 the y-coordinate of the second point
     * @return the squared distance between the points
     */
    public static double distanceSquared(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return dx * dx + dy * dy;
    }

    /**
     * Checks if a point is within a circular radius.
     *
     * @param x the x-coordinate of the point
     * @param y the y-coordinate of the point
     * @param centerX the x-coordinate of the circle center
     * @param centerY the y-coordinate of the circle center
     * @param radius the radius of the circle
     * @return true if the point is within the radius
     */
    public static boolean withinRadius(double x, double y, double centerX, double centerY, double radius) {
        return distanceSquared(x, y, centerX, centerY) <= radius * radius;
    }
}
