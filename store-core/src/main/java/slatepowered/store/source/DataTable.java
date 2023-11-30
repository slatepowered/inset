package slatepowered.store.source;

import slatepowered.store.serializer.SerializationOutput;

/**
 * Represents a collection/table into a {@link DataSource}.
 */
public interface DataTable {

    /**
     * Get the data source for this table.
     *
     * @return The source.
     */
    DataSource getSource();

    /**
     * Synchronously update the given output data in the data table.
     *
     * @param output The output.
     */
    void updateOneSync(SerializationOutput output);

}
