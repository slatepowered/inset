package slatepowered.inset.cache;

import slatepowered.inset.datastore.DataItem;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

/**
 * A thread-safe double backed (by a list and map) implementation of
 * {@link DataCache}.
 *
 * @see DataCache
 */
final class DoubleBackedCache<K, T> implements DataCache<K, T> {

    final ConcurrentHashMap<K, DataItem<K, T>> map = new ConcurrentHashMap<>();
    final ConcurrentLinkedQueue<DataItem<K, T>> list = new ConcurrentLinkedQueue<>();

    @Override
    public DataItem<K, T> get(K key) {
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
    public Iterator<DataItem<K, T>> iterator() {
        return list.iterator();
    }

}
