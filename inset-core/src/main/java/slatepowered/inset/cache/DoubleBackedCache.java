package slatepowered.inset.cache;

import lombok.RequiredArgsConstructor;
import slatepowered.inset.datastore.DataItem;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * A thread-safe double backed (by a list and map) implementation of
 * {@link DataCache}.
 *
 * @see DataCache
 */
@RequiredArgsConstructor
final class DoubleBackedCache<K, T> implements DataCache<K, T> {

    final Map<K, DataItem<K, T>> map;
    final Collection<DataItem<K, T>> list;

    @Override
    public DataItem<K, T> getOrNull(K key) {
        return map.get(key);
    }

    @Override
    public DataItem<K, T> getOrCompute(K key, Function<K, DataItem<K, T>> function) {
        return map.computeIfAbsent(key, k -> {
            DataItem<K, T> val = function.apply(k);
            list.add(val);
            return val;
        });
    }

    @Override
    public void remove(DataItem<K, T> item) {
        map.remove(item.key());
        list.remove(item);
    }

    @Override
    public void remove(K key) {
        map.remove(key);
        list.removeIf(item -> item.key().equals(key));
    }

    @Override
    public Iterator<DataItem<K, T>> iterator() {
        return list.iterator();
    }

    @Override
    public Stream<DataItem<K, T>> stream() {
        return list.stream();
    }

    @Override
    public int size() {
        int r;
        if (map.size() != (r = list.size()))
            throw new IllegalStateException("Cache count inconsistency across map and list, map.size() = " + map.size() + ", list.size() " + list.size());
        return r;
    }

    @Override
    public void removeAll(Predicate<DataItem<K, T>> predicate) {
        Iterator<DataItem<K, T>> iterator = list.iterator();
        DataItem<K, T> item;
        while (iterator.hasNext()) {
            item = iterator.next();

            if (predicate.test(item)) {
                iterator.remove();
                map.remove(item.key());
            }
        }
    }

}
