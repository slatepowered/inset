package slatepowered.store;

import lombok.Builder;
import lombok.Getter;
import slatepowered.store.serializer.SerializerRegistry;

/**
 * Manages all resources related to datastores, -sources, etc.
 */
@Builder
@Getter
public class DataManager {

    /**
     * The general serializer registry.
     */
    protected final SerializerRegistry serializerRegistry;

}
