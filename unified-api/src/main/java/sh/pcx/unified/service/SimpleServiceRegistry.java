/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.service;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Simple thread-safe implementation of {@link ServiceRegistry}.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public class SimpleServiceRegistry implements ServiceRegistry {

    private final Map<Class<? extends Service>, List<ServiceEntry<?>>> services = new ConcurrentHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    @Override
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

        lock.writeLock().lock();
        try {
            services.computeIfAbsent(serviceType, k -> new ArrayList<>())
                    .add(new ServiceEntry<>(provider, service, null, priority));
            sortByPriority(serviceType);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public <T extends Service> void register(@NotNull Class<T> serviceType, @NotNull T service) {
        register(serviceType, this, service, ServicePriority.NORMAL);
    }

    @Override
    public <T extends Service> void registerLazy(
            @NotNull Class<T> serviceType,
            @NotNull Object provider,
            @NotNull Supplier<T> supplier,
            @NotNull ServicePriority priority
    ) {
        Objects.requireNonNull(serviceType, "serviceType cannot be null");
        Objects.requireNonNull(provider, "provider cannot be null");
        Objects.requireNonNull(supplier, "supplier cannot be null");
        Objects.requireNonNull(priority, "priority cannot be null");

        lock.writeLock().lock();
        try {
            services.computeIfAbsent(serviceType, k -> new ArrayList<>())
                    .add(new ServiceEntry<>(provider, null, supplier, priority));
            sortByPriority(serviceType);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public <T extends Service> boolean unregister(@NotNull Class<T> serviceType, @NotNull Object provider) {
        lock.writeLock().lock();
        try {
            List<ServiceEntry<?>> entries = services.get(serviceType);
            if (entries == null) {
                return false;
            }
            boolean removed = entries.removeIf(e -> e.provider.equals(provider));
            if (entries.isEmpty()) {
                services.remove(serviceType);
            }
            return removed;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public int unregisterAll(@NotNull Object provider) {
        lock.writeLock().lock();
        try {
            int count = 0;
            Iterator<Map.Entry<Class<? extends Service>, List<ServiceEntry<?>>>> it = services.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Class<? extends Service>, List<ServiceEntry<?>>> entry = it.next();
                int before = entry.getValue().size();
                entry.getValue().removeIf(e -> e.provider.equals(provider));
                count += before - entry.getValue().size();
                if (entry.getValue().isEmpty()) {
                    it.remove();
                }
            }
            return count;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public <T extends Service> Optional<T> get(@NotNull Class<T> serviceType) {
        lock.readLock().lock();
        try {
            List<ServiceEntry<?>> entries = services.get(serviceType);
            if (entries == null || entries.isEmpty()) {
                return Optional.empty();
            }
            ServiceEntry<?> entry = entries.get(0);
            return Optional.of((T) entry.getService());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    public <T extends Service> Collection<T> getAll(@NotNull Class<T> serviceType) {
        lock.readLock().lock();
        try {
            List<ServiceEntry<?>> entries = services.get(serviceType);
            if (entries == null) {
                return Collections.emptyList();
            }
            return entries.stream()
                    .map(e -> (T) e.getService())
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public <T extends Service> boolean isRegistered(@NotNull Class<T> serviceType) {
        lock.readLock().lock();
        try {
            List<ServiceEntry<?>> entries = services.get(serviceType);
            return entries != null && !entries.isEmpty();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @NotNull
    public <T extends Service> Optional<Object> getProvider(@NotNull Class<T> serviceType) {
        lock.readLock().lock();
        try {
            List<ServiceEntry<?>> entries = services.get(serviceType);
            if (entries == null || entries.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(entries.get(0).provider);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @NotNull
    public Collection<Class<? extends Service>> getRegisteredServices() {
        lock.readLock().lock();
        try {
            return new HashSet<>(services.keySet());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int size() {
        lock.readLock().lock();
        try {
            return services.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    private void sortByPriority(Class<? extends Service> serviceType) {
        List<ServiceEntry<?>> entries = services.get(serviceType);
        if (entries != null) {
            entries.sort((a, b) -> Integer.compare(b.priority.getValue(), a.priority.getValue()));
        }
    }

    private static class ServiceEntry<T extends Service> {
        final Object provider;
        volatile T service;
        final Supplier<T> supplier;
        final ServicePriority priority;

        ServiceEntry(Object provider, T service, Supplier<T> supplier, ServicePriority priority) {
            this.provider = provider;
            this.service = service;
            this.supplier = supplier;
            this.priority = priority;
        }

        @SuppressWarnings("unchecked")
        T getService() {
            if (service == null && supplier != null) {
                synchronized (this) {
                    if (service == null) {
                        service = supplier.get();
                    }
                }
            }
            return service;
        }
    }
}
