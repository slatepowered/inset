package slatepowered.store.datastore;

import slatepowered.store.serializer.SerializationContext;
import slatepowered.store.serializer.SerializationOutput;
import slatepowered.store.source.DataTable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a reference to an item in a datastore.
 *
 * @param <K> The primary key type.
 * @param <T> The data type.
 */
public class DataItem<K, T> {

    public DataItem(Datastore<K, T> datastore, K key) {
        this.datastore = datastore;
        this.key = key;
        this.createdTime = System.currentTimeMillis();
        this.lastReferenceTime = 0;
    }

    /**
     * The datastore this item is a part of.
     */
    private final Datastore<K, T> datastore;

    /**
     * The primary key.
     */
    private final K key;

    /**
     * The loaded value, null if absent.
     */
    private T value;

    /**
     * The creation time as in {@link System#currentTimeMillis()}.
     */
    private final long createdTime;

    /**
     * The last time the cached item was loaded from the database,
     * as an offset onto the created time of this item.
     */
    protected int lastPullTime = -1;

    /**
     * The last time the cached item was referenced,
     * as an offset onto the created time of this item.
     */
    protected int lastReferenceTime;

    /**
     * Get the primary key for this item.
     *
     * @return The key.
     */
    public K key() {
        return key;
    }

    /**
     * Get whether this item has a value present.
     */
    public boolean present() {
        return value != null;
    }

    /**
     * Get the loaded value for this item.
     *
     * @return The value or null if absent/unloaded.
     */
    public T get() {
        return value;
    }

    /**
     * Get the loaded value for this item wrapped in an optional.
     *
     * @return The value or an empty optional if absent.
     */
    public Optional<T> optional() {
        return Optional.ofNullable(value);
    }

    /**
     * Get the datastore this item is contained by.
     *
     * @return The datastore.
     */
    public Datastore<K, T> datastore() {
        return datastore;
    }

    /**
     * Get the creation time as in {@link System#currentTimeMillis()}.
     */
    public long timeCreated() {
        return createdTime;
    }

    /**
     * The last time the cached item was loaded from the database.
     * Returns -1 if the item was never pulled/loaded from the database.
     */
    public long lastPullTime() {
        return lastPullTime == -1 ? -1 : createdTime + lastPullTime;
    }

    /**
     * The last time the cached item was referenced.
     */
    public long lastReferenceTime() {
        return createdTime + lastReferenceTime;
    }

    /**
     * Remove this item from the local datastore's cache.
     *
     * @return This.
     */
    public DataItem<K, T> dispose() {
        datastore.cachedMap.remove(key);
        return this;
    }

    /**
     * Synchronously serialize and update this item in the remote data storage.
     *
     * @return This.
     */
    public DataItem<K, T> pushSync() {
        if (value == null) {
            return this;
        }

        DataTable table = datastore.getTable();

        // serialize value
        SerializationOutput output = table.getSource().createDocumentSerializationOutput();
        output.setKey(key);
        SerializationContext context = new SerializationContext(datastore.getDataManager());
        datastore.getDataMarshaller().serialize(context, value, output);

        // perform update
        table.updateOneSync(output);
        return this;
    }

    /**
     * Asynchronously serialize and update this item in the remote data storage.
     *
     * @see #pushSync()
     * @return This.
     */
    public CompletableFuture<DataItem<K, T>> push() {
        return CompletableFuture.supplyAsync(this::pushSync);
    }

    // Update the lastReferenceTime to represent the
    // current instant
    protected DataItem<K, T> referencedNow() {
        long t = System.currentTimeMillis() - createdTime;
        if (t < 0) // overflow
            t = Integer.MAX_VALUE;
        lastReferenceTime = (int) t;
        return this;
    }

    // Update the lastPullTime to represent the
    // current instant
    protected DataItem<K, T> pulledNow() {
        long t = System.currentTimeMillis() - createdTime;
        if (t < 0) // overflow
            t = Integer.MAX_VALUE;
        lastPullTime = (int) t;
        return this;
    }

}
