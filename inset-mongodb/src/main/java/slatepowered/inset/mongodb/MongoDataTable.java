package slatepowered.inset.mongodb;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.bson.conversions.Bson;
import slatepowered.inset.bson.DocumentEncodeOutput;
import slatepowered.inset.codec.EncodeOutput;
import slatepowered.inset.query.Query;
import slatepowered.inset.source.*;

import javax.print.Doc;

/**
 * Abstraction for a MongoDB collection.
 */
@RequiredArgsConstructor
public class MongoDataTable implements DataTable {

    // The MongoDB data source
    protected final MongoDataSource source;

    // The MongoDB collection
    protected final MongoCollection<Document> collection;

    @Override
    public DataSource getSource() {
        return source;
    }

    @Override
    public void replaceOneSync(EncodeOutput output) throws DataSourceException {
        DocumentEncodeOutput encodeOutput = output.requireType(DocumentEncodeOutput.class);
        Document document = encodeOutput.getOutputDocument();
        Object key = output.getSetKey();
        String keyField = output.getSetKeyField();

        // create filter for item
        collection.replaceOne(Filters.eq(keyField, key), document, new ReplaceOptions().upsert(true));
    }

    @Override
    public DataSourceFindResult findOneSync(Query query) throws DataSourceException {
        String keyFieldOverride = source.getKeyFieldOverride();
        Bson filter = MongoQueries.serializeQueryToFindFilter(keyFieldOverride, query);

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
            Bson filter = MongoQueries.serializeQueryToFindFilter(keyFieldOverride, query);
            iterable = collection.find(filter);
        } else {
            iterable = collection.find();
        }

        return MongoQueries.createBulkIterable(keyFieldOverride, query, iterable);
    }

    @Override
    public boolean deleteOne(Query query) {
        String keyFieldOverride = source.getKeyFieldOverride();
        Bson filter = MongoQueries.serializeQueryToFindFilter(keyFieldOverride, query);

        return collection.deleteOne(filter).getDeletedCount() > 0;
    }

}
