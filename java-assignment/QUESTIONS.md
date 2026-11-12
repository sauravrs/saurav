# Questions

## 1. In this code base, we have some different implementation strategies when it comes to database access layer and manipulation. If you would maintain this code base, would you refactor any of those? Why?

**Answer:**

Yes. I would standardize the persistence approach around repositories/ports instead of mixing Panache Active Record (`Store`) and repository-based access (`Product` and `Warehouse`).

For a maintainable enterprise application, I would prefer:

- Resource/controller -> application use case/service -> repository port -> persistence adapter.
- Keep JPA/Panache entities inside the persistence adapter where practical.
- Keep business rules in the application/domain layer rather than in REST resources.
- Use transactions at the application/use-case boundary.
- Return DTOs from APIs instead of exposing persistence entities directly.

The current `ProductRepository` approach is already closer to this model. I would gradually move `Store` away from static Active Record calls and introduce a `StoreRepository`.

I would also avoid generic `persist()` calls for updates when the entity is already managed, and I would make query intent explicit, especially for the warehouse history use case where an active record and archived records share the same Business Unit Code.

The goal is not to refactor everything at once. I would first standardize the code that is changing frequently and leave stable code untouched until there is a business reason to change it.

---

## 2. When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded directly everything. What would be your thoughts about what are the pros and cons of each approach and what would be your choice?

**Answer:**

I would prefer contract-first OpenAPI for public or integration-heavy APIs.

**OpenAPI/contract-first advantages:**
- The API contract is explicit and reviewable.
- Frontend and other teams can work against a stable contract.
- Request/response models and documentation can be generated.
- It reduces accidental differences between documentation and implementation.
- It works well for API governance and versioning.

**Disadvantages:**
- Generated code adds build complexity.
- Developers need discipline around generated sources.
- Small API changes can require regeneration.
- Poorly designed specifications can produce inconvenient generated interfaces.

**Code-first advantages:**
- Faster for small internal APIs.
- Less tooling and generation overhead.
- Developers can iterate directly in Java.

**Disadvantages:**
- Documentation can drift from implementation.
- API design may become coupled to implementation details.
- Cross-team consumers get less of a stable contract.

My choice would be contract-first OpenAPI for APIs consumed by multiple teams or external systems, and code-first can be acceptable for small internal/private APIs. In either case, I would keep DTOs and API versioning explicit and automate contract validation in CI.

---

## 3. Given the need to balance thorough testing with time and resource constraints, how would you prioritize and implement tests for this project? Which types of tests would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**

I would use a risk-based testing pyramid.

**First priority: unit tests for business rules**
- Location resolution.
- Duplicate active Business Unit Code.
- Invalid location.
- Maximum warehouse count.
- Capacity and stock validation.
- Replacement capacity accommodation.
- Replacement stock matching.
- Archive behavior.

These tests are fast and cover the highest-value logic.

**Second priority: API/integration tests**
- Create warehouse returns 201.
- Invalid input returns 400.
- Missing warehouse returns 404.
- Archive returns 204.
- Archived warehouses are not returned in the active list.
- Replacement archives the old record and creates the new active record.
- Store legacy synchronization happens only after a successful transaction.

**Third priority: persistence tests**
Verify repository queries, especially active-versus-archived warehouse history and Business Unit Code reuse.

I would also add negative-path tests because most business defects occur around validation and failure handling.

To keep coverage effective over time, I would run tests in CI for every pull request, keep unit tests deterministic, use coverage as a signal rather than a target, and require tests for every new business rule or production bug fix. For critical flows, I would also add contract tests against the OpenAPI specification.

