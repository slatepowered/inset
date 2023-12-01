package slatepowered.inset.mongodb;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import lombok.*;
import org.bson.Document;
import org.bson.UuidRepresentation;
import slatepowered.inset.bson.DocumentEncodeOutput;
import slatepowered.inset.codec.EncodeOutput;
import slatepowered.inset.source.DataSource;
import slatepowered.inset.source.DataTable;
import slatepowered.veru.functional.ThrowingSupplier;

import java.util.HashMap;
import java.util.Map;

/**
 * Data source using a MongoDB connection.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class MongoDataSource implements DataSource {

    // The MongoDB database instance
    private final MongoDatabase database;

    // All created data tables
    private final Map<String, MongoDataTable> dataTableMap = new HashMap<>();

    @Getter
    @Setter
    private String keyFieldOverride; // The key field to use

    @Override
    public EncodeOutput createDocumentSerializationOutput() {
        return new DocumentEncodeOutput(
                keyFieldOverride,
                new Document()
        );
    }

    @Override
    public DataTable table(String name) {
        return dataTableMap.computeIfAbsent(name, __ -> new MongoDataTable(
                this,
                database.getCollection(name)
        ));
    }

    /**
     * Creates a new builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        /* Options */
        private MongoDatabase database;
        private String keyFieldOverride = "_id";

        public Builder connect(MongoDatabase database) {
            this.database = database;
            return this;
        }

        public Builder connect(ThrowingSupplier<MongoDatabase> supplier) {
            return connect(supplier.get());
        }

        public Builder connect(MongoClientSettings settings, String name) {
            return connect(() -> MongoClients.create(settings).getDatabase(name));
        }

        public Builder connect(MongoClientSettings.Builder settingsBuilder, String name) {
            return connect(settingsBuilder.build(), name);
        }

        public Builder connect(String connectionString, String name) {
            return connect(MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(connectionString))
                    .uuidRepresentation(UuidRepresentation.STANDARD)
                    .retryWrites(true)
                    .build(), name);
        }

        public Builder keyFieldOverride(String keyFieldOverride) {
            this.keyFieldOverride = keyFieldOverride;
            return this;
        }

        public MongoDataSource build() {
            MongoDataSource source = new MongoDataSource(
                    database
            );

            source.setKeyFieldOverride(keyFieldOverride);

            return source;
        }
    }

}
