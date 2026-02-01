/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.core.util.stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.*;
import java.util.stream.*;

/**
 * Utility class providing additional stream operations and helpers.
 *
 * <p>Extends Java's Stream API with commonly needed operations for game
 * development and Minecraft plugin development.
 *
 * <h2>Filtering Utilities</h2>
 * <pre>{@code
 * // Filter by type
 * List<Player> players = StreamUtils.filterByType(entities, Player.class);
 *
 * // Filter non-null
 * List<String> names = StreamUtils.filterNonNull(nullableNames);
 *
 * // Distinct by property
 * List<Player> uniqueByName = players.stream()
 *     .filter(StreamUtils.distinctByKey(Player::getName))
 *     .toList();
 * }</pre>
 *
 * <h2>Collection Operations</h2>
 * <pre>{@code
 * // Partition a stream
 * Map<Boolean, List<Player>> partitioned = StreamUtils.partition(
 *     players.stream(),
 *     Player::isOnline
 * );
 *
 * // Batch processing
 * StreamUtils.batch(items.stream(), 10).forEach(batch -> {
 *     processBatch(batch);
 * });
 *
 * // Zip two streams
 * Stream<Pair<A, B>> zipped = StreamUtils.zip(streamA, streamB);
 * }</pre>
 *
 * <h2>Async Operations</h2>
 * <pre>{@code
 * // Map async
 * CompletableFuture<List<Data>> results = StreamUtils.mapAsync(
 *     players.stream(),
 *     player -> loadPlayerDataAsync(player)
 * );
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
public final class StreamUtils {

    private StreamUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // ==================== Filtering ====================

    /**
     * Creates a predicate that filters elements to those of a specific type.
     *
     * @param type the class type to filter by
     * @param <T>  the source type
     * @param <R>  the target type
     * @return a predicate that matches the type
     * @since 1.0.0
     */
    @NotNull
    public static <T, R> Predicate<T> isInstanceOf(@NotNull Class<R> type) {
        Objects.requireNonNull(type, "type cannot be null");
        return type::isInstance;
    }

    /**
     * Filters a stream to only elements of the specified type and casts them.
     *
     * @param stream the source stream
     * @param type   the type to filter by
     * @param <T>    the source type
     * @param <R>    the target type
     * @return a stream of the target type
     * @since 1.0.0
     */
    @NotNull
    public static <T, R> Stream<R> filterByType(@NotNull Stream<T> stream, @NotNull Class<R> type) {
        Objects.requireNonNull(stream, "stream cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        return stream.filter(type::isInstance).map(type::cast);
    }

    /**
     * Filters a collection to only elements of the specified type.
     *
     * @param collection the source collection
     * @param type       the type to filter by
     * @param <T>        the source type
     * @param <R>        the target type
     * @return a list of matching elements
     * @since 1.0.0
     */
    @NotNull
    public static <T, R> List<R> filterByType(@NotNull Collection<T> collection, @NotNull Class<R> type) {
        return filterByType(collection.stream(), type).toList();
    }

    /**
     * Filters null elements from a stream.
     *
     * @param stream the source stream
     * @param <T>    the element type
     * @return a stream with nulls removed
     * @since 1.0.0
     */
    @NotNull
    public static <T> Stream<T> filterNonNull(@NotNull Stream<T> stream) {
        return stream.filter(Objects::nonNull);
    }

    /**
     * Filters null elements from a collection.
     *
     * @param collection the source collection
     * @param <T>        the element type
     * @return a list with nulls removed
     * @since 1.0.0
     */
    @NotNull
    public static <T> List<T> filterNonNull(@Nullable Collection<T> collection) {
        if (collection == null) return List.of();
        return filterNonNull(collection.stream()).toList();
    }

    /**
     * Creates a predicate that filters to distinct elements by a key.
     *
     * <p>This is useful when you want to filter to unique elements based on
     * a property rather than the element itself.
     *
     * @param keyExtractor the function to extract the key
     * @param <T>          the element type
     * @return a stateful predicate for use with filter()
     * @since 1.0.0
     */
    @NotNull
    public static <T> Predicate<T> distinctByKey(@NotNull Function<? super T, ?> keyExtractor) {
        Objects.requireNonNull(keyExtractor, "keyExtractor cannot be null");
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    /**
     * Filters to distinct elements by a key.
     *
     * @param stream       the source stream
     * @param keyExtractor the function to extract the key
     * @param <T>          the element type
     * @return a stream with distinct elements by key
     * @since 1.0.0
     */
    @NotNull
    public static <T> Stream<T> distinctBy(@NotNull Stream<T> stream,
                                           @NotNull Function<? super T, ?> keyExtractor) {
        return stream.filter(distinctByKey(keyExtractor));
    }

    // ==================== Transformation ====================

    /**
     * Flat maps an optional stream (filters out empty optionals).
     *
     * @param stream the source stream of optionals
     * @param <T>    the element type
     * @return a stream of present values
     * @since 1.0.0
     */
    @NotNull
    public static <T> Stream<T> flatMapOptional(@NotNull Stream<Optional<T>> stream) {
        return stream.filter(Optional::isPresent).map(Optional::get);
    }

    /**
     * Maps elements using a function that returns an Optional, filtering empty results.
     *
     * @param stream the source stream
     * @param mapper the mapping function
     * @param <T>    the source type
     * @param <R>    the result type
     * @return a stream of mapped values
     * @since 1.0.0
     */
    @NotNull
    public static <T, R> Stream<R> mapOptional(@NotNull Stream<T> stream,
                                                @NotNull Function<? super T, Optional<R>> mapper) {
        return stream.map(mapper).filter(Optional::isPresent).map(Optional::get);
    }

    /**
     * Maps elements, filtering out null results.
     *
     * @param stream the source stream
     * @param mapper the mapping function
     * @param <T>    the source type
     * @param <R>    the result type
     * @return a stream of non-null mapped values
     * @since 1.0.0
     */
    @NotNull
    public static <T, R> Stream<R> mapNonNull(@NotNull Stream<T> stream,
                                               @NotNull Function<? super T, R> mapper) {
        return stream.map(mapper).filter(Objects::nonNull);
    }

    // ==================== Collection Operations ====================

    /**
     * Partitions a stream into two groups based on a predicate.
     *
     * @param stream    the source stream
     * @param predicate the partitioning predicate
     * @param <T>       the element type
     * @return a map with true/false keys and corresponding lists
     * @since 1.0.0
     */
    @NotNull
    public static <T> Map<Boolean, List<T>> partition(@NotNull Stream<T> stream,
                                                       @NotNull Predicate<T> predicate) {
        return stream.collect(Collectors.partitioningBy(predicate));
    }

    /**
     * Batches a stream into fixed-size chunks.
     *
     * @param stream    the source stream
     * @param batchSize the size of each batch
     * @param <T>       the element type
     * @return a stream of lists, each containing up to batchSize elements
     * @since 1.0.0
     */
    @NotNull
    public static <T> Stream<List<T>> batch(@NotNull Stream<T> stream, int batchSize) {
        if (batchSize < 1) {
            throw new IllegalArgumentException("batchSize must be >= 1");
        }

        Iterator<T> iterator = stream.iterator();
        if (!iterator.hasNext()) {
            return Stream.empty();
        }

        return StreamSupport.stream(
                new Spliterators.AbstractSpliterator<List<T>>(Long.MAX_VALUE, Spliterator.ORDERED) {
                    @Override
                    public boolean tryAdvance(Consumer<? super List<T>> action) {
                        if (!iterator.hasNext()) {
                            return false;
                        }
                        List<T> batch = new ArrayList<>(batchSize);
                        for (int i = 0; i < batchSize && iterator.hasNext(); i++) {
                            batch.add(iterator.next());
                        }
                        action.accept(batch);
                        return true;
                    }
                },
                false
        );
    }

    /**
     * Batches a collection into fixed-size chunks.
     *
     * @param collection the source collection
     * @param batchSize  the size of each batch
     * @param <T>        the element type
     * @return a list of batches
     * @since 1.0.0
     */
    @NotNull
    public static <T> List<List<T>> batch(@NotNull Collection<T> collection, int batchSize) {
        return batch(collection.stream(), batchSize).toList();
    }

    /**
     * Zips two streams together into pairs.
     *
     * <p>The resulting stream has the length of the shorter input stream.
     *
     * @param first  the first stream
     * @param second the second stream
     * @param <A>    the first element type
     * @param <B>    the second element type
     * @return a stream of pairs
     * @since 1.0.0
     */
    @NotNull
    public static <A, B> Stream<Pair<A, B>> zip(@NotNull Stream<A> first, @NotNull Stream<B> second) {
        return zip(first, second, Pair::of);
    }

    /**
     * Zips two streams together using a combiner function.
     *
     * @param first    the first stream
     * @param second   the second stream
     * @param combiner the combiner function
     * @param <A>      the first element type
     * @param <B>      the second element type
     * @param <R>      the result type
     * @return a stream of combined elements
     * @since 1.0.0
     */
    @NotNull
    public static <A, B, R> Stream<R> zip(@NotNull Stream<A> first, @NotNull Stream<B> second,
                                           @NotNull BiFunction<? super A, ? super B, ? extends R> combiner) {
        Objects.requireNonNull(first, "first cannot be null");
        Objects.requireNonNull(second, "second cannot be null");
        Objects.requireNonNull(combiner, "combiner cannot be null");

        Iterator<A> iteratorA = first.iterator();
        Iterator<B> iteratorB = second.iterator();

        return StreamSupport.stream(
                new Spliterators.AbstractSpliterator<R>(Long.MAX_VALUE, Spliterator.ORDERED) {
                    @Override
                    public boolean tryAdvance(Consumer<? super R> action) {
                        if (iteratorA.hasNext() && iteratorB.hasNext()) {
                            action.accept(combiner.apply(iteratorA.next(), iteratorB.next()));
                            return true;
                        }
                        return false;
                    }
                },
                false
        );
    }

    /**
     * Zips a stream with indices (0-based).
     *
     * @param stream the source stream
     * @param <T>    the element type
     * @return a stream of indexed elements
     * @since 1.0.0
     */
    @NotNull
    public static <T> Stream<Indexed<T>> zipWithIndex(@NotNull Stream<T> stream) {
        Iterator<T> iterator = stream.iterator();
        return StreamSupport.stream(
                new Spliterators.AbstractSpliterator<Indexed<T>>(Long.MAX_VALUE, Spliterator.ORDERED) {
                    int index = 0;

                    @Override
                    public boolean tryAdvance(Consumer<? super Indexed<T>> action) {
                        if (iterator.hasNext()) {
                            action.accept(new Indexed<>(index++, iterator.next()));
                            return true;
                        }
                        return false;
                    }
                },
                false
        );
    }

    /**
     * Creates a stream from an iterator.
     *
     * @param iterator the iterator
     * @param <T>      the element type
     * @return a stream
     * @since 1.0.0
     */
    @NotNull
    public static <T> Stream<T> stream(@NotNull Iterator<T> iterator) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
                false
        );
    }

    /**
     * Creates a stream from an iterable.
     *
     * @param iterable the iterable
     * @param <T>      the element type
     * @return a stream
     * @since 1.0.0
     */
    @NotNull
    public static <T> Stream<T> stream(@NotNull Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    /**
     * Creates a stream from an enumeration.
     *
     * @param enumeration the enumeration
     * @param <T>         the element type
     * @return a stream
     * @since 1.0.0
     */
    @NotNull
    public static <T> Stream<T> stream(@NotNull Enumeration<T> enumeration) {
        return stream(enumeration.asIterator());
    }

    // ==================== Async Operations ====================

    /**
     * Maps elements asynchronously and collects results.
     *
     * @param stream the source stream
     * @param mapper the async mapping function
     * @param <T>    the source type
     * @param <R>    the result type
     * @return a future containing the list of results
     * @since 1.0.0
     */
    @NotNull
    public static <T, R> CompletableFuture<List<R>> mapAsync(
            @NotNull Stream<T> stream,
            @NotNull Function<T, CompletableFuture<R>> mapper) {

        List<CompletableFuture<R>> futures = stream.map(mapper).toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .toList());
    }

    /**
     * Filters elements asynchronously.
     *
     * @param stream    the source stream
     * @param predicate the async predicate
     * @param <T>       the element type
     * @return a future containing the filtered list
     * @since 1.0.0
     */
    @NotNull
    public static <T> CompletableFuture<List<T>> filterAsync(
            @NotNull Stream<T> stream,
            @NotNull Function<T, CompletableFuture<Boolean>> predicate) {

        List<T> elements = stream.toList();
        List<CompletableFuture<Boolean>> futures = elements.stream()
                .map(predicate)
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    List<T> result = new ArrayList<>();
                    for (int i = 0; i < elements.size(); i++) {
                        if (futures.get(i).join()) {
                            result.add(elements.get(i));
                        }
                    }
                    return result;
                });
    }

    // ==================== Aggregation ====================

    /**
     * Finds the maximum element by a comparable key.
     *
     * @param stream       the source stream
     * @param keyExtractor the key extractor
     * @param <T>          the element type
     * @param <U>          the key type
     * @return an optional containing the max element
     * @since 1.0.0
     */
    @NotNull
    public static <T, U extends Comparable<? super U>> Optional<T> maxBy(
            @NotNull Stream<T> stream,
            @NotNull Function<T, U> keyExtractor) {
        return stream.max(Comparator.comparing(keyExtractor));
    }

    /**
     * Finds the minimum element by a comparable key.
     *
     * @param stream       the source stream
     * @param keyExtractor the key extractor
     * @param <T>          the element type
     * @param <U>          the key type
     * @return an optional containing the min element
     * @since 1.0.0
     */
    @NotNull
    public static <T, U extends Comparable<? super U>> Optional<T> minBy(
            @NotNull Stream<T> stream,
            @NotNull Function<T, U> keyExtractor) {
        return stream.min(Comparator.comparing(keyExtractor));
    }

    /**
     * Collects a stream to a LinkedHashSet (maintains insertion order).
     *
     * @param stream the source stream
     * @param <T>    the element type
     * @return a LinkedHashSet
     * @since 1.0.0
     */
    @NotNull
    public static <T> Set<T> toLinkedHashSet(@NotNull Stream<T> stream) {
        return stream.collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Collects a stream to an EnumSet.
     *
     * @param stream    the source stream
     * @param enumClass the enum class
     * @param <E>       the enum type
     * @return an EnumSet
     * @since 1.0.0
     */
    @NotNull
    public static <E extends Enum<E>> EnumSet<E> toEnumSet(@NotNull Stream<E> stream,
                                                           @NotNull Class<E> enumClass) {
        return stream.collect(Collectors.toCollection(() -> EnumSet.noneOf(enumClass)));
    }

    // ==================== Helper Records ====================

    /**
     * A simple pair of two values.
     *
     * @param first  the first value
     * @param second the second value
     * @param <A>    the first type
     * @param <B>    the second type
     * @since 1.0.0
     */
    public record Pair<A, B>(A first, B second) {
        /**
         * Creates a new Pair.
         *
         * @param first  the first value
         * @param second the second value
         * @param <A>    the first type
         * @param <B>    the second type
         * @return a new Pair
         */
        @NotNull
        public static <A, B> Pair<A, B> of(A first, B second) {
            return new Pair<>(first, second);
        }
    }

    /**
     * A value with its index in a stream.
     *
     * @param index the 0-based index
     * @param value the value
     * @param <T>   the value type
     * @since 1.0.0
     */
    public record Indexed<T>(int index, T value) {
        /**
         * Creates a new Indexed value.
         *
         * @param index the index
         * @param value the value
         * @param <T>   the value type
         * @return a new Indexed
         */
        @NotNull
        public static <T> Indexed<T> of(int index, T value) {
            return new Indexed<>(index, value);
        }
    }
}
