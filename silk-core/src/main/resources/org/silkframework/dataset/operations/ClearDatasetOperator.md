Clears the dataset that is connected to the output of this operator, e.g. deletes all triples of a Knowledge Graph or removes the contents of a CSV file.

The operator itself only emits a clear instruction. The **dataset node connected to its output** performs the physical clear when that node executes. Clearing a read-only dataset fails the workflow.

## Execution order

A dataset node fed by this operator (a "clear node") clears the dataset when the node executes. Its execution order relative to other nodes writing to the same dataset is undefined unless made explicit. Without an explicit order the clear may run before or after those writes. A clear that runs after them silently removes the just-written data. The workflow reports a warning when it detects this situation.

The order is explicit if one of the following holds:

- A (transitive) data-flow or dependency path connects the clear node and the writing node; the direction of the path determines which of the two runs first.
- Clear and write happen on the same dataset node; the input port order decides: a clear input on an earlier port runs before data inputs on later ports.

## Recipes

### Clear, then write single dataset

Example: a `Customers` dataset that is rebuilt from scratch on every run.

Connect the output of this operator to the first input port of the `Customers` node and the output of the data-producing task to a later input port of the same node. The port order guarantees that the dataset is emptied before the new data is written. No dependency connections are needed.

### Clear before write on separate nodes

If the clear cannot share a node with the writes (for example because different branches of the workflow write to their own `Customers` nodes), place `Customers` on the canvas one more time and connect the output of this operator to it (the clear node). Then draw a *dependency* connection from the clear node to every node that `Customers` is written to, so all writes run after the clear.

A dataset can also be cleared several times in one workflow (e.g. to reuse it freshly in a later phase), but then each clear node must be ordered against all writes this way. A clear that is left unordered may run after the writes and silently remove them.

### Write before clear

Example: a temporary `Staging` dataset that is filled and consumed during the workflow and should be left empty at the end.

Draw a dependency connection from the node that writes to `Staging` to this operator (or to its clear node), so the clear runs after the write as the final step.
