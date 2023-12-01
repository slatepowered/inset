package slatepowered.inset;

import lombok.Builder;
import lombok.Getter;
import slatepowered.inset.codec.CodecRegistry;

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

}
