package slatepowered.inset.query;

import slatepowered.inset.datastore.Datastore;
import slatepowered.inset.datastore.OperationStatus;
import slatepowered.inset.modifier.Projection;
import slatepowered.inset.modifier.Sorting;
import slatepowered.inset.source.DataSourceBulkIterable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * The status/result of a {@link Datastore#findAll(Query)} operation.
 *
 * @param <K> The data item key type.
 * @param <T> The data item value type.
 */
public class FindAllStatus<K, T> extends OperationStatus<K, T, FindAllStatus<K, T>> {

    /**
     * The data source result iterable.
     */
    protected DataSourceBulkIterable iterable;

    public FindAllStatus(Datastore<K, T> datastore, Query query) {
        super(datastore, query);
    }

    /**
     * When this query is successfully completed, call the given consumer.
     *
     * @param consumer The consumer.
     * @return This.
     */
    public FindAllStatus<K, T> then(Consumer<FindAllStatus<K, T>> consumer) {
        future.whenComplete((status, throwable) -> {
            if (status != null && status.success()) {
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
     * Add a new asynchronous action to be executed when the previous
     * stage of this query finishes successfully.
     *
     * @param action The action.
     * @return This.
     */
    public FindAllStatus<K, T> thenApplyAsync(Consumer<FindAllStatus<K, T>> action) {
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
    public FindAllStatus<K, T> thenApply(Consumer<FindAllStatus<K, T>> action) {
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
    public FindAllStatus<K, T> exceptionally(Consumer<FindAllStatus<K, T>> consumer) {
        future.whenComplete((status, throwable) -> {
            if (status.error != null) {
                consumer.accept(status);
            }
        });

        return this;
    }

    /**
     * Get the backing iterable for this status.
     *
     * @return The backing iterable.
     */
    public DataSourceBulkIterable iterable() {
        return iterable;
    }

    /**
     * Set the batch size of this iterable.
     *
     * @param size The batch size.
     * @return This.
     */
    public FindAllStatus<K, T> batch(int size) {
        iterable = iterable.batch(size);
        return this;
    }

    /**
     * Limit the maximum amount of results retrieved from the database.
     *
     * @param size The limit.
     * @return This.
     */
    public FindAllStatus<K, T> limit(int size) {
        iterable = iterable.limit(size);
        return this;
    }

    /**
     * Further filter the results retrieved from the database.
     *
     * @param query The filter query.
     * @return This.
     */
    public FindAllStatus<K, T> filter(Query query) {
        iterable = iterable.filter(query);
        return this;
    }

    /**
     * Apply the given projection to this iterable query, this will cause
     * the data to be partial so to deserialize full objects it will need
     * to be fetched from the database.
     *
     * @param projection The projection to apply.
     * @return This.
     */
    public FindAllStatus<K, T> projection(Projection projection) {
        iterable.projection(projection);
        return this;
    }

    /**
     * Sort this the results of this iterable.
     *
     * @param sorting The sorting to apply.
     * @return This.
     */
    public FindAllStatus<K, T> sort(Sorting sorting) {
        iterable.sort(sorting);
        return this;
    }

    /**
     * Check whether there is another item to be loaded.
     *
     * @return Whether there is another item.
     */
    public boolean hasNext() {
        return iterable.hasNext();
    }

    // asynchronously execute the given function
    private <A> CompletableFuture<A> async(Supplier<A> supplier) {
        return CompletableFuture.supplyAsync(supplier, datastore.getDataManager().getExecutorService());
    }

    // qualify the given item for this query
    private FoundItem<K, T> qualify(FoundItem<?, ?> item) {
        return item.qualify(this);
    }

    /**
     * Get, fetch and qualify the next item if present.
     *
     * This may have to fetch it from the database which could take time. It is recommended to
     * execute this function asynchronously.
     *
     * @return The item.
     */
    public Optional<FoundItem<K, T>> next() {
        return iterable.next().map(this::qualify);
    }

    /**
     * Asynchronously get, fetch and qualify the next item if present.
     *
     * This may have to fetch it from the database.
     *
     * @return The item.
     */
    public CompletableFuture<Optional<FoundItem<K, T>>> nextAsync() {
        return async(this::next);
    }

    /**
     * Get, fetch and qualify the first item if present.
     *
     * This may have to fetch it from the database which could take time. It is recommended to
     * execute this function asynchronously.
     *
     * @return The item.
     */
    public Optional<FoundItem<K, T>> first() {
        return iterable.first().map(this::qualify);
    }

    /**
     * Asynchronously get, fetch and qualify the first item if present.
     *
     * This may have to fetch it from the database.
     *
     * @return The item.
     */
    public CompletableFuture<Optional<FoundItem<K, T>>> firstAsync() {
        return async(this::first);
    }

    /**
     * Get, fetch and qualify all items resolved by this query.
     *
     * This most likely has to fetch the items from the database, so it
     * is recommended to call this asynchronously.
     *
     * @return The list of items.
     */
    @SuppressWarnings("unchecked")
    public List<FoundItem<K, T>> list() {
        List<FoundItem<K, T>> list = (List<FoundItem<K,T>>) (Object) iterable.list();
        for (FoundItem<K, T> item : list) {
            this.qualify(item);
        }

        return list;
    }

    /**
     * Asynchronously get, fetch and qualify all items resolved by this query.
     *
     * This most likely has to fetch the items from the database.
     *
     * @return The list of items.
     */
    public CompletableFuture<List<FoundItem<K, T>>> listAsync() {
        return async(this::list);
    }

    /**
     * Stream the process of getting, fetching and qualifying all items
     * resolved by this query.
     *
     * This most likely has to fetch the items from the database.
     *
     * @return The stream of items.
     */
    public Stream<FoundItem<K, T>> stream() {
        return iterable.stream().map(this::qualify);
    }

    public boolean failed() {
        return error != null;
    }

    @Override
    protected String describeOperation() {
        return "executing bulk find";
    }

    public boolean success() {
        return error == null;
    }

    /**
     * Complete this query with the given parameters.
     */
    protected synchronized FindAllStatus<K, T> completeInternal(DataSourceBulkIterable iterable, Object error) {
        this.iterable = iterable;
        this.error = error;
        completeInternal(this);
        return this;
    }

    public synchronized FindAllStatus<K, T> completeSuccessfully(DataSourceBulkIterable iterable) {
        return completeInternal(iterable, null);
    }

    public synchronized FindAllStatus<K, T> completeFailed(Object error) {
        return completeInternal(null, error);
    }

}
