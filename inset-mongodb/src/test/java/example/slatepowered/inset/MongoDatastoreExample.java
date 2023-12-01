package example.slatepowered.inset;

import slatepowered.inset.DataManager;
import slatepowered.inset.codec.CodecRegistry;
import slatepowered.inset.datastore.DataItem;
import slatepowered.inset.datastore.Datastore;
import slatepowered.inset.mongodb.MongoDataSource;
import slatepowered.inset.query.Query;
import slatepowered.inset.query.QueryStatus;
import slatepowered.inset.reflective.Key;
import slatepowered.inset.reflective.ReflectiveCodecFactory;
import slatepowered.inset.source.DataTable;

import java.util.Optional;
import java.util.UUID;

public class MongoDatastoreExample {

    public class Stats {
        @Key
        protected UUID uuid;

        // no support for primitives yet bc im too
        // lazy to make that work with Unsafe rn
        protected Integer kills = 0;
        protected Integer deaths = 0;
    }

    public static void main(String[] args) {
        // Create the data manager
        DataManager dataManager = DataManager.builder()
                .codecRegistry(new CodecRegistry(ReflectiveCodecFactory.builder().build()))
                .build();

        // Connect to the data source (database)
        MongoDataSource dataSource = MongoDataSource.builder()
                .keyFieldOverride("_id")
                .connect("", "test-db")
                .build();
        DataTable dataTable = dataSource.table("test");

        // Create the datastore
        Datastore<UUID, Stats> datastore = dataManager.createDatastore(dataTable, UUID.class, Stats.class);

        // Load an item by key
        QueryStatus<UUID, Stats> queryStatus1 =
                datastore.find(Query.key(UUID.randomUUID()));
        queryStatus1.then(result /* = queryStatus1 */ -> {
            // This is called if the query succeeds, this doesn't mean
            // it would've found anything it just means no errors occurred

            if (result.absent()) { // No item found in database or locally
                /* same as */ if (!result.present()) { /* ... */ }

                System.out.println("Could not find item");
                return;
            }

            if (result.found()) { // Item was cached

            }

            if (result.loaded()) { // Item was loaded from the database

            }

            // ensure the item is loaded again if it was cached
            result.pullSyncIfCached();

            DataItem<UUID, Stats> item = result.item();

            Optional<Stats> statsOptional = item.optional();
            Stats stats = item.get();
            UUID key = item.key();

            stats.deaths++;
            item.saveAsync().whenComplete((__, err) -> {
                System.out.println("saved data n shit");
            });
        });
    }

}
