package slatepowered.inset.datastore;

import slatepowered.inset.codec.*;
import slatepowered.inset.internal.ProjectionInterface;
import slatepowered.inset.operation.Sorting;
import slatepowered.inset.query.FindOperation;
import slatepowered.inset.query.FindResult;
import slatepowered.inset.query.Query;
import slatepowered.inset.source.DataSourceFindResult;
import slatepowered.inset.source.DataTable;

import java.lang.reflect.Type;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Represents a reference to an item in a datastore.
 *
 * @param <K> The primary key type.
 * @param <T> The data type.
 */
public class DataItem<K, T> extends PartialItem<K, T> {

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
    private volatile T value;

    /**
     * The creation time as in {@link System#currentTimeMillis()}.
     */
    private final long createdTime;

    /**
     * The last time the cached item was loaded from the database,
     * as an offset onto the created time of this item.
     */
    protected volatile int lastFetchTime = -1;

    /**
     * The last time the cached item was referenced,
     * as an offset onto the created time of this item.
     */
    protected volatile int lastReferenceTime;

    private double[] cachedOrderCoefficient; // The cached order coefficient array
    private int currentSortId; // The ID the cached order coefficient is for

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
    public boolean isPresent() {
        return value != null;
    }

    /**
     * If a value is present, run the given consumer.
     *
     * @param valueConsumer The consumer.
     * @return This.
     */
    public DataItem<K, T> ifPresent(Consumer<T> valueConsumer) {
        if (isPresent()) {
            valueConsumer.accept(get());
        }

        return this;
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
    public long lastFetchTime() {
        return lastFetchTime == -1 ? -1 : createdTime + lastFetchTime;
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
    @Override
    public DataItem<K, T> dispose() {
        datastore.getDataCache().remove(this);
        return this;
    }

    @Override
    public DataItem<K, T> delete() {
        super.delete();
        return this;
    }

    /**
     * Create a new default value for this item if the value is absent.
     *
     * @return This.
     */
    public DataItem<K, T> defaultIfAbsent() {
        if (!isPresent()) {
            value = datastore.getDataCodec().createDefault(this);
        }

        return this;
    }

    /**
     * Forces the current value to be disposed and a new default
     * value to be created regardless of whether a current value exists.
     *
     * @return This.
     */
    public DataItem<K, T> resetToDefaults() {
        value = datastore.getDataCodec().createDefault(this);
        return this;
    }

    /**
     * Synchronously serialize and update this item in the remote data storage.
     *
     * @return This.
     */
    public DataItem<K, T> saveSync() {
        if (value == null) {
            return this;
        }

        DataTable table = datastore.getSourceTable();

        // serialize value
        EncodeOutput output = table.getSource().createDocumentSerializationOutput();
        CodecContext context = new CodecContext(datastore.getDataManager());
        output.setSetKey(context, datastore.getDataCodec().getPrimaryKeyFieldName(), key);
        datastore.getDataCodec().encode(context, value, output);

        // perform update
        table.replaceOneSync(output);
        return this;
    }

    /**
     * Asynchronously serialize and update this item in the remote data storage.
     *
     * @see #saveSync()
     * @return This.
     */
    public CompletableFuture<DataItem<K, T>> saveAsync() {
        return CompletableFuture.supplyAsync(this::saveSync, datastore.getExecutorService());
    }

    /**
     * Decode the value for this item for the given nullable input.
     *
     * @param input The input.
     * @return This.
     */
    public DataItem<K, T> decode(DecodeInput input) {
        if (input == null) {
            return this;
        }

        DataCodec<K, T> myCodec = datastore.getDataCodec();
        CodecContext context = datastore.newCodecContext();
        T value = myCodec.construct(context, input);
        myCodec.decode(context, value, input);
        this.value = value;

        return this;
    }

    /**
     * Synchronously fetch and decode the value for this data item.
     *
     * @return This.
     */
    public DataItem<K, T> fetchSync() {
        DataSourceFindResult queryResult = datastore.getSourceTable()
                .findOneSync(Query.byKey(key));
        return decode(queryResult.input()).fetchedNow();
    }

    @Override
    protected Datastore<K, T> assertQualified() {
        return this.datastore;
    }

    @Override
    public boolean isPartial() {
        return false;
    }

    @Override
    public DecodeInput input() {
        throw new UnsupportedOperationException("TODO: Create decode input for DataItem"); // TODO
    }

    @Override
    public K getOrReadKey(String fieldName, Type expectedType) {
        return key;
    }

    @Override
    public K getKey() {
        return key;
    }

    @Override
    public <V> V getField(String fieldName, Type expectedType) {
        return value != null ? datastore.getDataCodec().getField(value, fieldName) : null;
    }

    @Override
    public FindOperation<K, T> find() {
        return new FindOperation<>(datastore, null).completeSuccessfully(FindResult.CACHED, this);
    }

    @Override
    public Optional<DataItem<K, T>> findCached() {
        return Optional.of(this);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <V> V projectInterface(ProjectionInterface projectionInterface) {
        if (projectionInterface.getKlass().isInstance(value)) {
            return (V) value;
        }

        final DataCodec<K, T> codec = datastore.getDataCodec();
        return (V) projectionInterface.createProxy(() -> key, (name, type) -> codec.getField(value, name));
    }

    /**
     * Asynchronously fetch and decode the value for this data item.
     *
     * @return The future.
     */
    public CompletableFuture<DataItem<K, T>> fetchAsync() {
        return datastore.getSourceTable()
                .findOneAsync(Query.byKey(key))
                .thenApply(result -> this.decode(result.input()).fetchedNow());
    }

    @Override
    public double[] createFastOrderCoefficients(String[] fields, Sorting sorting) {
        final int len = fields.length;
        final double[] arr = new double[len];
        final DataCodec<K, T> codec = datastore.getDataCodec();

        for (int i = 0; i < len; i++) {
            String field = fields[i];
            Object obj = codec.getField(value, field);

            if (obj instanceof Number)
                arr[i] = ((Number) obj).doubleValue();
        }

        return arr;
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
    protected DataItem<K, T> fetchedNow() {
        long t = System.currentTimeMillis() - createdTime;
        if (t < 0) // overflow
            t = Integer.MAX_VALUE;
        lastFetchTime = (int) t;
        return this;
    }

    @Override
    public String toString() {
        return "DataItem(" + key + " = " + value + ')';
    }

}
