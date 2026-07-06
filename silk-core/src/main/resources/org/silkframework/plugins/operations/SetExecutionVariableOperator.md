Sets a single **execution-scope** template variable from this operator's input and passes the input through
unchanged, so it can be inserted anywhere in a workflow chain.

The variable is written to the `execution` scope and can be read by any downstream node as
`{{execution.<name>}}`. A value set by this operator replaces an execution variable of the same name that was
defined as a default on the workflow or provided when the run was started. There is no fallback to other
scopes: referencing an execution variable that has not been set fails. This operator only takes effect while
running inside a workflow execution, where all nodes share one execution-variable holder.

It is the workflow-operator counterpart of the **Set execution variable** transformer (`setExecutionVariable`):
use this operator to pass a value between workflow nodes without embedding a transform.

## Parameters

- **Name of the execution variable** (`variableName`): the variable to set, addressed downstream as
  `execution.<name>`.
- **Source path** (`sourcePath`, optional): the input attribute/path that supplies the value. If left empty,
  the first value of the input is used.

## Behaviour

- Reads the first value (of `sourcePath`, or the first available value when it is empty) of the input.
- Writes it to the execution scope under `variableName`.
- Forwards the input entities unchanged (connect the output to keep the chain going, or leave it unconnected to
  use this purely as a side-effecting node).
