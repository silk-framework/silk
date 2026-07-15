Clears the dataset that is connected to the output of this operator, e.g. deletes all triples of a Knowledge Graph or removes the contents of a CSV file.

The operator itself only emits a clear instruction. The **dataset node connected to its output** performs the physical clear when that node executes. Clearing a read-only dataset fails the workflow.

## Execution order

A dataset node fed by this operator (a "clear node") clears the dataset when the node executes. Its execution order relative to **other** nodes writing to the same dataset is undefined unless made explicit. Without an explicit order the clear may run before or after those writes — a clear that runs after them silently removes the just-written data. The workflow reports a warning when it detects this situation.

The order is explicit if one of the following holds:

- A (transitive) data-flow or dependency connection links the clear node and the writing node (in either direction).
- The clear node has an explicit *output priority*.
- Clear and write happen on the **same** dataset node: the input port order decides (a clear input on an earlier port runs before data inputs on later ports).

## Recipes

- **Clear before write (full load):** place the target dataset twice on the canvas — one node fed by this operator (clear role), one node fed by the data-producing tasks (write role). Connect **every** writing node to the clear node with a *dependency* connection, so all writes run after the clear. Use ONE clear node per dataset; multiple clear nodes would remove each other's writes.
- **Write before clear:** connect the write path to this operator (or the clear node) with a dependency connection, so the clear runs last.

## Notes

- Unlike the deprecated per-dataset "clear before execution" parameters (which clear once, at workflow start), this operator clears at a defined point *within* the data flow.
- A clear inside a nested workflow is not ordered against nodes of the parent workflow.
