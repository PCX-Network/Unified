/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.redis.lettuce;

import io.lettuce.core.RedisFuture;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;
import sh.pcx.unified.data.redis.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Lettuce-based implementation of {@link RedisService}.
 *
 * <p>This implementation uses Lettuce for Redis operations, providing native
 * async support with CompletableFuture. Lettuce connections are thread-safe
 * and can be shared across threads.
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li>Native async operations (no thread pool needed)</li>
 *   <li>Thread-safe connection sharing</li>
 *   <li>Automatic reconnection</li>
 *   <li>Reactive streams support</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create the service
 * RedisConfig config = RedisConfig.builder()
 *     .host("localhost")
 *     .port(6379)
 *     .client(RedisClient.LETTUCE)
 *     .build();
 *
 * LettuceRedisService redis = new LettuceRedisService(config);
 *
 * // Sync operations
 * redis.set("key", "value");
 * Optional<String> value = redis.get("key");
 *
 * // Native async operations
 * redis.setAsync("asyncKey", "asyncValue")
 *     .thenCompose(v -> redis.getAsync("asyncKey"))
 *     .thenAccept(v -> System.out.println("Value: " + v.orElse("null")));
 *
 * // Shutdown
 * redis.shutdown();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see RedisService
 * @see LettuceProvider
 */
public class LettuceRedisService implements RedisService {

    private final RedisConfig config;
    private final LettuceProvider provider;
    private final LettucePubSubImpl pubSub;
    private final Map<String, RedisLuaScript> scriptCache = new ConcurrentHashMap<>();
    private volatile boolean shutdown = false;

    /**
     * Creates a new Lettuce-based Redis service.
     *
     * @param config the Redis configuration
     * @since 1.0.0
     */
    public LettuceRedisService(@NotNull RedisConfig config) {
        this.config = Objects.requireNonNull(config, "config cannot be null");
        this.provider = new LettuceProvider(config);
        this.pubSub = new LettucePubSubImpl(this);
    }

    /**
     * Gets synchronous commands.
     */
    private RedisCommands<String, String> sync() {
        checkNotShutdown();
        return provider.sync();
    }

    /**
     * Gets asynchronous commands.
     */
    private RedisAsyncCommands<String, String> async() {
        checkNotShutdown();
        return provider.async();
    }

    @Override
    @NotNull
    public RedisConfig getConfig() {
        return config;
    }

    @Override
    @NotNull
    public RedisConnection getConnection() {
        checkNotShutdown();
        return new LettuceConnectionWrapper(provider);
    }

    @Override
    public <T> T withConnection(@NotNull Function<RedisConnection, T> function) {
        checkNotShutdown();
        try (RedisConnection conn = getConnection()) {
            return function.apply(conn);
        }
    }

    @Override
    @NotNull
    public <T> CompletableFuture<T> withConnectionAsync(@NotNull Function<RedisConnection, T> function) {
        checkNotShutdown();
        return CompletableFuture.supplyAsync(() -> withConnection(function));
    }

