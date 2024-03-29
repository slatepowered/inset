package slatepowered.inset.cache;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import slatepowered.inset.datastore.DataItem;

import java.util.Iterator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Caffeine-cache based implementation of {@link DataCache}.
 *
 * @see DataCache
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class CaffeineCache<K, T> implements DataCache<K, T> {

    /**
     * Create a new Caffeine data cache instance essentially
     * wrapping the given Caffeine cache object.
     *
     * @param cache The cache to wrap.
     * @param <K> The key type.
     * @param <T> The value type.
     * @return The wrapper cache.
     */
    public static <K, T> CaffeineCache<K, T> of(Cache<K, DataItem<K, T>> cache) {
        return new CaffeineCache<>(cache);
    }

    protected final Cache<K, DataItem<K, T>> cache;

    @Override
    public DataItem<K, T> getOrNull(K key) {
        return cache.getIfPresent(key);
    }

    @Override
    public DataItem<K, T> getOrCompute(K key, Function<K, DataItem<K, T>> function) {
        return cache.get(key, function);
    }

    @Override
    public void remove(DataItem<K, T> item) {
        cache.invalidate(item.key());
    }

    @Override
    public void remove(K key) {
        cache.invalidate(key);
    }

    @Override
    public int size() {
        return cache.asMap().size();
    }

    @Override
    public Stream<DataItem<K, T>> stream() {
        return cache.asMap().values().stream();
    }

    @Override
    public void removeAll(Predicate<DataItem<K, T>> predicate) {
        throw new UnsupportedOperationException("todo");
    }

    @Override
    public void put(DataItem<K, T> item) {
        cache.put(item.key(), item);
    }

    @Override
    public Iterator<DataItem<K, T>> iterator() {
        return cache.asMap().values().iterator(); // this kinda sucks
    }

}
