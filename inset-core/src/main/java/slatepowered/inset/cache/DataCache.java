package slatepowered.inset.cache;

import slatepowered.inset.datastore.DataItem;

import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Provides caching capabilities for data items in datastores with
 * key type {@code K} and value type {@code T}.
 */
public interface DataCache<K, T> extends Iterable<DataItem<K, T>> {

    /**
     * Get a cached item by the given key or return null if absent.
     *
     * @param key The key.
     * @return The item.
     */
    DataItem<K, T> getOrNull(K key);

    /**
     * Get a cached item by the given key or create a new cached instance
     * using the provided factory function.
     *
     * @param key The key.
     * @param function The factory function.
     * @return The cached data item.
     */
    DataItem<K, T> getOrCompute(K key, Function<K, DataItem<K, T>> function);

    /**
     * Remove the given item from this cache.
     *
     * @param item The item.
     */
    void remove(DataItem<K, T> item);

    /**
     * Remove the given key from this cache.
     *
     * @param key The key.
     */
    void remove(K key);

    /**
     * Get the count of items in this cache.
     *
     * @return The count of cached items.
     */
    int size();

    /**
     * Stream the values in this cache.
     *
     * @return The values.
     */
    Stream<DataItem<K, T>> stream();

    /**
     * A simple, permanent data cache backed by a {@link ConcurrentHashMap} for
     * fast lookup and a {@link Vector} for fast iteration. With no special mechanisms,
     * all values cached will remain until manually cleared.
     *
     * @param <K> The key type.
     * @param <T> The value type.
     * @return The cache.
     */
    static <K, T> DataCache<K, T> doubleBackedConcurrent() {
        return new DoubleBackedCache<>(new ConcurrentHashMap<>(), new ConcurrentLinkedQueue<>());
    }

    static <K, T> DataCache<K, T> doubleBacked(Map<K, DataItem<K, T>> map, List<DataItem<K, T>> list) {
        return new DoubleBackedCache<>(map, list);
    }

}
