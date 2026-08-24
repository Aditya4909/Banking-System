package com.javabank.repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Generic in-memory repository implementation.
 * Demonstrates Generics, Maps, and Functional interfaces.
 *
 * @param <T>  the entity type.
 * @param <ID> the identifier type of the entity.
 */
public class InMemoryRepository<T, ID> implements Repository<T, ID> {
    protected final Map<ID, T> storage = new ConcurrentHashMap<>();
    private final Function<T, ID> idExtractor;

    public InMemoryRepository(Function<T, ID> idExtractor) {
        if (idExtractor == null) {
            throw new IllegalArgumentException("ID Extractor function cannot be null.");
        }
        this.idExtractor = idExtractor;
    }

    @Override
    public synchronized void save(T entity) throws Exception {
        if (entity == null) {
            throw new IllegalArgumentException("Entity to save cannot be null.");
        }
        storage.put(idExtractor.apply(entity), entity);
    }

    @Override
    public Optional<T> findById(ID id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<T> findAll() {
        // Returns a shallow copy list of the entries to prevent direct cached reference corruption
        return new ArrayList<>(storage.values());
    }

    @Override
    public List<T> find(Predicate<T> filter) {
        return storage.values().stream()
                .filter(filter)
                .collect(Collectors.toList());
    }

    @Override
    public synchronized void deleteById(ID id) throws Exception {
        if (id == null) {
            throw new IllegalArgumentException("ID to delete cannot be null.");
        }
        storage.remove(id);
    }
}
