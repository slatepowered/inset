package example.slatepowered.inset;

import slatepowered.inset.DataManager;
import slatepowered.inset.codec.CodecRegistry;
import slatepowered.inset.mongodb.MongoDataSource;
import slatepowered.inset.reflective.ReflectiveCodecFactory;

public class MongoDatastoreExample {

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

        // Conn
    }

}
