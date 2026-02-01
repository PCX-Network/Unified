/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */

/**
 * Service infrastructure for registering and discovering services.
 *
 * <p>This package provides the service registry system:
 * <ul>
 *   <li>{@link sh.pcx.unified.service.Service} - Marker interface for services</li>
 *   <li>{@link sh.pcx.unified.service.ServiceRegistry} - Service registration/lookup</li>
 *   <li>{@link sh.pcx.unified.service.ServiceProvider} - SPI for service providers</li>
 * </ul>
 *
 * <h2>Registering a Service</h2>
 * <pre>{@code
 * // Define a service interface
 * public interface MyService extends Service {
 *     void doSomething();
 * }
 *
 * // Register the service
 * ServiceRegistry services = UnifiedAPI.getInstance().services();
 * services.register(MyService.class, new MyServiceImpl());
 *
 * // Retrieve the service
 * MyService service = services.get(MyService.class).orElseThrow();
 * service.doSomething();
 * }</pre>
 *
 * <h2>Service Provider SPI</h2>
 * <p>Services can also be provided via the {@link sh.pcx.unified.service.ServiceProvider}
 * SPI, allowing automatic discovery and registration.
 *
 * @since 1.0.0
 * @author Supatuck
 */
package sh.pcx.unified.service;
