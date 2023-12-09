package slatepowered.inset.query;

import slatepowered.inset.codec.DataCodec;
import slatepowered.inset.datastore.DataItem;
import slatepowered.inset.datastore.Datastore;
import slatepowered.inset.datastore.OperationStatus;
import slatepowered.inset.modifier.Projection;
import slatepowered.inset.modifier.Sorting;
import slatepowered.inset.source.DataSourceBulkIterable;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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

    /**
     * Whether the source iterable had any items.
     */
    protected boolean sourceHadAny;

    /**
     * The stream of cached items, this will be null if
     * no cached items are used in this query.
     */
    protected Stream<? extends DataItem<K, T>> cachedStream;

    /**
     * The list of cached items, this will be null if
     * no cached items are used in this query.
     */
    protected List<? extends DataItem<K, T>> cachedItems;

    /**
     * The stream of items.
     */
    protected Stream<? extends FoundItem<K, T>> stream;

    // The cached stream iterator
    protected Iterator<? extends FoundItem<K, T>> streamIterator;

    public FindAllStatus(Datastore<K, T> datastore, Query query) {
        super(datastore, query);
    }

    // update the current stream instance to the given instance
    private void updateStream(Stream<? extends FoundItem<K, T>> stream) {
        this.stream = stream;
    }

    // ensure the presence of a usable stream iterator to apply
    // to the current output stream, this is a terminal operation
    private Iterator<? extends FoundItem<K, T>> streamIterator() {
        if (streamIterator == null) {
            this.streamIterator = stream.iterator();
        }

        return streamIterator;
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
     * Get the backing source iterable for this status.
     *
     * @return The backing iterable.
     */
    public DataSourceBulkIterable sourceIterable() {
        return iterable;
    }

    /**
     * Set the batch size of this iterable.
     *
     * @param size The batch size.
     * @return This.
     */
    public FindAllStatus<K, T> batch(int size) {
        iterable.batch(size);
        return this;
    }

    /**
     * Limit the maximum amount of results retrieved from the database.
     *
     * @param size The limit.
     * @return This.
     */
    public FindAllStatus<K, T> limit(int size) {
        stream = stream.limit(size);
        return this;
    }

    /**
     * Further filter the results retrieved from the database.
     *
     * @param query The filter query.
     * @return This.
     */
    public FindAllStatus<K, T> filter(Query query) {
        if (cachedStream != null) throw new UnsupportedOperationException("Filtering on a partially cached iterable is currently not supported");
        else iterable = iterable.filter(query);
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
     * Apply the exclusive projection created from the data codec
     * of the given data class to this iterable.
     *
     * @param vClass The data class.
     * @return This.
     */
    public <V> FindAllStatus<K, T> projection(Class<V> vClass) {
        DataCodec<K, V> dataCodec = datastore.getCodecRegistry().getCodec(vClass).expect(DataCodec.class);
        Projection projection = dataCodec.createExclusiveProjection(iterable.getPrimaryKeyFieldOverride());
        return projection(projection);
    }

    /**
     * Sort this the results of this iterable.
     *
     * @param sorting The sorting to apply.
     * @return This.
     */
    public FindAllStatus<K, T> sort(Sorting sorting) {
        if (cachedStream != null) {
            // todo: efficient stream sorting and a (partial) sorting comparator generator
            throw new UnsupportedOperationException("Sorting on a partially cached iterable is currently unsupported");
        } else {
            iterable.sort(sorting);
        }

        return this;
    }

    /**
     * Check whether there are/were any items as a result.
     *
     * @return If there is any data.
     */
    public boolean hasAny() {
        return cachedItems != null ? !cachedItems.isEmpty() || sourceHadAny : sourceHadAny;
    }

    /**
     * Check whether there is another item to be loaded.
     *
     * This is a terminal operation for the backing stream. meaning after this
     * you can no longer use other terminal methods like {@link #first()}.
     *
     * @return Whether there is another item.
     */
    public boolean hasNext() {
        return iterable.hasNext() || cachedStream != null ? streamIterator().hasNext() : iterable.hasNext();
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
     * This is a terminal operation for the backing stream. meaning after this
     * you can no longer use other terminal methods like {@link #first()}.
     *
     * @return The item.
     */
    public Optional<? extends FoundItem<K, T>> next() {
        return cachedStream != null ?
                streamIterator().hasNext() ? Optional.of(streamIterator().next()) : Optional.empty() :
                iterable.next().map(this::qualify);
    }

    /**
     * Asynchronously get, fetch and qualify the next item if present.
     *
     * This may have to fetch it from the database.
     *
     * This is a terminal operation for the backing stream. meaning after this
     * you can no longer use other terminal methods like {@link #first()}.
     *
     * @return The item.
     */
    public CompletableFuture<Optional<? extends FoundItem<K, T>>> nextAsync() {
        return async(this::next);
    }

    /**
     * Get, fetch and qualify the first item if present, this is a terminal operation.
     *
     * This may have to fetch it from the database which could take time. It is recommended to
     * execute this function asynchronously.
     *
     * This is a terminal operation, meaning after this this iterable is closed.
     *
     * @return The item.
     */
    public Optional<? extends FoundItem<K, T>> first() {
        return cachedStream != null ?
                stream.findFirst() :
                iterable.first().map(this::qualify);
    }

    /**
     * Asynchronously get, fetch and qualify the first item if present.
     *
     * This may have to fetch it from the database.
     *
     * This is a terminal operation, meaning after this this iterable is closed.
     *
     * @return The item.
     */
    public CompletableFuture<Optional<? extends FoundItem<K, T>>> firstAsync() {
        return async(this::first);
    }

    /**
     * Get, fetch and qualify all items resolved by this query.
     *
     * This most likely has to fetch the items from the database, so it
     * is recommended to call this asynchronously.
     *
     * This is a terminal operation, meaning after this this iterable is closed.
     *
     * @return The list of items.
     */
    @SuppressWarnings("unchecked")
    public List<? extends FoundItem<K, T>> list() {
        if (cachedStream != null) {
            return stream.collect(Collectors.toList());
        }

        List<? extends FoundItem<K, T>> list = (List<? extends FoundItem<K,T>>) iterable.list();
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
     * This is a terminal operation, meaning after this this iterable is closed.
     *
     * @return The list of items.
     */
    public CompletableFuture<List<? extends FoundItem<K, T>>> listAsync() {
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
    public Stream<? extends FoundItem<K, T>> stream() {
        return stream;
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
     * Zip the given list of cached items to the current stream of items.
     *
     * @param list The list of cached items.
     * @return This.
     */
    public synchronized FindAllStatus<K, T> withCached(List<? extends DataItem<K, T>> list) {
        if (this.cachedStream != null)
            throw new IllegalStateException("Already has an attached cached stream");
        this.cachedItems = list;
        this.cachedStream = list.stream();
        this.stream = this.stream != null ?
                Stream.concat(this.stream, cachedStream) :
                cachedStream;
        return this;
    }

    /**
     * Complete this query with the given parameters.
     */
    protected synchronized FindAllStatus<K, T> completeInternal(DataSourceBulkIterable iterable, Object error) {
        this.iterable = iterable;
        this.error = error;

        if (iterable != null) {
            this.sourceHadAny = iterable.hasNext();

            // update stream with iterable items
            Stream<FoundItem<K, T>> iterableStream = iterable.stream().map(this::qualify);
            updateStream(stream != null ? Stream.concat(stream, iterableStream) : iterableStream);
        }

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
