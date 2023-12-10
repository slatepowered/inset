package slatepowered.inset.operation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of {@link Sorting} which sorts a certain set of fields.
 */
@RequiredArgsConstructor
@Getter
public class FieldOrderSorting implements Sorting {

    protected final List<String> fieldNames;
    protected final List<FieldOrdering> fieldOrderings;

    /**
     * Get the count of field orderings in this sorting.
     *
     * @return The size.
     */
    public int size() {
        int r;
        if (fieldNames.size() != (r = fieldOrderings.size()))
            throw new IllegalStateException("Field list size mismatch");
        return r;
    }

    /**
     * Builds a {@link FieldOrderSorting}.
     */
    public static class Builder {
        protected List<String> fieldNames = new ArrayList<>();
        protected List<FieldOrdering> fieldOrderings = new ArrayList<>();

        public Builder order(String field, FieldOrdering ordering) {
            fieldNames.add(field);
            fieldOrderings.add(ordering);
            return this;
        }

        public Builder ascend(String field) {
            return order(field, FieldOrdering.ASCENDING);
        }

        public Builder descend(String field) {
            return order(field, FieldOrdering.DESCENDING);
        }

        public FieldOrderSorting build() {
            return new FieldOrderSorting(fieldNames, fieldOrderings);
        }
    }

}
