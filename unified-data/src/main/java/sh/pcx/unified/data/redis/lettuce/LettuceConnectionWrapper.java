/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.redis.lettuce;

import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.ScriptOutputType;
import sh.pcx.unified.data.redis.RedisConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Lettuce-based implementation of {@link RedisConnection}.
 *
 * <p>This class wraps a Lettuce connection and provides both synchronous and
 * asynchronous operations. Unlike Jedis, Lettuce connections are thread-safe
 * and can be shared.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see RedisConnection
 */
public class LettuceConnectionWrapper implements RedisConnection {

    private final LettuceProvider provider;
    private volatile boolean closed = false;

    /**
     * Creates a new connection wrapper.
     *
     * @param provider the Lettuce provider
     */
    public LettuceConnectionWrapper(@NotNull LettuceProvider provider) {
        this.provider = provider;
    }

    private RedisCommands<String, String> sync() {
        checkClosed();
        return provider.sync();
    }

    private RedisAsyncCommands<String, String> async() {
        checkClosed();
        return provider.async();
    }

    @Override
    public boolean isConnected() {
        return !closed && provider.getConnection().isOpen();
    }

    @Override
    @NotNull
    public String ping() {
        return sync().ping();
    }

    @Override
    @NotNull
    public CompletableFuture<String> pingAsync() {
        return async().ping().toCompletableFuture();
    }

    @Override
    public void close() {
        // Lettuce connections are shared, so we just mark this wrapper as closed
        closed = true;
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
        return async().get(key).thenApply(Optional::ofNullable).toCompletableFuture();
    }

    @Override
    public void set(@NotNull String key, @NotNull String value) {
        sync().set(key, value);
    }

    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public CompletableFuture<Void> setAsync(@NotNull String key, @NotNull String value) {
        return (CompletableFuture<Void>) async().set(key, value).thenApply(v -> (Void) null).toCompletableFuture();
    }

    @Override
    public void setex(@NotNull String key, @NotNull String value, @NotNull Duration ttl) {
        sync().setex(key, ttl.toSeconds(), value);
    }

    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public CompletableFuture<Void> setexAsync(@NotNull String key, @NotNull String value, @NotNull Duration ttl) {
        return (CompletableFuture<Void>) async().setex(key, ttl.toSeconds(), value).thenApply(v -> (Void) null).toCompletableFuture();
    }

