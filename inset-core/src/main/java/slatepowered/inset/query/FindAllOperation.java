package slatepowered.inset.query;

import lombok.Builder;
import lombok.Getter;
import slatepowered.inset.datastore.DataItem;
import slatepowered.inset.datastore.Datastore;
import slatepowered.inset.datastore.OperationStatus;
import slatepowered.inset.datastore.PartialItem;
import slatepowered.inset.internal.CachedStreams;
import slatepowered.inset.internal.ProjectionType;
import slatepowered.inset.internal.ProjectionTypes;
import slatepowered.inset.operation.Projection;
import slatepowered.inset.operation.Sorting;
import slatepowered.inset.source.DataSourceBulkIterable;
import slatepowered.inset.source.SourcedItem;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The status/result of a {@link Datastore#findAll(Query)} operation.
 *
 * @param <K> The data item key type.
 * @param <T> The data item value type.
 */
public class FindAllOperation<K, T> extends OperationStatus<K, T, FindAllOperation<K, T>> {

    @Builder(toBuilder = true)
    @Getter
    public static class Options {
        /**
         * Whether to include cached items, this has the benefit of
         * always having the latest local changes but can make certain
         * operations on the iterable perform slower.
         */
        protected final boolean useCaches;
    }

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
     * THe stream of items as a result of the data source iterable.
     */
    protected Stream<? extends PartialItem<K, T>> iterableStream;

    /**
     * The stream of items.
     */
    protected Stream<? extends PartialItem<K, T>> stream;

    // The cached stream iterator
    protected Iterator<? extends PartialItem<K, T>> streamIterator;

    /**
     * The options passed on this operation.
     */
    @Getter
    protected final Options options;

    public FindAllOperation(Datastore<K, T> datastore, Query query, Options options) {
        super(datastore, query);
        this.options = options;
    }

    // update the current stream instance to the given instance
    private synchronized void updateStream(Stream<? extends PartialItem<K, T>> stream) {
        this.stream = stream;
    }

    // ensure the presence of a usable stream iterator to apply
    // to the current output stream, this is a terminal operation
    private Iterator<? extends PartialItem<K, T>> streamIterator() {
        if (streamIterator == null) {
            this.streamIterator = stream.iterator();
        }

        return streamIterator;
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
    public FindAllOperation<K, T> batch(int size) {
        iterable.batch(size);
        return this;
    }

    /**
     * Limit the maximum amount of results retrieved from the database.
     *
     * @param size The limit.
     * @return This.
     */
    public FindAllOperation<K, T> limit(int size) {
        updateStream(stream.limit(size));
        iterable.limit(size);
        return this;
    }

    /**
     * Further filter the results retrieved from the database.
     *
     * @param query The filter query.
     * @return This.
     */
    public FindAllOperation<K, T> filter(Query query) {
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
    public FindAllOperation<K, T> projection(Projection projection) {
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
    public <V> FindAllOperation<K, T> projection(Class<V> vClass) {
        ProjectionType projectionType = ProjectionTypes.getProjectionType(vClass, datastore);
        Projection projection = projectionType.createExclusiveProjection(iterable.getPrimaryKeyFieldOverride());
        return projection(projection);
    }

    /**
     * Sort this the results of this iterable.
     *
     * @param sorting The sorting to apply.
     * @return This.
     */
    public FindAllOperation<K, T> sort(Sorting sorting) {
        iterable.sort(sorting);
        if (cachedStream != null) {
            updateStream(CachedStreams.sortPartialStream(
                    datastore, stream,
                    sorting,
                    cachedStream, iterableStream));
        }

        return this;
    }

    /**
     * Skip the given amount of items found by this operation.
     *
     * @param amount The amount.
     * @return This.
     */
    public FindAllOperation<K, T> skip(int amount) {
        if (cachedStream != null) stream = stream.skip(amount);
        else iterable.skip(amount);
        return this;
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
        return CompletableFuture.supplyAsync(supplier, datastore.getExecutorService());
    }

    // qualify the given item for this query
    private SourcedItem<K, T> qualify(SourcedItem<?, ?> item) {
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
    public Optional<? extends PartialItem<K, T>> next() {
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
    public CompletableFuture<Optional<? extends PartialItem<K, T>>> nextAsync() {
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
    public Optional<? extends PartialItem<K, T>> first() {
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
    public CompletableFuture<Optional<? extends PartialItem<K, T>>> firstAsync() {
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
    public List<? extends PartialItem<K, T>> list() {
        if (cachedStream != null) {
            return stream.collect(Collectors.toList());
        }

        List<SourcedItem<K, T>> list = (List<SourcedItem<K,T>>) (Object) iterable.list();
        for (SourcedItem<K, T> item : list) {
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
    public CompletableFuture<List<? extends PartialItem<K, T>>> listAsync() {
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
    public Stream<? extends PartialItem<K, T>> stream() {
        return stream;
    }

    /**
     * Execute this consumer for each found item in the stream as a
     * part of the pipeline.
     *
     * @param consumer The consumer.
     * @return This.
     */
    public FindAllOperation<K, T> peek(Consumer<PartialItem<K, T>> consumer) {
        stream = stream.peek(consumer);
        return this;
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
     * Zip the given stream of cached items to the current stream of items.
     *
     * @param stream The stream of cached items.
     * @return This.
     */
    public synchronized FindAllOperation<K, T> withCached(Stream<? extends DataItem<K, T>> stream) {
        if (this.cachedStream != null)
            throw new IllegalStateException("Already has an attached cached stream");
        this.cachedStream = stream;
        updateStream(CachedStreams.zipStreamsDistinct(cachedStream, this.stream));
        return this;
    }

    /**
     * Complete this query with the given parameters.
     */
    protected synchronized FindAllOperation<K, T> completeInternal(DataSourceBulkIterable iterable, Object error) {
        this.iterable = iterable;
        this.error = error;

        if (iterable != null) {
            this.sourceHadAny = iterable.hasNext();

            // update stream with iterable items
            Stream<PartialItem<K, T>> iterableStream = iterable.stream().map(this::qualify);
            this.iterableStream = iterableStream;
            updateStream(CachedStreams.zipStreamsDistinct(this.stream, iterableStream));
        }

        completeInternal(this);
        return this;
    }

    public synchronized FindAllOperation<K, T> completeSuccessfully(DataSourceBulkIterable iterable) {
        return completeInternal(iterable, null);
    }

    public synchronized FindAllOperation<K, T> completeFailed(Object error) {
        return completeInternal(null, error);
    }

}
