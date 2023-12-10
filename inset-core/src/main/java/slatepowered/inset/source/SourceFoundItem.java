package slatepowered.inset.source;

import slatepowered.inset.codec.CodecContext;
import slatepowered.inset.codec.DecodeInput;
import slatepowered.inset.datastore.DataItem;
import slatepowered.inset.datastore.Datastore;
import slatepowered.inset.internal.ProjectionInterface;
import slatepowered.inset.operation.Sorting;
import slatepowered.inset.query.*;

import java.lang.reflect.Type;
import java.util.Optional;

/**
 * Represents a {@link PartialItem} retrieved from a data source.
 *
 * @param <K> The key type.
 * @param <T> The value type.
 */
public abstract class SourceFoundItem<K, T> extends PartialItem<K, T> {

    /**
     * The operation status this found item is a part of.
     *
     * This is only available after qualified.
     */
    protected Datastore<?, ?> source;

    protected CodecContext partialCodecContext; // The context used to read from the partial data
    protected K cachedKey; // The cached key object

    @SuppressWarnings("unchecked")
    public  <K2, T2> SourceFoundItem<K2, T2> qualify(FindAllOperation<K2, T2> source) {
        this.source = source.getDatastore();
        return (SourceFoundItem<K2, T2>) this;
    }

    // ensure a codec context for the reading
    // of partial data exists and return it
    private CodecContext ensurePartialCodecContext() {
        if (partialCodecContext == null) {
            partialCodecContext = assertQualified().newCodecContext();
        }

        return partialCodecContext;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected final Datastore<K, T> assertQualified() {
        if (source == null)
            throw new IllegalStateException("Sourced item is not qualified");
        return (Datastore<K, T>) source;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> V getField(String fieldName, Type expectedType) {
        return (V) getOrCreateInput().read(ensurePartialCodecContext(), fieldName, expectedType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public K getOrReadKey(String fieldName, Type expectedType) {
        return (K) getOrCreateInput().getOrReadKey(fieldName, expectedType);
    }

    @Override
    public K getKey() {
        if (cachedKey == null) {
            Datastore<K, T> datastore = assertQualified();
            cachedKey = getOrReadKey(datastore.getDataCodec().getPrimaryKeyFieldName(), datastore.getKeyClass());
        }

        return cachedKey;
    }

    @Override
    @SuppressWarnings("unchecked")
    public FindOperation<K, T> find() {
        Datastore<K, T> datastore = assertQualified();

        // if complete there is no need to fetch the
        // full data item from the database
        if (!isPartial()) {
            DecodeInput input = input();
            return new FindOperation<>(datastore, null).completeSuccessfully(FindResult.FETCHED, datastore.decodeFetched(input));
        }

        // fetch a new item from the database
        return datastore.findOne(getOrReadKey(datastore.getDataCodec().getPrimaryKeyFieldName(), datastore.getKeyClass()));
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <V> V projectInterface(ProjectionInterface projectionInterface) {
        return (V) projectionInterface.createProxy(this::getKey, this::getField);
    }

    @Override
    public double[] createFastOrderCoefficients(String[] fields, Sorting sorting) {
        final int len = fields.length;
        final double[] arr = new double[len];

        for (int i = 0; i < len; i++) {
            String field = fields[i];
            Object obj = getField(field, Number.class);
            if (obj instanceof Number)
                arr[i] = ((Number) obj).doubleValue();
        }

        return arr;
    }

    @Override
    public Optional<DataItem<K, T>> findCached() {
        return Optional.ofNullable(assertQualified().findOneCached(Query.byKey(getKey())));
    }

}
