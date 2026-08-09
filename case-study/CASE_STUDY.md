# Case Study Scenarios to discuss

## Scenario 1: Cost Allocation and Tracking
**Situation**: The company needs to track and allocate costs accurately across different Warehouses and Stores. The costs include labor, inventory, transportation, and overhead expenses.

**Task**: Discuss the challenges in accurately tracking and allocating costs in a fulfillment environment. Think about what are important considerations for this, what are previous experiences that you have you could related to this problem and elaborate some questions and considerations

**Questions you may have and considerations:**
Cost tracking in a warehouse can be difficult due a lot of variability in tasks and orders. We could have small orders that are easy to fulfill that earn a lot, and we might have big orders that take a lot of effort to fulfill, multiple people envolved and might not bring in a lot of revenue, eg iPhone vs heavy bed frame. I dont have warehouse cost management experience, but I would try to find the identifiers which would help us differentiate between orders fairly. For eg items size (small,med,large), no of labour envolved, no of hours per labour, cost per machinery per hr (eg fork lift, truck) etc and try to come up with a cost system that covers all these areas instead of a single factor. The cost calculated this way might be more than real cost (multiple orders could've been fulfilled together) but it does giveus a system to work with and see cost per order independently of other factors.

## Scenario 2: Cost Optimization Strategies
**Situation**: The company wants to identify and implement cost optimization strategies for its fulfillment operations. The goal is to reduce overall costs without compromising service quality.

**Task**: Discuss potential cost optimization strategies for fulfillment operations and expected outcomes from that. How would you identify, prioritize and implement these strategies?

**Questions you may have and considerations:**
There are multiple legs in a fulfillment operation where cost can be saved.

1. Identify how items are stored. This gives us an opportunity to optimize on the time it takes to fulfill an order with multiple different types of order and thereby reducing labour time for that order. Eg items that are frequently ordered together (mattress + bedsheet) could be placed closer
2. Identify how we deliver orders for a location. Hoow many trucks are involved, do multiple trucks go to same location ever? What are the idle times for trucks and delivery boys
3. Analyse fulfilment times w.r.t warehouse locations. It might save us a lot of money to open up a warehouse in hot zones were we get a lot of orders
4. There would also be some cost optimization scope w.r.t the software being used, the talent of people etc which we could explore

## Scenario 3: Integration with Financial Systems
**Situation**: The Cost Control Tool needs to integrate with existing financial systems to ensure accurate and timely cost data. The integration should support real-time data synchronization and reporting.

**Task**: Discuss the importance of integrating the Cost Control Tool with financial systems. What benefits the company would have from that and how would you ensure seamless integration and data synchronization?

**Questions you may have and considerations:**
This integration would help us get a single view on our system instead of having to track 2 systems seperately. This would also be very crucial if data from one system is flowing into another eg, cost from orders showing up in financial system grouped by vendors/delivery partners etc which can give us a clear picture on our operations. This integration wopuld also help us in periodic reportings. Also we dont really need realtime system for report as reports go out periodically and a batch system is sufficient here. 

## Scenario 4: Budgeting and Forecasting
**Situation**: The company needs to develop budgeting and forecasting capabilities for its fulfillment operations. The goal is to predict future costs and allocate resources effectively.

**Task**: Discuss the importance of budgeting and forecasting in fulfillment operations and what would you take into account designing a system to support accurate budgeting and forecasting?

**Questions you may have and considerations:**
Budgeting and forecasting needs some historicxal data and trends. We need to know the actual capacity each warehouse in each location works at. After we have some data with use for a few months we can start planning ahead. We can only keep an x% above the max need for each resource to avoid extra cost and have some buffer for each location which can be distributed when needed. We also need to consider any peak upcoming peak events like christmas as they would drive up our sales as well as costs, so we should allocated extra resources 2-3x in such seasons.

## Scenario 5: Cost Control in Warehouse Replacement
**Situation**: The company is planning to replace an existing Warehouse with a new one. The new Warehouse will reuse the Business Unit Code of the old Warehouse. The old Warehouse will be archived, but its cost history must be preserved.

**Task**: Discuss the cost control aspects of replacing a Warehouse. Why is it important to preserve cost history and how this relates to keeping the new Warehouse operation within budget?

**Questions you may have and considerations:**
It is important to have the ability to preserve the cost history for a business unit. We might need to pull up historical records or might need to do some comparison in the future. So the archived warehouse records must be archived too with some identifier, and the new business unit should have a fresh book of records

## Instructions for Candidates
Before starting the case study, read the [BRIEFING.md](BRIEFING.md) to quickly understand the domain, entities, business rules, and other relevant details.

**Analyze the Scenarios**: Carefully analyze each scenario and consider the tasks provided. To make informed decisions about the project's scope and ensure valuable outcomes, what key information would you seek to gather before defining the boundaries of the work? Your goal is to bridge technical aspects with business value, bringing a high level discussion; no need to deep dive.
