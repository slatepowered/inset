package slatepowered.inset.datastore;

import lombok.Builder;
import lombok.Getter;
import slatepowered.inset.DataManager;
import slatepowered.inset.cache.DataCache;
import slatepowered.inset.codec.*;
import slatepowered.inset.codec.DecodeInput;
import slatepowered.inset.operation.DeleteAllOperation;
import slatepowered.inset.query.FindAllOperation;
import slatepowered.inset.query.Query;
import slatepowered.inset.query.FindResult;
import slatepowered.inset.query.FindOperation;
import slatepowered.inset.source.DataTable;
import slatepowered.inset.util.DebugLogging;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Represents a datastore of values of type {@code T} with primary key type {@code K}.
 *
 * @param <K> The primary key type.
 * @param <T> The data type.
 */
@Builder
public class Datastore<K, T> implements DebugLogging {

    public Datastore(DataCache<K, T> dataCache,
                     DataManager dataManager,
                     Class<K> keyClass,
                     DataTable sourceTable,
                     DataCodec<K, T> dataCodec) {
        this.dataCache = dataCache;
        this.dataManager = dataManager;
        this.keyClass = keyClass;
        this.sourceTable = sourceTable;
        this.dataCodec = dataCodec;
    }

    /** The data caching provider. */
    @Getter
    protected final DataCache<K, T> dataCache;

    @Getter
    protected final DataManager dataManager;

    /**
     * The primary key type.
     */
    @Getter
    protected final Class<K> keyClass;

    /** The database table to load/save data from/to. */
    @Getter
    protected final DataTable sourceTable;

    /** The marshaller for the local data objects. */
    @Getter
    protected final DataCodec<K, T> dataCodec;

    /**
     * Get the codec registry to be used by this datastore
     * and it's operations.
     *
     * @return The codec registry.
     */
    public CodecRegistry getCodecRegistry() {
        return dataManager.getCodecRegistry();
    }

    /**
     * Get the executor service to be used by this datastore
     * and it's operations.
     *
     * @return The executor service.
     */
    public ExecutorService getExecutorService() {
        return dataManager.getExecutorService();
    }

    /**
     * Create a new {@link CodecContext} to be utilized
     * by this datastore and it's operations to decode and encode
     * data.
     *
     * @return The codec context.
     */
    public CodecContext newCodecContext() {
        return new CodecContext(dataManager);
    }

    /**
     * Get or create a reference data item for the given key.
     *
     * This data item may be loaded if the object by the given key
     * was cached, it may also be a previously created reference instance
     * or a new instance is created.
     *
     * @param key The key.
     * @return The never-null data item.
     */
    public DataItem<K, T> getOrReference(K key) {
        return dataCache.getOrCompute(key, k -> new DataItem<>(this, k));
    }

    /**
     * Get or create a data item with a value present for the given key.
     *
     * @see #getOrReference(Object)
     * @see DataItem#defaultIfAbsent()
     * @param key The key.
     * @return The never-null data item.
     */
    public DataItem<K, T> getOrCreate(K key) {
        return getOrReference(key).defaultIfAbsent();
    }

    /**
     * Get a cached data item by the given key, returning an
     * empty optional if absent.
     *
     * @param key The key.
     * @return The optional with the value if present.
     */
    public Optional<DataItem<K, T>> getOptional(K key) {
        return Optional.ofNullable(dataCache.getOrNull(key));
    }

    /**
     * Get an existent (potentially empty) data item for the given key or
     * return null if absent.
     *
     * @param key The key.
     * @return The data item or null if absent.
     */
    public DataItem<K, T> getOrNull(K key) {
        return dataCache.getOrNull(key);
    }

