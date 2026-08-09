# Java Code Assignment

This is a short code assignment that explores various aspects of software development, including API implementation, documentation, persistence layer handling, and testing.

## About the assignment

You will find the tasks of this assignment on [CODE_ASSIGNMENT](CODE_ASSIGNMENT.md) file

## Implementation overview

All four tasks are implemented. Design notes for the larger pieces live under
[`docs/plans/`](../docs/plans).

- **Task 1 — Location:** `LocationGateway.resolveByIdentifier` resolves a location by identifier
  (null-safe, trims input); the gateway is a CDI bean so it can be injected as a `LocationResolver`.
- **Task 2 — Store:** the `LegacyStoreManagerGateway` sync now happens only *after* the database
  transaction commits. `StoreResource` fires a `StoreSyncEvent`; a listener observes it during
  `TransactionPhase.AFTER_SUCCESS`, so a rolled-back change is never propagated.
- **Task 3 — Warehouse:** create / retrieve / replace / archive, built in a hexagonal style
  (REST adapter → use cases → ports → repository, with a domain model mapped to a `DbWarehouse`
  entity). All the required validations are enforced in the use cases and surfaced as structured
  JSON errors via a `WarehouseExceptionMapper`.
- **Bonus — Fulfilment:** associate a `Warehouse` as a fulfilment unit of a `Product` for a
  `Store`, enforcing at most 2 warehouses per (store, product), 3 warehouses per store, and 5
  product types per warehouse.

### Endpoints

| Method & path | Description |
|---|---|
| `GET /warehouse` | List active warehouses |
| `POST /warehouse` | Create a warehouse (201) |
| `GET /warehouse/{id}` | Get an active warehouse by numeric id |
| `DELETE /warehouse/{id}` | Archive a warehouse by numeric id (204) |
| `POST /warehouse/{businessUnitCode}/replacement` | Replace the active warehouse for a business unit code |
| `GET/POST /store`, `GET/PUT/PATCH/DELETE /store/{id}` | Store CRUD |
| `GET/POST /product`, `GET/PUT/DELETE /product/{id}` | Product CRUD |
| `GET/POST /fulfillment`, `GET/DELETE /fulfillment/{id}` | Manage fulfilment associations |

Validation failures return `400`, unknown resources `404`, and conflicts (duplicate business unit
code, already-archived warehouse, duplicate/over-limit fulfilment) `409`.

## About the code base

This is based on https://github.com/quarkusio/quarkus-quickstarts

### Requirements

To compile and run this demo you will need:

- JDK 17+

In addition, you will need either a PostgreSQL database, or Docker to run one.

### Configuring JDK 17+

Make sure that `JAVA_HOME` environment variables has been set, and that a JDK 17+ `java` command is on the path.

## Building the demo

Execute the Maven build on the root of the project:

```sh
./mvnw package
```

## Running the demo

### Live coding with Quarkus

The Maven Quarkus plugin provides a development mode that supports
live coding. To try this out:

```sh
./mvnw quarkus:dev
```

In this mode you can make changes to the code and have the changes immediately applied, by just refreshing your browser.

    Hot reload works even when modifying your JPA entities.
    Try it! Even the database schema will be updated on the fly.

## (Optional) Run Quarkus in JVM mode

When you're done iterating in developer mode, you can run the application as a conventional jar file.

First compile it:

```sh
./mvnw package
```

Next we need to make sure you have a PostgreSQL instance running (Quarkus automatically starts one for dev and test mode). To set up a PostgreSQL database with Docker:

```sh
docker run -it --rm=true --name quarkus_test -e POSTGRES_USER=quarkus_test -e POSTGRES_PASSWORD=quarkus_test -e POSTGRES_DB=quarkus_test -p 15432:5432 postgres:13.3
```

Connection properties for the Agroal datasource are defined in the standard Quarkus configuration file,
`src/main/resources/application.properties`.

Then run it:

```sh
java -jar ./target/quarkus-app/quarkus-run.jar
```
    Have a look at how fast it boots.
    Or measure total native memory consumption...


## Running the tests

The test suite (JUnit / `@QuarkusTest`) and the JVM integration test run against a real
**PostgreSQL** — the same engine as production. Where Docker Dev Services are available, Quarkus
starts a throwaway database automatically; otherwise start one yourself (the `%test` and `%prod`
datasources expect it on `localhost:15432`).

1. Start PostgreSQL:

   ```sh
   docker compose -f docker-compose-test.yml up -d
   ```

   (equivalently: `docker run -it --rm --name quarkus_test -e POSTGRES_USER=quarkus_test -e POSTGRES_PASSWORD=quarkus_test -e POSTGRES_DB=quarkus_test -p 15432:5432 postgres:16-alpine`)

2. Run the tests:

   ```sh
   ./mvnw test        # unit + @QuarkusTest tests
   ./mvnw verify      # the above, plus the WarehouseEndpointIT integration test
   ```

   Run a single test class:

   ```sh
   ./mvnw test -Dtest=WarehouseResourceImplTest
   ```

3. A code-coverage report (JaCoCo) is generated at `target/jacoco-report/index.html`.

4. Stop the database when finished:

   ```sh
   docker compose -f docker-compose-test.yml down
   ```


## See the demo in your browser

Navigate to:

<http://localhost:8080/index.html>

Have fun, and join the team of contributors!

## Troubleshooting

Using **IntelliJ**, in case the generated code is not recognized and you have compilation failures, you may need to add `target/.../jaxrs` folder as "generated sources".