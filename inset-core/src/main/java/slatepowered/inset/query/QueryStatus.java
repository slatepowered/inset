package slatepowered.inset.query;

import slatepowered.inset.datastore.DataItem;
import slatepowered.inset.datastore.Datastore;
import slatepowered.inset.datastore.OperationStatus;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Represents an awaitable query status/result.
 *
 * @param <K> The primary key type.
 * @param <T> The data type.
 */
public class QueryStatus<K, T> extends OperationStatus<K, T, QueryStatus<K, T>> {

    /* Result Fields (set when the query completed) */
    private volatile QueryResult result;  // The type of result
    private volatile DataItem<K, T> item; // The data item if successful
    private volatile Object error;        // The error object if it failed

    public QueryStatus(Datastore<K, T> datastore, Query query) {
        super(datastore, query);
    }

    /**
     * When this query is successfully completed, call the given consumer.
     *
     * @param consumer The consumer.
     * @return This.
     */
    public QueryStatus<K, T> then(Consumer<QueryStatus<K, T>> consumer) {
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
    public QueryStatus<K, T> thenDefaultIfAbsent() {
        if (!query.hasKey())
            return this;

        future = future.thenApply(status -> {
            if (status.absent()) {
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
    public QueryStatus<K, T> thenFetchIfCached() {
        future = future.thenApplyAsync(status -> {
            if (status.cached()) {
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
    public QueryStatus<K, T> ifPresent(Consumer<QueryStatus<K, T>> consumer) {
        if (isPresent()) {
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
    public QueryStatus<K, T> ifPresentUse(Consumer<DataItem<K, T>> consumer) {
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
    public QueryStatus<K, T> thenUse(Consumer<DataItem<K, T>> consumer) {
        return then(status -> consumer.accept(status.item()));
    }

    /**
     * Add a new asynchronous action to be executed when the previous
     * stage of this query finishes successfully.
     *
     * @param action The action.
     * @return This.
     */
    public QueryStatus<K, T> thenApplyAsync(Consumer<QueryStatus<K, T>> action) {
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
    public QueryStatus<K, T> thenApply(Consumer<QueryStatus<K, T>> action) {
        future = future.thenApply(status -> {
            action.accept(status);
            return status;
        });

        return this;
    }

    /**
     * When the query fails, call the given consumer.
     *
     * @param consumer The consumer.
     * @return This.
     */
    public QueryStatus<K, T> exceptionally(Consumer<QueryStatus<K, T>> consumer) {
        future.whenComplete((status, throwable) -> {
            if (status != null && !status.result().isSuccessful()) {
                consumer.accept(status);
            }
        });

        return this;
    }

    /**
     * Get the result type.
     */
    public QueryResult result() {
        return result;
    }

    /**
     * Get the error if the query failed.
     * This could be a {@link Throwable}, a string, an integer error code, etc.
     */
    public Object error() {
        return error;
    }

    /**
     * Get the error as the inferred type if the query failed.
     * This could be a {@link Throwable}, a string, an integer error code, etc.
     */
    @SuppressWarnings("unchecked")
    public <E> E errorAs() {
        return (E) error;
    }

    /**
     * Get the error as the given type if the query failed.
     * This could be a {@link Throwable}, a string, an integer error code, etc.
     */
    @SuppressWarnings("unchecked")
    public <E> E errorAs(Class<E> eClass) {
        return (E) error;
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
     * Get the key of the resolved item, this may
     * be null if the query failed or is not completed yet.
     */
    public K key() {
        return item != null ? item.key() : null;
    }

    /**
     * Load the data from the database anyways if the
     * item was found in the cache.
     *
     * @return This.
     */
    public QueryStatus<K, T> fetchSyncIfCached() {
        if (result == QueryResult.CACHED) {
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
    protected synchronized QueryStatus<K, T> completeInternal(QueryResult result, DataItem<K, T> item, Object error) {
        this.completed = true;
        this.result = result;
        this.item = item;
        this.error = error;
        completeInternal(this);
        return this;
    }

    public synchronized QueryStatus<K, T> completeSuccessfully(QueryResult result, DataItem<K, T> item) {
        return completeInternal(result, item, null);
    }

    public synchronized QueryStatus<K, T> completeFailed(Object error) {
        return completeInternal(QueryResult.FAILED, null, error);
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

    public boolean absent() {
        return result != null && !result.isValue();
    }

    public boolean cached() {
        return result != null && result == QueryResult.CACHED;
    }

    public boolean fetched() {
        return result != null && result == QueryResult.FETCHED;
    }

}
