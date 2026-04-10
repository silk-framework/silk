The **in-workflow dataset** is an embedded RDF store that holds all data **in memory**, scoped to a single workflow execution. It is intended as a **transient working graph** for passing data between operators within one run.

Typical use cases:
- Passing intermediate RDF results between operators within a single workflow execution.
- Storing triples produced by one operator for consumption by a downstream operator in the same run.
- Keeping workflow-local data isolated from other concurrent workflow executions.

**Nested workflows:** When a workflow contains a nested workflow, the nested workflow shares the same in-workflow dataset model as the parent for each dataset task with the same identifier. Data written by the parent workflow is available in the nested workflow, and data written by the nested workflow is visible to the parent after the nested workflow completes. Dataset tasks with different identifiers remain isolated.

If the dataset is read from outside a workflow, the data from the most recently started executor will be returned.
For large graphs, use an external RDF store.