package slatepowered.inset.mongodb;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;
import slatepowered.inset.bson.DocumentDecodeInput;
import slatepowered.inset.codec.DecodeInput;
import slatepowered.inset.query.FoundItem;
import slatepowered.inset.query.Query;
import slatepowered.inset.query.constraint.CommonFieldConstraint;
import slatepowered.inset.query.constraint.FieldConstraint;
import slatepowered.inset.source.DataSourceBulkIterable;
import slatepowered.inset.source.DataSourceFindResult;

import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Support/helpers/utilities for working with {@link Query} abstractions
 * with MongoDB.
 */
final class MongoQueries {

    /**
     * Serializes the given query abstraction to a MongoDB filter.
     *
     * @param query The query.
     * @return The filter BSON.
     */
    public static Bson serializeQueryToFindFilter(String keyFieldNameOverride, Query query) {
        // check for primary key
        if (query.hasKey()) {
            String keyField = keyFieldNameOverride != null ? keyFieldNameOverride : query.getKeyField();
            return Filters.eq(keyField , query.getKey());
        }

        Map<String, FieldConstraint<?>> constraintMap = query.getFieldConstraints();
        final int count = constraintMap.size();
        if (count < 1) {
            throw new IllegalArgumentException("A query with zero field constraints can not be used as a filter");
        }

        Bson[] bsonArray = new Bson[count];
        int i = 0;
        for (Map.Entry<String, FieldConstraint<?>> entry : constraintMap.entrySet()) {
            bsonArray[i] = constraintToBson(entry.getKey(), entry.getValue());
            i++;
        }

        return Filters.and(bsonArray);
    }

    /**
     * Convert the given field constraint for a field with the given name
     * into a BSON filter.
     *
     * @param fieldName The field name.
     * @param constraint The constraint.
     * @return The BSON filter.
     */
    public static Bson constraintToBson(String fieldName, FieldConstraint<?> constraint) {
        /* Common Constraints */
        if (constraint instanceof CommonFieldConstraint) {
            CommonFieldConstraint<?> commonConstraint = (CommonFieldConstraint<?>) constraint;

            switch (commonConstraint.getType()) {
                case EQUAL: return Filters.eq(fieldName, encodeFieldConstraintOperand(constraint, commonConstraint.getOperand()));
                case NOT_EQUAL: return Filters.ne(fieldName, encodeFieldConstraintOperand(constraint, commonConstraint.getOperand()));
                case GREATER: return Filters.gt(fieldName, encodeFieldConstraintOperand(constraint, commonConstraint.getOperand()));
                case LESS: return Filters.lt(fieldName, encodeFieldConstraintOperand(constraint, commonConstraint.getOperand()));
                case GREATER_OR_EQUAL: return Filters.gte(fieldName, encodeFieldConstraintOperand(constraint, commonConstraint.getOperand()));
                case LESS_OR_EQUAL: return Filters.lte(fieldName, encodeFieldConstraintOperand(constraint, commonConstraint.getOperand()));
                case EXISTS: return Filters.exists(fieldName);
                case ONE_OF: return Filters.in(fieldName, (Iterable<?>) encodeFieldConstraintOperand(constraint, commonConstraint.getOperand()));
            }
        }
        
        throw new UnsupportedOperationException("Unsupported field constraint type " + constraint.getClass().getName() + " for field `" + fieldName + "`");
    }

    /**
     * Convert the given operand into a BSON/MongoDB-supported field value.
     *
     * @param constraint The constraint this operand is for.
     * @param operand The operand value.
     * @return The encoded operand.
     */
    public static Object encodeFieldConstraintOperand(FieldConstraint<?> constraint, Object operand) {
        return operand; // todo maybe idk if this is necessary
    }

    public static DataSourceFindResult noneQueryResult(Query query) {
        return new DataSourceFindResult() {
            @Override
            public Query getQuery() {
                return query;
            }

            @Override
            public boolean found() {
                return false;
            }

            @Override
            public DecodeInput input() {
                return null;
            }
        };
    }

    public static DataSourceFindResult foundQueryResult(Query query, String keyFieldOverride, Document document) {
        return new DataSourceFindResult() {
            @Override
            public Query getQuery() {
                return query;
            }

            @Override
            public boolean found() {
                return true;
            }

            @Override
            public DecodeInput input() {
                return new DocumentDecodeInput(keyFieldOverride, document);
            }
        };
    }

    /**
     * Creates a new {@link DataSourceBulkIterable} from the given MongoDB
     * result iterable.
     *
     * @param iterable The MongoDB iterable.
     * @return The result set.
     */
    public static DataSourceBulkIterable createBulkIterable(final String keyFieldNameOverride,
                                                            final Query query,
                                                            final FindIterable<Document> iterable) {
        MongoCursor<Document> cursor = iterable.cursor();
        return new DataSourceBulkIterable() {
            // Whether any projections happened causing the
            // data to only be partial.
            boolean partial = false;

            @Override
            public Query getQuery() {
                return query;
            }

            @Override
            public DataSourceBulkIterable batch(int size) {
                iterable.batchSize(size);
                return this;
            }

            @Override
            public DataSourceBulkIterable filter(Query query) {
                iterable.filter(serializeQueryToFindFilter(keyFieldNameOverride, query));
                return this;
            }

            @Override
            public DataSourceBulkIterable limit(int limit) {
                iterable.limit(limit);
                return this;
            }

            // convert the given document to a bulk item result
            private FoundItem<?, ?> convert(Document document) {
                return toBulkItem(document, keyFieldNameOverride, partial);
            }

            // convert the given optional document to a bulk item result
            private Optional<FoundItem<?, ?>> convertNullable(Document document) {
                return document == null ? Optional.empty() : Optional.of(convert(document));
            }

            @Override
            public Optional<FoundItem<?, ?>> first() {
                return convertNullable(iterable.first());
            }

            @Override
            public Optional<FoundItem<?, ?>> next() {
                return convertNullable(cursor.tryNext());
            }

            @Override
            public boolean hasNext() {
                return cursor.hasNext();
            }

            @Override
            public List<FoundItem<?, ?>> list() {
                List<FoundItem<?, ?>> list = new ArrayList<>();
                while (cursor.hasNext()) {
                    Document doc = cursor.tryNext();
                    if (doc == null)
                        continue;

                    list.add(convert(doc));
                }

                return list;
            }

            @Override
            public Stream<FoundItem<?, ?>> stream() {
                return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterable.iterator(), Spliterator.ORDERED), false)
                        .filter(Objects::nonNull)
                        .map(this::convert);
            }
        };
    }

    /**
     * Convert the given document with the given metadata to a found bulk item.
     */
    public static FoundItem<?, ?> toBulkItem(Document document,
                                             String keyFieldOverride,
                                             boolean partial) {
        return new FoundItem<Object, Object>() {
            @Override
            public boolean isPartial() {
                return partial;
            }

            @Override
            public DecodeInput input() {
                return new DocumentDecodeInput(keyFieldOverride, document);
            }
        };
    }

}
