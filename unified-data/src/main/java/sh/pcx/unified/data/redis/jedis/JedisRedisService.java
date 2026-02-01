/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.redis.jedis;

import sh.pcx.unified.data.redis.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.params.SetParams;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Jedis-based implementation of {@link RedisService}.
 *
 * <p>This implementation uses Jedis for Redis operations, with connection
 * pooling provided by {@link JedisPoolManager}. Async operations are executed
 * on a dedicated executor.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create the service
 * RedisConfig config = RedisConfig.builder()
 *     .host("localhost")
 *     .port(6379)
 *     .build();
 *
 * JedisRedisService redis = new JedisRedisService(config);
 *
 * // Use the service
 * redis.set("key", "value");
 * Optional<String> value = redis.get("key");
 *
 * // Async operations
 * redis.setAsync("asyncKey", "asyncValue")
 *     .thenRun(() -> System.out.println("Set complete"));
 *
 * // Shutdown
 * redis.shutdown();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 * @see RedisService
 * @see JedisProvider
 */
public class JedisRedisService implements RedisService {

    private final RedisConfig config;
    private final JedisProvider provider;
    private final ExecutorService asyncExecutor;
    private final JedisPubSubImpl pubSub;
    private final Map<String, RedisLuaScript> scriptCache = new ConcurrentHashMap<>();
    private volatile boolean shutdown = false;

