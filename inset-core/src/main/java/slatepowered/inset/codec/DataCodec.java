package slatepowered.inset.codec;

import slatepowered.inset.datastore.DataItem;
import slatepowered.inset.modifier.Projection;
import slatepowered.inset.query.Query;

import java.util.function.Predicate;

/**
 * An extension of the {@link ValueCodec} class which provides further insight
 * on the composition of data value type {@code T}, such as the primary key.
 *
 * @param <K> The key type.
 * @param <T> The data value type.
 */
public interface DataCodec<K, T> extends ValueCodec<T> {

    /**
     * Retrieve the primary key from the given value if present.
     *
     * @param value The value.
     * @return The key.
     */
    K getPrimaryKey(T value);

    /**
     * Get the name of the primary key field.
     *
     * @return The field name.
     */
    String getPrimaryKeyFieldName();

    /**
     * Create a new instance of {@code T} for the given data item.
     *
     * @param item The item, containing the key and stuff.
     * @return The instance.
     */
    T createDefault(DataItem<K, T> item);

    /**
     * Build a complex filter predicate for the given query,
     * which should check if any item of type {@code T} matches
     * the given query.
     *
     * @param query The query.
     * @return The predicate.
     */
    Predicate<T> getFilterPredicate(Query query);

    /**
     * Create a new projection which only includes the fields
     * applicable to this data.
     *
     * @param primaryKeyName The primary key field name override.
     * @return The projection.
     */
    Projection createExclusiveProjection(String primaryKeyName);

}
