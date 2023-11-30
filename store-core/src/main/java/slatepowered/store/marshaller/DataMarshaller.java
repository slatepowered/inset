package slatepowered.store.marshaller;

import slatepowered.store.datastore.DataItem;
import slatepowered.store.query.Query;
import slatepowered.store.serializer.DataSerializer;

import java.util.function.Predicate;

/**
 * An extension of the {@link DataSerializer} class which provides further insight
 * on the composition of data value type {@code T}, such as the primary key.
 *
 * @param <K> The key type.
 * @param <T> The data value type.
 */
public interface DataMarshaller<K, T> extends DataSerializer<T> {

    /**
     * Retrieve the primary key from the given value if present.
     *
     * @param value The value.
     * @return The key.
     */
    K getPrimaryKey(T value);

    /**
     * Create a new instance of {@code T} for the given data item.
     *
     * @param item The item, containing the key and stuff.
     * @return The instance.
     */
    T createDefault(DataItem<K, T> item);

    /**
     * Build a complex query comparator for the given query,
     * which should check if any item of type {@code T} matches
     * the given query.
     *
     * @param query The query.
     * @return The comparator.
     */
    Predicate<T> getQueryComparator(Query query);

}
