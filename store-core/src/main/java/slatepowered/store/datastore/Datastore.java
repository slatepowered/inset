package slatepowered.store.datastore;

import lombok.Builder;
import lombok.Getter;
import slatepowered.store.DataManager;
import slatepowered.store.marshaller.DataMarshaller;
import slatepowered.store.query.Query;
import slatepowered.store.query.QueryResult;
import slatepowered.store.query.QueryStatus;
import slatepowered.store.source.DataTable;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Represents a datastore of values of type {@code T} with primary key type {@code K}.
 *
 * @param <K> The primary key type.
 * @param <T> The data type.
 */
@Builder
public class Datastore<K, T> {

    /** All cached data items in a list for fast iteration. */
    protected final List<DataItem<K, T>> cachedList = new LinkedList<>();

    /** All cached data items by key. */
    protected final Map<K, DataItem<K, T>> cachedMap = new ConcurrentHashMap<>();

    @Getter
    protected final DataManager dataManager;

    /** The database table to load/save data from/to. */
    @Getter
    protected final DataTable table;

    /** The marshaller for the local data objects. */
    @Getter
    protected final DataMarshaller<K, T> dataMarshaller;

    /**
     * Get or create a reference data item for the given key.
     *
     * This data item may be loaded if the object by the given key
     * was cached, it may also be a previously created reference instance
     * or a new instance is created.
     *
     * @param key The key.
     * @return The item.
     */
    public DataItem<K, T> reference(K key) {
        return cachedMap.computeIfAbsent(key, k -> new DataItem<>(this, k));
    }

    /**
     * Find a cached item by the given query. This only includes loaded
     * items. If an item is not loaded it may be ignored by this method
     * and it may return null.
     *
     * The item will also not be loaded from the database after being returned
     * from this method.
     *
     * @param query The query.
     * @return The item or null if no loaded item is present.
     */
    @SuppressWarnings("unchecked")
    public DataItem<K, T> findCached(Query query) {
        if (query.hasKey()) {
            DataItem<K, T> item = cachedMap.get((K) query.getKey());
            if (item != null && item.present()) {
                item.referencedNow();
                return item;
            }

            return null;
        }

        // iterate over each item and compare the value
        // with the given query
        Predicate<T> comparator = dataMarshaller.getQueryComparator(query);
        for (DataItem<K, T> item : cachedList) {
            if (!item.present()) {
                continue;
            }

            if (comparator.test(item.get())) {
                item.referencedNow();
                return item;
            }
        }

        return null;
    }

    /**
     * Potentially asynchronously find a database item for the given
     * query. This will first look for a present cached item matching the query
     * and will otherwise attempt to load it from the database.
     *
     * @param query The query.
     * @return The query status object.
     */
    public QueryStatus<K, T> find(Query query) {
        DataItem<K, T> cachedItem = findCached(query);
        if (cachedItem != null) {
            return new QueryStatus<K, T>().completeSuccessfully(QueryResult.FOUND, cachedItem);
        }

        // asynchronously try to load the item
        // from the database
        return null; // todo
    }

}
