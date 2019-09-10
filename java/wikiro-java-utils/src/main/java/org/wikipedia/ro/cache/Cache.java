package org.wikipedia.ro.cache;

import java.util.HashMap;
import java.util.Map;

import org.wikipedia.ro.utils.ThrowingFunction;

public class Cache<T, E extends Exception> {

    private Map<String, T> cache = new HashMap<>();
    
    private ThrowingFunction<T, String, E> expensiveFunction;
    
    public Cache(ThrowingFunction<T, String, E> func) {
        expensiveFunction = func;
    }
    
    public T get(String key) throws E {
        if (!cache.containsKey(key)) {
            T value = expensiveFunction.apply(key);
            cache.put(key, value);
        }
        return cache.get(key);
    }
}
