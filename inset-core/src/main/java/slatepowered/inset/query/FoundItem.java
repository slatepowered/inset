package slatepowered.inset.query;

import lombok.Getter;
import slatepowered.inset.codec.CodecContext;
import slatepowered.inset.codec.DataCodec;
import slatepowered.inset.codec.DecodeInput;
import slatepowered.inset.datastore.DataItem;
import slatepowered.inset.datastore.Datastore;

import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;

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

    protected DecodeInput cachedInput;          // The cached input, used by this class to read partial data

    @SuppressWarnings("unchecked")
    protected <K2, T2> FoundItem<K2, T2> qualify(FindAllStatus<K2, T2> source) {
        this.source = source;
        return (FoundItem<K2, T2>) this;
    }

    // assert this item has been qualified
    @SuppressWarnings("unchecked")
    protected final FindAllStatus<K, T> assertQualified() {
        if (source == null)
            throw new IllegalStateException("Item has not been qualified yet");
        return (FindAllStatus<K, T>) source;
    }

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
     * Get and use the cached input or create a new input.
     *
     * @return The input.
     */
    public DecodeInput getOrCreateInput() {
        if (cachedInput == null) {
            cachedInput = input();
        }

        return cachedInput;
    }

    /**
     * Get or read the primary key for this data item.
     *
     * @param fieldName The set name for the key field.
     * @param expectedType The type expected for the key.
     * @see DecodeInput#getOrReadKey(String, Type)
     * @return The primary key.
     */
    public abstract K getKey(String fieldName, Type expectedType);

    /**
     * Get or read the given field for this data item.
     *
     * @param fieldName The field name.
     * @param expectedType The type expected for the value.
     * @see DecodeInput#read(CodecContext, String, Type)
     * @return The primary key.
     */
    public abstract <V> V getField(String fieldName, Type expectedType);

    /**
     * Project the potentially partial data onto a new instance of the given
     * data type.
     *
     * @param vClass The data class.
     * @param <V> The data type.
     * @return The data instance with the projected data.
     */
    public abstract <V> V project(Class<V> vClass);

    /**
     * Fetch a data item from the database if this result was partial,
     * otherwise ensure it is cached.
     *
     * @return The data item.
     */
    public abstract DataItem<K, T> fetch();

    /**
     * Asynchronously fetch a data item from the database if this result was partial,
     * otherwise ensure it is cached.
     *
     * @return The data item.
     */
    public CompletableFuture<DataItem<K, T>> fetchAsync() {
        return CompletableFuture.supplyAsync(this::fetch, assertQualified().getDatastore().getDataManager().getExecutorService());
    }

}
