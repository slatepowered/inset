package slatepowered.inset.internal;

import slatepowered.inset.operation.Projection;

/**
 * Represents a type which can be used as a projection.
 */
public interface ProjectionType {

    /**
     * Create a new projection which only includes the fields
     * applicable to this data.
     *
     * @param primaryKeyNameOverride The primary key field name override.
     * @return The projection.
     */
    Projection createExclusiveProjection(String primaryKeyNameOverride);

}
