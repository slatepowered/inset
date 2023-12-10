package example.slatepowered.inset;

import lombok.ToString;
import slatepowered.inset.DataManager;
import slatepowered.inset.cache.DataCache;
import slatepowered.inset.codec.CodecRegistry;
import slatepowered.inset.datastore.DataItem;
import slatepowered.inset.datastore.Datastore;
import slatepowered.inset.operation.Sorting;
import slatepowered.inset.mongodb.MongoDataSource;
import slatepowered.inset.query.*;
import slatepowered.inset.reflective.Key;
import slatepowered.inset.reflective.ReflectiveCodecFactory;
import slatepowered.inset.source.DataTable;

import java.util.Arrays;
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

    @ToString
    public static class PartialStats {
        @Key
        UUID uuid;

        protected Integer deaths;
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
        FindOperation<UUID, Stats> queryStatus1 = datastore.findOne(Query.byKey(key));
        queryStatus1.then(result /* = queryStatus1 */ -> {
            // This is called if the query succeeds, this doesn't mean
            // it would've found anything it just means no errors occurred

            if (result.isAbsent()) { // No item found in database or locally
//                System.out.println("Could not find item, inserting new item with key: " + key);
                datastore.getOrCreate(key).ifPresent(stats -> stats.deaths++)
//                        .saveSync()
                        ;
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
                .then(result -> result.ifPresentUse(item -> /* System.out.println(item.get().uuid) */ { }))
                .exceptionally(result -> result.errorAs(Throwable.class).printStackTrace());

        // Load 7 random items into the cache
        datastore.findAll(Query.all())
                .await()
                .limit(7)
                .stream()
                .forEach(FoundItem::fetch);

        // Randomize cache values
        datastore.getDataCache().forEach(item -> item.get().deaths += (int)(Math.random() * 10));

        // Try to get sorted list of items including locally cached values
        datastore.findAll(Query.all(), FindAllOperation.Options.builder().useCaches(true).build())
                .await()
                .throwIfFailed()
                .projection(PartialStats.class)
                .sort(Sorting.builder().descend("deaths").build())
                .stream()
                .forEachOrdered(foundItem -> System.out.println(foundItem.fetch()));

        dataManager.await();
    }

}
