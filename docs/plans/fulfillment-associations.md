# Bonus — Warehouse ⇄ Product ⇄ Store Fulfilment Associations (Spec / Plan)

Status: **DRAFT**
Branch: `feat/fulfillment-associations` (stacked on `feat/warehouse-operations`)
Scope: the BONUS task — associate `Warehouses` as fulfilment units of certain `Products` for
determined `Stores`.

## 1. Goal
Model and expose the ability to say *"warehouse W fulfils product P for store S"*, enforcing:

1. Each **Product** can be fulfilled by a maximum of **2** different Warehouses **per Store**.
2. Each **Store** can be fulfilled by a maximum of **3** different Warehouses (across all products).
3. Each **Warehouse** can store a maximum of **5** types of Products (across all stores).

## 2. Domain model
New package `com.fulfilment.application.monolith.fulfillment`.

**`Fulfillment`** (`PanacheEntity`, table `fulfillment`) — a link row:
- `@ManyToOne Store store`
- `@ManyToOne Product product`
- `@ManyToOne DbWarehouse warehouse` (the active warehouse row)
- Unique constraint on `(store_id, product_id, warehouse_id)` — a triple is registered once.

Warehouses are referenced by **business unit code** at the API boundary and resolved to the
active `DbWarehouse` (via `WarehouseRepository.findByBusinessUnitCode`). Stores and products are
referenced by their numeric **id**.

## 3. Constraint semantics (distinct counts)
When registering `(S, P, W)`:
- **Duplicate**: if `(S, P, W)` already exists → **409**.
- **Rule 1**: number of *distinct warehouses* already fulfilling `(S, P)` must be `< 2`
  (adding a new warehouse to that pair must not make it 3).
- **Rule 2**: number of *distinct warehouses* already fulfilling store `S` must be `< 3` — unless
  `W` already fulfils `S` for some product (then it's not a new distinct warehouse).
- **Rule 3**: number of *distinct products* already stored by warehouse `W` must be `< 5` — unless
  `P` is already stored by `W` (not a new distinct product).

## 4. Components
- **`FulfillmentRepository`** (`PanacheRepository<Fulfillment>`) — persistence + count/exists
  queries:
  - `exists(store, product, warehouse)`
  - `countDistinctWarehousesForStoreAndProduct(store, product)`
  - `countDistinctWarehousesForStore(store)` / `isWarehouseServingStore(store, warehouse)`
  - `countDistinctProductsForWarehouse(warehouse)` / `isProductInWarehouse(product, warehouse)`
- **Domain exceptions** + mapper: `FulfillmentNotFoundException` (404),
  `DuplicateFulfillmentException` (409), `FulfillmentLimitExceededException` (409), a
  `FulfillmentException` base carrying the status, and a `FulfillmentExceptionMapper`.
- **`FulfillmentService`** (`@ApplicationScoped`) — resolves the entities (404 if missing),
  enforces §3, persists. Transaction boundary at the REST layer.
- **`FulfillmentResource`** (`@Path("fulfillment")`):
  | Method & path | Success | Errors |
  |---|---|---|
  | `POST /fulfillment` (body: storeId, productId, warehouseBusinessUnitCode) | 201 | 400 / 404 / 409 |
  | `GET /fulfillment` | 200 list | — |
  | `GET /fulfillment/{id}` | 200 | 404 |
  | `DELETE /fulfillment/{id}` | 204 | 404 |

  Request/response are small DTO records; the response exposes the fulfilment id plus store id,
  product id, and warehouse business unit code.

## 5. Error mapping
- Missing store/product/warehouse → **404**.
- Duplicate association → **409**.
- Any of the 3 limits exceeded → **409**.
- Missing/blank request fields → **400**.

## 6. Testing
- **Service unit tests** (mock repository + store/product/warehouse lookups): happy path, each of
  the 3 limits (at boundary: allowed at limit-1, rejected at limit), duplicate, missing entities,
  and the "already-serving / already-stored" exemptions for rules 2 & 3.
- **REST integration tests** (`@QuarkusTest` + H2 + RestAssured): create 201, list, get 200/404,
  delete 204/404, and 409 for a limit breach.

## 7. Out of scope
- Re-pointing fulfilments when a warehouse is replaced/archived (lifecycle beyond this task).
- Any UI.

## 8. Implementation order
1. `Fulfillment` entity + `FulfillmentRepository`.
2. Exceptions + `FulfillmentService` (constraint logic) + unit tests.
3. `FulfillmentResource` + mapper + REST tests.