    /**
     * Creates a new Jedis-based Redis service.
     *
     * @param config the Redis configuration
     * @since 1.0.0
     */
    public JedisRedisService(@NotNull RedisConfig config) {
        this.config = Objects.requireNonNull(config, "config cannot be null");
        this.provider = new JedisProvider(config);
        this.asyncExecutor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "jedis-async");
            t.setDaemon(true);
            return t;
        });
        this.pubSub = new JedisPubSubImpl(this);
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
        return new JedisConnectionWrapper(provider.getResource(), provider.getPoolManager());
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
        return CompletableFuture.supplyAsync(() -> withConnection(function), asyncExecutor);
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
        return CompletableFuture.supplyAsync(this::isHealthy, asyncExecutor);
    }

    @Override
    @NotNull
    public PoolStats getPoolStats() {
        return provider.getPoolManager().getStats();
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
        try (Jedis jedis = provider.getResource()) {
            return Optional.ofNullable(jedis.get(key));
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Optional<String>> getAsync(@NotNull String key) {
        return CompletableFuture.supplyAsync(() -> get(key), asyncExecutor);
    }

    @Override
    public void set(@NotNull String key, @NotNull String value) {
        try (Jedis jedis = provider.getResource()) {
            jedis.set(key, value);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Void> setAsync(@NotNull String key, @NotNull String value) {
        return CompletableFuture.runAsync(() -> set(key, value), asyncExecutor);
    }

    @Override
    public void setex(@NotNull String key, @NotNull String value, @NotNull Duration ttl) {
        try (Jedis jedis = provider.getResource()) {
            jedis.setex(key, ttl.toSeconds(), value);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Void> setexAsync(@NotNull String key, @NotNull String value, @NotNull Duration ttl) {
        return CompletableFuture.runAsync(() -> setex(key, value, ttl), asyncExecutor);
    }

    @Override
    public boolean setnx(@NotNull String key, @NotNull String value) {
        try (Jedis jedis = provider.getResource()) {
            return jedis.setnx(key, value) == 1;
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> setnxAsync(@NotNull String key, @NotNull String value) {
        return CompletableFuture.supplyAsync(() -> setnx(key, value), asyncExecutor);
    }

    @Override
    @NotNull
    public List<@Nullable String> mget(@NotNull String... keys) {
        try (Jedis jedis = provider.getResource()) {
            return jedis.mget(keys);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<List<@Nullable String>> mgetAsync(@NotNull String... keys) {
        return CompletableFuture.supplyAsync(() -> mget(keys), asyncExecutor);
    }

    @Override
    public void mset(@NotNull Map<String, String> keyValues) {
        if (keyValues.isEmpty()) return;
        try (Jedis jedis = provider.getResource()) {
            String[] keysValues = new String[keyValues.size() * 2];
            int i = 0;
            for (Map.Entry<String, String> entry : keyValues.entrySet()) {
                keysValues[i++] = entry.getKey();
                keysValues[i++] = entry.getValue();
            }
            jedis.mset(keysValues);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Void> msetAsync(@NotNull Map<String, String> keyValues) {
        return CompletableFuture.runAsync(() -> mset(keyValues), asyncExecutor);
    }

    // ========== Key Operations ==========

    @Override
    public long del(@NotNull String... keys) {
        try (Jedis jedis = provider.getResource()) {
            return jedis.del(keys);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Long> delAsync(@NotNull String... keys) {
        return CompletableFuture.supplyAsync(() -> del(keys), asyncExecutor);
    }

    @Override
    public boolean exists(@NotNull String key) {
        try (Jedis jedis = provider.getResource()) {
            return jedis.exists(key);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> existsAsync(@NotNull String key) {
        return CompletableFuture.supplyAsync(() -> exists(key), asyncExecutor);
    }

    @Override
    public boolean expire(@NotNull String key, @NotNull Duration ttl) {
        try (Jedis jedis = provider.getResource()) {
            return jedis.expire(key, ttl.toSeconds()) == 1;
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> expireAsync(@NotNull String key, @NotNull Duration ttl) {
        return CompletableFuture.supplyAsync(() -> expire(key, ttl), asyncExecutor);
    }

    @Override
    @NotNull
    public Optional<Duration> ttl(@NotNull String key) {
        try (Jedis jedis = provider.getResource()) {
            long ttl = jedis.ttl(key);
            if (ttl < 0) {
                return Optional.empty();
            }
            return Optional.of(Duration.ofSeconds(ttl));
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Optional<Duration>> ttlAsync(@NotNull String key) {
        return CompletableFuture.supplyAsync(() -> ttl(key), asyncExecutor);
    }

    // ========== Numeric Operations ==========

    @Override
    public long incr(@NotNull String key) {
        try (Jedis jedis = provider.getResource()) {
            return jedis.incr(key);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Long> incrAsync(@NotNull String key) {
        return CompletableFuture.supplyAsync(() -> incr(key), asyncExecutor);
    }

    @Override
    public long incrBy(@NotNull String key, long increment) {
        try (Jedis jedis = provider.getResource()) {
            return jedis.incrBy(key, increment);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Long> incrByAsync(@NotNull String key, long increment) {
        return CompletableFuture.supplyAsync(() -> incrBy(key, increment), asyncExecutor);
    }

    @Override
    public long decr(@NotNull String key) {
        try (Jedis jedis = provider.getResource()) {
            return jedis.decr(key);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Long> decrAsync(@NotNull String key) {
        return CompletableFuture.supplyAsync(() -> decr(key), asyncExecutor);
    }

    // ========== Hash Operations ==========

    @Override
    @NotNull
    public Optional<String> hget(@NotNull String key, @NotNull String field) {
        try (Jedis jedis = provider.getResource()) {
            return Optional.ofNullable(jedis.hget(key, field));
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Optional<String>> hgetAsync(@NotNull String key, @NotNull String field) {
        return CompletableFuture.supplyAsync(() -> hget(key, field), asyncExecutor);
    }

    @Override
    public boolean hset(@NotNull String key, @NotNull String field, @NotNull String value) {
        try (Jedis jedis = provider.getResource()) {
            return jedis.hset(key, field, value) == 1;
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> hsetAsync(@NotNull String key, @NotNull String field, @NotNull String value) {
        return CompletableFuture.supplyAsync(() -> hset(key, field, value), asyncExecutor);
    }

    @Override
    @NotNull
    public Map<String, String> hgetAll(@NotNull String key) {
        try (Jedis jedis = provider.getResource()) {
            return jedis.hgetAll(key);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Map<String, String>> hgetAllAsync(@NotNull String key) {
        return CompletableFuture.supplyAsync(() -> hgetAll(key), asyncExecutor);
    }

    @Override
    public void hmset(@NotNull String key, @NotNull Map<String, String> fieldValues) {
        if (fieldValues.isEmpty()) return;
        try (Jedis jedis = provider.getResource()) {
            jedis.hmset(key, fieldValues);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Void> hmsetAsync(@NotNull String key, @NotNull Map<String, String> fieldValues) {
        return CompletableFuture.runAsync(() -> hmset(key, fieldValues), asyncExecutor);
    }

    @Override
    public long hdel(@NotNull String key, @NotNull String... fields) {
        try (Jedis jedis = provider.getResource()) {
            return jedis.hdel(key, fields);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Long> hdelAsync(@NotNull String key, @NotNull String... fields) {
        return CompletableFuture.supplyAsync(() -> hdel(key, fields), asyncExecutor);
    }

    // ========== List Operations ==========

    @Override
    public long lpush(@NotNull String key, @NotNull String... values) {
        try (Jedis jedis = provider.getResource()) {
            return jedis.lpush(key, values);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Long> lpushAsync(@NotNull String key, @NotNull String... values) {
        return CompletableFuture.supplyAsync(() -> lpush(key, values), asyncExecutor);
    }

    @Override
    public long rpush(@NotNull String key, @NotNull String... values) {
        try (Jedis jedis = provider.getResource()) {
            return jedis.rpush(key, values);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Long> rpushAsync(@NotNull String key, @NotNull String... values) {
        return CompletableFuture.supplyAsync(() -> rpush(key, values), asyncExecutor);
    }

    @Override
    @NotNull
    public Optional<String> lpop(@NotNull String key) {
        try (Jedis jedis = provider.getResource()) {
            return Optional.ofNullable(jedis.lpop(key));
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Optional<String>> lpopAsync(@NotNull String key) {
        return CompletableFuture.supplyAsync(() -> lpop(key), asyncExecutor);
    }

    @Override
    @NotNull
    public Optional<String> rpop(@NotNull String key) {
        try (Jedis jedis = provider.getResource()) {
            return Optional.ofNullable(jedis.rpop(key));
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Optional<String>> rpopAsync(@NotNull String key) {
        return CompletableFuture.supplyAsync(() -> rpop(key), asyncExecutor);
    }

    @Override
    @NotNull
    public List<String> lrange(@NotNull String key, long start, long stop) {
        try (Jedis jedis = provider.getResource()) {
            return jedis.lrange(key, start, stop);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<List<String>> lrangeAsync(@NotNull String key, long start, long stop) {
        return CompletableFuture.supplyAsync(() -> lrange(key, start, stop), asyncExecutor);
    }

    // ========== Set Operations ==========

    @Override
    public long sadd(@NotNull String key, @NotNull String... members) {
        try (Jedis jedis = provider.getResource()) {
            return jedis.sadd(key, members);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Long> saddAsync(@NotNull String key, @NotNull String... members) {
        return CompletableFuture.supplyAsync(() -> sadd(key, members), asyncExecutor);
    }

    @Override
    @NotNull
    public Set<String> smembers(@NotNull String key) {
        try (Jedis jedis = provider.getResource()) {
            return jedis.smembers(key);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Set<String>> smembersAsync(@NotNull String key) {
        return CompletableFuture.supplyAsync(() -> smembers(key), asyncExecutor);
    }

    @Override
    public boolean sismember(@NotNull String key, @NotNull String member) {
        try (Jedis jedis = provider.getResource()) {
            return jedis.sismember(key, member);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> sismemberAsync(@NotNull String key, @NotNull String member) {
        return CompletableFuture.supplyAsync(() -> sismember(key, member), asyncExecutor);
    }

    @Override
    public long srem(@NotNull String key, @NotNull String... members) {
        try (Jedis jedis = provider.getResource()) {
            return jedis.srem(key, members);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Long> sremAsync(@NotNull String key, @NotNull String... members) {
        return CompletableFuture.supplyAsync(() -> srem(key, members), asyncExecutor);
    }

    // ========== Pub/Sub ==========

    @Override
    public long publish(@NotNull String channel, @NotNull String message) {
        try (Jedis jedis = provider.getResource()) {
            return jedis.publish(channel, message);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Long> publishAsync(@NotNull String channel, @NotNull String message) {
        return CompletableFuture.supplyAsync(() -> publish(channel, message), asyncExecutor);
    }

    @Override
    @NotNull
    public PubSubSubscription subscribe(@NotNull String channel, @NotNull Consumer<String> listener) {
        RedisPubSub.Subscription sub = pubSub.subscribe(channel, listener);
        return new PubSubSubscriptionAdapter(channel, sub);
    }

    @Override
    @NotNull
    public PubSubSubscription psubscribe(@NotNull String pattern, @NotNull PubSubListener listener) {
        RedisPubSub.Subscription sub = pubSub.psubscribe(pattern, listener);
        return new PubSubSubscriptionAdapter(pattern, sub);
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
        try (Jedis jedis = provider.getResource()) {
            return jedis.eval(script, keys, args);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Object> evalAsync(@NotNull String script, @NotNull List<String> keys, @NotNull List<String> args) {
        return CompletableFuture.supplyAsync(() -> eval(script, keys, args), asyncExecutor);
    }

    @Override
    @NotNull
    public RedisLuaScript loadScript(@NotNull String script) {
        return scriptCache.computeIfAbsent(script, s -> {
            try (Jedis jedis = provider.getResource()) {
                String sha1 = jedis.scriptLoad(s);
                return new JedisLuaScript(this, s, sha1);
            }
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
            asyncExecutor.shutdown();
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
     * Gets the Jedis provider.
     *
     * @return the provider
     * @since 1.0.0
     */
    @NotNull
    public JedisProvider getProvider() {
        return provider;
    }

    /**
     * Gets the async executor.
     *
     * @return the executor service
     * @since 1.0.0
     */
    @NotNull
    public ExecutorService getAsyncExecutor() {
        return asyncExecutor;
    }

    // ========== Inner Classes ==========

    /**
     * Jedis Lua script implementation.
     */
    private static class JedisLuaScript extends RedisLuaScript.AbstractLuaScript {
        private final JedisRedisService service;

        JedisLuaScript(JedisRedisService service, String script, String sha1) {
            super(script, sha1);
            this.service = service;
        }

        @Override
        public boolean isLoaded() {
            try (Jedis jedis = service.provider.getResource()) {
                return jedis.scriptExists(sha1);
            }
        }

        @Override
        @NotNull
        public CompletableFuture<Boolean> isLoadedAsync() {
            return CompletableFuture.supplyAsync(this::isLoaded, service.asyncExecutor);
        }

        @Override
        public void ensureLoaded() {
            if (!isLoaded()) {
                try (Jedis jedis = service.provider.getResource()) {
                    jedis.scriptLoad(script);
                }
            }
        }

        @Override
        @NotNull
        public CompletableFuture<Void> ensureLoadedAsync() {
            return CompletableFuture.runAsync(this::ensureLoaded, service.asyncExecutor);
        }

        @Override
        @Nullable
        public Object execute(@NotNull List<String> keys, @NotNull List<String> args) {
            try (Jedis jedis = service.provider.getResource()) {
                try {
                    return jedis.evalsha(sha1, keys, args);
                } catch (Exception e) {
                    // Script may have been evicted, reload and retry
                    jedis.scriptLoad(script);
                    return jedis.evalsha(sha1, keys, args);
                }
            }
        }

        @Override
        @NotNull
        public CompletableFuture<Object> executeAsync(@NotNull List<String> keys, @NotNull List<String> args) {
            return CompletableFuture.supplyAsync(() -> execute(keys, args), service.asyncExecutor);
        }
    }

    /**
     * Jedis Pub/Sub implementation.
     */
    private static class JedisPubSubImpl implements RedisPubSub {
        private final JedisRedisService service;
        private final Map<String, SubscriptionHolder> subscriptions = new ConcurrentHashMap<>();
        private final Map<String, SubscriptionHolder> patternSubscriptions = new ConcurrentHashMap<>();

        JedisPubSubImpl(JedisRedisService service) {
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
            return CompletableFuture.supplyAsync(() -> publishToMany(message, channels), service.asyncExecutor);
        }

        @Override
        @NotNull
        public Subscription subscribe(@NotNull String channel, @NotNull Consumer<String> listener) {
            SubscriptionHolder holder = new SubscriptionHolder(Set.of(channel), false);
            subscriptions.put(channel, holder);

            JedisPubSub pubSub = new JedisPubSub() {
                @Override
                public void onMessage(String ch, String msg) {
                    holder.incrementMessageCount();
                    listener.accept(msg);
                }
            };

            holder.setPubSub(pubSub);

            service.asyncExecutor.execute(() -> {
                try (Jedis jedis = service.provider.getResource()) {
                    jedis.subscribe(pubSub, channel);
                }
            });

            return holder;
        }

        @Override
        @NotNull
        public Subscription subscribe(@NotNull Consumer<String> listener, @NotNull String... channels) {
            SubscriptionHolder holder = new SubscriptionHolder(Set.of(channels), false);
            for (String channel : channels) {
                subscriptions.put(channel, holder);
            }

            JedisPubSub pubSub = new JedisPubSub() {
                @Override
                public void onMessage(String ch, String msg) {
                    holder.incrementMessageCount();
                    listener.accept(msg);
                }
            };

            holder.setPubSub(pubSub);

            service.asyncExecutor.execute(() -> {
                try (Jedis jedis = service.provider.getResource()) {
                    jedis.subscribe(pubSub, channels);
                }
            });

            return holder;
        }

        @Override
        @NotNull
        public Subscription psubscribe(@NotNull String pattern, @NotNull PubSubListener listener) {
            SubscriptionHolder holder = new SubscriptionHolder(Set.of(pattern), true);
            patternSubscriptions.put(pattern, holder);

            JedisPubSub pubSub = new JedisPubSub() {
                @Override
                public void onPMessage(String pat, String channel, String msg) {
                    holder.incrementMessageCount();
                    listener.onMessage(channel, msg);
                }
            };

            holder.setPubSub(pubSub);

            service.asyncExecutor.execute(() -> {
                try (Jedis jedis = service.provider.getResource()) {
                    jedis.psubscribe(pubSub, pattern);
                }
            });

            return holder;
        }

        @Override
        @NotNull
        public Subscription psubscribe(@NotNull PubSubListener listener, @NotNull String... patterns) {
            SubscriptionHolder holder = new SubscriptionHolder(Set.of(patterns), true);
            for (String pattern : patterns) {
                patternSubscriptions.put(pattern, holder);
            }

            JedisPubSub pubSub = new JedisPubSub() {
                @Override
                public void onPMessage(String pat, String channel, String msg) {
                    holder.incrementMessageCount();
                    listener.onMessage(channel, msg);
                }
            };

            holder.setPubSub(pubSub);

            service.asyncExecutor.execute(() -> {
                try (Jedis jedis = service.provider.getResource()) {
                    jedis.psubscribe(pubSub, patterns);
                }
            });

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
            try (Jedis jedis = service.provider.getResource()) {
                Map<String, Long> result = jedis.pubsubNumSub(channel);
                Long count = result.get(channel);
                return count != null ? count : 0;
            }
        }

        @Override
        @NotNull
        public CompletableFuture<Map<String, Long>> getSubscriberCountsAsync(@NotNull String... channels) {
            return CompletableFuture.supplyAsync(() -> {
                try (Jedis jedis = service.provider.getResource()) {
                    Map<String, Long> result = jedis.pubsubNumSub(channels);
                    Map<String, Long> counts = new HashMap<>();
                    for (String channel : channels) {
                        Long count = result.get(channel);
                        counts.put(channel, count != null ? count : 0L);
                    }
                    return counts;
                }
            }, service.asyncExecutor);
        }

        @Override
        @NotNull
        public Set<String> listActiveChannels(@NotNull String pattern) {
            try (Jedis jedis = service.provider.getResource()) {
                return new HashSet<>(jedis.pubsubChannels(pattern));
            }
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
            return new JedisSubscriptionBuilder(this);
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
        private JedisPubSub pubSub;

        SubscriptionHolder(Set<String> channels, boolean isPattern) {
            this.channels = channels;
            this.isPattern = isPattern;
        }

        void setPubSub(JedisPubSub pubSub) {
            this.pubSub = pubSub;
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
                if (pubSub != null && pubSub.isSubscribed()) {
                    if (isPattern) {
                        pubSub.punsubscribe();
                    } else {
                        pubSub.unsubscribe();
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
    private static class JedisSubscriptionBuilder implements RedisPubSub.SubscriptionBuilder {
        private final JedisPubSubImpl pubSub;
        private final Set<String> channels = new HashSet<>();
        private final Set<String> patterns = new HashSet<>();
        private Consumer<String> messageHandler;
        private PubSubListener patternHandler;
        private Consumer<String> subscribedCallback;
        private Consumer<String> unsubscribedCallback;
        private java.util.function.BiConsumer<String, Throwable> errorHandler;

        JedisSubscriptionBuilder(JedisPubSubImpl pubSub) {
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
            // Subscribe to channels
            RedisPubSub.Subscription channelSub = null;
            if (!channels.isEmpty() && messageHandler != null) {
                channelSub = pubSub.subscribe(messageHandler, channels.toArray(new String[0]));
                if (unsubscribedCallback != null) {
                    for (String ch : channels) {
                        channelSub.onUnsubscribe(() -> unsubscribedCallback.accept(ch));
                    }
                }
            }

            // Subscribe to patterns
            RedisPubSub.Subscription patternSub = null;
            if (!patterns.isEmpty() && patternHandler != null) {
                patternSub = pubSub.psubscribe(patternHandler, patterns.toArray(new String[0]));
                if (unsubscribedCallback != null) {
                    for (String p : patterns) {
                        patternSub.onUnsubscribe(() -> unsubscribedCallback.accept(p));
                    }
                }
            }

            // Return combined subscription
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

    /**
     * Adapter to convert RedisPubSub.Subscription to PubSubSubscription.
     */
    private static class PubSubSubscriptionAdapter implements RedisService.PubSubSubscription {
        private final String channel;
        private final RedisPubSub.Subscription delegate;

        PubSubSubscriptionAdapter(String channel, RedisPubSub.Subscription delegate) {
            this.channel = channel;
            this.delegate = delegate;
        }

        @Override
        public boolean isActive() {
            return delegate.isActive();
        }

        @Override
        @NotNull
        public String getChannel() {
            return channel;
        }

        @Override
        public void unsubscribe() {
            delegate.unsubscribe();
        }
    }
}
