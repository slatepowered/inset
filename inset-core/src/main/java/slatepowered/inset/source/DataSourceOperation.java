package slatepowered.inset.source;

/**
 * Common class to the (result of) any data source operation.
 */
public interface DataSourceOperation {

    /**
     * Get the override for the name of the primary key field
     * in the output data.
     *
     * A return value of null is interpreted as to use the key
     * field name provided by the data codec to decode the key.
     *
     * @return The primary key field override.
     */
    String getPrimaryKeyFieldOverride();

}