    /**
     * Find a cached item by the given query. This only includes loaded
     * items. If an item is not loaded it may be ignored by this method
     * and it may return null.
     *
     * The item will also not be fetched from the database after being returned
     * from this method.
     *
     * This action is always performed synchronously.
     *
     * @param query The query.
     * @return The item or null if no loaded item is present.
     */
    @SuppressWarnings("unchecked")
    public DataItem<K, T> findOneCached(Query query) {
        if (query.hasKey()) {
            DataItem<K, T> item = dataCache.getOrNull((K) query.getKey());
            if (item != null && item.isPresent()) {
                item.referencedNow();
                return item;
            }

            return null;
        }

        // iterate over each item and compare the value
        // with the given query
        Predicate<T> predicate = dataCodec.getFilterPredicate(query);
        for (DataItem<K, T> item : dataCache) {
            if (!item.isPresent()) {
                continue;
            }

            if (predicate.test(item.get())) {
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
    @SuppressWarnings("unchecked")
    public FindOperation<K, T> findOne(Query query) {
        if (DEBUG_LOGGING_LEVEL >= TRACE) log("FindOne(" + query + ")");
        DataItem<K, T> cachedItem = findOneCached(query);
        if (cachedItem != null) {
            if (DEBUG_LOGGING_LEVEL >= TRACE) log("  Found cached item: " + cachedItem);
            return new FindOperation<>(this, query).completeSuccessfully(FindResult.CACHED, cachedItem);
        }

        query = query.qualify(this);

        // asynchronously try to load the item from the datatable
        FindOperation<K, T> queryStatus = new FindOperation<>(this, query);
        if (DEBUG_LOGGING_LEVEL >= TRACE) log("  Created FindOperation, executing source table query");
        Query finalQuery = query;
        getSourceTable().findOneAsync(query)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        if (DEBUG_LOGGING_LEVEL >= TRACE) log("  Query failed with error " + throwable);
                        queryStatus.completeFailed(throwable);
                        return;
                    }

                    try {
                        // check if an item was found
                        if (!result.found()) {
                            if (DEBUG_LOGGING_LEVEL >= TRACE) log("  Query completed, result absent");
                            queryStatus.completeSuccessfully(FindResult.ABSENT, null);
                            return;
                        }

                        if (DEBUG_LOGGING_LEVEL >= TRACE) log("  Decoding DataItem from input " + result.input());
                        DataItem<K, T> item = decodeFetched(result.input());
                        if (DEBUG_LOGGING_LEVEL >= TRACE) log("  Query completed successfully with " + item);
                        queryStatus.completeSuccessfully(FindResult.FETCHED, item);
                    } catch (Throwable t) {
                        if (DEBUG_LOGGING_LEVEL >= TRACE) { log("  Uncaught error while decoding fetch result: " + t); t.printStackTrace(); }
                        queryStatus.completeFailed(new CodecException("Uncaught error while decoding fetch result of query `" + finalQuery + "` on datastore " + this, t));
                    }
                });

        return queryStatus;
    }

    /**
     * Try to find an item by the given key.
     *
     * @see #findOne(Query)
     * @see Query#byKey(Object)
     * @param key The key.
     * @return This.
     */
    public FindOperation<K, T> findOne(K key) {
        return findOne(Query.byKey(key));
    }

    /**
     * Try to find an item by the given key.
     *
     * @see #findOne(Query)
     * @see Query#byKey(Object)
     * @param key The key.
     * @return This.
     */
    public FindOperation<K, T> findOne(K key, ExecutorService executor) {
        return findOne(Query.byKey(key).withExecutor(executor));
    }

    /**
     * Find all cached items matching the given query in the datastore.
     *
     * This action is always performed synchronously.
     *
     * @param query The filter query.
     * @return The list of cached items matching the given filter.
     */
    public List<DataItem<K, T>> findAllCached(Query query) {
        // pre-allocate a list with an estimated size
        int fieldConstraintCount = query.getFieldConstraints().size();
        List<DataItem<K, T>> list = new ArrayList<>(dataCache.size() / (fieldConstraintCount + 1));

        // iterate over each item and compare the value
        // with the given query
        Predicate<T> predicate = fieldConstraintCount > 0 ? dataCodec.getFilterPredicate(query) : __ -> true;
        for (DataItem<K, T> item : dataCache) {
            if (!item.isPresent()) {
                continue;
            }

            if (predicate.test(item.get())) {
                item.referencedNow();
                list.add(item);
            }
        }

        return list;
    }

