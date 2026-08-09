# Questions

Here we have 3 questions related to the code base for you to answer. It is not about right or wrong, but more about what's the reasoning behind your decisions.

1. In this code base, we have some different implementation strategies when it comes to database access layer and manipulation. If you would maintain this code base, would you refactor any of those? Why?

**Answer:**
```
Yes I'd refactor, but incrementally and to bring the codebase to consistency

The codebase mixes three data-access styles for essentially the same job: Active Record used directly in the resource (Store, via Store.findById in StoreResource), the repository pattern (Product/ProductRepository), and full hexagonal ports-and-adapters with domain/entity separation (warehouses). I'd converge on the repository pattern as the default as it is consistent and mockable (Active Record's static calls are hard to unit-test), keeping the use-case layer only where there's real domain logic like warehouses. I'd also consolidate the duplicated ExceptionMapper<Exception> classes in StoreResource/ProductResource.

```
----
2. When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded directly everything. What would be your thoughts about what are the pros and cons of each approach and what would be your choice?

**Answer:**
```txt
Spec-first / contract-first (Warehouse — OpenAPI YAML → generated WarehouseResource)
- Pros: the contract is a single source of truth, language-agnostic; the API is designed deliberately up front; you can generate clients, server stubs, docs and mocks from the same file; producer and consumers stay in agreement which would be valuable for public or multi-team APIs.
- Cons: codegen friction and less flexibility — e.g. the generated interface fixes the return type to the bean, so @ResponseStatus was ignored and I had to set 201 via the Vert.x response; extra build/IDE setup (the README even documents marking target/.../jaxrs as generated sources); and bean↔domain mapping boilerplate. This is too much overhead for tiny internal endpoints.

Code-first (Product/Store — handwritten resources)
- Pros: fast, flexible, minimal ceremony, full control over responses/status; ideal for small, internal, fast-evolving endpoints. And it does give us the option to emit an OpenAPI doc from the code
- Cons: no enforced contract which means a hand-maintained spec drifts from reality, difficult development when multiple teams are oworking on the project and difficult integration

I would try to pick one approach and apply it consistently. For an externally-consumed or multi-team API I'd go spec-first; for an internal service like this I'd lean code-first with an auto-generated OpenAPI document (contract without the codegen rigidity), and in either case introduce dedicated DTOs so entities don't leak through the API.
```
----
3. Given the need to balance thorough testing with time and resource constraints, how would you prioritize and implement tests for this project? Which types of tests would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
```txt
When I worked on this project I focused my testing energy on the parts that actually carry business rules, mainly the warehouse creation, replacement and archive logic and the fulfilment limits, because that is where mistakes are most likely and most expensive. Most of my tests are plain unit tests on the use cases with the ports mocked, since they run fast, need no database and let me check every rule and boundary precisely. On top of that I added a smaller set of integration tests that boot the app against a real Postgres to confirm the things unit tests cannot see, like the persistence queries, the transaction behaviour and the HTTP status codes coming back from the endpoints. I did not spend much effort testing trivial getters or mapping code, and I tried to assert each rule only once at the cheapest level that proves it. To keep coverage useful over time I added tests alongside every change rather than chasing a number afterwards, kept the test database the same engine as production so the results stay trustworthy, and used the coverage report as a hint about missed branches instead of treating it as the goal.

```