/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root.
 */

/**
 * Shared Library Provider for the UnifiedPlugin API framework.
 *
 * <p>This package provides centralized library management that eliminates the need
 * for plugins to shade common dependencies. Libraries are loaded once by the
 * framework and shared across all dependent plugins.
 *
 * <h2>Package Overview</h2>
 * <table border="1">
 *   <caption>Library Package Components</caption>
 *   <tr><th>Category</th><th>Classes</th><th>Purpose</th></tr>
 *   <tr>
 *     <td>Core API</td>
 *     <td>{@link sh.pcx.unified.library.LibraryProvider},
 *         {@link sh.pcx.unified.library.LibraryRegistry},
 *         {@link sh.pcx.unified.library.Library}</td>
 *     <td>Main interfaces for library access and registration</td>
 *   </tr>
 *   <tr>
 *     <td>Versioning</td>
 *     <td>{@link sh.pcx.unified.library.LibraryVersion},
 *         {@link sh.pcx.unified.library.VersionRange}</td>
 *     <td>Semantic versioning and version range matching</td>
 *   </tr>
 *   <tr>
 *     <td>Classloaders</td>
 *     <td>{@link sh.pcx.unified.library.LibraryClassLoader},
 *         {@link sh.pcx.unified.library.IsolatedClassLoader}</td>
 *     <td>Specialized classloaders for library loading</td>
 *   </tr>
 *   <tr>
 *     <td>Discovery</td>
 *     <td>{@link sh.pcx.unified.library.ServiceDiscovery},
 *         {@link sh.pcx.unified.library.UnifiedServiceLoader}</td>
 *     <td>SPI-based service discovery</td>
 *   </tr>
 *   <tr>
 *     <td>Runtime</td>
 *     <td>{@link sh.pcx.unified.library.Libraries},
 *         {@link sh.pcx.unified.library.LibraryStatus}</td>
 *     <td>Static utility access and status tracking</td>
 *   </tr>
 *   <tr>
 *     <td>Plugin Integration</td>
 *     <td>{@link sh.pcx.unified.library.PluginLibraries},
 *         {@link sh.pcx.unified.library.DependencyResolver}</td>
 *     <td>Per-plugin library configuration</td>
 *   </tr>
 * </table>
 *
 * <h2>Provided Libraries</h2>
 * <p>The UnifiedPlugin API provides these shared libraries:
 * <ul>
 *   <li><b>Google Guice 7.0.0</b> - Dependency injection framework</li>
 *   <li><b>Configurate 4.2.0</b> - Configuration management</li>
 *   <li><b>Adventure 4.26.1</b> - Text component API</li>
 *   <li><b>HikariCP 7.0.2</b> - High-performance JDBC connection pooling</li>
 *   <li><b>Caffeine 3.2.3</b> - High-performance caching</li>
 *   <li><b>Jedis 7.2.0</b> - Synchronous Redis client</li>
 *   <li><b>Lettuce 7.2.0.RELEASE</b> - Asynchronous Redis client</li>
 *   <li><b>MongoDB Driver 5.5.0</b> - MongoDB Java driver</li>
 *   <li><b>Gson 2.13.2</b> - JSON serialization</li>
 *   <li><b>SLF4J 2.0.17</b> - Simple Logging Facade for Java</li>
 * </ul>
 *
 * <h2>Quick Start</h2>
 * <pre>{@code
 * // Check library availability
 * if (Libraries.isAvailable("hikaricp")) {
 *     // Use HikariCP - no shading required!
 *     HikariDataSource ds = new HikariDataSource();
 * }
 *
 * // Get library information
 * String version = Libraries.getVersion("guice"); // "7.0.0"
 * Library lib = Libraries.getLibrary("adventure");
 *
 * // Implement PluginLibraries for validation
 * public class MyPlugin extends UnifiedPlugin implements PluginLibraries {
 *     public Collection<LibraryDependency> getRequiredLibraries() {
 *         return List.of(
 *             LibraryDependency.required("guice", "[7.0.0,8.0.0)"),
 *             LibraryDependency.required("hikaricp")
 *         );
 *     }
 * }
 * }</pre>
 *
 * <h2>Architecture</h2>
 * <pre>
 * +-----------------------+
 * |  UnifiedPluginAPI.jar |
 * |  (loaded at STARTUP)  |
 * +-----------------------+
 *           |
 *           v
 * +--------------------+      +-------------------+
 * | LibraryProvider    |----->| LibraryRegistry   |
 * +--------------------+      +-------------------+
 *           |
 *    +------+------+------+
 *    |      |      |      |
 *    v      v      v      v
 * +------+ +------+ +------+ +------+
 * | Guice| |Hikari| |Advent| | ...  |
 * +------+ +------+ +------+ +------+
 *           |
 *           v
 * +--------------------+
 * |  Plugin ClassPath  |
 * | (compileOnly deps) |
 * +--------------------+
 * </pre>
 *
 * @author Supatuck
 * @since 1.0.0
 * @see sh.pcx.unified.library.Libraries
 * @see sh.pcx.unified.library.LibraryProvider
 */
package sh.pcx.unified.library;
