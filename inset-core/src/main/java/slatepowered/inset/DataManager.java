package slatepowered.inset;

import lombok.Builder;
import lombok.Getter;
import slatepowered.inset.codec.CodecRegistry;
import slatepowered.inset.codec.DataCodec;
import slatepowered.inset.datastore.Datastore;
import slatepowered.inset.source.DataTable;

/**
 * Manages all resources related to datastores, -sources, etc.
 */
@Builder
@Getter
public class DataManager {

    /**
     * The general codec registry.
     */
    protected final CodecRegistry codecRegistry;

    /**
     * Create a new datastore from the given table for the
     * given key and value types using this codec registry.
     *
     * @param <K> The key type.
     * @param <T> The value type.
     * @return The datastore.
     */
    public <K, T> Datastore<K, T> createDatastore(
            DataTable table,
            Class<K> kClass,
            Class<T> tClass
    ) {
        return new Datastore<>(this, kClass, table, (DataCodec<K, T>) codecRegistry.getCodec(tClass));
    }

}