    @Override
    public boolean setnx(@NotNull String key, @NotNull String value) {
        return Boolean.TRUE.equals(sync().setnx(key, value));
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> setnxAsync(@NotNull String key, @NotNull String value) {
        return async().setnx(key, value).thenApply(Boolean.TRUE::equals).toCompletableFuture();
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
        return async().mget(keys)
                .thenApply(kvs -> kvs.stream()
                        .map(kv -> kv.hasValue() ? kv.getValue() : null)
                        .toList())
                .toCompletableFuture();
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
        return async().mset(keyValues).thenApply(v -> (Void) null).toCompletableFuture().thenApply(x -> null);
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
        return async().incr(key).thenApply(r -> r != null ? r : 0L).toCompletableFuture();
    }

    @Override
    public long incrBy(@NotNull String key, long increment) {
        Long result = sync().incrby(key, increment);
        return result != null ? result : 0;
    }

    @Override
    @NotNull
    public CompletableFuture<Long> incrByAsync(@NotNull String key, long increment) {
        return async().incrby(key, increment).thenApply(r -> r != null ? r : 0L).toCompletableFuture();
    }

    @Override
    public long decr(@NotNull String key) {
        Long result = sync().decr(key);
        return result != null ? result : 0;
    }

    @Override
    @NotNull
    public CompletableFuture<Long> decrAsync(@NotNull String key) {
        return async().decr(key).thenApply(r -> r != null ? r : 0L).toCompletableFuture();
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
        return async().del(keys).thenApply(r -> r != null ? r : 0L).toCompletableFuture();
    }

    @Override
    public boolean exists(@NotNull String key) {
        Long result = sync().exists(key);
        return result != null && result > 0;
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> existsAsync(@NotNull String key) {
        return async().exists(key).thenApply(r -> r != null && r > 0).toCompletableFuture();
    }

    @Override
    public boolean expire(@NotNull String key, @NotNull Duration ttl) {
        return Boolean.TRUE.equals(sync().expire(key, ttl.toSeconds()));
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> expireAsync(@NotNull String key, @NotNull Duration ttl) {
        return async().expire(key, ttl.toSeconds()).thenApply(Boolean.TRUE::equals).toCompletableFuture();
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
        return async().ttl(key)
                .thenApply(ttl -> {
                    if (ttl == null || ttl < 0) {
                        return Optional.<Duration>empty();
                    }
                    return Optional.of(Duration.ofSeconds(ttl));
                })
                .toCompletableFuture();
    }

    @Override
    @NotNull
    public Set<String> keys(@NotNull String pattern) {
        return new HashSet<>(sync().keys(pattern));
    }

    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public CompletableFuture<Set<String>> keysAsync(@NotNull String pattern) {
        return (CompletableFuture<Set<String>>) (CompletableFuture<?>) async().keys(pattern).thenApply(list -> (Set<String>) new HashSet<>(list)).toCompletableFuture();
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
        return async().hget(key, field).thenApply(Optional::ofNullable).toCompletableFuture();
    }

    @Override
    public boolean hset(@NotNull String key, @NotNull String field, @NotNull String value) {
        return Boolean.TRUE.equals(sync().hset(key, field, value));
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> hsetAsync(@NotNull String key, @NotNull String field, @NotNull String value) {
        return async().hset(key, field, value).thenApply(Boolean.TRUE::equals).toCompletableFuture();
    }

    @Override
    @NotNull
    public Map<String, String> hgetAll(@NotNull String key) {
        return sync().hgetall(key);
    }

    @Override
    @NotNull
    public CompletableFuture<Map<String, String>> hgetAllAsync(@NotNull String key) {
        return async().hgetall(key).toCompletableFuture();
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
        return async().hmset(key, fieldValues).<Void>thenApply(v -> null).toCompletableFuture();
    }

    @Override
    public long hdel(@NotNull String key, @NotNull String... fields) {
        Long result = sync().hdel(key, fields);
        return result != null ? result : 0;
    }

    @Override
    @NotNull
    public CompletableFuture<Long> hdelAsync(@NotNull String key, @NotNull String... fields) {
        return async().hdel(key, fields).thenApply(r -> r != null ? r : 0L).toCompletableFuture();
    }

    @Override
    public boolean hexists(@NotNull String key, @NotNull String field) {
        return Boolean.TRUE.equals(sync().hexists(key, field));
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> hexistsAsync(@NotNull String key, @NotNull String field) {
        return async().hexists(key, field).thenApply(Boolean.TRUE::equals).toCompletableFuture();
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
        return async().lpush(key, values).thenApply(r -> r != null ? r : 0L).toCompletableFuture();
    }

    @Override
    public long rpush(@NotNull String key, @NotNull String... values) {
        Long result = sync().rpush(key, values);
        return result != null ? result : 0;
    }

    @Override
    @NotNull
    public CompletableFuture<Long> rpushAsync(@NotNull String key, @NotNull String... values) {
        return async().rpush(key, values).thenApply(r -> r != null ? r : 0L).toCompletableFuture();
    }

    @Override
    @NotNull
    public Optional<String> lpop(@NotNull String key) {
        return Optional.ofNullable(sync().lpop(key));
    }

    @Override
    @NotNull
    public CompletableFuture<Optional<String>> lpopAsync(@NotNull String key) {
        return async().lpop(key).thenApply(Optional::ofNullable).toCompletableFuture();
    }

    @Override
    @NotNull
    public Optional<String> rpop(@NotNull String key) {
        return Optional.ofNullable(sync().rpop(key));
    }

    @Override
    @NotNull
    public CompletableFuture<Optional<String>> rpopAsync(@NotNull String key) {
        return async().rpop(key).thenApply(Optional::ofNullable).toCompletableFuture();
    }

    @Override
    @NotNull
    public List<String> lrange(@NotNull String key, long start, long stop) {
        return sync().lrange(key, start, stop);
    }

    @Override
    @NotNull
    public CompletableFuture<List<String>> lrangeAsync(@NotNull String key, long start, long stop) {
        return async().lrange(key, start, stop).toCompletableFuture();
    }

    @Override
    public long llen(@NotNull String key) {
        Long result = sync().llen(key);
        return result != null ? result : 0;
    }

    @Override
    @NotNull
    public CompletableFuture<Long> llenAsync(@NotNull String key) {
        return async().llen(key).thenApply(r -> r != null ? r : 0L).toCompletableFuture();
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
        return async().sadd(key, members).thenApply(r -> r != null ? r : 0L).toCompletableFuture();
    }

    @Override
    @NotNull
    public Set<String> smembers(@NotNull String key) {
        return sync().smembers(key);
    }

    @Override
    @NotNull
    public CompletableFuture<Set<String>> smembersAsync(@NotNull String key) {
        return async().smembers(key).toCompletableFuture();
    }

    @Override
    public boolean sismember(@NotNull String key, @NotNull String member) {
        return Boolean.TRUE.equals(sync().sismember(key, member));
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> sismemberAsync(@NotNull String key, @NotNull String member) {
        return async().sismember(key, member).thenApply(Boolean.TRUE::equals).toCompletableFuture();
    }

    @Override
    public long srem(@NotNull String key, @NotNull String... members) {
        Long result = sync().srem(key, members);
        return result != null ? result : 0;
    }

    @Override
    @NotNull
    public CompletableFuture<Long> sremAsync(@NotNull String key, @NotNull String... members) {
        return async().srem(key, members).thenApply(r -> r != null ? r : 0L).toCompletableFuture();
    }

    // ========== Sorted Set Operations ==========

    @Override
    public long zadd(@NotNull String key, @NotNull Map<String, Double> scoreMembers) {
        Long result = sync().zadd(key, scoreMembers.entrySet().stream()
                .map(e -> io.lettuce.core.ScoredValue.just(e.getValue(), e.getKey()))
                .toArray(io.lettuce.core.ScoredValue[]::new));
        return result != null ? result : 0;
    }

    @Override
    @NotNull
    public CompletableFuture<Long> zaddAsync(@NotNull String key, @NotNull Map<String, Double> scoreMembers) {
        return async().zadd(key, scoreMembers.entrySet().stream()
                .map(e -> io.lettuce.core.ScoredValue.just(e.getValue(), e.getKey()))
                .toArray(io.lettuce.core.ScoredValue[]::new))
                .thenApply(r -> r != null ? r : 0L)
                .toCompletableFuture();
    }

    @Override
    @NotNull
    public Optional<Double> zscore(@NotNull String key, @NotNull String member) {
        return Optional.ofNullable(sync().zscore(key, member));
    }

    @Override
    @NotNull
    public CompletableFuture<Optional<Double>> zscoreAsync(@NotNull String key, @NotNull String member) {
        return async().zscore(key, member).thenApply(Optional::ofNullable).toCompletableFuture();
    }

    @Override
    @NotNull
    public List<String> zrange(@NotNull String key, long start, long stop) {
        return sync().zrange(key, start, stop);
    }

    @Override
    @NotNull
    public CompletableFuture<List<String>> zrangeAsync(@NotNull String key, long start, long stop) {
        return async().zrange(key, start, stop).toCompletableFuture();
    }

    // ========== Pub/Sub Operations ==========

    @Override
    public long publish(@NotNull String channel, @NotNull String message) {
        Long result = sync().publish(channel, message);
        return result != null ? result : 0;
    }

    @Override
    @NotNull
    public CompletableFuture<Long> publishAsync(@NotNull String channel, @NotNull String message) {
        return async().publish(channel, message).thenApply(r -> r != null ? r : 0L).toCompletableFuture();
    }

    // ========== Script Operations ==========

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
        return async().eval(script, ScriptOutputType.MULTI, keyArray, argArray).toCompletableFuture();
    }

    @Override
    @Nullable
    public Object evalsha(@NotNull String sha1, @NotNull List<String> keys, @NotNull List<String> args) {
        String[] keyArray = keys.toArray(new String[0]);
        String[] argArray = args.toArray(new String[0]);
        return sync().evalsha(sha1, ScriptOutputType.MULTI, keyArray, argArray);
    }

    @Override
    @NotNull
    public String scriptLoad(@NotNull String script) {
        return sync().scriptLoad(script);
    }

    // ========== Transaction Support ==========

    @Override
    @NotNull
    public RedisTransaction multi() {
        checkClosed();
        sync().multi();
        return new LettuceTransactionWrapper(sync());
    }

    private void checkClosed() {
        if (closed) {
            throw new RedisException("Connection is closed");
        }
    }

    /**
     * Lettuce transaction wrapper.
     */
    private static class LettuceTransactionWrapper implements RedisTransaction {
        private final RedisCommands<String, String> commands;

        LettuceTransactionWrapper(RedisCommands<String, String> commands) {
            this.commands = commands;
        }

        @Override
        @NotNull
        public RedisTransaction set(@NotNull String key, @NotNull String value) {
            commands.set(key, value);
            return this;
        }

        @Override
        @NotNull
        public RedisTransaction get(@NotNull String key) {
            commands.get(key);
            return this;
        }

        @Override
        @NotNull
        public RedisTransaction del(@NotNull String... keys) {
            commands.del(keys);
            return this;
        }

        @Override
        @NotNull
        public RedisTransaction incr(@NotNull String key) {
            commands.incr(key);
            return this;
        }

        @Override
        @NotNull
        public RedisTransaction expire(@NotNull String key, @NotNull Duration ttl) {
            commands.expire(key, ttl.toSeconds());
            return this;
        }

        @Override
        @NotNull
        public List<Object> exec() {
            return commands.exec().stream().toList();
        }

        @Override
        public void discard() {
            commands.discard();
        }
    }
}
