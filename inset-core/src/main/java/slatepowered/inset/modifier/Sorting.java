package slatepowered.inset.modifier;

/**
 * Represents a sorting specification for a set of fields on specific data.
 */
public interface Sorting {

    /**
     * Creates a builder for the standard sorting implementation
     * of {@link FieldOrderSorting}.
     *
     * @return The builder.
     */
    static FieldOrderSorting.Builder builder() {
        return new FieldOrderSorting.Builder();
    }

}
