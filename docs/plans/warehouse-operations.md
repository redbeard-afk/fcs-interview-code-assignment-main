# Task 3 — Warehouse Operations (Spec / Plan)

Status: **DRAFT — awaiting review**
Branch: `feat/warehouse-operations`
Scope: Task 3 "Warehouse (Must have)" only. The bonus task (Product/Store/Warehouse
fulfilment associations) is **out of scope**.

---

## 1. Goal

Implement the API endpoint handlers and domain use cases for all Warehouse operations —
**create, retrieve (single + list), replace, archive** — with the business validations
required by `CODE_ASSIGNMENT.md`, returning proper HTTP responses.

Today these are stubs that throw `UnsupportedOperationException` (REST impl, repository,
and the three use cases).

---

## 2. Architecture (hexagonal — already scaffolded)

```
REST adapter (in)            Domain (use cases + ports)              Adapters (out)
--------------------         ----------------------------           ------------------------
WarehouseResourceImpl  --->  CreateWarehouseOperation   (port in)
  (com.warehouse.api           implemented by CreateWarehouseUseCase --> WarehouseStore (port out)
   generated interface)      ReplaceWarehouseOperation  (port in)         implemented by
                               implemented by ReplaceWarehouseUseCase       WarehouseRepository (Panache)
                             ArchiveWarehouseOperation  (port in)
                               implemented by ArchiveWarehouseUseCase --> LocationResolver (port out)
                                                                            implemented by
                                                                            LocationGateway (Task 1)
```

Design intent: **the REST adapter depends on the domain *ports* (the `*Operation`
interfaces)**, not on the repository, for the mutating operations. It may use the
repository (via `WarehouseStore`) directly for read-only list/get.

---

## 3. Domain model & invariants

