package slatepowered.inset.query;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents the type of result of a query.
 */
@RequiredArgsConstructor
@Getter
public enum QueryResult {

    /**
     * No cached item was found so it was successfully loaded from the database.
     */
    LOADED(true, true),

    /**
     * No present cached item was found and no data was found in the database.
     * The query was unsuccessful but there wasn't a technical error.
     */
    ABSENT(true, false),

    /**
     * A cached item was found.
     */
    FOUND(true, true),

    /**
     * An error occurred.
     */
    FAILED(false, false),

    ;

    /**
     * Whether the query was technically successful (no errors occurred).
     */
    private final boolean successful;

    /**
     * Whether a value was found.
     */
    private final boolean value;

}
