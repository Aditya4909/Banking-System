package com.javabank.repository;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Generic repository interface defining standard CRUD operations.
 * Demonstrates Generics and functional programming integrations.
 */
public interface Repository<T, ID> {
    void save(T entity) throws Exception;
    Optional<T> findById(ID id) throws Exception;
    List<T> findAll() throws Exception;
    List<T> find(Predicate<T> filter) throws Exception;
    void deleteById(ID id) throws Exception;
}
