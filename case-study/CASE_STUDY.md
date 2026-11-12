# Case Study Scenarios to discuss

## Scenario 1: Cost Allocation and Tracking

**Situation:** The company needs to track and allocate costs accurately across different Warehouses and Stores. The costs include labor, inventory, transportation, and overhead expenses.

**Task:** Discuss the challenges in accurately tracking and allocating costs in a fulfillment environment.

**Questions you may have and considerations:**

- First I would clarify the cost model: what is the source of truth for labor, inventory, transportation and overhead, and what is the required cost granularity (warehouse, store, product, shipment, business unit)?
- Define allocation rules. For example, transportation may be allocated by shipment weight/volume, labor by hours worked, and overhead by a configured percentage or activity driver.
- Separate actual, committed and estimated costs so reporting does not mix financial states.
- Define a unique business unit/warehouse identifier and effective dates so historical costs remain attributable to the correct operation even after a warehouse is replaced.
- Identify the frequency and latency requirements: real-time operational visibility versus daily/monthly financial close.
- Handle corrections, late-arriving invoices, refunds and adjustments without changing already-closed periods.
- Make cost records auditable: source system, source transaction ID, calculation rule, timestamp and user/system that changed the record.
- Avoid double counting when the same event is received more than once. Use idempotency keys and reconciliation.
- Establish reconciliation controls between the operational cost tool and the financial system.

**Useful clarification questions:**
1. What are the authoritative source systems for each cost type?
2. Are allocation rules centrally configured and can they change over time?
3. Do we need historical reporting using the rules that existed at that time?
4. What is the required reporting latency and financial close process?
5. What level of auditability is required?

---

## Scenario 2: Cost Optimization Strategies

**Situation:** The company wants to identify and implement cost optimization strategies for its fulfillment operations.

**Questions you may have and considerations:**

I would start with data rather than immediately proposing savings. Build a baseline cost per warehouse, store, order and shipment, then identify the largest cost drivers.

Potential strategies:
- Warehouse capacity utilization and footprint optimization.
- Consolidate or relocate underutilized warehouses.
- Optimize transportation routes and carrier selection.
- Reduce unnecessary handling/movement between warehouses.
- Improve inventory placement and demand-based replenishment.
- Optimize labor scheduling based on demand.
- Automate repetitive operational processes where the ROI is positive.
- Reduce expedited shipping and avoidable returns.
- Negotiate supplier/carrier contracts using measured volume and service data.

I would prioritize initiatives using a score such as:

**Priority = expected annual savings × confidence × strategic value / implementation effort**

I would run a pilot for high-value, low-risk changes, define measurable KPIs, and compare actual savings with the baseline.

Key KPIs could include cost per order, cost per shipment, warehouse utilization, labor cost per unit, transportation cost per unit, SLA adherence and customer-impact metrics.

The important point is that cost reduction cannot be optimized independently of service quality. I would use guardrails such as delivery SLA, stock availability and customer satisfaction.

---

## Scenario 3: Integration with Financial Systems

**Situation:** The Cost Control Tool needs to integrate with existing financial systems to ensure accurate and timely cost data.

**Questions you may have and considerations:**

The financial integration creates a single flow from operational events to financial reporting and reconciliation. It can provide timely visibility, reduce manual reconciliation and improve auditability.

I would define:
- System-of-record ownership for each data element.
- Canonical cost/transaction model.
- APIs/events/files supported by each financial system.
- Idempotency and duplicate handling.
- Retry and dead-letter handling.
- Ordering requirements where financial events depend on sequence.
- Reconciliation between source and target totals.
- Security, authentication, encryption and least-privilege access.
- Audit trail and correlation IDs.
- Monitoring and alerting.

For real-time synchronization, I would prefer an event-driven integration where appropriate. For financial systems that support batch interfaces, I would use scheduled reconciliation in addition to real-time events.

I would not assume that a successful API call means the financial data is permanently reconciled. The system should support retries, replay and reconciliation.

A practical architecture could be:

**Operational system -> domain event -> integration layer/message broker -> financial adapter -> financial system**

with an outbox pattern on the source side so a committed business transaction and its integration event cannot diverge.

---

## Scenario 4: Budgeting and Forecasting

**Situation:** The company needs budgeting and forecasting capabilities for its fulfillment operations.

**Questions you may have and considerations:**

I would separate:
- Approved budget.
- Current forecast.
- Actual cost.
- Variance.

The system should support dimensions such as warehouse, store, cost category, business unit and time period.

For forecasting, I would consider:
- Historical spend.
- Seasonality.
- Order volume and demand forecasts.
- Warehouse capacity and utilization.
- Labor rates and planned staffing.
- Transportation rates and fuel costs.
- Supplier/carrier changes.
- Planned warehouse openings/replacements.
- One-time exceptional costs.

I would make assumptions explicit and version forecasts so users can compare forecast versions.

A useful flow is:

**Actuals + operational drivers + business assumptions -> forecast -> variance analysis -> corrective action**

I would also define thresholds for variance alerts, for example when actual spend exceeds budget by a configured percentage.

Before finalizing the design, I would ask whether the business needs statistical forecasting, rule-based forecasting, manual adjustments, or a combination.

---

## Scenario 5: Cost Control in Warehouse Replacement

**Situation:** The company is planning to replace an existing Warehouse with a new one. The new Warehouse will reuse the Business Unit Code of the old Warehouse. The old Warehouse will be archived, but its cost history must be preserved.

**Questions you may have and considerations:**

The Business Unit Code identifies the continuing business unit, while the warehouse record represents a particular physical/operational incarnation. Therefore, I would never overwrite the old record.

Instead:

1. Mark the existing warehouse as archived with an `archivedAt` timestamp.
2. Preserve all historical costs and operational records linked to the old warehouse instance.
3. Create a new active warehouse using the same Business Unit Code.
4. Give the new warehouse its own creation timestamp and internal identity.
5. Ensure reports can distinguish historical costs from current costs.
6. Keep the replacement operation transactional so the old warehouse is not archived without the new warehouse being created.
7. Validate that the new warehouse can accommodate the carried stock and satisfy location constraints.

This allows reporting such as:

- Total historical cost for Business Unit `MWH.001`.
- Cost incurred by the old warehouse before replacement.
- Cost incurred by the new warehouse after replacement.
- Current active warehouse budget versus historical baseline.

The key design principle is **stable business identity + separate physical lifecycle records**. Reusing the Business Unit Code should not mean overwriting the historical warehouse record.

