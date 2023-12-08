package example.slatepowered.inset;

import lombok.ToString;
import slatepowered.inset.DataManager;
import slatepowered.inset.cache.DataCache;
import slatepowered.inset.codec.CodecRegistry;
import slatepowered.inset.datastore.DataItem;
import slatepowered.inset.datastore.Datastore;
import slatepowered.inset.modifier.Sorting;
import slatepowered.inset.mongodb.MongoDataSource;
import slatepowered.inset.query.FoundItem;
import slatepowered.inset.query.Query;
import slatepowered.inset.query.FindStatus;
import slatepowered.inset.reflective.Key;
import slatepowered.inset.reflective.ReflectiveCodecFactory;
import slatepowered.inset.source.DataTable;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ForkJoinPool;

public class MongoDatastoreExample {

    @ToString
    public static class Stats {
        @Key
        protected UUID uuid;

        // no support for primitives yet bc im too
        // lazy to make that work with Unsafe rn
        protected Integer kills = 0;
        protected Integer deaths = 0;
    }

    public static void main(String[] args) throws InterruptedException {
        // Create the data manager
        DataManager dataManager = DataManager.builder()
                .executorService(ForkJoinPool.commonPool())
                .codecRegistry(new CodecRegistry(ReflectiveCodecFactory.builder().build()))
                .build();

        // Connect to the data source (database)
        MongoDataSource dataSource = MongoDataSource.builder(dataManager)
                .keyFieldOverride("_id")
                .connect(System.getProperty("test.slatepowered.inset.mongoUri"), "test")
                .build();
        DataTable dataTable = dataSource.table("test");

        // Create the datastore
        Datastore<UUID, Stats> datastore = dataManager.datastore(UUID.class, Stats.class)
                .sourceTable(dataTable)
                .dataCache(DataCache.doubleBackedConcurrent())
                .build();

        // Load an item by key
        final UUID key = UUID.randomUUID();
        FindStatus<UUID, Stats> queryStatus1 = datastore.findOne(Query.byKey(key));
        queryStatus1.then(result /* = queryStatus1 */ -> {
            // This is called if the query succeeds, this doesn't mean
            // it would've found anything it just means no errors occurred

            if (result.isAbsent()) { // No item found in database or locally
                System.out.println("Could not find item, inserting new item with key: " + key);
                datastore.getOrCreate(key).ifPresent(stats -> stats.deaths++).saveSync();
                return;
            }

            if (result.wasCached()) { // Item was cached

            }

            if (result.wasFetched()) { // Item was loaded from the database

            }

            // ensure the item is loaded again if it was cached
            result.fetchSyncIfCached();

            DataItem<UUID, Stats> item = result.item();

            Optional<Stats> statsOptional = item.optional();
            Stats stats = item.get();
            UUID itemKey = item.key(); // should be the same as the above defined `key` value in this case

            stats.deaths++;
            item.saveAsync().whenComplete((__, err) -> {
                System.out.println("saved data n shit");
            });
        });

        datastore.findOne(Query.builder()
                .eq("kills", 0)
                .eq("deaths", 1)
                .build()
        )
                .then(result -> result.ifPresentUse(item -> System.out.println(item.get().uuid)))
                .exceptionally(result -> result.errorAs(Throwable.class).printStackTrace());

        datastore.findAll(Query.all())
                .await()
                .sort(Sorting.builder().descend("deaths").build())
                .throwIfFailed()
                .stream()
                .map(FoundItem::fetch)
                .forEach(System.out::println);

        dataManager.await();
    }

}
