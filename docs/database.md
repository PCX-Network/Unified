# Database Guide

UnifiedPlugin provides comprehensive database support for SQL databases (MySQL, PostgreSQL, SQLite, H2), Redis, and MongoDB. This guide covers setup, configuration, and usage patterns for each.

## Table of Contents

- [SQL Databases](#sql-databases)
  - [Configuration](#sql-configuration)
  - [Query Building](#query-building)
  - [Transactions](#transactions)
  - [Repository Pattern](#repository-pattern)
- [Redis](#redis)
  - [Configuration](#redis-configuration)
  - [Basic Operations](#basic-operations)
  - [Pub/Sub](#pubsub)
  - [Namespacing](#key-namespacing)
- [MongoDB](#mongodb)
  - [Configuration](#mongodb-configuration)
  - [CRUD Operations](#crud-operations)
  - [Aggregations](#aggregations)

---

## SQL Databases

The `DatabaseService` provides a high-level API for SQL database operations with connection pooling via HikariCP.

### SQL Configuration

Create a database configuration in your config file:

```yaml
database:
  type: MYSQL  # MYSQL, POSTGRESQL, SQLITE, H2
  host: localhost
  port: 3306
  database: minecraft
  username: root
  password: secret
  pool:
    maximum-size: 10
    minimum-idle: 2
    connection-timeout: 30000
    idle-timeout: 600000
    max-lifetime: 1800000
```

### Getting DatabaseService

```java
import sh.pcx.unified.data.sql.DatabaseService;
import javax.inject.Inject;

public class MyPlugin extends UnifiedPlugin {

    @Inject
    private DatabaseService database;

    @Override
    public void onEnable() {
        // Check database health
        if (!database.isHealthy()) {
            getLogger().severe("Database connection failed!");
            return;
        }

        getLogger().info("Connected to " + database.getDatabaseType());
    }
}
```

### Query Building

#### Select Queries

```java
// Using the fluent query builder with entity mapping
List<PlayerData> players = database.query(PlayerData.class)
    .where("level", ">", 10)
    .and("vip", true)
    .orderBy("level", Order.DESC)
    .limit(100)
    .execute();

// Async execution
database.query(PlayerData.class)
    .where("uuid", uuid)
    .executeAsync()
    .thenAccept(results -> {
        results.forEach(p -> logger.info("Found: " + p.getName()));
    });

// First result only
Optional<PlayerData> player = database.query(PlayerData.class)
    .where("uuid", uuid)
    .first();
```

#### Raw SQL Queries

```java
// Query with result mapping
List<String> names = database.query(
    "SELECT name FROM players WHERE level > ?",
    rs -> rs.getString("name"),
    10
);

// Query first result
Optional<PlayerData> player = database.queryFirst(
    "SELECT * FROM players WHERE uuid = ?",
    rs -> new PlayerData(rs),
    uuid.toString()
);

// Async query
database.queryAsync("SELECT * FROM players WHERE vip = ?", rs -> {
    return new PlayerData(rs.getString("uuid"), rs.getString("name"));
}, true).thenAccept(vipPlayers -> {
    vipPlayers.forEach(p -> logger.info("VIP: " + p.getName()));
});
```

#### Insert, Update, Delete

```java
// Execute update statement
int affected = database.execute(
    "UPDATE players SET balance = ? WHERE uuid = ?",
    newBalance, uuid.toString()
);

// Async execution
database.executeAsync("UPDATE players SET online = false WHERE server = ?", serverName)
    .thenAccept(affected -> logger.info("Marked " + affected + " players offline"));
```

### Transactions

```java
// Transaction with return value
boolean success = database.transaction(conn -> {
    conn.executeUpdate(
        "UPDATE accounts SET balance = balance - ? WHERE id = ?",
        amount, fromId
    );
    conn.executeUpdate(
        "UPDATE accounts SET balance = balance + ? WHERE id = ?",
        amount, toId
    );
    return true;  // Commit
});

// Async transaction
database.transactionAsync(conn -> {
    conn.executeUpdate("INSERT INTO audit_log (action) VALUES (?)", "transfer");
    conn.executeUpdate("UPDATE accounts SET balance = balance - ? WHERE id = ?", amount, from);
    conn.executeUpdate("UPDATE accounts SET balance = balance + ? WHERE id = ?", amount, to);
    return true;
}).thenAccept(success -> {
    if (success) {
        player.sendMessage("Transfer complete!");
    }
});

// Void transaction (no return value)
database.transactionVoid(conn -> {
    conn.executeUpdate("DELETE FROM expired_sessions WHERE expires < NOW()");
    conn.executeUpdate("INSERT INTO cleanup_log (timestamp) VALUES (NOW())");
});
```

### Repository Pattern

Define entity classes with annotations:

```java
import sh.pcx.unified.data.sql.orm.Table;
import sh.pcx.unified.data.sql.orm.Column;
import sh.pcx.unified.data.sql.orm.Id;

@Table("players")
public class PlayerData {

    @Id
    @Column("uuid")
    private UUID uuid;

    @Column("name")
    private String name;

    @Column("balance")
    private double balance;

    @Column("level")
    private int level;

    @Column("vip")
    private boolean vip;

    @Column("last_login")
    private Instant lastLogin;

    // Getters and setters...
}
```

Use the repository:

```java
import sh.pcx.unified.data.sql.orm.Repository;

// Get repository for entity
Repository<PlayerData, UUID> playerRepo = database.getRepository(PlayerData.class);

// Save entity
playerRepo.save(playerData);

// Find by ID
Optional<PlayerData> player = playerRepo.findById(uuid).join();

// Find all matching criteria
List<PlayerData> vips = playerRepo.findAll(query -> query.where("vip", true)).join();

// Delete
playerRepo.deleteById(uuid);

// Count
long count = playerRepo.count().join();
```

### Schema Management

```java
// Check if table exists
boolean exists = database.tableExists("players");

// Create table from entity class
database.createTableAsync(PlayerData.class)
    .thenRun(() -> logger.info("Player data table created"));
```

---

## Redis

The `RedisService` provides Redis operations with support for both Jedis and Lettuce clients.

### Redis Configuration

```yaml
redis:
  enabled: true
  host: localhost
  port: 6379
  password: ""
  database: 0
  pool:
    max-total: 16
    max-idle: 8
    min-idle: 2
```

### Getting RedisService

```java
import sh.pcx.unified.data.redis.RedisService;
import javax.inject.Inject;

@Inject
private RedisService redis;

// Check health
if (redis.isHealthy()) {
    logger.info("Redis connected!");
}
```

### Basic Operations

#### Strings

```java
// Set and get
redis.set("player:uuid:name", "Steve");
Optional<String> name = redis.get("player:uuid:name");

// With expiration
redis.setex("session:token", sessionData, Duration.ofHours(1));

// Set if not exists
boolean wasSet = redis.setnx("lock:resource", "locked");

// Async operations
redis.getAsync("key")
    .thenAccept(value -> System.out.println("Value: " + value.orElse("not found")));

// Multiple keys
redis.mset(Map.of(
    "key1", "value1",
    "key2", "value2"
));

List<String> values = redis.mget("key1", "key2", "key3");
```

#### Numeric Operations

```java
// Increment/decrement
long newValue = redis.incr("player:kills");
long decreased = redis.decr("player:lives");

// Increment by amount
long score = redis.incrBy("player:score", 100);
```

#### Key Operations

```java
// Delete keys
long deleted = redis.del("key1", "key2");

// Check existence
boolean exists = redis.exists("player:uuid");

// Set TTL
redis.expire("session:token", Duration.ofMinutes(30));

// Get TTL
Optional<Duration> ttl = redis.ttl("session:token");
```

#### Hashes

```java
// Single field
redis.hset("player:uuid", "name", "Steve");
Optional<String> name = redis.hget("player:uuid", "name");

// Multiple fields
redis.hmset("player:uuid", Map.of(
    "name", "Steve",
    "level", "42",
    "vip", "true"
));

// Get all fields
Map<String, String> playerData = redis.hgetAll("player:uuid");

// Delete fields
redis.hdel("player:uuid", "tempData");
```

#### Lists

```java
// Push to list
redis.lpush("queue:messages", "message1", "message2");
redis.rpush("queue:messages", "message3");

// Pop from list
Optional<String> message = redis.lpop("queue:messages");

// Get range
List<String> messages = redis.lrange("queue:messages", 0, 9);  // First 10
```

#### Sets

```java
// Add to set
redis.sadd("online:players", "Steve", "Alex");

// Get all members
Set<String> onlinePlayers = redis.smembers("online:players");

// Check membership
boolean isOnline = redis.sismember("online:players", "Steve");

// Remove from set
redis.srem("online:players", "Steve");
```

### Pub/Sub

```java
// Subscribe to channel
PubSubSubscription subscription = redis.subscribe("chat:global", message -> {
    logger.info("Received: " + message);
    broadcastToPlayers(message);
});

// Pattern subscription
PubSubSubscription patternSub = redis.psubscribe("chat:*", (channel, message) -> {
    logger.info("Channel " + channel + ": " + message);
});

// Publish message
long subscribers = redis.publish("chat:global", "Hello, world!");

// Unsubscribe
subscription.unsubscribe();
// Or use try-with-resources
try (var sub = redis.subscribe("events", this::handleEvent)) {
    // Subscription active
}
```

### Key Namespacing

Prevent key collisions between plugins:

```java
// Create namespace
KeyNamespace ns = redis.namespace("myplugin");

// All operations use the prefix
ns.set("data", "value");           // Actually stores "myplugin:data"
ns.get("data");                     // Gets "myplugin:data"
ns.hset("player", "name", "Steve"); // Uses "myplugin:player"

// Nested namespaces
KeyNamespace playerNs = redis.namespace("myplugin", "players");
playerNs.set("uuid", "data");  // Stores "myplugin:players:uuid"
```

### Connection Management

```java
// Direct connection access
try (RedisConnection conn = redis.getConnection()) {
    conn.multi()
        .set("key1", "value1")
        .set("key2", "value2")
        .incr("counter")
        .exec();
}

// Using connection with function
String result = redis.withConnection(conn -> {
    conn.set("temp", "data");
    return conn.get("temp");
});

// Pool statistics
RedisService.PoolStats stats = redis.getPoolStats();
logger.info("Active connections: " + stats.active());
logger.info("Idle connections: " + stats.idle());
```

### Lua Scripts

```java
// Execute Lua script
Object result = redis.eval(
    "return redis.call('get', KEYS[1])",
    List.of("mykey"),
    List.of()
);

// Load and cache script
RedisLuaScript script = redis.loadScript(
    "local current = redis.call('get', KEYS[1]) or 0 " +
    "local new = current + ARGV[1] " +
    "redis.call('set', KEYS[1], new) " +
    "return new"
);

// Execute cached script
Long newValue = (Long) script.execute(List.of("counter"), List.of("10"));
```

---

## MongoDB

The `MongoService` provides async-first MongoDB operations using the MongoDB Java Driver.

### MongoDB Configuration

```yaml
mongodb:
  enabled: true
  connection-string: mongodb://localhost:27017
  database: minecraft
  options:
    max-pool-size: 20
    min-pool-size: 5
    connection-timeout: 10000
```

### Getting MongoService

```java
import sh.pcx.unified.data.mongo.MongoService;
import javax.inject.Inject;

@Inject
private MongoService mongo;

// Verify connection
mongo.ping().thenRun(() -> logger.info("MongoDB connected!"));
```

### CRUD Operations

#### Insert

```java
import org.bson.Document;

// Insert single document
Document player = new Document("uuid", uuid.toString())
    .append("name", "Steve")
    .append("balance", 1000.0)
    .append("joinedAt", Instant.now());

mongo.insertOne("players", player)
    .thenAccept(result -> {
        logger.info("Inserted player with id: " + result.getInsertedId());
    });

// Insert multiple
List<Document> players = List.of(player1, player2, player3);
mongo.insertMany("players", players)
    .thenAccept(result -> {
        logger.info("Inserted " + result.getInsertedIds().size() + " players");
    });
```

#### Find

```java
import static com.mongodb.client.model.Filters.*;

// Find all documents
mongo.find("players")
    .thenAccept(docs -> docs.forEach(d -> logger.info(d.toJson())));

// Find with filter
mongo.find("players", eq("name", "Steve"))
    .thenAccept(docs -> {
        docs.forEach(d -> logger.info("Found: " + d.getString("name")));
    });

// Find one
mongo.findOne("players", eq("uuid", uuid.toString()))
    .thenAccept(optDoc -> {
        optDoc.ifPresent(doc -> {
            logger.info("Player balance: " + doc.getDouble("balance"));
        });
    });

// Find by ID
mongo.findById("players", objectId)
    .thenAccept(optDoc -> {
        // Handle result
    });

// Check existence
mongo.exists("players", eq("vip", true))
    .thenAccept(hasVips -> {
        logger.info("Has VIP players: " + hasVips);
    });

// Count documents
mongo.countDocuments("players", gte("level", 10))
    .thenAccept(count -> logger.info("High level players: " + count));
```

#### Update

```java
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;

// Update one document
mongo.updateOne("players",
    eq("uuid", uuid.toString()),
    set("balance", 2000.0))
    .thenAccept(result -> {
        logger.info("Modified: " + result.getModifiedCount());
    });

// Update multiple
mongo.updateMany("players",
    lt("lastLogin", thirtyDaysAgo),
    set("inactive", true))
    .thenAccept(result -> {
        logger.info("Marked " + result.getModifiedCount() + " inactive");
    });

// Multiple update operations
mongo.updateOne("players",
    eq("uuid", uuid.toString()),
    combine(
        set("balance", 2000.0),
        inc("loginCount", 1),
        currentDate("lastLogin")
    ));

// Find and update atomically
mongo.findOneAndUpdate("players",
    eq("uuid", uuid.toString()),
    inc("balance", 100.0))
    .thenAccept(optDoc -> {
        // Returns document BEFORE update
    });

// Replace entire document
mongo.replaceOne("players",
    eq("uuid", uuid.toString()),
    newPlayerDocument);
```

#### Delete

```java
// Delete one
mongo.deleteOne("players", eq("uuid", uuid.toString()))
    .thenAccept(result -> {
        logger.info("Deleted: " + result.getDeletedCount());
    });

// Delete many
mongo.deleteMany("players", lt("lastLogin", oneYearAgo))
    .thenAccept(result -> {
        logger.info("Removed " + result.getDeletedCount() + " inactive players");
    });

// Find and delete
mongo.findOneAndDelete("queue", new Document())  // First document
    .thenAccept(optDoc -> {
        optDoc.ifPresent(doc -> processQueueItem(doc));
    });
```

### Type-Safe Collections

```java
// Get typed collection wrapper
MongoCollectionWrapper<PlayerData> players =
    mongo.getCollection("players", PlayerData.class);

// Operations with type safety
players.findOne(eq("uuid", uuid.toString()))
    .thenAccept(optPlayer -> {
        optPlayer.ifPresent(p -> {
            logger.info("Balance: " + p.getBalance());
        });
    });

players.insertOne(new PlayerData(uuid, "Steve", 1000.0));
```

### Query Builder

```java
// Fluent query building
List<Document> results = mongo.query("players")
    .filter(gte("level", 10))
    .sort("level", -1)
    .skip(0)
    .limit(10)
    .project("name", "level", "balance")
    .execute()
    .join();

// Typed query
List<PlayerData> topPlayers = mongo.query("players", PlayerData.class)
    .filter(eq("vip", true))
    .sort("balance", -1)
    .limit(10)
    .execute()
    .join();
```

### Aggregations

```java
import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Accumulators.*;

// Build aggregation pipeline
List<Bson> pipeline = List.of(
    match(gte("level", 10)),
    group("$region",
        sum("totalPlayers", 1),
        avg("avgBalance", "$balance"),
        max("highestLevel", "$level")
    ),
    sort(descending("totalPlayers")),
    limit(10)
);

mongo.aggregate("players", pipeline)
    .thenAccept(results -> {
        results.forEach(doc -> {
            String region = doc.getString("_id");
            int count = doc.getInteger("totalPlayers");
            logger.info(region + ": " + count + " players");
        });
    });

// Using aggregation builder
mongo.aggregation("players")
    .match(gte("createdAt", lastMonth))
    .group("$country", sum("count", 1))
    .sort("count", -1)
    .execute()
    .thenAccept(this::displayStats);
```

### Indexes

```java
import static com.mongodb.client.model.Indexes.*;

// Create index
mongo.createIndex("players", ascending("uuid"))
    .thenAccept(indexName -> logger.info("Created index: " + indexName));

// Compound index
mongo.createIndex("players", compoundIndex(
    ascending("region"),
    descending("level")
), "region_level_idx");

// List indexes
mongo.listIndexes("players")
    .thenAccept(indexes -> {
        indexes.forEach(idx -> logger.info("Index: " + idx.toJson()));
    });

// Drop index
mongo.dropIndex("players", "old_index");
```

### Change Streams

```java
// Watch for changes
ChangeStreamManager changeManager = mongo.getChangeStreamManager();

changeManager.watch("players", change -> {
    switch (change.getOperationType()) {
        case INSERT:
            logger.info("New player: " + change.getFullDocument());
            break;
        case UPDATE:
            logger.info("Updated: " + change.getDocumentKey());
            break;
        case DELETE:
            logger.info("Deleted: " + change.getDocumentKey());
            break;
    }
});
```

---

## Complete Example

```java
public class PlayerDataService {

    @Inject private DatabaseService sql;
    @Inject private RedisService redis;
    @Inject private MongoService mongo;

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    public CompletableFuture<Optional<PlayerData>> getPlayer(UUID uuid) {
        String cacheKey = "player:" + uuid;

        // Check Redis cache first
        return redis.getAsync(cacheKey)
            .thenCompose(cached -> {
                if (cached.isPresent()) {
                    return CompletableFuture.completedFuture(
                        Optional.of(deserialize(cached.get()))
                    );
                }

                // Cache miss - load from database
                return sql.query(PlayerData.class)
                    .where("uuid", uuid)
                    .executeAsync()
                    .thenApply(results -> results.isEmpty()
                        ? Optional.empty()
                        : Optional.of(results.getFirst()))
                    .thenApply(optPlayer -> {
                        // Cache the result
                        optPlayer.ifPresent(p ->
                            redis.setexAsync(cacheKey, serialize(p), CACHE_TTL)
                        );
                        return optPlayer;
                    });
            });
    }

    public CompletableFuture<Void> savePlayer(PlayerData player) {
        String cacheKey = "player:" + player.getUuid();

        return sql.getRepository(PlayerData.class)
            .saveAsync(player)
            .thenCompose(v ->
                // Update cache
                redis.setexAsync(cacheKey, serialize(player), CACHE_TTL)
            )
            .thenCompose(v ->
                // Log to MongoDB for analytics
                mongo.insertOne("player_updates", new Document()
                    .append("uuid", player.getUuid().toString())
                    .append("action", "save")
                    .append("timestamp", Instant.now()))
            )
            .thenApply(r -> null);
    }

    public CompletableFuture<Void> invalidateCache(UUID uuid) {
        return redis.delAsync("player:" + uuid)
            .thenApply(count -> null);
    }
}
```

---

## Next Steps

- [Modules Guide](modules.md) - Build modular plugin architectures
- [Commands Guide](commands.md) - Create annotation-based commands
- [Testing Guide](testing.md) - Test database operations with mocks
