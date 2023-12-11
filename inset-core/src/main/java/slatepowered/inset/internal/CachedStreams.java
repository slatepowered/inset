package slatepowered.inset.internal;

import slatepowered.inset.datastore.Datastore;
import slatepowered.inset.operation.FieldOrderSorting;
import slatepowered.inset.operation.FieldOrdering;
import slatepowered.inset.operation.Sorting;
import slatepowered.inset.datastore.PartialItem;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Support for working with (partially) cached streams.
 */
public final class CachedStreams {

    /**
     * Combine the given streams into one exclusive stream (no duplicates)
     *
     * @param priorityStream The priority base stream.
     * @param addedStream The added stream.
     * @param <K> The key type.
     * @param <T> The value type.
     * @return The stream.
     */
    public static <K, T> Stream<? extends PartialItem<K, T>> zipStreamsDistinct(Stream<? extends PartialItem<K, T>> priorityStream,
                                                                                Stream<? extends PartialItem<K, T>> addedStream) {
        if (priorityStream == null) return addedStream;
        if (addedStream == null) return priorityStream;
        return Stream.concat(priorityStream, addedStream).distinct();
    }

    /**
     * Sort a partially cached stream according to the given sorting in the
     * context of the given datastore.
     *
     * @param <K> The key type.
     * @param <T> The value type.
     * @param datastore The datastore.
     * @param stream The partially cached stream.
     * @param sorting The sorting.
     * @param cachedStream The cached stream.
     * @param iterableStream The iterable result stream.
     */
    public static <K, T> Stream<? extends PartialItem<K, T>> sortPartialStream(Datastore<K, T> datastore,
                                                                               Stream<? extends PartialItem<K, T>> stream,
                                                                               Sorting sorting,
                                                                               Stream<? extends PartialItem<K, T>> cachedStream,
                                                                               Stream<? extends PartialItem<K, T>> iterableStream) {
        final Comparator<PartialItem<K, T>> fastComparator = createFastComparator(datastore, sorting);

        // TODO: this is slow and inefficient, preferably, since the iterable
        //  stream has been sorted server side, sort the cached stream and combine
        //  the two sorted streams removing duplicates, i assume that would be faster
        return stream.sorted(fastComparator);
    }

    /**
     * Create a fast comparator for the given sorting in the context of the
     * given datastore.
     *
     * @param datastore The datastore.
     * @param sorting The sorting.
     * @param <K> The key type.
     * @param <T> The value type.
     * @return The comparator.
     */
    public static <K, T> Comparator<PartialItem<K, T>> createFastComparator(final Datastore<K, T> datastore,
                                                                            final Sorting sorting) {
        if (!(sorting instanceof FieldOrderSorting)) {
            throw new UnsupportedOperationException("Unsupported sorting type for fast comparator: " + sorting.getClass().getName());
        }

        FieldOrderSorting fieldOrderSorting = (FieldOrderSorting) sorting;
        final String[] fields = fieldOrderSorting.getFieldNames().toArray(new String[0]);
        final int fieldCount = fields.length;
        final List<FieldOrdering> orderings = fieldOrderSorting.getFieldOrderings();
        final int[] orderingFactors = new int[fieldCount];
        for (int i = 0; i < fieldCount; i++) {
            orderingFactors[i] = orderings.get(i).getFactor();
        }

        return (first, second) -> {
            double[] firstArr = first.getFastOrderCoefficients(fields, sorting);
            double[] secondArr = second.getFastOrderCoefficients(fields, sorting);
            final int count = firstArr.length;
            if (count != secondArr.length || count != fieldCount)
                throw new IllegalStateException("Ordering coefficient array length mismatch");

            for (int i = 0; i < count; i++) {
                double a = firstArr[i];
                double b = secondArr[i];

                if (a < b) return -orderingFactors[i];
                else if (a > b) return orderingFactors[i];
            }

            return 0;
        };
    }

}
