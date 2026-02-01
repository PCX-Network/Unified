/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.data.redis.jedis;

import sh.pcx.unified.data.redis.RedisConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Jedis-based implementation of {@link RedisConnection}.
 *
 * <p>This class wraps a Jedis connection and provides both synchronous and
 * asynchronous operations. The async operations use a shared executor.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see RedisConnection
 * @see Jedis
 */
public class JedisConnectionWrapper implements RedisConnection {

    private static final ExecutorService ASYNC_EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "jedis-conn-async");
        t.setDaemon(true);
        return t;
    });

    private final Jedis jedis;
    private final JedisPoolManager poolManager;
    private volatile boolean closed = false;

    /**
     * Creates a new connection wrapper.
     *
     * @param jedis       the Jedis connection
     * @param poolManager the pool manager for statistics
     */
    public JedisConnectionWrapper(@NotNull Jedis jedis, @NotNull JedisPoolManager poolManager) {
        this.jedis = jedis;
        this.poolManager = poolManager;
        poolManager.recordBorrowed();
    }

    @Override
    public boolean isConnected() {
        return !closed && jedis.isConnected();
    }

    @Override
    @NotNull
    public String ping() {
        checkClosed();
        try {
            return jedis.ping();
        } catch (Exception e) {
            throw new RedisException("Ping failed", e);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<String> pingAsync() {
        return CompletableFuture.supplyAsync(this::ping, ASYNC_EXECUTOR);
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            poolManager.recordReturned();
            jedis.close();
        }
    }

    // ========== String Operations ==========

    @Override
    @NotNull
    public Optional<String> get(@NotNull String key) {
        checkClosed();
        try {
            return Optional.ofNullable(jedis.get(key));
        } catch (Exception e) {
            throw new RedisException("GET failed for key: " + key, e);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Optional<String>> getAsync(@NotNull String key) {
        return CompletableFuture.supplyAsync(() -> get(key), ASYNC_EXECUTOR);
    }

    @Override
    public void set(@NotNull String key, @NotNull String value) {
        checkClosed();
        try {
            jedis.set(key, value);
        } catch (Exception e) {
            throw new RedisException("SET failed for key: " + key, e);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Void> setAsync(@NotNull String key, @NotNull String value) {
        return CompletableFuture.runAsync(() -> set(key, value), ASYNC_EXECUTOR);
    }

    @Override
    public void setex(@NotNull String key, @NotNull String value, @NotNull Duration ttl) {
        checkClosed();
        try {
            jedis.setex(key, ttl.toSeconds(), value);
        } catch (Exception e) {
            throw new RedisException("SETEX failed for key: " + key, e);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Void> setexAsync(@NotNull String key, @NotNull String value, @NotNull Duration ttl) {
        return CompletableFuture.runAsync(() -> setex(key, value, ttl), ASYNC_EXECUTOR);
    }

    @Override
    public boolean setnx(@NotNull String key, @NotNull String value) {
        checkClosed();
        try {
            return jedis.setnx(key, value) == 1;
        } catch (Exception e) {
            throw new RedisException("SETNX failed for key: " + key, e);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> setnxAsync(@NotNull String key, @NotNull String value) {
        return CompletableFuture.supplyAsync(() -> setnx(key, value), ASYNC_EXECUTOR);
    }

    @Override
    @NotNull
    public List<@Nullable String> mget(@NotNull String... keys) {
        checkClosed();
        try {
            return jedis.mget(keys);
        } catch (Exception e) {
            throw new RedisException("MGET failed", e);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<List<@Nullable String>> mgetAsync(@NotNull String... keys) {
        return CompletableFuture.supplyAsync(() -> mget(keys), ASYNC_EXECUTOR);
    }

    @Override
    public void mset(@NotNull Map<String, String> keyValues) {
        checkClosed();
        if (keyValues.isEmpty()) return;
        try {
            String[] keysValues = new String[keyValues.size() * 2];
            int i = 0;
            for (Map.Entry<String, String> entry : keyValues.entrySet()) {
                keysValues[i++] = entry.getKey();
                keysValues[i++] = entry.getValue();
            }
            jedis.mset(keysValues);
        } catch (Exception e) {
            throw new RedisException("MSET failed", e);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Void> msetAsync(@NotNull Map<String, String> keyValues) {
        return CompletableFuture.runAsync(() -> mset(keyValues), ASYNC_EXECUTOR);
    }

    // ========== Numeric Operations ==========

    @Override
    public long incr(@NotNull String key) {
        checkClosed();
        try {
            return jedis.incr(key);
        } catch (Exception e) {
            throw new RedisException("INCR failed for key: " + key, e);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Long> incrAsync(@NotNull String key) {
        return CompletableFuture.supplyAsync(() -> incr(key), ASYNC_EXECUTOR);
    }

    @Override
    public long incrBy(@NotNull String key, long increment) {
        checkClosed();
        try {
            return jedis.incrBy(key, increment);
        } catch (Exception e) {
            throw new RedisException("INCRBY failed for key: " + key, e);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Long> incrByAsync(@NotNull String key, long increment) {
        return CompletableFuture.supplyAsync(() -> incrBy(key, increment), ASYNC_EXECUTOR);
    }

    @Override
    public long decr(@NotNull String key) {
        checkClosed();
        try {
            return jedis.decr(key);
        } catch (Exception e) {
            throw new RedisException("DECR failed for key: " + key, e);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Long> decrAsync(@NotNull String key) {
        return CompletableFuture.supplyAsync(() -> decr(key), ASYNC_EXECUTOR);
    }

    // ========== Key Operations ==========

    @Override
    public long del(@NotNull String... keys) {
        checkClosed();
        try {
            return jedis.del(keys);
        } catch (Exception e) {
            throw new RedisException("DEL failed", e);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Long> delAsync(@NotNull String... keys) {
        return CompletableFuture.supplyAsync(() -> del(keys), ASYNC_EXECUTOR);
    }

    @Override
    public boolean exists(@NotNull String key) {
        checkClosed();
        try {
            return jedis.exists(key);
        } catch (Exception e) {
            throw new RedisException("EXISTS failed for key: " + key, e);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> existsAsync(@NotNull String key) {
        return CompletableFuture.supplyAsync(() -> exists(key), ASYNC_EXECUTOR);
    }

    @Override
    public boolean expire(@NotNull String key, @NotNull Duration ttl) {
        checkClosed();
        try {
            return jedis.expire(key, ttl.toSeconds()) == 1;
        } catch (Exception e) {
            throw new RedisException("EXPIRE failed for key: " + key, e);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> expireAsync(@NotNull String key, @NotNull Duration ttl) {
        return CompletableFuture.supplyAsync(() -> expire(key, ttl), ASYNC_EXECUTOR);
    }

    @Override
    @NotNull
    public Optional<Duration> ttl(@NotNull String key) {
        checkClosed();
        try {
            long ttl = jedis.ttl(key);
            if (ttl < 0) {
                return Optional.empty();
            }
            return Optional.of(Duration.ofSeconds(ttl));
        } catch (Exception e) {
            throw new RedisException("TTL failed for key: " + key, e);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Optional<Duration>> ttlAsync(@NotNull String key) {
        return CompletableFuture.supplyAsync(() -> ttl(key), ASYNC_EXECUTOR);
    }

    @Override
    @NotNull
    public Set<String> keys(@NotNull String pattern) {
        checkClosed();
        try {
            return jedis.keys(pattern);
        } catch (Exception e) {
            throw new RedisException("KEYS failed for pattern: " + pattern, e);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Set<String>> keysAsync(@NotNull String pattern) {
        return CompletableFuture.supplyAsync(() -> keys(pattern), ASYNC_EXECUTOR);
    }

    // ========== Hash Operations ==========

    @Override
    @NotNull
    public Optional<String> hget(@NotNull String key, @NotNull String field) {
        checkClosed();
        try {
            return Optional.ofNullable(jedis.hget(key, field));
        } catch (Exception e) {
            throw new RedisException("HGET failed for key: " + key, e);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Optional<String>> hgetAsync(@NotNull String key, @NotNull String field) {
        return CompletableFuture.supplyAsync(() -> hget(key, field), ASYNC_EXECUTOR);
    }

    @Override
    public boolean hset(@NotNull String key, @NotNull String field, @NotNull String value) {
        checkClosed();
        try {
            return jedis.hset(key, field, value) == 1;
        } catch (Exception e) {
            throw new RedisException("HSET failed for key: " + key, e);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> hsetAsync(@NotNull String key, @NotNull String field, @NotNull String value) {
        return CompletableFuture.supplyAsync(() -> hset(key, field, value), ASYNC_EXECUTOR);
    }

    @Override
    @NotNull
    public Map<String, String> hgetAll(@NotNull String key) {
        checkClosed();
        try {
            return jedis.hgetAll(key);
        } catch (Exception e) {
            throw new RedisException("HGETALL failed for key: " + key, e);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Map<String, String>> hgetAllAsync(@NotNull String key) {
        return CompletableFuture.supplyAsync(() -> hgetAll(key), ASYNC_EXECUTOR);
    }

    @Override
    public void hmset(@NotNull String key, @NotNull Map<String, String> fieldValues) {
        checkClosed();
        if (fieldValues.isEmpty()) return;
        try {
            jedis.hmset(key, fieldValues);
        } catch (Exception e) {
            throw new RedisException("HMSET failed for key: " + key, e);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Void> hmsetAsync(@NotNull String key, @NotNull Map<String, String> fieldValues) {
        return CompletableFuture.runAsync(() -> hmset(key, fieldValues), ASYNC_EXECUTOR);
    }

    @Override
    public long hdel(@NotNull String key, @NotNull String... fields) {
        checkClosed();
        try {
            return jedis.hdel(key, fields);
        } catch (Exception e) {
            throw new RedisException("HDEL failed for key: " + key, e);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Long> hdelAsync(@NotNull String key, @NotNull String... fields) {
        return CompletableFuture.supplyAsync(() -> hdel(key, fields), ASYNC_EXECUTOR);
    }

    @Override
    public boolean hexists(@NotNull String key, @NotNull String field) {
        checkClosed();
        try {
            return jedis.hexists(key, field);
        } catch (Exception e) {
            throw new RedisException("HEXISTS failed for key: " + key, e);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> hexistsAsync(@NotNull String key, @NotNull String field) {
        return CompletableFuture.supplyAsync(() -> hexists(key, field), ASYNC_EXECUTOR);
    }

    // ========== List Operations ==========

    @Override
    public long lpush(@NotNull String key, @NotNull String... values) {
        checkClosed();
        try {
            return jedis.lpush(key, values);
        } catch (Exception e) {
            throw new RedisException("LPUSH failed for key: " + key, e);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Long> lpushAsync(@NotNull String key, @NotNull String... values) {
        return CompletableFuture.supplyAsync(() -> lpush(key, values), ASYNC_EXECUTOR);
    }

    @Override
    public long rpush(@NotNull String key, @NotNull String... values) {
        checkClosed();
        try {
            return jedis.rpush(key, values);
        } catch (Exception e) {
            throw new RedisException("RPUSH failed for key: " + key, e);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Long> rpushAsync(@NotNull String key, @NotNull String... values) {
        return CompletableFuture.supplyAsync(() -> rpush(key, values), ASYNC_EXECUTOR);
    }

    @Override
    @NotNull
    public Optional<String> lpop(@NotNull String key) {
        checkClosed();
        try {
            return Optional.ofNullable(jedis.lpop(key));
        } catch (Exception e) {
            throw new RedisException("LPOP failed for key: " + key, e);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Optional<String>> lpopAsync(@NotNull String key) {
        return CompletableFuture.supplyAsync(() -> lpop(key), ASYNC_EXECUTOR);
    }

    @Override
    @NotNull
    public Optional<String> rpop(@NotNull String key) {
        checkClosed();
        try {
            return Optional.ofNullable(jedis.rpop(key));
        } catch (Exception e) {
            throw new RedisException("RPOP failed for key: " + key, e);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Optional<String>> rpopAsync(@NotNull String key) {
        return CompletableFuture.supplyAsync(() -> rpop(key), ASYNC_EXECUTOR);
    }

    @Override
    @NotNull
    public List<String> lrange(@NotNull String key, long start, long stop) {
        checkClosed();
        try {
            return jedis.lrange(key, start, stop);
        } catch (Exception e) {
            throw new RedisException("LRANGE failed for key: " + key, e);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<List<String>> lrangeAsync(@NotNull String key, long start, long stop) {
        return CompletableFuture.supplyAsync(() -> lrange(key, start, stop), ASYNC_EXECUTOR);
    }

    @Override
    public long llen(@NotNull String key) {
        checkClosed();
        try {
            return jedis.llen(key);
        } catch (Exception e) {
            throw new RedisException("LLEN failed for key: " + key, e);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Long> llenAsync(@NotNull String key) {
        return CompletableFuture.supplyAsync(() -> llen(key), ASYNC_EXECUTOR);
    }

    // ========== Set Operations ==========

    @Override
    public long sadd(@NotNull String key, @NotNull String... members) {
        checkClosed();
        try {
            return jedis.sadd(key, members);
        } catch (Exception e) {
            throw new RedisException("SADD failed for key: " + key, e);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Long> saddAsync(@NotNull String key, @NotNull String... members) {
        return CompletableFuture.supplyAsync(() -> sadd(key, members), ASYNC_EXECUTOR);
    }

    @Override
    @NotNull
    public Set<String> smembers(@NotNull String key) {
        checkClosed();
        try {
            return jedis.smembers(key);
        } catch (Exception e) {
            throw new RedisException("SMEMBERS failed for key: " + key, e);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Set<String>> smembersAsync(@NotNull String key) {
        return CompletableFuture.supplyAsync(() -> smembers(key), ASYNC_EXECUTOR);
    }

    @Override
    public boolean sismember(@NotNull String key, @NotNull String member) {
        checkClosed();
        try {
            return jedis.sismember(key, member);
        } catch (Exception e) {
            throw new RedisException("SISMEMBER failed for key: " + key, e);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> sismemberAsync(@NotNull String key, @NotNull String member) {
        return CompletableFuture.supplyAsync(() -> sismember(key, member), ASYNC_EXECUTOR);
    }

    @Override
    public long srem(@NotNull String key, @NotNull String... members) {
        checkClosed();
        try {
            return jedis.srem(key, members);
        } catch (Exception e) {
            throw new RedisException("SREM failed for key: " + key, e);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Long> sremAsync(@NotNull String key, @NotNull String... members) {
        return CompletableFuture.supplyAsync(() -> srem(key, members), ASYNC_EXECUTOR);
    }

    // ========== Sorted Set Operations ==========

    @Override
    public long zadd(@NotNull String key, @NotNull Map<String, Double> scoreMembers) {
        checkClosed();
        try {
            return jedis.zadd(key, scoreMembers);
        } catch (Exception e) {
            throw new RedisException("ZADD failed for key: " + key, e);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Long> zaddAsync(@NotNull String key, @NotNull Map<String, Double> scoreMembers) {
        return CompletableFuture.supplyAsync(() -> zadd(key, scoreMembers), ASYNC_EXECUTOR);
    }

    @Override
    @NotNull
    public Optional<Double> zscore(@NotNull String key, @NotNull String member) {
        checkClosed();
        try {
            return Optional.ofNullable(jedis.zscore(key, member));
        } catch (Exception e) {
            throw new RedisException("ZSCORE failed for key: " + key, e);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Optional<Double>> zscoreAsync(@NotNull String key, @NotNull String member) {
        return CompletableFuture.supplyAsync(() -> zscore(key, member), ASYNC_EXECUTOR);
    }

    @Override
    @NotNull
    public List<String> zrange(@NotNull String key, long start, long stop) {
        checkClosed();
        try {
            return jedis.zrange(key, start, stop);
        } catch (Exception e) {
            throw new RedisException("ZRANGE failed for key: " + key, e);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<List<String>> zrangeAsync(@NotNull String key, long start, long stop) {
        return CompletableFuture.supplyAsync(() -> zrange(key, start, stop), ASYNC_EXECUTOR);
    }

    // ========== Pub/Sub Operations ==========

    @Override
    public long publish(@NotNull String channel, @NotNull String message) {
        checkClosed();
        try {
            return jedis.publish(channel, message);
        } catch (Exception e) {
            throw new RedisException("PUBLISH failed for channel: " + channel, e);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Long> publishAsync(@NotNull String channel, @NotNull String message) {
        return CompletableFuture.supplyAsync(() -> publish(channel, message), ASYNC_EXECUTOR);
    }

    // ========== Script Operations ==========

    @Override
    @Nullable
    public Object eval(@NotNull String script, @NotNull List<String> keys, @NotNull List<String> args) {
        checkClosed();
        try {
            return jedis.eval(script, keys, args);
        } catch (Exception e) {
            throw new RedisException("EVAL failed", e);
        }
    }

    @Override
    @NotNull
    public CompletableFuture<Object> evalAsync(@NotNull String script, @NotNull List<String> keys, @NotNull List<String> args) {
        return CompletableFuture.supplyAsync(() -> eval(script, keys, args), ASYNC_EXECUTOR);
    }

    @Override
    @Nullable
    public Object evalsha(@NotNull String sha1, @NotNull List<String> keys, @NotNull List<String> args) {
        checkClosed();
        try {
            return jedis.evalsha(sha1, keys, args);
        } catch (Exception e) {
            throw new RedisException("EVALSHA failed", e);
        }
    }

    @Override
    @NotNull
    public String scriptLoad(@NotNull String script) {
        checkClosed();
        try {
            return jedis.scriptLoad(script);
        } catch (Exception e) {
            throw new RedisException("SCRIPT LOAD failed", e);
        }
    }

    // ========== Transaction Support ==========

    @Override
    @NotNull
    public RedisTransaction multi() {
        checkClosed();
        return new JedisTransactionWrapper(jedis.multi());
    }

    private void checkClosed() {
        if (closed) {
            throw new RedisException("Connection is closed");
        }
    }

    /**
     * Jedis transaction wrapper.
     */
    private static class JedisTransactionWrapper implements RedisTransaction {
        private final Transaction transaction;

        JedisTransactionWrapper(Transaction transaction) {
            this.transaction = transaction;
        }

        @Override
        @NotNull
        public RedisTransaction set(@NotNull String key, @NotNull String value) {
            transaction.set(key, value);
            return this;
        }

        @Override
        @NotNull
        public RedisTransaction get(@NotNull String key) {
            transaction.get(key);
            return this;
        }

        @Override
        @NotNull
        public RedisTransaction del(@NotNull String... keys) {
            transaction.del(keys);
            return this;
        }

        @Override
        @NotNull
        public RedisTransaction incr(@NotNull String key) {
            transaction.incr(key);
            return this;
        }

        @Override
        @NotNull
        public RedisTransaction expire(@NotNull String key, @NotNull Duration ttl) {
            transaction.expire(key, ttl.toSeconds());
            return this;
        }

        @Override
        @NotNull
        public List<Object> exec() {
            return transaction.exec();
        }

        @Override
        public void discard() {
            transaction.discard();
        }
    }
}
