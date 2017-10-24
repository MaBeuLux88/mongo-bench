package fr.polux;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.WriteModel;
import org.bson.Document;

import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;

public class Main {

    /**
     * Need Mongo in standalone build or docker.
     * docker run -d -p 27017:27017 mongo:3.4.9
     *
     * Login arguments: username, password, databasename
     */
    public static void main(String[] args) throws UnknownHostException {
        MongoCredential credential = null;
        try {
            String userName = args[0];
            String password = args[1];
            String databaseName = args[2];
            credential = MongoCredential.createCredential(userName, databaseName, password.toCharArray());
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println ("no login passed, or missing one argument, switching to no auth connection");
        }
        ServerAddress address = new ServerAddress(InetAddress.getLocalHost(), 27017);

        // without this timeout, it takes 30 seconds to get the exception...
        MongoClientOptions options = MongoClientOptions.builder().serverSelectionTimeout(5000).build();

        MongoClient mongoClient = ofNullable(credential)
          .map(c -> new MongoClient(address, singletonList(c), options))
          .orElse(new MongoClient(address));

        MongoCollection<Document> collection = mongoClient.getDatabase("test").getCollection("testcol");

        AtomicInteger ai = new AtomicInteger();
        ExecutorService executorService = Executors.newFixedThreadPool(8);
        List<Future<?>> futures = new ArrayList<>();
        for (int l = 0; l < 4; l++) {
            futures.add(executorService.submit(() -> {
                for (int k = 0; k < 1000; k++) {

                    ArrayList<WriteModel<Document>> list = new ArrayList<>();
                    for (int i = 0; i < 500; i++) {
                        Document document = new Document();
                        int ID = ai.getAndIncrement();
                        // document.put("_id", ID);
                        for (int j = 0; j < 80; j++) {
                            String key = j + "" + Math.random() + "LHGHGHJIFLDSHFLJFHLDSHFLDSKJHFLS";
                            document.put(key, key);
                        }
                        UpdateOneModel<Document> doc = new UpdateOneModel<>(Filters.eq("_id", ID),
                                                                            new Document("$set", document),
                                                                            new UpdateOptions().upsert(true));
                        list.add(doc);
                    }

                    BulkWriteResult bulkWriteResult = collection.bulkWrite(list, new BulkWriteOptions().ordered(false));
                    System.out.println(" Done     : " + bulkWriteResult.wasAcknowledged());
                    System.out.println(" Matched  : " + bulkWriteResult.getMatchedCount());
                    System.out.println(" Inserted : " + bulkWriteResult.getInsertedCount());
                    System.out.println(" Updated  : " + bulkWriteResult.getModifiedCount());
                    System.out.println(" Upserts  : " + bulkWriteResult.getUpserts().size());
                }
            }));
        }
        executorService.shutdown();
        futures.forEach(f -> {
            try {
                f.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });

        System.exit(0);
    }
}
