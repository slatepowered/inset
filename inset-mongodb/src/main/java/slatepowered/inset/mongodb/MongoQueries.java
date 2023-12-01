package slatepowered.inset.mongodb;

import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;
import slatepowered.inset.bson.DocumentDecodeInput;
import slatepowered.inset.codec.DecodeInput;
import slatepowered.inset.query.Query;
import slatepowered.inset.source.DataSourceQueryResult;

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
    public static Bson serializeQueryToFilter(String keyFieldNameOverride, Query query) {
        // check for primary key
        if (query.hasKey()) {
            String keyField = keyFieldNameOverride != null ? keyFieldNameOverride : query.getKeyField();
            return Filters.eq(keyField , query.getKey());
        }

        // TODO: implement actual query serialization lol
        throw new UnsupportedOperationException("Unsupported query properties for MongoDB");
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
