package edu.azure.cosmosdb.sql;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import com.azure.cosmos.ConnectionPolicy;
import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.CosmosAsyncDatabase;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosClientException;
import com.azure.cosmos.implementation.HttpConstants;
import com.azure.cosmos.models.CosmosAsyncContainerResponse;
import com.azure.cosmos.models.CosmosAsyncDatabaseResponse;
import com.azure.cosmos.models.CosmosAsyncItemResponse;
import com.azure.cosmos.models.CosmosContainerProperties;
import com.azure.cosmos.models.FeedOptions;
import com.azure.cosmos.models.PartitionKey;
import com.azure.cosmos.util.CosmosPagedFlux;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import edu.common.JsonReader;
import edu.domain.User;
import lombok.*;
import reactor.core.publisher.Mono;


@Data
public class AzCosmosDBSQLBasics {

    private CosmosAsyncClient client;
    private CosmosAsyncDatabase database;
    private CosmosAsyncContainer container;

    private String accountEndpoint;
    private String accountKey;
    private String databaseName;
    private String containerName;

    public void close() {
        client.close();
    }

    private void basicOperations() throws Exception {
        final Yaml yaml = new Yaml();
        try {
            final InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("remote.yaml");
            final Map<String, Object> object = yaml.load(inputStream);
            this.setAccountEndpoint((String) object.get("accountEndpoint"));
            this.setAccountKey((String) object.get("accountKey"));
            this.setDatabaseName((String) object.get("databaseName"));
            this.setContainerName((String) object.get("containerName"));
        } catch (YAMLException ye) {
            System.out.println("Caught YAML Execption \n");
            ye.printStackTrace();
        }
        System.out.println("connection details : \n\t" + "AccountEndPoint=" + this.getAccountEndpoint()
                + "\n\tAccountKey=" + this.getAccountKey() + "\n\tDatabaseName=" + this.getDatabaseName()
                + "\n\tContainerName=" + this.getContainerName());
        client = new CosmosClientBuilder().endpoint(this.getAccountEndpoint()).key(this.getAccountKey())
                .connectionPolicy(ConnectionPolicy.getDefaultPolicy()).consistencyLevel(ConsistencyLevel.EVENTUAL)
                .buildAsyncClient();

        System.out.println("Created cosmos client - " + client);

        createDatabaseIfNotExists();
        createContainerIfNotExists();

        JsonReader usersReader = new JsonReader();
        List<User> users = usersReader.readUsers("target/classes/users.json");
        System.out.println("Read following users");
        users.stream().forEach(System.out::println);
        for (User user : users) {
            this.createDocumentIfNotExists(user);
        }

        users.stream().forEach(user -> {
            try {
                readItem(user);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });

        queryItems();
        users.get(0).setLastName("Mundaje");
        replaceItem(users.get(0));
        deleteItem(users.get(1));

    }

    public void createDatabaseIfNotExists() throws IOException {
        writeToConsoleAndPromptToContinue(
                "Press any key to check if Database " + databaseName + " exists. Create if it doesn't.");

        Mono<CosmosAsyncDatabaseResponse> databaseIfNotExists = client
                .createDatabaseIfNotExists(this.getDatabaseName());
        databaseIfNotExists.flatMap(gotResponse -> {
            database = gotResponse.getDatabase();
            System.out.println("Check for database " + database.getId() + " finshed. Details below :\n" + database);
            return Mono.empty();
        }).block();

    }

    public void createContainerIfNotExists() throws IOException {
        writeToConsoleAndPromptToContinue(
                "Press any key to check if Container " + containerName + " exists. Create if it doesn't");
        if (database == null) {
            System.out.println("Database invalid!!");
            createDatabaseIfNotExists();
        }
        CosmosContainerProperties containerProperties = new CosmosContainerProperties(containerName, "/UserId");
        Mono<CosmosAsyncContainerResponse> containerIfNotExists = database
                .createContainerIfNotExists(containerProperties);

        containerIfNotExists.flatMap(gotResponse -> {
            container = gotResponse.getContainer();
            System.out.println("Check for container " + container.getId() + " finsihed. Details below: \n" + container);
            return Mono.empty();
        }).block();

    }

    public void createDocumentIfNotExists(User user) throws IOException {
        writeToConsoleAndPromptToContinue("Create user record for user " + user.getUserId());
        if (container == null) {
            System.out.println("Container Invalid");
            createContainerIfNotExists();
        }
        Mono<User> userMono = Mono.just(user);
        final CountDownLatch completionLatch = new CountDownLatch(1);

        // userMono.flatMap(mUser -> {
        // Mono<CosmosAsyncItemResponse<User>> asyncItemResponseMono =
        // container.readItem(mUser.getId(), new PartitionKey(mUser.getUserId()),
        // User.class);
        // return asyncItemResponseMono;
        // })
        // .subscribe(itemResponse ->
        // System.out.println(itemResponse.getItem().getUserId() + " with ID: " +
        // itemResponse.getItem().getId()+" already exists") ,
        // err -> {
        // if(err instanceof CosmosClientException){
        // CosmosClientException cerr = (CosmosClientException)err;
        // if(cerr.getStatusCode() == HttpConstants.StatusCodes.NOTFOUND){
        // System.out.println("User " + user.getUserId() +" doesn't already exist.
        // Proceeding with creating this user");
        // userMono.flatMap(mUser -> container.createItem(mUser))
        // .subscribe(itemResponse ->
        // System.out.println(itemResponse.getItem().getUserId()+ " Created") ,
        // crerr -> {
        // if(crerr instanceof CosmosClientException)
        // ((CosmosClientException)crerr).printStackTrace();
        // else
        // cerr.printStackTrace();
        // completionLatch.countDown();
        // }
        // );
        // } else {
        // cerr.printStackTrace();
        // }
        // } else{
        // err.printStackTrace();
        // }
        // completionLatch.countDown();
        // },
        // () -> {completionLatch.countDown();}

        // );
        userMono.flatMap(mUser -> container.readItem(mUser.getId(), new PartitionKey(mUser.getUserId()), User.class))
                .onErrorResume(
                        err -> err instanceof CosmosClientException
                                && ((CosmosClientException) err).getStatusCode() == HttpConstants.StatusCodes.NOTFOUND,
                        e -> userMono.flatMap(emUser -> container.createItem(emUser)))
                .subscribe(r -> System.out.println("User " + r.getItem().getUserId() + " Created"),
                        e -> e.printStackTrace(), () -> completionLatch.countDown());
        try {
            completionLatch.await();
        } catch (InterruptedException ie) {
            throw new AssertionError("Unexpected Interruption", ie);
        }
    }

    private void readItem(User user) throws IOException {
        writeToConsoleAndPromptToContinue("Read user record for user " + user.getUserId());
        if (container == null) {
            System.out.println("Container Invalid");
            createContainerIfNotExists();
        }
        Mono<User> userMono = Mono.just(user);
        final CountDownLatch completionLatch = new CountDownLatch(1);

        userMono.flatMap(mUser -> container.readItem(mUser.getId(), new PartitionKey(mUser.getUserId()), User.class))
                .subscribe(
                        response -> System.out.println("Successfully read user " + response.getItem().getUserId()
                                + " with request charge " + response.getRequestCharge() + " and latency "
                                + response.getRequestLatency() + " Full User :" + response.getItem()),
                        e -> System.out.println("Error reading response " + e.getMessage()),
                        () -> completionLatch.countDown());

        try {
            completionLatch.await();
        } catch (InterruptedException ie) {
            throw new AssertionError("Unexpected Interruption", ie);
        }
    }

    public void deleteItem(User user) throws IOException {
        writeToConsoleAndPromptToContinue("Delete user record for user " + user.getUserId());
        if (container == null) {
            System.out.println("Container Invalid");
            createContainerIfNotExists();
        }
        Mono<User> userMono = Mono.just(user);
        final CountDownLatch completionLatch = new CountDownLatch(1);
        userMono.flatMap(mUser -> container.deleteItem(mUser.getId(), new PartitionKey(mUser.getUserId()))).subscribe(
                response -> System.out.println("Successfully deleted user " + response),
                e -> System.out.println("Error delteing user :" + e.getMessage() + "\n trace:" + e), () -> completionLatch.countDown());

        try {
            completionLatch.await();
        } catch (InterruptedException ie) {
            throw new AssertionError("Unexpected Interruption", ie);
        }

    }

    public void replaceItem(User user) throws IOException{
        writeToConsoleAndPromptToContinue("Replace user record for user " + user.getUserId());
        if (container == null) {
            System.out.println("Container Invalid");
            createContainerIfNotExists();
        }
        Mono<User> userMono = Mono.just(user);
        final CountDownLatch completionLatch = new CountDownLatch(1);

        userMono.flatMap(mUser -> container.replaceItem(user, user.getId(), new PartitionKey(user.getUserId())))
                .subscribe(
                        response -> System.out.println("Successfully replaced user " + response.getItem().getUserId() 
                                                        + " with request charge = " + response.getRequestCharge() 
                                                        + " and with latency = " + response.getRequestLatency()
                                                        + " \n Full Item = " + response.getItem()
                                                      )
                        ,e -> System.out.println("Error Updating User " + e.getMessage() + " \n trace: " + e)
                        ,() -> completionLatch.countDown()
                        );
                
        try {
            completionLatch.await();
        } catch (InterruptedException ie) {
            throw new AssertionError("Unexpected Interruption", ie);
        }
    }

    private void queryItems() {
        FeedOptions queryOptions = new FeedOptions();

        queryOptions.setPopulateQueryMetrics(true);

        CosmosPagedFlux<User> queryResponse = container.queryItems(
                "SELECT * FROM User WHERE User.Dividend='800.50' ", queryOptions,
                User.class);

        final CountDownLatch completionLatch = new CountDownLatch(1);

        queryResponse.byPage(2)
                .subscribe(response -> System.out.println("Got page of results with  " + response.getResults().size()
                        + " item count and request charge = " + response.getRequestCharge() + "\n Item Ids : "
                        + response.getResults().stream().map(User::getId).collect(Collectors.toList()))

                        , err -> System.out.println("Error Runnig Query : " + err.getMessage() + "\n  trace: " + err),
                        () -> completionLatch.countDown());

        try {
            completionLatch.await();
        } catch (InterruptedException ie) {
            throw new AssertionError("Unexpected Interruption", ie);
        }
    }

    public void writeToConsoleAndPromptToContinue(String message) throws IOException {
        System.out.println(message);
        System.out.println("Press Any Key To Continue...");
        System.in.read();
    }
    public static void main( String[] args )
    {
        AzCosmosDBSQLBasics azCosmosDBSQLBasics = new AzCosmosDBSQLBasics();
        try {
            
            azCosmosDBSQLBasics.basicOperations();
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            azCosmosDBSQLBasics.close();
        }
    }
}
