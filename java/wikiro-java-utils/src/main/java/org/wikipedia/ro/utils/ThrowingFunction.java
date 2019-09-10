package org.wikipedia.ro.utils;

@FunctionalInterface
public interface ThrowingFunction<T, R, E extends Exception> {
    T apply(R r) throws E;
}
