package slatepowered.inset.query;

import slatepowered.inset.datastore.DataItem;
import slatepowered.inset.datastore.Datastore;
import slatepowered.inset.datastore.OperationStatus;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Represents an awaitable find-one query status/result.
 *
 * @param <K> The primary key type.
 * @param <T> The data type.
 */
public class FindStatus<K, T> extends OperationStatus<K, T, FindStatus<K, T>> {

    /* Result Fields (set when the query completed) */
    private volatile FindResult result;   // The type of result
    private volatile DataItem<K, T> item; // The data item if successful

    public FindStatus(Datastore<K, T> datastore, Query query) {
        super(datastore, query);
    }

    /**
     * When this query is successfully completed, call the given consumer.
     *
     * @param consumer The consumer.
     * @return This.
     */
    public FindStatus<K, T> then(Consumer<FindStatus<K, T>> consumer) {
        future.whenComplete((status, throwable) -> {
            if (status != null && status.result().isSuccessful()) {
                try {
                    consumer.accept(status);
                } catch (Throwable t) {
                    System.err.println("Error while processing query result");
                    t.printStackTrace();
                }
            }
        });

        return this;
    }

    /**
     * On successful completion, if the value is absent in the data source
     * but we do know the key from the query, create a new reference item and
     * fill it with the default value.
     *
     * @return This.
     */
    public FindStatus<K, T> thenDefaultIfAbsent() {
        if (!query.hasKey())
            return this;

        future = future.thenApply(status -> {
            if (status.isAbsent()) {
                this.item = datastore.getOrCreate(key());
            }

            return this;
        });

        return this;
    }

    /**
     * Fetch the data from the data source on successful completion if that
     * wasn't already done as part of the original query.
     *
     * @return This.
     */
    public FindStatus<K, T> thenFetchIfCached() {
        future = future.thenApplyAsync(status -> {
            if (status.wasCached()) {
                status.item().fetchSync();
            }

            return status;
        });

        return this;
    }

    /**
     * If a value is present, run the given consumer.
     *
     * @param consumer The consumer.
     * @return This.
     */
    public FindStatus<K, T> ifPresent(Consumer<FindStatus<K, T>> consumer) {
        if (isPresent()) {
            consumer.accept(this);
        }

        return this;
    }

    /**
     * If a value is absent, run the given consumer.
     *
     * @param consumer The consumer.
     * @return This.
     */
    public FindStatus<K, T> ifAbsent(Consumer<FindStatus<K, T>> consumer) {
        if (isAbsent()) {
            consumer.accept(this);
        }

        return this;
    }

    /**
     * If a value is present, run the given consumer.
     *
     * @param consumer The consumer.
     * @return This.
     */
    public FindStatus<K, T> ifPresentUse(Consumer<DataItem<K, T>> consumer) {
        if (isPresent()) {
            consumer.accept(this.item);
        }

        return this;
    }

    /**
     * When the query is successfully completed, call the given consumer
     * with the always non-null {@link DataItem} instance.
     *
     * @param consumer The consumer.
     * @return This.
     */
    public FindStatus<K, T> thenUse(Consumer<DataItem<K, T>> consumer) {
        return then(status -> consumer.accept(status.item()));
    }

    /**
     * Add a new asynchronous action to be executed when the previous
     * stage of this query finishes successfully.
     *
     * @param action The action.
     * @return This.
     */
    public FindStatus<K, T> thenApplyAsync(Consumer<FindStatus<K, T>> action) {
        future = future.thenApplyAsync(status -> {
            action.accept(status);
            return status;
        }, datastore.getDataManager().getExecutorService());
        return this;
    }

    /**
     * Add a new synchronous action to be executed when the previous
     * stage of this query finishes successfully.
     *
     * @param action The action.
     * @return This.
     */
    public FindStatus<K, T> thenApply(Consumer<FindStatus<K, T>> action) {
        future = future.thenApply(status -> {
            action.accept(status);
            return status;
        });

        return this;
    }

    /**
     * Add a new synchronous action to be executed when the previous
     * stage of this query finishes with a value.
     *
     * @param action The action.
     * @return This.
     */
    public FindStatus<K, T> thenApplyIfPresent(Consumer<FindStatus<K, T>> action) {
        future = future.thenApply(status -> {
            if (status.isPresent()) {
                action.accept(status);
            }

            return status;
        });

        return this;
    }

