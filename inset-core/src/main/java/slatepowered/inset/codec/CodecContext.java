package slatepowered.inset.codec;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import slatepowered.inset.DataManager;

/**
 * The context in which data serialization/marshalling occurs.
 */
@RequiredArgsConstructor
public class CodecContext {

    @Getter
    protected final DataManager dataManager;

    /**
     * Find or create an applicable value/data codec
     * for the given class.
     *
     * @param vClass The class.
     * @param <V> The value type.
     * @return The codec or null if absent.
     */
    public <V> ValueCodec<V> findCodec(Class<V> vClass) {
        return dataManager.getCodecRegistry().getCodec(vClass);
    }

    public ClassDistinctionReader findClassDistinctionReader(Class<?> baseClass) {
        return dataManager.getClassDistinctionReaders().get(baseClass);
    }

}