    @Override
    public boolean isHealthy() {
        try {
            return provider.isHealthy();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> isHealthyAsync() {
        return toCompletableFuture(async().ping())
                .thenApply("PONG"::equals)
                .exceptionally(e -> false);
    }

    @Override
    @NotNull
    public PoolStats getPoolStats() {
        return provider.getConnectionManager().getStats();
    }

    @Override
    @NotNull
    public KeyNamespace namespace(@NotNull String prefix) {
        return new KeyNamespace(this, prefix);
    }

    @Override
    @NotNull
    public KeyNamespace namespace(@NotNull String... prefixes) {
        return new KeyNamespace(this, prefixes);
    }

    // ========== String Operations ==========

    @Override
    @NotNull
    public Optional<String> get(@NotNull String key) {
        return Optional.ofNullable(sync().get(key));
    }

    @Override
    @NotNull
    public CompletableFuture<Optional<String>> getAsync(@NotNull String key) {
        return toCompletableFuture(async().get(key))
                .thenApply(Optional::ofNullable);
    }

    @Override
    public void set(@NotNull String key, @NotNull String value) {
        sync().set(key, value);
    }

    @Override
    @NotNull
    public CompletableFuture<Void> setAsync(@NotNull String key, @NotNull String value) {
        return toCompletableFuture(async().set(key, value))
                .thenApply(v -> null);
    }

    @Override
    public void setex(@NotNull String key, @NotNull String value, @NotNull Duration ttl) {
        sync().setex(key, ttl.toSeconds(), value);
    }

    @Override
    @NotNull
    public CompletableFuture<Void> setexAsync(@NotNull String key, @NotNull String value, @NotNull Duration ttl) {
        return toCompletableFuture(async().setex(key, ttl.toSeconds(), value))
                .thenApply(v -> null);
    }

    @Override
    public boolean setnx(@NotNull String key, @NotNull String value) {
        return Boolean.TRUE.equals(sync().setnx(key, value));
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> setnxAsync(@NotNull String key, @NotNull String value) {
        return toCompletableFuture(async().setnx(key, value))
                .thenApply(Boolean.TRUE::equals);
    }

    @Override
    @NotNull
    public List<@Nullable String> mget(@NotNull String... keys) {
        return sync().mget(keys).stream()
                .map(kv -> kv.hasValue() ? kv.getValue() : null)
                .toList();
    }

    @Override
    @NotNull
    public CompletableFuture<List<@Nullable String>> mgetAsync(@NotNull String... keys) {
        return toCompletableFuture(async().mget(keys))
                .thenApply(kvs -> kvs.stream()
                        .map(kv -> kv.hasValue() ? kv.getValue() : null)
                        .toList());
    }

    @Override
    public void mset(@NotNull Map<String, String> keyValues) {
        if (!keyValues.isEmpty()) {
            sync().mset(keyValues);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Void> msetAsync(@NotNull Map<String, String> keyValues) {
        if (keyValues.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return toCompletableFuture(async().mset(keyValues))
                .thenApply(v -> null);
    }

    // ========== Key Operations ==========

    @Override
    public long del(@NotNull String... keys) {
        Long result = sync().del(keys);
        return result != null ? result : 0;
    }

    @Override
    @NotNull
    public CompletableFuture<Long> delAsync(@NotNull String... keys) {
        return toCompletableFuture(async().del(keys))
                .thenApply(r -> r != null ? r : 0L);
    }

    @Override
    public boolean exists(@NotNull String key) {
        Long result = sync().exists(key);
        return result != null && result > 0;
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> existsAsync(@NotNull String key) {
        return toCompletableFuture(async().exists(key))
                .thenApply(r -> r != null && r > 0);
    }

    @Override
    public boolean expire(@NotNull String key, @NotNull Duration ttl) {
        return Boolean.TRUE.equals(sync().expire(key, ttl.toSeconds()));
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> expireAsync(@NotNull String key, @NotNull Duration ttl) {
        return toCompletableFuture(async().expire(key, ttl.toSeconds()))
                .thenApply(Boolean.TRUE::equals);
    }

    @Override
    @NotNull
    public Optional<Duration> ttl(@NotNull String key) {
        Long ttl = sync().ttl(key);
        if (ttl == null || ttl < 0) {
            return Optional.empty();
        }
        return Optional.of(Duration.ofSeconds(ttl));
    }

    @Override
    @NotNull
    public CompletableFuture<Optional<Duration>> ttlAsync(@NotNull String key) {
        return toCompletableFuture(async().ttl(key))
                .thenApply(ttl -> {
                    if (ttl == null || ttl < 0) {
                        return Optional.empty();
                    }
                    return Optional.of(Duration.ofSeconds(ttl));
                });
    }

    // ========== Numeric Operations ==========

    @Override
    public long incr(@NotNull String key) {
        Long result = sync().incr(key);
        return result != null ? result : 0;
    }

    @Override
    @NotNull
    public CompletableFuture<Long> incrAsync(@NotNull String key) {
        return toCompletableFuture(async().incr(key))
                .thenApply(r -> r != null ? r : 0L);
    }

    @Override
    public long incrBy(@NotNull String key, long increment) {
        Long result = sync().incrby(key, increment);
        return result != null ? result : 0;
    }

    @Override
    @NotNull
    public CompletableFuture<Long> incrByAsync(@NotNull String key, long increment) {
        return toCompletableFuture(async().incrby(key, increment))
                .thenApply(r -> r != null ? r : 0L);
    }

    @Override
    public long decr(@NotNull String key) {
        Long result = sync().decr(key);
        return result != null ? result : 0;
    }

    @Override
    @NotNull
    public CompletableFuture<Long> decrAsync(@NotNull String key) {
        return toCompletableFuture(async().decr(key))
                .thenApply(r -> r != null ? r : 0L);
    }

    // ========== Hash Operations ==========

    @Override
    @NotNull
    public Optional<String> hget(@NotNull String key, @NotNull String field) {
        return Optional.ofNullable(sync().hget(key, field));
    }

    @Override
    @NotNull
    public CompletableFuture<Optional<String>> hgetAsync(@NotNull String key, @NotNull String field) {
        return toCompletableFuture(async().hget(key, field))
                .thenApply(Optional::ofNullable);
    }

    @Override
    public boolean hset(@NotNull String key, @NotNull String field, @NotNull String value) {
        return Boolean.TRUE.equals(sync().hset(key, field, value));
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> hsetAsync(@NotNull String key, @NotNull String field, @NotNull String value) {
        return toCompletableFuture(async().hset(key, field, value))
                .thenApply(Boolean.TRUE::equals);
    }

    @Override
    @NotNull
    public Map<String, String> hgetAll(@NotNull String key) {
        return sync().hgetall(key);
    }

    @Override
    @NotNull
    public CompletableFuture<Map<String, String>> hgetAllAsync(@NotNull String key) {
        return toCompletableFuture(async().hgetall(key));
    }

    @Override
    public void hmset(@NotNull String key, @NotNull Map<String, String> fieldValues) {
        if (!fieldValues.isEmpty()) {
            sync().hmset(key, fieldValues);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Void> hmsetAsync(@NotNull String key, @NotNull Map<String, String> fieldValues) {
        if (fieldValues.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return toCompletableFuture(async().hmset(key, fieldValues))
                .thenApply(v -> null);
    }

    @Override
    public long hdel(@NotNull String key, @NotNull String... fields) {
        Long result = sync().hdel(key, fields);
        return result != null ? result : 0;
    }

    @Override
    @NotNull
    public CompletableFuture<Long> hdelAsync(@NotNull String key, @NotNull String... fields) {
        return toCompletableFuture(async().hdel(key, fields))
                .thenApply(r -> r != null ? r : 0L);
    }

    // ========== List Operations ==========

    @Override
    public long lpush(@NotNull String key, @NotNull String... values) {
        Long result = sync().lpush(key, values);
        return result != null ? result : 0;
    }

    @Override
    @NotNull
    public CompletableFuture<Long> lpushAsync(@NotNull String key, @NotNull String... values) {
        return toCompletableFuture(async().lpush(key, values))
                .thenApply(r -> r != null ? r : 0L);
    }

    @Override
    public long rpush(@NotNull String key, @NotNull String... values) {
        Long result = sync().rpush(key, values);
        return result != null ? result : 0;
    }

    @Override
    @NotNull
    public CompletableFuture<Long> rpushAsync(@NotNull String key, @NotNull String... values) {
        return toCompletableFuture(async().rpush(key, values))
                .thenApply(r -> r != null ? r : 0L);
    }

    @Override
    @NotNull
    public Optional<String> lpop(@NotNull String key) {
        return Optional.ofNullable(sync().lpop(key));
    }

    @Override
    @NotNull
    public CompletableFuture<Optional<String>> lpopAsync(@NotNull String key) {
        return toCompletableFuture(async().lpop(key))
                .thenApply(Optional::ofNullable);
    }

    @Override
    @NotNull
    public Optional<String> rpop(@NotNull String key) {
        return Optional.ofNullable(sync().rpop(key));
    }

    @Override
    @NotNull
    public CompletableFuture<Optional<String>> rpopAsync(@NotNull String key) {
        return toCompletableFuture(async().rpop(key))
                .thenApply(Optional::ofNullable);
    }

    @Override
    @NotNull
    public List<String> lrange(@NotNull String key, long start, long stop) {
        return sync().lrange(key, start, stop);
    }

    @Override
    @NotNull
    public CompletableFuture<List<String>> lrangeAsync(@NotNull String key, long start, long stop) {
        return toCompletableFuture(async().lrange(key, start, stop));
    }

    // ========== Set Operations ==========

    @Override
    public long sadd(@NotNull String key, @NotNull String... members) {
        Long result = sync().sadd(key, members);
        return result != null ? result : 0;
    }

    @Override
    @NotNull
    public CompletableFuture<Long> saddAsync(@NotNull String key, @NotNull String... members) {
        return toCompletableFuture(async().sadd(key, members))
                .thenApply(r -> r != null ? r : 0L);
    }

    @Override
    @NotNull
    public Set<String> smembers(@NotNull String key) {
        return sync().smembers(key);
    }

    @Override
    @NotNull
    public CompletableFuture<Set<String>> smembersAsync(@NotNull String key) {
        return toCompletableFuture(async().smembers(key));
    }

    @Override
    public boolean sismember(@NotNull String key, @NotNull String member) {
        return Boolean.TRUE.equals(sync().sismember(key, member));
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> sismemberAsync(@NotNull String key, @NotNull String member) {
        return toCompletableFuture(async().sismember(key, member))
                .thenApply(Boolean.TRUE::equals);
    }

    @Override
    public long srem(@NotNull String key, @NotNull String... members) {
        Long result = sync().srem(key, members);
        return result != null ? result : 0;
    }

    @Override
    @NotNull
    public CompletableFuture<Long> sremAsync(@NotNull String key, @NotNull String... members) {
        return toCompletableFuture(async().srem(key, members))
                .thenApply(r -> r != null ? r : 0L);
    }

    // ========== Pub/Sub ==========

    @Override
    public long publish(@NotNull String channel, @NotNull String message) {
        Long result = sync().publish(channel, message);
        return result != null ? result : 0;
    }

    @Override
    @NotNull
    public CompletableFuture<Long> publishAsync(@NotNull String channel, @NotNull String message) {
        return toCompletableFuture(async().publish(channel, message))
                .thenApply(r -> r != null ? r : 0L);
    }

    @Override
    @NotNull
    public PubSubSubscription subscribe(@NotNull String channel, @NotNull Consumer<String> listener) {
        RedisPubSub.Subscription sub = pubSub.subscribe(channel, listener);
        return wrapSubscription(channel, sub);
    }

    @Override
    @NotNull
    public PubSubSubscription psubscribe(@NotNull String pattern, @NotNull PubSubListener listener) {
        RedisPubSub.Subscription sub = pubSub.psubscribe(pattern, listener);
        return wrapSubscription(pattern, sub);
    }

    private PubSubSubscription wrapSubscription(String channelOrPattern, RedisPubSub.Subscription sub) {
        return new PubSubSubscription() {
            @Override
            public boolean isActive() {
                return sub.isActive();
            }

            @Override
            @NotNull
            public String getChannel() {
                return channelOrPattern;
            }

            @Override
            public void unsubscribe() {
                sub.unsubscribe();
            }
        };
    }

    @Override
    @NotNull
    public RedisPubSub pubSub() {
        return pubSub;
    }

    // ========== Lua Scripts ==========

    @Override
    @Nullable
    public Object eval(@NotNull String script, @NotNull List<String> keys, @NotNull List<String> args) {
        String[] keyArray = keys.toArray(new String[0]);
        String[] argArray = args.toArray(new String[0]);
        return sync().eval(script, ScriptOutputType.MULTI, keyArray, argArray);
    }

    @Override
    @NotNull
    public CompletableFuture<Object> evalAsync(@NotNull String script, @NotNull List<String> keys, @NotNull List<String> args) {
        String[] keyArray = keys.toArray(new String[0]);
        String[] argArray = args.toArray(new String[0]);
        return toCompletableFuture(async().eval(script, ScriptOutputType.MULTI, keyArray, argArray));
    }

    @Override
    @NotNull
    public RedisLuaScript loadScript(@NotNull String script) {
        return scriptCache.computeIfAbsent(script, s -> {
            String sha1 = sync().scriptLoad(s);
            return new LettuceLuaScript(this, s, sha1);
        });
    }

    // ========== Serialization ==========

    @Override
    @NotNull
    public <T> Optional<T> get(@NotNull String key, @NotNull Class<T> type, @NotNull RedisSerializer<T> serializer) {
        return get(key).map(data -> serializer.deserialize(data, type));
    }

    @Override
    public <T> void set(@NotNull String key, @NotNull T value, @NotNull RedisSerializer<T> serializer) {
        set(key, serializer.serialize(value));
    }

    @Override
    public <T> void setex(@NotNull String key, @NotNull T value, @NotNull Duration ttl, @NotNull RedisSerializer<T> serializer) {
        setex(key, serializer.serialize(value), ttl);
    }

    // ========== Lifecycle ==========

    @Override
    public void shutdown() {
        if (!shutdown) {
            shutdown = true;
            pubSub.unsubscribeAll();
            provider.close();
        }
    }

    @Override
    public boolean isAvailable() {
        return !shutdown && isHealthy();
    }

    private void checkNotShutdown() {
        if (shutdown) {
            throw new IllegalStateException("RedisService is shut down");
        }
    }

    /**
     * Gets the Lettuce provider.
     *
     * @return the provider
     * @since 1.0.0
     */
    @NotNull
    public LettuceProvider getProvider() {
        return provider;
    }

    /**
     * Converts a RedisFuture to a CompletableFuture.
     */
    private <T> CompletableFuture<T> toCompletableFuture(RedisFuture<T> future) {
        return future.toCompletableFuture();
    }

    // ========== Inner Classes ==========

    /**
     * Lettuce Lua script implementation.
     */
    private static class LettuceLuaScript extends RedisLuaScript.AbstractLuaScript {
        private final LettuceRedisService service;

        LettuceLuaScript(LettuceRedisService service, String script, String sha1) {
            super(script, sha1);
            this.service = service;
        }

        @Override
        public boolean isLoaded() {
            List<Boolean> result = service.sync().scriptExists(sha1);
            return !result.isEmpty() && Boolean.TRUE.equals(result.get(0));
        }

        @Override
        @NotNull
        public CompletableFuture<Boolean> isLoadedAsync() {
            return service.toCompletableFuture(service.async().scriptExists(sha1))
                    .thenApply(list -> !list.isEmpty() && Boolean.TRUE.equals(list.get(0)));
        }

        @Override
        public void ensureLoaded() {
            if (!isLoaded()) {
                service.sync().scriptLoad(script);
            }
        }

        @Override
        @NotNull
        public CompletableFuture<Void> ensureLoadedAsync() {
            return isLoadedAsync().thenCompose(loaded -> {
                if (!loaded) {
                    return service.toCompletableFuture(service.async().scriptLoad(script))
                            .thenApply(v -> null);
                }
                return CompletableFuture.completedFuture(null);
            });
        }

        @Override
        @Nullable
        public Object execute(@NotNull List<String> keys, @NotNull List<String> args) {
            String[] keyArray = keys.toArray(new String[0]);
            String[] argArray = args.toArray(new String[0]);
            try {
                return service.sync().evalsha(sha1, ScriptOutputType.MULTI, keyArray, argArray);
            } catch (Exception e) {
                // Script may have been evicted, reload and retry
                service.sync().scriptLoad(script);
                return service.sync().evalsha(sha1, ScriptOutputType.MULTI, keyArray, argArray);
            }
        }

        @Override
        @NotNull
        public CompletableFuture<Object> executeAsync(@NotNull List<String> keys, @NotNull List<String> args) {
            String[] keyArray = keys.toArray(new String[0]);
            String[] argArray = args.toArray(new String[0]);
            return service.toCompletableFuture(
                    service.async().evalsha(sha1, ScriptOutputType.MULTI, keyArray, argArray)
            ).exceptionallyCompose(e -> {
                // Script may have been evicted, reload and retry
                return service.toCompletableFuture(service.async().scriptLoad(script))
                        .thenCompose(v -> service.toCompletableFuture(
                                service.async().evalsha(sha1, ScriptOutputType.MULTI, keyArray, argArray)
                        ));
            });
        }
    }

    /**
     * Lettuce Pub/Sub implementation.
     */
    private static class LettucePubSubImpl implements RedisPubSub {
        private final LettuceRedisService service;
        private final Map<String, SubscriptionHolder> subscriptions = new ConcurrentHashMap<>();
        private final Map<String, SubscriptionHolder> patternSubscriptions = new ConcurrentHashMap<>();

        LettucePubSubImpl(LettuceRedisService service) {
            this.service = service;
        }

        @Override
        public long publish(@NotNull String channel, @NotNull String message) {
            return service.publish(channel, message);
        }

        @Override
        @NotNull
        public CompletableFuture<Long> publishAsync(@NotNull String channel, @NotNull String message) {
            return service.publishAsync(channel, message);
        }

        @Override
        public long publishToMany(@NotNull String message, @NotNull String... channels) {
            long total = 0;
            for (String channel : channels) {
                total += publish(channel, message);
            }
            return total;
        }

        @Override
        @NotNull
        public CompletableFuture<Long> publishToManyAsync(@NotNull String message, @NotNull String... channels) {
            List<CompletableFuture<Long>> futures = new ArrayList<>();
            for (String channel : channels) {
                futures.add(publishAsync(channel, message));
            }
            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> futures.stream()
                            .mapToLong(f -> f.join())
                            .sum());
        }

        @Override
        @NotNull
        public Subscription subscribe(@NotNull String channel, @NotNull Consumer<String> listener) {
            SubscriptionHolder holder = new SubscriptionHolder(Set.of(channel), false);
            subscriptions.put(channel, holder);

            StatefulRedisPubSubConnection<String, String> conn = service.provider.getPubSubConnection();
            conn.addListener(new RedisPubSubListener<>() {
                @Override
                public void message(String ch, String msg) {
                    if (channel.equals(ch) && holder.isActive()) {
                        holder.incrementMessageCount();
                        listener.accept(msg);
                    }
                }

                @Override
                public void message(String pattern, String ch, String msg) {}

                @Override
                public void subscribed(String ch, long count) {}

                @Override
                public void psubscribed(String pattern, long count) {}

                @Override
                public void unsubscribed(String ch, long count) {}

                @Override
                public void punsubscribed(String pattern, long count) {}
            });

            conn.async().subscribe(channel);
            holder.setConnection(conn);

            return holder;
        }

        @Override
        @NotNull
        public Subscription subscribe(@NotNull Consumer<String> listener, @NotNull String... channels) {
            SubscriptionHolder holder = new SubscriptionHolder(Set.of(channels), false);
            for (String channel : channels) {
                subscriptions.put(channel, holder);
            }

            StatefulRedisPubSubConnection<String, String> conn = service.provider.getPubSubConnection();
            conn.addListener(new RedisPubSubListener<>() {
                @Override
                public void message(String ch, String msg) {
                    if (holder.isActive()) {
                        holder.incrementMessageCount();
                        listener.accept(msg);
                    }
                }

                @Override
                public void message(String pattern, String ch, String msg) {}

                @Override
                public void subscribed(String ch, long count) {}

                @Override
                public void psubscribed(String pattern, long count) {}

                @Override
                public void unsubscribed(String ch, long count) {}

                @Override
                public void punsubscribed(String pattern, long count) {}
            });

            conn.async().subscribe(channels);
            holder.setConnection(conn);

            return holder;
        }

        @Override
        @NotNull
        public Subscription psubscribe(@NotNull String pattern, @NotNull PubSubListener listener) {
            SubscriptionHolder holder = new SubscriptionHolder(Set.of(pattern), true);
            patternSubscriptions.put(pattern, holder);

            StatefulRedisPubSubConnection<String, String> conn = service.provider.getPubSubConnection();
            conn.addListener(new RedisPubSubListener<>() {
                @Override
                public void message(String ch, String msg) {}

                @Override
                public void message(String pat, String channel, String msg) {
                    if (pattern.equals(pat) && holder.isActive()) {
                        holder.incrementMessageCount();
                        listener.onMessage(channel, msg);
                    }
                }

                @Override
                public void subscribed(String ch, long count) {}

                @Override
                public void psubscribed(String pat, long count) {}

                @Override
                public void unsubscribed(String ch, long count) {}

                @Override
                public void punsubscribed(String pat, long count) {}
            });

            conn.async().psubscribe(pattern);
            holder.setConnection(conn);

            return holder;
        }

        @Override
        @NotNull
        public Subscription psubscribe(@NotNull PubSubListener listener, @NotNull String... patterns) {
            SubscriptionHolder holder = new SubscriptionHolder(Set.of(patterns), true);
            for (String pattern : patterns) {
                patternSubscriptions.put(pattern, holder);
            }

            StatefulRedisPubSubConnection<String, String> conn = service.provider.getPubSubConnection();
            conn.addListener(new RedisPubSubListener<>() {
                @Override
                public void message(String ch, String msg) {}

                @Override
                public void message(String pat, String channel, String msg) {
                    if (holder.isActive()) {
                        holder.incrementMessageCount();
                        listener.onMessage(channel, msg);
                    }
                }

                @Override
                public void subscribed(String ch, long count) {}

                @Override
                public void psubscribed(String pat, long count) {}

                @Override
                public void unsubscribed(String ch, long count) {}

                @Override
                public void punsubscribed(String pat, long count) {}
            });

            conn.async().psubscribe(patterns);
            holder.setConnection(conn);

            return holder;
        }

        @Override
        @NotNull
        public Set<String> getSubscribedChannels() {
            return new HashSet<>(subscriptions.keySet());
        }

        @Override
        @NotNull
        public Set<String> getSubscribedPatterns() {
            return new HashSet<>(patternSubscriptions.keySet());
        }

        @Override
        public int getSubscriptionCount() {
            return subscriptions.size() + patternSubscriptions.size();
        }

        @Override
        public boolean hasSubscriptions() {
            return !subscriptions.isEmpty() || !patternSubscriptions.isEmpty();
        }

        @Override
        public void unsubscribeAll() {
            subscriptions.values().forEach(SubscriptionHolder::unsubscribe);
            patternSubscriptions.values().forEach(SubscriptionHolder::unsubscribe);
            subscriptions.clear();
            patternSubscriptions.clear();
        }

        @Override
        public long getSubscriberCount(@NotNull String channel) {
            Map<String, Long> result = service.sync().pubsubNumsub(channel);
            return result.getOrDefault(channel, 0L);
        }

        @Override
        @NotNull
        public CompletableFuture<Map<String, Long>> getSubscriberCountsAsync(@NotNull String... channels) {
            return service.toCompletableFuture(service.async().pubsubNumsub(channels));
        }

        @Override
        @NotNull
        public Set<String> listActiveChannels(@NotNull String pattern) {
            return new HashSet<>(service.sync().pubsubChannels(pattern));
        }

        @Override
        public <T> long publish(@NotNull String channel, @NotNull T message, @NotNull RedisSerializer<T> serializer) {
            return publish(channel, serializer.serialize(message));
        }

        @Override
        @NotNull
        public <T> Subscription subscribe(@NotNull String channel, @NotNull Class<T> type,
                                          @NotNull RedisSerializer<T> serializer, @NotNull Consumer<T> listener) {
            return subscribe(channel, msg -> {
                T obj = serializer.deserialize(msg, type);
                listener.accept(obj);
            });
        }

        @Override
        @NotNull
        public SubscriptionBuilder builder() {
            return new LettuceSubscriptionBuilder(this);
        }
    }

    /**
     * Subscription holder for tracking subscriptions.
     */
    private static class SubscriptionHolder implements RedisPubSub.Subscription {
        private final Set<String> channels;
        private final boolean isPattern;
        private final AtomicBoolean active = new AtomicBoolean(true);
        private final AtomicLong messageCount = new AtomicLong();
        private final List<Runnable> unsubscribeCallbacks = new ArrayList<>();
        private StatefulRedisPubSubConnection<String, String> connection;

        SubscriptionHolder(Set<String> channels, boolean isPattern) {
            this.channels = channels;
            this.isPattern = isPattern;
        }

        void setConnection(StatefulRedisPubSubConnection<String, String> connection) {
            this.connection = connection;
        }

        void incrementMessageCount() {
            messageCount.incrementAndGet();
        }

        @Override
        @NotNull
        public Set<String> getChannels() {
            return channels;
        }

        @Override
        public boolean isPatternSubscription() {
            return isPattern;
        }

        @Override
        public boolean isActive() {
            return active.get();
        }

        @Override
        public void unsubscribe() {
            if (active.compareAndSet(true, false)) {
                if (connection != null) {
                    RedisPubSubAsyncCommands<String, String> async = connection.async();
                    if (isPattern) {
                        async.punsubscribe(channels.toArray(new String[0]));
                    } else {
                        async.unsubscribe(channels.toArray(new String[0]));
                    }
                }
                unsubscribeCallbacks.forEach(Runnable::run);
            }
        }

        @Override
        @NotNull
        public RedisPubSub.Subscription onUnsubscribe(@NotNull Runnable callback) {
            unsubscribeCallbacks.add(callback);
            return this;
        }

        @Override
        public long getMessageCount() {
            return messageCount.get();
        }
    }

    /**
     * Subscription builder implementation.
     */
    private static class LettuceSubscriptionBuilder implements RedisPubSub.SubscriptionBuilder {
        private final LettucePubSubImpl pubSub;
        private final Set<String> channels = new HashSet<>();
        private final Set<String> patterns = new HashSet<>();
        private Consumer<String> messageHandler;
        private PubSubListener patternHandler;
        private Consumer<String> subscribedCallback;
        private Consumer<String> unsubscribedCallback;
        private java.util.function.BiConsumer<String, Throwable> errorHandler;

        LettuceSubscriptionBuilder(LettucePubSubImpl pubSub) {
            this.pubSub = pubSub;
        }

        @Override
        @NotNull
        public RedisPubSub.SubscriptionBuilder channels(@NotNull String... channels) {
            this.channels.addAll(Arrays.asList(channels));
            return this;
        }

        @Override
        @NotNull
        public RedisPubSub.SubscriptionBuilder patterns(@NotNull String... patterns) {
            this.patterns.addAll(Arrays.asList(patterns));
            return this;
        }

        @Override
        @NotNull
        public RedisPubSub.SubscriptionBuilder onMessage(@NotNull Consumer<String> handler) {
            this.messageHandler = handler;
            return this;
        }

        @Override
        @NotNull
        public RedisPubSub.SubscriptionBuilder onPatternMessage(@NotNull PubSubListener handler) {
            this.patternHandler = handler;
            return this;
        }

        @Override
        @NotNull
        public RedisPubSub.SubscriptionBuilder onSubscribed(@NotNull Consumer<String> callback) {
            this.subscribedCallback = callback;
            return this;
        }

        @Override
        @NotNull
        public RedisPubSub.SubscriptionBuilder onUnsubscribed(@NotNull Consumer<String> callback) {
            this.unsubscribedCallback = callback;
            return this;
        }

        @Override
        @NotNull
        public RedisPubSub.SubscriptionBuilder onError(@NotNull java.util.function.BiConsumer<String, Throwable> handler) {
            this.errorHandler = handler;
            return this;
        }

        @Override
        @NotNull
        public RedisPubSub.Subscription subscribe() {
            RedisPubSub.Subscription channelSub = null;
            if (!channels.isEmpty() && messageHandler != null) {
                channelSub = pubSub.subscribe(messageHandler, channels.toArray(new String[0]));
                if (unsubscribedCallback != null) {
                    for (String ch : channels) {
                        channelSub.onUnsubscribe(() -> unsubscribedCallback.accept(ch));
                    }
                }
            }

            RedisPubSub.Subscription patternSub = null;
            if (!patterns.isEmpty() && patternHandler != null) {
                patternSub = pubSub.psubscribe(patternHandler, patterns.toArray(new String[0]));
                if (unsubscribedCallback != null) {
                    for (String p : patterns) {
                        patternSub.onUnsubscribe(() -> unsubscribedCallback.accept(p));
                    }
                }
            }

            RedisPubSub.Subscription finalChannelSub = channelSub;
            RedisPubSub.Subscription finalPatternSub = patternSub;

            Set<String> allChannels = new HashSet<>();
            allChannels.addAll(channels);
            allChannels.addAll(patterns);

            return new RedisPubSub.Subscription() {
                @Override
                @NotNull
                public Set<String> getChannels() {
                    return allChannels;
                }

                @Override
                public boolean isPatternSubscription() {
                    return !patterns.isEmpty();
                }

                @Override
                public boolean isActive() {
                    return (finalChannelSub != null && finalChannelSub.isActive()) ||
                           (finalPatternSub != null && finalPatternSub.isActive());
                }

                @Override
                public void unsubscribe() {
                    if (finalChannelSub != null) finalChannelSub.unsubscribe();
                    if (finalPatternSub != null) finalPatternSub.unsubscribe();
                }

                @Override
                @NotNull
                public RedisPubSub.Subscription onUnsubscribe(@NotNull Runnable callback) {
                    if (finalChannelSub != null) finalChannelSub.onUnsubscribe(callback);
                    if (finalPatternSub != null) finalPatternSub.onUnsubscribe(callback);
                    return this;
                }

                @Override
                public long getMessageCount() {
                    return (finalChannelSub != null ? finalChannelSub.getMessageCount() : 0) +
                           (finalPatternSub != null ? finalPatternSub.getMessageCount() : 0);
                }
            };
        }
    }
}
