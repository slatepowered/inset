package slatepowered.store.query;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collection;

/**
 * Describes the model of a query, including what fields are set
 * and other data such as it.
 */
@RequiredArgsConstructor
@Getter
public class QueryModel {

    /**
     * All constrained base fields involved with the query.
     */
    final Collection<String> constrainedFields;

}
