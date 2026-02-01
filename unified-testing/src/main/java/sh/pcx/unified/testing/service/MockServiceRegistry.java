/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.testing.service;

import sh.pcx.unified.service.Service;
import sh.pcx.unified.service.ServiceRegistry;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Mock implementation of ServiceRegistry for testing service interactions.
 *
 * <p>MockServiceRegistry provides a way to register mock services and
 * automatically generate mock implementations for interfaces.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Manual mock registration</li>
 *   <li>Automatic mock generation using proxies</li>
 *   <li>Service call tracking</li>
 *   <li>Method stubbing</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * MockServiceRegistry registry = new MockServiceRegistry();
 *
 * // Register a manual mock
 * EconomyService mockEconomy = new MockEconomyService();
 * registry.registerMock(EconomyService.class, mockEconomy);
 *
 * // Auto-generate a mock
 * DatabaseService mockDb = registry.createMock(DatabaseService.class);
 *
 * // Get the mock
 * EconomyService economy = registry.get(EconomyService.class).orElseThrow();
 * }</pre>
 *
 * @since 1.0.0
 * @author Supatuck
 */
public final class MockServiceRegistry implements ServiceRegistry {

    private final Map<Class<?>, ServiceHolder> services = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<MethodCall>> callHistory = new ConcurrentHashMap<>();
    private final Map<Class<?>, Map<String, Object>> stubbedReturns = new ConcurrentHashMap<>();

    /**
     * Creates a new mock service registry.
     */
    public MockServiceRegistry() {
    }

    /**
     * Registers a mock service implementation.
     *
     * @param <T>         the service type
     * @param serviceType the service interface class
     * @param mock        the mock implementation
     */
    public <T> void registerMock(@NotNull Class<T> serviceType, @NotNull T mock) {
        Objects.requireNonNull(serviceType, "serviceType cannot be null");
        Objects.requireNonNull(mock, "mock cannot be null");

        services.put(serviceType, new ServiceHolder(mock, this, ServicePriority.NORMAL));
    }

    /**
     * Creates an auto-generated mock for a service interface.
     *
     * <p>The generated mock will:
     * <ul>
     *   <li>Track all method calls</li>
     *   <li>Return stubbed values if configured</li>
     *   <li>Return default values for unstubbed methods</li>
     * </ul>
     *
     * @param <T>         the service type
     * @param serviceType the service interface class
     * @return the generated mock
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> T createMock(@NotNull Class<T> serviceType) {
        Objects.requireNonNull(serviceType, "serviceType cannot be null");

        if (!serviceType.isInterface()) {
            throw new IllegalArgumentException("serviceType must be an interface");
        }

        T mock = (T) Proxy.newProxyInstance(
            serviceType.getClassLoader(),
            new Class<?>[]{serviceType},
            (proxy, method, args) -> {
                // Record the call
                recordCall(serviceType, method.getName(), args);

                // Check for stubbed return
                Map<String, Object> stubs = stubbedReturns.get(serviceType);
                if (stubs != null && stubs.containsKey(method.getName())) {
                    return stubs.get(method.getName());
                }

                // Return default value based on return type
                Class<?> returnType = method.getReturnType();
                return getDefaultValue(returnType);
            }
        );

        registerMock(serviceType, mock);
        return mock;
    }

    /**
     * Stubs a method to return a specific value.
     *
     * @param <T>         the service type
     * @param serviceType the service interface class
     * @param methodName  the method name
     * @param returnValue the value to return
     */
    public <T> void stubReturn(
        @NotNull Class<T> serviceType,
        @NotNull String methodName,
        Object returnValue
    ) {
        Objects.requireNonNull(serviceType, "serviceType cannot be null");
        Objects.requireNonNull(methodName, "methodName cannot be null");

        stubbedReturns
            .computeIfAbsent(serviceType, k -> new ConcurrentHashMap<>())
            .put(methodName, returnValue);
    }

    /**
     * Gets the call history for a service.
     *
     * @param serviceType the service interface class
     * @return list of method calls
     */
    @NotNull
    public List<MethodCall> getCallHistory(@NotNull Class<?> serviceType) {
        return Collections.unmodifiableList(
            callHistory.getOrDefault(serviceType, Collections.emptyList())
        );
    }

    /**
     * Clears the call history for a service.
     *
     * @param serviceType the service interface class
     */
    public void clearCallHistory(@NotNull Class<?> serviceType) {
        callHistory.remove(serviceType);
    }

    /**
     * Clears all call history.
     */
    public void clearAllCallHistory() {
        callHistory.clear();
    }

    /**
     * Verifies that a method was called.
     *
     * @param serviceType the service interface class
     * @param methodName  the method name
     * @return true if the method was called
     */
    public boolean wasCalled(@NotNull Class<?> serviceType, @NotNull String methodName) {
        List<MethodCall> calls = callHistory.get(serviceType);
        return calls != null && calls.stream().anyMatch(c -> c.methodName().equals(methodName));
    }

