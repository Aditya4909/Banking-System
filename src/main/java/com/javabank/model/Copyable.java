package com.javabank.model;

/**
 * Generic contract for implementing cloning or duplication logic.
 * Serves as a type-safe cloning abstraction.
 *
 * @param <T> the type of object being duplicated.
 */
public interface Copyable<T> {
    /**
     * Performs a duplication of the target object.
     * Depending on requirements, this can represent a shallow or deep copy.
     */
    T copy();
}
