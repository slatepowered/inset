package slatepowered.inset.operation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Represents a field projection, excluding or including specific
 * fields from the query or results.
 */
public interface Projection {

    static CommonProjection include(Collection<String> collection) {
        return new CommonProjection(CommonProjection.Action.INCLUDE, collection instanceof List ? (List<String>) collection : new ArrayList<>(collection));
    }

    static CommonProjection include(String... fields) {
        return new CommonProjection(CommonProjection.Action.INCLUDE, Arrays.asList(fields));
    }

    static CommonProjection exclude(Collection<String> collection) {
        return new CommonProjection(CommonProjection.Action.EXCLUDE, collection instanceof List ? (List<String>) collection : new ArrayList<>(collection));
    }

    static CommonProjection exclude(String... fields) {
        return new CommonProjection(CommonProjection.Action.EXCLUDE, Arrays.asList(fields));
    }

}
