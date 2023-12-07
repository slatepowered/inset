package slatepowered.inset.query;

import lombok.RequiredArgsConstructor;
import slatepowered.inset.codec.DecodeInput;
import slatepowered.inset.source.DataSourceBulkIterable;

import java.lang.reflect.Type;

/**
 * Represents a found item in a {@link FindAllStatus}.
 *
 * @param <K> The key type.
 * @param <T> The value type.
 */
public abstract class FoundItem<K, T> {

    /**
     * The operation status this found item is a part of.
     *
     * This is only available after qualified.
     */
    protected FindAllStatus<?, ?> source;

    @SuppressWarnings("unchecked")
    protected <K2, T2> FoundItem<K2, T2> qualify(FindAllStatus<K2, T2> source) {
        this.source = source;
        return (FoundItem<K2, T2>) this;
    }

    protected Object cachedKey;

    /**
     * Whether the data of this item was projected and should be
     * fetched further if needed or is complete and ready to be
     * decoded into a valid object.
     *
     * @return Whether the data is incomplete/partial.
     */
    public abstract boolean isPartial();

    /**
     * Get the data input to decode the retrieved data of this item.
     *
     * @return The input.
     */
    public abstract DecodeInput input();

    /**
     * Get or read the primary key for this data item.
     *
     * @param fieldName The set name for the key field.
     * @param expectedType The type expected for the key.
     * @see DecodeInput#getOrReadKey(String, Type)
     * @return The primary key.
     */
    public Object getKey(String fieldName, Type expectedType) {
        if (cachedKey == null) {
            cachedKey = input().getOrReadKey(fieldName, expectedType);
        }

        return cachedKey;
    }

}
