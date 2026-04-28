## 1. Purpose

The **in-memory dataset** is a small embedded RDF store that keeps all data **in memory** and exposes it via SPARQL. It is intended as a **temporary working graph** inside workflows, not as a large or persistent storage.

Typical use cases:
- Collecting intermediate results during a workflow run.
- Storing small lookup graphs used by downstream operators.
- Testing or prototyping workflows without configuring an external RDF store.

## 2. Behaviour and lifecycle

The dataset maintains a single in-memory RDF model and exposes it via a SPARQL endpoint. Two lifecycle modes are available, controlled by the `workflowScoped` parameter:

**Application-scoped mode** (default, `workflowScoped = false`):
- A single shared model is created when the dataset is instantiated.
- Data persists for the lifetime of the running application process.
- All workflow executions share the same in-memory graph.
- After an application restart, the dataset contents are empty again.

**Workflow-scoped mode** (`workflowScoped = true`):
- A separate model is created for each workflow execution.
- Concurrent workflow executions are fully isolated from each other.
- A dataset task in a nested workflow shares the same model as the parent workflow for the same task identifier. Data written by the parent is available in the nested workflow and vice versa.
- If the dataset is read from outside a workflow context, the data from the most recently started executor is returned.
- When the workflow execution ends, the per-execution data is removed automatically.

## 3. Reading data

- When used as a **source**, the dataset exposes its data as a SPARQL endpoint.
- Queries and retrievals behave like against a normal SPARQL dataset:
    - Entity retrieval, path/type discovery, sampling, etc. are executed via SPARQL.
- There is no file backing this dataset; everything comes from what has been written into the in-memory model during the lifetime of the process (application-scoped) or the workflow execution (workflow-scoped).

## 4. Writing data

The in-memory dataset accepts RDF data through:

- **Entity sink**
    - Entities written by upstream components are converted to RDF triples and stored in the in-memory model.

- **Link sink**
    - Links are written as RDF triples in the same model.

- **Triple sink**
    - Triples are directly added to the in-memory model via SPARQL operations.

All three sinks ultimately write into the same in-memory graph; there is no separate physical storage per sink type.

## 5. Configuration

### Workflow scoped <a id="parameter_doc_workflowScoped"></a>

- **Parameter:** `workflowScoped` (boolean)
- **Default:** `false`

When `true` (workflow-scoped mode):
- Data is stored in a separate in-memory graph for each workflow execution.
- Concurrent workflow executions are fully isolated from each other.
- A dataset task in a nested workflow shares the same graph as the parent for the same task identifier. Data written by the parent is available in the nested workflow and vice versa.
- If the dataset is read from outside a workflow context, the data from the most recently started executor is returned.
- When the workflow execution ends, the per-execution data is removed automatically.

When `false` (default, application-scoped mode):
- Data persists in a single shared graph for the lifetime of the running process.
- All workflow executions share the same graph.

### Clear graph before workflow execution <a id="parameter_doc_clearGraphBeforeExecution"></a>

- **Parameter:** `clearGraphBeforeExecution` (boolean, **deprecated**)
- **Default:** `false`

This parameter is deprecated. Use the **Clear dataset** operator in the workflow instead.

Behaviour (application-scoped mode only):

- If **true**:
    - Before the dataset is used in a workflow execution, the graph is cleared.
    - The workflow sees a **fresh, empty in-memory graph** at the start of the run.

- If **false**:
    - Existing data in the in-memory graph is **preserved** when the workflow starts.
    - New data is added on top of whatever is already stored in the model.

This parameter has no effect when `workflowScoped = true` (the executor manages the lifecycle).

## 6. Limitations and recommendations

- **Memory-bound**
    - All data is kept in memory; large graphs will increase memory usage and may impact performance.
    - For large or production RDF graphs, use an external RDF store and a SPARQL dataset instead.
    - A size limit is enforced: once the estimated size of data written to the dataset exceeds the value of `org.silkframework.runtime.resource.Resource.maxInMemorySize`, the workflow fails with an error. This prevents the JVM from running out of heap memory.

- **No persistence**
    - Contents are lost when the application/server is restarted.
    - Do not treat this dataset as long-term storage.

- **SPARQL engine**
  - The dataset is backed by [Apache Jena](https://jena.apache.org/), exposed through a Jena in-memory SPARQL endpoint.

- **No named-graph support**
  - Only the **default graph** is available. Writing triples into a named graph is not possible.

- **Scope**
    - Best suited for:
        - small to medium intermediate results,
        - testing and prototyping,
        - temporary data that can be regenerated by re-running workflows.

## 7. Example usage scenarios

- Use as a **temporary integration graph** (application-scoped):
    - Multiple sources write into the in-memory dataset.
    - A downstream SPARQL-based operator queries the combined graph.

- Use as a **scratch area for experimentation** (application-scoped):
    - Quickly test mapping or linking logic by writing output into the in-memory dataset.
    - Inspect the result via SPARQL without configuring an external endpoint.

- Use as a **small lookup store** (application-scoped):
    - Preload a small set of reference triples (e.g. codes or mappings).
    - Let workflows query these during execution.

- Use as a **workflow-local intermediate store** (workflow-scoped):
    - Multiple operators in a single workflow run write intermediate RDF results.
    - Downstream operators in the same run read from the dataset without affecting parallel runs.

- Use in **nested workflows** (workflow-scoped):
    - A parent workflow writes data into a workflow-scoped dataset.
    - A nested sub-workflow reads and enriches the same data.
    - After the nested workflow completes, the parent can read the enriched result.
