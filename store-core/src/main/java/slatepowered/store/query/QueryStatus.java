package slatepowered.store.query;

import slatepowered.store.datastore.DataItem;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Represents an awaitable query status/result.
 *
 * @param <K> The primary key type.
 * @param <T> The data type.
 */
public class QueryStatus<K, T> {

    /**
     * Whether the query completed at all, success is not required.
     */
    private volatile boolean completed = false;

    /**
     * The completable future.
     */
    private final CompletableFuture<QueryStatus<K, T>> future = new CompletableFuture<>();

    /* Result Fields (set when the query completed) */
    private volatile QueryResult result;  // The type of result
    private volatile DataItem<K, T> item; // The data item if successful
    private volatile Object error;        // The error object if it failed

    /**
     * Get whether this query completed at all.
     *
     * @return Whether it completed.
     */
    public boolean isCompleted() {
        return completed;
    }

    /**
     * Get the awaitable future.
     *
     * @return The future.
     */
    public CompletableFuture<QueryStatus<K, T>> future() {
        return future;
    }

    /**
     * Block this thread to await completion of the query.
     *
     * @return This.
     */
    public QueryStatus<K, T> await() {
        return future.join();
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
                consumer.accept(status);
            }
        });

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

        future.complete(this);
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

    public boolean present() {
        return result != null && result.isValue();
    }

    public boolean absent() {
        return result != null && !result.isValue();
    }

    public boolean found() {
        return result != null && result == QueryResult.FOUND;
    }

    public boolean loaded() {
        return result != null && result == QueryResult.LOADED;
    }

}