    /**
     * Counts how many times a method was called.
     *
     * @param serviceType the service interface class
     * @param methodName  the method name
     * @return the call count
     */
    public int callCount(@NotNull Class<?> serviceType, @NotNull String methodName) {
        List<MethodCall> calls = callHistory.get(serviceType);
        if (calls == null) {
            return 0;
        }
        return (int) calls.stream().filter(c -> c.methodName().equals(methodName)).count();
    }

    private void recordCall(Class<?> serviceType, String methodName, Object[] args) {
        callHistory
            .computeIfAbsent(serviceType, k -> Collections.synchronizedList(new ArrayList<>()))
            .add(new MethodCall(methodName, args != null ? args : new Object[0]));
    }

    private Object getDefaultValue(Class<?> type) {
        if (type == boolean.class || type == Boolean.class) return false;
        if (type == byte.class || type == Byte.class) return (byte) 0;
        if (type == short.class || type == Short.class) return (short) 0;
        if (type == int.class || type == Integer.class) return 0;
        if (type == long.class || type == Long.class) return 0L;
        if (type == float.class || type == Float.class) return 0.0f;
        if (type == double.class || type == Double.class) return 0.0;
        if (type == char.class || type == Character.class) return '\0';
        if (type == void.class) return null;
        if (type == Optional.class) return Optional.empty();
        if (type == List.class) return Collections.emptyList();
        if (type == Set.class) return Collections.emptySet();
        if (type == Map.class) return Collections.emptyMap();
        if (type == Collection.class) return Collections.emptyList();
        return null;
    }

    // ==================== ServiceRegistry Implementation ====================

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T extends Service> void register(
        @NotNull Class<T> serviceType,
        @NotNull Object provider,
        @NotNull T service,
        @NotNull ServicePriority priority
    ) {
        Objects.requireNonNull(serviceType, "serviceType cannot be null");
        Objects.requireNonNull(provider, "provider cannot be null");
        Objects.requireNonNull(service, "service cannot be null");
        Objects.requireNonNull(priority, "priority cannot be null");

        services.put(serviceType, new ServiceHolder(service, provider, priority));
    }

    @Override
    public <T extends Service> void register(@NotNull Class<T> serviceType, @NotNull T service) {
        register(serviceType, this, service, ServicePriority.NORMAL);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Service> void registerLazy(
        @NotNull Class<T> serviceType,
        @NotNull Object provider,
        @NotNull Supplier<T> supplier,
        @NotNull ServicePriority priority
    ) {
        Objects.requireNonNull(serviceType, "serviceType cannot be null");
        Objects.requireNonNull(supplier, "supplier cannot be null");

        // For mock, we just call the supplier immediately
        register(serviceType, provider, supplier.get(), priority);
    }

    @Override
    public <T extends Service> boolean unregister(@NotNull Class<T> serviceType, @NotNull Object provider) {
        ServiceHolder holder = services.get(serviceType);
        if (holder != null && holder.provider().equals(provider)) {
            services.remove(serviceType);
            return true;
        }
        return false;
    }

    @Override
    public int unregisterAll(@NotNull Object provider) {
        int count = 0;
        Iterator<Map.Entry<Class<?>, ServiceHolder>> iter = services.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Class<?>, ServiceHolder> entry = iter.next();
            if (entry.getValue().provider().equals(provider)) {
                iter.remove();
                count++;
            }
        }
        return count;
    }

    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public <T extends Service> Optional<T> get(@NotNull Class<T> serviceType) {
        ServiceHolder holder = services.get(serviceType);
        return holder != null ? Optional.of((T) holder.service()) : Optional.empty();
    }

    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public <T extends Service> Collection<T> getAll(@NotNull Class<T> serviceType) {
        ServiceHolder holder = services.get(serviceType);
        return holder != null ? List.of((T) holder.service()) : Collections.emptyList();
    }

    @Override
    public <T extends Service> boolean isRegistered(@NotNull Class<T> serviceType) {
        return services.containsKey(serviceType);
    }

    @Override
    @NotNull
    public <T extends Service> Optional<Object> getProvider(@NotNull Class<T> serviceType) {
        ServiceHolder holder = services.get(serviceType);
        return holder != null ? Optional.of(holder.provider()) : Optional.empty();
    }

    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public Collection<Class<? extends Service>> getRegisteredServices() {
        return (Collection<Class<? extends Service>>) (Collection<?>)
            Collections.unmodifiableSet(services.keySet());
    }

    @Override
    public int size() {
        return services.size();
    }

    /**
     * Clears all registered services.
     */
    public void clear() {
        services.clear();
        callHistory.clear();
        stubbedReturns.clear();
    }

    /**
     * Record representing a service holder.
     */
    private record ServiceHolder(Object service, Object provider, ServicePriority priority) {}

    /**
     * Record representing a method call.
     *
     * @param methodName the method name
     * @param arguments  the method arguments
     */
    public record MethodCall(String methodName, Object[] arguments) {
        @Override
        public String toString() {
            return methodName + "(" + Arrays.toString(arguments) + ")";
        }
    }
}