    static final FindAllOperation.Options DEFAULT_FIND_ALL_OPTIONS = FindAllOperation.Options.builder()
            .useCaches(false)
            .build();

    /**
     * Find all items matching the given query in the database.
     *
     * Note that the aggregation/find operation is never cached and
     * always references the database, the individual items may be resolved
     * from the cache or cached though.
     *
     * @param query The filter query.
     * @return The status of the operation.
     */
    public FindAllOperation<K, T> findAll(Query query) {
        return findAll(query, DEFAULT_FIND_ALL_OPTIONS);
    }

    /**
     * Find all items matching the given query in the database.
     *
     * Note that the aggregation/find operation is never cached if {@code useCaches} is
     * disabled and always references the database, the individual items may be
     * resolved from the cache or cached though.
     *
     * @param query The filter query.
     * @param options The options for this operation.
     * @return The status of the operation.
     */
    public FindAllOperation<K, T> findAll(Query query, FindAllOperation.Options options) {
        query = query.qualify(this);
        FindAllOperation<K, T> status = new FindAllOperation<>(this, query, options);

        // filter cached item stream
        if (options.isUseCaches()) {
            final Predicate<T> filterPredicate = dataCodec.getFilterPredicate(query);
            Stream<DataItem<K, T>> cachedStream = dataCache.stream().filter(dataItem -> dataItem.isPresent() && filterPredicate.test(dataItem.get()));
            status.withCached(cachedStream);
        }

        getSourceTable().findAllAsync(query)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        status.completeFailed(throwable);
                        return;
                    }

                    // 'complete' the operation with the bulk iterable
                    status.completeSuccessfully(result);
                });

        return status;
    }

    /**
     * Find all items matching the given query and drop them from the cache,
     * then instruct the database or other remote data storage to delete
     * all items matching the given query.
     *
     * @param query The query.
     * @return The operation status.
     */
    public DeleteAllOperation<K, T> deleteAll(Query query) {
        DeleteAllOperation<K, T> operation = new DeleteAllOperation<>(this, query);
        query = query.qualify(this);

        // enqueue the deletion in the database
        sourceTable.deleteAllAsync(query).whenComplete((count, throwable) -> {
            operation.completeDataTableOperation(throwable, count);
        });

        // drop cached items
        final Predicate<T> filterPredicate = dataCodec.getFilterPredicate(query);
        dataCache.removeIf(filterPredicate);
        operation.completeCacheClear();

        return operation;
    }

    /**
     * Get the key from the given input, reference the data item,
     * decode the input into the referenced data item and finally
     * register it was fetched now.
     *
     * @param input The input data.
     * @return The item.
     */
    @SuppressWarnings("unchecked")
    public DataItem<K, T> decodeFetched(DecodeInput input) {
        K key = (K) input.getOrReadKey(null, getKeyClass());
        if (key == null) {
            throw new IllegalArgumentException("Query result does not a valid contain primary key");
        }

        DataItem<K, T> item = getOrReference(key);
        item.decode(input);
        item.fetchedNow();

        return item;
    }

    @Override
    public String toString() {
        return "Datastore(source: " + sourceTable + ", codec: " + dataCodec + ")";
    }

    /**
     * Lombok generated, have to declare this otherwise the
     * Javadoc task cries.
     *
     * Builds instances of {@link Datastore}. It is recommended not to
     * directly instantiate this class and instead use {@link DataManager#datastore(Class, Class)}
     * to create a builder.
     *
     * @param <K> The key type.
     * @param <T> The value type.
     */
    public static class DatastoreBuilder<K, T> { /* will be generated by lombok */ }

}
