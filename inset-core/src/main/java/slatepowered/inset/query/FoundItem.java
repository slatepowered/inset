package slatepowered.inset.query;

import lombok.RequiredArgsConstructor;
import slatepowered.inset.codec.DecodeInput;
import slatepowered.inset.datastore.DataItem;
import slatepowered.inset.datastore.Datastore;
import slatepowered.inset.source.DataSourceBulkIterable;

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

    @SuppressWarnings("unchecked")
    protected <K2, T2> FoundItem<K2, T2> qualify(FindAllStatus<K2, T2> source) {
        this.source = source;
        return (FoundItem<K2, T2>) this;
    }

    protected Object cachedKey;

    // assert this item has been qualified
    private FindAllStatus<?, ?> assertQualified() {
        if (source == null)
            throw new IllegalStateException("Item has not been qualified yet");
        return source;
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
     * Get or read the primary key for this data item.
     *
     * @param fieldName The set name for the key field.
     * @param expectedType The type expected for the key.
     * @see DecodeInput#getOrReadKey(String, Type)
     * @return The primary key.
     */
    @SuppressWarnings("unchecked")
    public K getKey(String fieldName, Type expectedType) {
        if (cachedKey == null) {
            cachedKey = input().getOrReadKey(fieldName, expectedType);
        }

        return (K) cachedKey;
    }

    /**
     * Fetch a data item from the database if this result was partial,
     * otherwise ensure it is cached.
     *
     * @return The data item.
     */
    @SuppressWarnings("unchecked")
    public DataItem<K, T> fetch() {
        FindAllStatus<?, ?> status = assertQualified();
        Datastore<K, T> datastore = (Datastore<K, T>) status.getDatastore();

        DecodeInput input = input();

        // if complete there is no need to fetch the
        // full data item from the database
        if (!isPartial()) {
            return datastore.decodeFetched(input);
        }

        // fetch a new item from the database
        FindStatus<K, T> findStatus = datastore.findOne(getKey(datastore.getDataCodec().getPrimaryKeyFieldName(), datastore.getKeyClass()))
                .await();
        if (findStatus.failed()) {
            Object error = findStatus.error();
            Throwable cause = error instanceof Throwable ? findStatus.errorAs() : null;
            throw new RuntimeException("Error while fetching data item from bulk result" +
                    (cause == null ? ": " + error : ""), cause);
        }

        return findStatus.item();
    }

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
