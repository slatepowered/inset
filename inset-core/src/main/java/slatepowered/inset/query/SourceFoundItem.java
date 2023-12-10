package slatepowered.inset.query;

import slatepowered.inset.codec.CodecContext;
import slatepowered.inset.codec.DataCodec;
import slatepowered.inset.codec.DecodeInput;
import slatepowered.inset.datastore.DataItem;
import slatepowered.inset.datastore.Datastore;
import slatepowered.inset.operation.Sorting;

import java.lang.reflect.Type;

/**
 * Represents a {@link FoundItem} retrieved from a data source.
 *
 * @param <K> The key type.
 * @param <T> The value type.
 */
public abstract class SourceFoundItem<K, T> extends FoundItem<K, T> {

    protected CodecContext partialCodecContext; // The context used to read from the partial data
    protected K cachedKey; // The cached key object

    // ensure a codec context for the reading
    // of partial data exists and return it
    private CodecContext ensurePartialCodecContext() {
        if (partialCodecContext == null) {
            partialCodecContext = assertQualified().getDatastore().newCodecContext();
        }

        return partialCodecContext;
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
            Datastore<K, T> datastore = assertQualified().getDatastore();
            cachedKey = getOrReadKey(datastore.getDataCodec().getPrimaryKeyFieldName(), datastore.getKeyClass());
        }

        return cachedKey;
    }

    @Override
    public <V> V project(Class<V> vClass) {
        FindAllOperation<K, T> status = assertQualified();
        Datastore<K, T> datastore = status.getDatastore();

        DataCodec<K, V> dataCodec = datastore.getCodecRegistry().getCodec(vClass).expect(DataCodec.class);
        CodecContext context = datastore.newCodecContext();
        return dataCodec.constructAndDecode(context, input());
    }

    @Override
    @SuppressWarnings("unchecked")
    public DataItem<K, T> fetch() {
        FindAllOperation<?, ?> status = assertQualified();
        Datastore<K, T> datastore = (Datastore<K, T>) status.getDatastore();

        DecodeInput input = input();

        // if complete there is no need to fetch the
        // full data item from the database
        if (!isPartial()) {
            return datastore.decodeFetched(input);
        }

        // fetch a new item from the database
        FindOperation<K, T> findStatus = datastore.findOne(getOrReadKey(datastore.getDataCodec().getPrimaryKeyFieldName(), datastore.getKeyClass()))
                .await();
        if (findStatus.failed()) {
            Object error = findStatus.error();
            Throwable cause = error instanceof Throwable ? findStatus.errorAs() : null;
            throw new RuntimeException("Error while fetching data item from bulk result" +
                    (cause == null ? ": " + error : ""), cause);
        }

        return findStatus.item();
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

}
