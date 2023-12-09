package slatepowered.inset.query;

import slatepowered.inset.codec.CodecContext;
import slatepowered.inset.codec.DataCodec;
import slatepowered.inset.codec.DecodeInput;
import slatepowered.inset.datastore.DataItem;
import slatepowered.inset.datastore.Datastore;

import java.lang.reflect.Type;

/**
 * Represents a {@link FoundItem} retrieved from a data source.
 *
 * @param <K> The key type.
 * @param <T> The value type.
 */
public abstract class SourceFoundItem<K, T> extends FoundItem<K, T> {

    protected CodecContext partialCodecContext; // The context used to read from the partial data
    protected Object cachedKey;

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
    public K getKey(String fieldName, Type expectedType) {
        if (cachedKey == null) {
            cachedKey = getOrCreateInput().getOrReadKey(fieldName, expectedType);
        }

        return (K) cachedKey;
    }

    @Override
    public <V> V project(Class<V> vClass) {
        FindAllStatus<K, T> status = assertQualified();
        Datastore<K, T> datastore = status.getDatastore();

        DataCodec<K, V> dataCodec = datastore.getCodecRegistry().getCodec(vClass).expect(DataCodec.class);
        CodecContext context = datastore.newCodecContext();
        return dataCodec.constructAndDecode(context, input());
    }

    @Override
    @SuppressWarnings("unchecked")
    public DataItem<K, T> fetch() {
        FindAllStatus<?, ?> status = assertQualified();
        Datastore<K, T> datastore = (Datastore<K, T>) status.getDatastore();

        DecodeInput input = input();

        // if complete there is no need to fetch the
        // full data item from the database
        if (!isPartial()) {
            return datastore.decodeFetched(input);
        }

        // fetch a new item from the database
        FindStatus<K, T> findStatus = datastore.findOne(getKey(datastore.getDataCodec().getPrimaryKeyFieldName(), datastore.getKeyClass()))
                .await();
        if (findStatus.failed()) {
            Object error = findStatus.error();
            Throwable cause = error instanceof Throwable ? findStatus.errorAs() : null;
            throw new RuntimeException("Error while fetching data item from bulk result" +
                    (cause == null ? ": " + error : ""), cause);
        }

        return findStatus.item();
    }

}
