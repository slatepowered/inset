package slatepowered.inset.source;

import slatepowered.inset.codec.EncodeOutput;

/**
 * Represents a type of data storage.
 */
public interface DataSource {

    /**
     * Creates a new document serialization output which produces
     * a result compatible with this data source.
     *
     * @return The output.
     */
    EncodeOutput createDocumentSerializationOutput();

    /**
     * Get or create a new table/collection with the given name.
     *
     * @param name The name.
     * @return The table/collection.
     */
    DataTable table(String name);

}