    /**
     * Add a new synchronous action to be executed when the previous
     * stage of this query finishes without a value.
     *
     * @param action The action.
     * @return This.
     */
    public FindStatus<K, T> thenApplyIfAbsent(Consumer<FindStatus<K, T>> action) {
        future = future.thenApply(status -> {
            if (status.isAbsent()) {
                action.accept(status);
            }

            return status;
        });

        return this;
    }

    /**
     * Get the result type.
     */
    public FindResult result() {
        return result;
    }

    /**
     * Get the result data item.
     *
     * If the data was absent, a cache item is still created for future
     * reference so this will only be null if the query failed.
     */
    public DataItem<K, T> item() {
        return item;
    }

    /**
     * Get an optional for the result data item, which will
     * be present if the query succeeded and found an item.
     *
     * @return The optional present if an item is present.
     */
    public Optional<DataItem<K, T>> optional() {
        return Optional.ofNullable(item);
    }

    /**
     * Get the resolved item or create a new reference
     * item.
     *
     * @return The item.
     */
    public DataItem<K, T> orReference() {
        K key = key();
        if (key == null)
            throw new IllegalStateException("No key was resolved or set in the query");
        return item != null ? item : (item = datastore.getOrReference(key()));
    }

    /**
     * Get the resolved item or create a new one with
     * the default value.
     *
     * @return The item.
     */
    public DataItem<K, T> orCreate() {
        K key = key();
        if (key == null)
            throw new IllegalStateException("No key was resolved or set in the query");
        return item != null ? item : (item = datastore.getOrCreate(key()));
    }

    /**
     * Get the resolved item if present or return the given default.
     *
     * @param def The default option.
     * @return The resolved item is present or the given default.
     */
    public DataItem<K, T> orElse(DataItem<K, T> def) {
        return item != null ? item : (item = def);
    }

    /**
     * Get the resolved item if present or compute and return
     * a default value using the given default supplier.
     *
     * @param supplier The default supplier.
     * @return The resolved item is present or the default value.
     */
    public DataItem<K, T> orElseGet(Supplier<DataItem<K, T>> supplier) {
        return item != null ? item : (item = supplier.get());
    }

    /**
     * Get the resolved item if present or compute and return
     * a default value using the given default action.
     *
     * @param action The default action.
     * @return The resolved item is present or the default value.
     */
    public DataItem<K, T> orElseCompute(Function<FindStatus<K, T>, DataItem<K, T>> action) {
        return item != null ? item : (item = action.apply(this));
    }

    /**
     * Get the key of the query/resolved item, this may
     * be null if the query does not have a key set and is absent,
     * failed or is not completed yet.
     */
    @SuppressWarnings("unchecked")
    public K key() {
        if (item != null) return item.key();
        if (query.hasKey()) return (K) query.getKey();
        return null;
    }

    /**
     * Load the data from the database anyways if the
     * item was found in the cache.
     *
     * @return This.
     */
    public FindStatus<K, T> fetchSyncIfCached() {
        if (result == FindResult.CACHED) {
            item.fetchSync();
        }

        return this;
    }

    /**
     * Complete this query with the given parameters.
     *
     * @param result The result type.
     * @param item The item, should be null if failed.
     * @param error The error, should be null if successful.
     * @return This.
     */
    protected synchronized FindStatus<K, T> completeInternal(FindResult result, DataItem<K, T> item, Object error) {
        this.completed = true;
        this.result = result;
        this.item = item;
        this.error = error;
        completeInternal(this);
        return this;
    }

    public synchronized FindStatus<K, T> completeSuccessfully(FindResult result, DataItem<K, T> item) {
        return completeInternal(result, item, null);
    }

    public synchronized FindStatus<K, T> completeFailed(Object error) {
        return completeInternal(FindResult.FAILED, null, error);
    }

    public boolean success() {
        return result != null && result.isSuccessful();
    }

    public boolean failed() {
        return result != null && !result.isSuccessful();
    }

    public boolean isPresent() {
        return result != null && result.isValue();
    }

    public boolean isAbsent() {
        return result != null && !result.isValue();
    }

    public boolean wasCached() {
        return result != null && result == FindResult.CACHED;
    }

    public boolean wasFetched() {
        return result != null && result == FindResult.FETCHED;
    }

    @Override
    protected String describeOperation() {
        return "executing find one query";
    }

}
