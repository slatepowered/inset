package slatepowered.inset.mongodb;

import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;
import slatepowered.inset.bson.DocumentDecodeInput;
import slatepowered.inset.codec.DecodeInput;
import slatepowered.inset.query.CommonFieldConstraint;
import slatepowered.inset.query.FieldConstraint;
import slatepowered.inset.query.Query;
import slatepowered.inset.source.DataSourceQueryResult;

import java.util.Map;

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
    public static Bson serializeQueryToFindOneFilter(String keyFieldNameOverride, Query query) {
        // check for primary key
        if (query.hasKey()) {
            String keyField = keyFieldNameOverride != null ? keyFieldNameOverride : query.getKeyField();
            return Filters.eq(keyField , query.getKey());
        }

        Map<String, FieldConstraint<?>> constraintMap = query.getFieldConstraints();
        final int count = constraintMap.size();
        if (count < 1) {
            throw new IllegalArgumentException("Query with zero field constraints can not be used to find one item");
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

    public static DataSourceQueryResult noneQueryResult(Query query) {
        return new DataSourceQueryResult() {
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

    public static DataSourceQueryResult foundQueryResult(Query query, String keyFieldOverride, Document document) {
        return new DataSourceQueryResult() {
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

}
