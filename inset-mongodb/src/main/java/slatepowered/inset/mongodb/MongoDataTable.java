package slatepowered.inset.mongodb;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import slatepowered.inset.bson.DocumentEncodeOutput;
import slatepowered.inset.codec.EncodeOutput;
import slatepowered.inset.query.Query;
import slatepowered.inset.source.*;

/**
 * Abstraction for a MongoDB collection.
 */
@RequiredArgsConstructor
@Getter
public class MongoDataTable implements DataTable {

    // The MongoDB data source
    protected final MongoDataSource source;
    protected final String name;

    // The MongoDB collection
    protected final MongoCollection<BsonDocument> bsonCollection; // TODO: switch everything to this
    protected final MongoCollection<Document> collection;

    @Override
    public DataSource getSource() {
        return source;
    }

    @Override
    public void drop() {
        collection.drop();
    }

    @Override
    public void replaceOneSync(EncodeOutput output) throws DataSourceException {
        DocumentEncodeOutput encodeOutput = output.requireType(DocumentEncodeOutput.class);
        BsonDocument document = encodeOutput.getOutputDocument();
        Object key = output.getSetKey();
        String keyField = output.getSetKeyField();

        // create filter for item
        bsonCollection.replaceOne(Filters.eq(keyField, key), document, new ReplaceOptions().upsert(true));
    }

    @Override
    public DataSourceFindResult findOneSync(Query query) throws DataSourceException {
        String keyFieldOverride = source.getKeyFieldOverride();
        Bson filter = MongoQueries.serializeQueryToFindFilter(query.getDatastore().getDataCodec(), keyFieldOverride, query);

        FindIterable<Document> iterable = collection.find(filter);
        Document result = iterable.first();

        return result != null ?
                MongoQueries.foundQueryResult(query, keyFieldOverride, result) :
                MongoQueries.noneQueryResult(query, keyFieldOverride);
    }

    @Override
    public DataSourceBulkIterable findAllSync(Query query) throws DataSourceException {
        String keyFieldOverride = source.getKeyFieldOverride();
        FindIterable<Document> iterable;

        if (query.fieldConstraintCount() > 0) {
            Bson filter = MongoQueries.serializeQueryToFindFilter(query.getDatastore().getDataCodec(), keyFieldOverride, query);
            iterable = collection.find(filter);
        } else {
            iterable = collection.find();
        }

        return MongoQueries.createBulkIterable(keyFieldOverride, query, iterable);
    }

    @Override
    public boolean deleteOne(Query query) {
        String keyFieldOverride = source.getKeyFieldOverride();
        Bson filter = MongoQueries.serializeQueryToFindFilter(query.getDatastore().getDataCodec(), keyFieldOverride, query);

        return collection.deleteOne(filter).getDeletedCount() > 0;
    }

    @Override
    public long deleteAll(Query query) {
        if (query.fieldConstraintCount() > 0) {
            String keyFieldOverride = source.getKeyFieldOverride();
            Bson filter = MongoQueries.serializeQueryToFindFilter(query.getDatastore().getDataCodec(), keyFieldOverride, query);

            return collection.deleteMany(filter).getDeletedCount();
        } else {
            long count = collection.countDocuments();
            collection.drop();
            return count;
        }
    }

    @Override
    public long count(Query query) {
        String keyFieldOverride = source.getKeyFieldOverride();
        Bson filter = MongoQueries.serializeQueryToFindFilter(query.getDatastore().getDataCodec(), keyFieldOverride, query);

        return collection.countDocuments(filter);
    }

    @Override
    public String toString() {
        return "MongoDataTable('" + name + "')";
    }
}