`Warehouse` (domain): `businessUnitCode` (unique id), `location`, `capacity`, `stock`,
`createdAt`, `archivedAt`.
`Location`: `identification`, `maxNumberOfWarehouses`, `maxCapacity`
(the comment defines `maxCapacity` as *"maximum capacity of the location summing all the
warehouse capacities"*).

**Core invariant:** at most **one active** warehouse per `businessUnitCode` at any time.
"Active" = `archivedAt == null`. Replace reuses the business unit code: it archives the
active one and creates a new active one under the same code, preserving history.

Because of this, all lookups by business unit code resolve the **active** record.

---

## 4. Business rules

### 4.1 Create (`POST /warehouse`)
Validated in `CreateWarehouseUseCase.create(Warehouse)` before persisting:

1. **Business Unit Code uniqueness** — no active warehouse already exists with this
   `businessUnitCode`.
2. **Location validity** — `locationResolver.resolveByIdentifier(location)` returns a
   location (not null).
3. **Creation feasibility** — number of active warehouses at that location is
   `< location.maxNumberOfWarehouses`.
4. **Capacity vs. location** — `sum(active capacities at location) + new.capacity <=
   location.maxCapacity`.
5. **Stock vs. capacity** — `new.stock <= new.capacity` ("can handle the stock informed").

On success: set `createdAt = now`, `archivedAt = null`, then `warehouseStore.create(...)`.

### 4.2 Replace (`POST /warehouse/{businessUnitCode}/replacement`)
Handled in `ReplaceWarehouseUseCase.replace(Warehouse newWarehouse)` (business unit code
comes from the path and is applied to `newWarehouse`):

1. **Target exists** — an active warehouse with `businessUnitCode` must exist (else 404).
2. **Location validity** — new warehouse location must be valid.
3. **Capacity accommodation** — `new.capacity >= old.stock` (new can hold the stock being
   carried over).
4. **Stock matching** — `new.stock == old.stock`.
5. **Capacity vs. location** — recheck location capacity after removing the old warehouse's
   contribution: `sum(active capacities at location excluding old) + new.capacity <=
   location.maxCapacity`.

Flow (single transaction): **archive the old** (`archivedAt = now`) → **create the new**
(same `businessUnitCode`, `createdAt = now`). Ordering matters: archiving first frees the
business-unit-code uniqueness and the old capacity for the create checks.

### 4.3 Archive (`DELETE /warehouse/{id}`)
`ArchiveWarehouseUseCase.archive(Warehouse)`: set `archivedAt = now` and persist via
`warehouseStore.update(...)`. 404 if the target does not exist. Returns 204.

### 4.4 Retrieve
- `GET /warehouse` — list all (already implemented via `WarehouseStore.getAll()`).
- `GET /warehouse/{id}` — return the warehouse or 404.

---

## 5. Component changes

### 5.1 `WarehouseRepository` (out adapter — implements `WarehouseStore`)
Implement the four stubbed methods using Panache:
- `create(Warehouse)` — map domain → new `DbWarehouse`, `persist`.
- `update(Warehouse)` — find the active `DbWarehouse` by business unit code, copy mutable
  fields (`location`, `capacity`, `stock`, `archivedAt`), rely on dirty checking.
- `remove(Warehouse)` — hard-delete the matching `DbWarehouse` (kept for completeness).
- `findByBusinessUnitCode(String)` — return the **active** warehouse
  (`businessUnitCode = ?1 and archivedAt is null`), or null.

Add a small helper for the location-capacity/count queries used by the use cases — see §6
for whether this lives on the port or as `getAll()` filtering.

`DbWarehouse` gains a `fromWarehouse(...)`/apply helper (or the mapping is done inline in
the repository).

### 5.2 Use cases
- Inject `LocationResolver` into `CreateWarehouseUseCase` and `ReplaceWarehouseUseCase`
  (constructor injection; `LocationGateway` is `@ApplicationScoped` since Task 1).
- Implement the validation logic in §4. Validation failures throw a **domain exception**
  (see §7), not a JAX-RS type, to keep the domain framework-agnostic.

### 5.3 `WarehouseResourceImpl` (in adapter)
- Inject the three operation ports + `WarehouseStore` (for list/get).
- Map `com.warehouse.api.beans.Warehouse` (REST bean) ⇄ domain `Warehouse`.
- Wire each endpoint to its use case; translate domain exceptions → HTTP status.
- Ensure correct status codes (201 on create — see §7 open question on how the generated
  interface expresses this).

### 5.4 Transactions
Mutating endpoints (`create`, `archive`, `replace`) run in a single transaction.
Proposed boundary: `@Transactional` on the `WarehouseResourceImpl` handler methods (the
entry point), so replace's archive+create commit atomically.

---

## 6. REST contract (from `warehouse-openapi.yaml`)

| Method & path | Handler | Success | Errors |
|---|---|---|---|
| `GET /warehouse` | `listAllWarehousesUnits` | 200 list | — |
| `POST /warehouse` | `createANewWarehouseUnit` | 201 created | 400 invalid |
| `GET /warehouse/{id}` | `getAWarehouseUnitByID` | 200 | 404 not found |
| `DELETE /warehouse/{id}` | `archiveAWarehouseUnitByID` | 204 | 404 not found |
| `POST /warehouse/{businessUnitCode}/replacement` | `replaceTheCurrentActiveWarehouse` | 200 | 404, 400 |

---

## 7. Open questions / decisions (please confirm)

1. **What is `{id}` in GET/DELETE `/warehouse/{id}`?**
   The replace endpoint uses `{businessUnitCode}` explicitly, while get/delete say `{id}`
   (OpenAPI example `"456"`, numeric). Two readings:
   - **(A, recommended)** `{id}` = `businessUnitCode` — matches the only lookup port we
     have (`findByBusinessUnitCode`) and the domain's notion of identity; archive/replace
     are then consistent (all keyed by business unit code).
   - **(B)** `{id}` = the DB primary key (`DbWarehouse.id`) — matches the numeric example,
     but needs a new find-by-id path and archives a specific historical row.

2. **`maxCapacity` interpretation** — sum of all active warehouse capacities at the
   location (**recommended**, per the field comment) vs. a simple per-warehouse cap
   (`warehouse.capacity <= location.maxCapacity`). Note the seed data (`MWH.001`, cap 100
   at `ZWOLLE-001` whose `maxCapacity` is 40) already violates either reading, so seeds are
   treated as grandfathered/illustrative.

3. **Error status for validation failures** — the OpenAPI only defines `400`. Duplicate
   business unit code is arguably a `409 Conflict`, but to honor the contract I propose
   **400 for all validation failures** and **404 for missing target**. OK?

4. **Exception → HTTP mapping** — add domain exceptions (`WarehouseValidationException`,
   `WarehouseNotFoundException`) and translate them in the REST adapter (or via a dedicated
   `ExceptionMapper`). A global `ExceptionMapper<Exception>` already exists in the `stores`
   package; I'd add a warehouse-specific mapper rather than lean on that. OK?

5. **Port extension vs. filtering** — location count/capacity checks need "active
   warehouses at location X". Options: add a method to `WarehouseStore`
   (cleaner/efficient) or filter `getAll()` in the use case (no port change). Leaning
   toward a **small port addition** for clarity and to avoid loading all rows. OK?

---

## 8. Testing strategy

- **Use case unit tests** (mock `WarehouseStore` + `LocationResolver`): every rule in §4,
  both pass and fail paths (dup BU code, invalid location, max-warehouses reached,
  capacity-exceeds-location, stock>capacity; replace: not-found, capacity accommodation,
  stock mismatch; archive: sets `archivedAt`).
- **Repository integration tests** (`@QuarkusTest` + H2): create/find/update/remove and
  active-only `findByBusinessUnitCode`.
- **REST integration tests** (`@QuarkusTest` + RestAssured): each endpoint, success +
  error codes (201/200/204/400/404), and replace archive-then-create behaviour.
- Aim for high coverage on the touched `warehouses` package (branches included), per the
  project's incremental-coverage practice.

---

## 9. Out of scope
- Bonus task (fulfilment associations between Warehouses, Products, Stores).
- Changes to `stores`/`location` packages beyond what Task 3 requires.

---

## 10. Implementation order (once approved)
1. `WarehouseRepository` + `DbWarehouse` mapping (+ port method if chosen).
2. Domain exceptions.
3. `CreateWarehouseUseCase` (+ tests).
4. `ArchiveWarehouseUseCase` (+ tests).
5. `ReplaceWarehouseUseCase` (+ tests).
6. `WarehouseResourceImpl` wiring + exception mapping (+ REST tests).
7. Full suite + coverage pass.
