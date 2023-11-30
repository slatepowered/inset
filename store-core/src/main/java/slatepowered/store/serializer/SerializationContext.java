package slatepowered.store.serializer;

import lombok.RequiredArgsConstructor;
import slatepowered.store.DataManager;

/**
 * The context in which data serialization/marshalling occurs.
 */
@RequiredArgsConstructor
public class SerializationContext {

    protected final DataManager dataManager;

    /**
     * Find or create an applicable data serializer
     * for the given class.
     *
     * @param vClass The class.
     * @param <V> The value type.
     * @return The serializer or null if absent.
     */
    public <V> DataSerializer<V> findSerializer(Class<V> vClass) {
        return dataManager.getSerializerRegistry().getOrCreate(vClass);
    }

}
