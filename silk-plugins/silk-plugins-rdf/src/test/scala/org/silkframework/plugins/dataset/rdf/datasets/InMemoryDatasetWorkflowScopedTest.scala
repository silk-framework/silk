package org.silkframework.plugins.dataset.rdf.datasets

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.config.{PlainTask, Prefixes}
import org.silkframework.dataset.{DatasetAccess, DatasetSpec}
import org.silkframework.dataset.rdf.{RdfDatasetAccess, SparqlEndpoint}
import org.silkframework.execution.local.LocalExecution
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.Identifier

class InMemoryDatasetWorkflowScopedTest extends AnyFlatSpec with Matchers {

  private implicit val userContext: UserContext = UserContext.Empty
  private implicit val prefixes: Prefixes = Prefixes.empty

  private val dataset = InMemoryDataset(workflowScoped = true)
  private val task = PlainTask("test", DatasetSpec(dataset))
  private val execution = LocalExecution()

  private val tripleCountQuery = "SELECT * WHERE {?s ?p ?o}"

  behavior of "InMemoryDataset (workflowScoped = true)"

  it should "store data in the executor, not in the dataset itself" in {
    val executor = new InMemoryDatasetExecutor()
    val executorEndpoint = sparqlEndpoint(executor.access(task, execution))

    executorEndpoint.update("INSERT DATA { <http://s1> <http://p> <http://o1> }")

    executorEndpoint.select(tripleCountQuery).bindings.size mustBe 1
    dataset.sparqlEndpoint.select(tripleCountQuery).bindings.size mustBe 1
  }

  it should "retain data after close() is called" in {
    val executor = new InMemoryDatasetExecutor()
    val executorEndpoint = sparqlEndpoint(executor.access(task, execution))

    executorEndpoint.update("INSERT DATA { <http://s1> <http://p> <http://o1> }")
    executorEndpoint.select(tripleCountQuery).bindings.size mustBe 1

    executor.close()

    executorEndpoint.select(tripleCountQuery).bindings.size mustBe 1
  }

  it should "update the dataset sparqlEndpoint to the latest executor's model" in {
    val dataset2 = InMemoryDataset(workflowScoped = true)
    val task2 = PlainTask("test2", DatasetSpec(dataset2))
    val execution1 = LocalExecution(false, workflowId = Some(Identifier("wf1")))
    val execution2 = LocalExecution(false, workflowId = Some(Identifier("wf2")))
    val executor1 = new InMemoryDatasetExecutor()
    val executor2 = new InMemoryDatasetExecutor()

    val endpoint1 = sparqlEndpoint(executor1.access(task2, execution1))
    endpoint1.update("INSERT DATA { <http://s1> <http://p> <http://o1> }")
    dataset2.sparqlEndpoint.select(tripleCountQuery).bindings.size mustBe 1

    executor2.access(task2, execution2)
    dataset2.sparqlEndpoint.select(tripleCountQuery).bindings.size mustBe 0
  }

  it should "isolate data between concurrent executions" in {
    val execution1 = LocalExecution(false, workflowId = Some(Identifier("wf1")))
    val execution2 = LocalExecution(false, workflowId = Some(Identifier("wf2")))
    val executor1 = new InMemoryDatasetExecutor()
    val executor2 = new InMemoryDatasetExecutor()
    val endpoint1 = sparqlEndpoint(executor1.access(task, execution1))
    val endpoint2 = sparqlEndpoint(executor2.access(task, execution2))

    endpoint1.update("INSERT DATA { <http://s1> <http://p> <http://o> }")
    endpoint2.update("INSERT DATA { <http://s2> <http://p> <http://o> }")
    endpoint2.update("INSERT DATA { <http://s3> <http://p> <http://o> }")

    endpoint1.select(tripleCountQuery).bindings.size mustBe 1
    endpoint2.select(tripleCountQuery).bindings.size mustBe 2
  }

  it should "use parent execution data in the nested executor" in {
    val nestedDataset = InMemoryDataset(workflowScoped = true)
    val nestedTask = PlainTask("nestedTest", DatasetSpec(nestedDataset))

    val parentExecution = LocalExecution(false, workflowId = Some(Identifier("parentWf")))
    val parentExecutor = new InMemoryDatasetExecutor()
    val parentEndpoint = sparqlEndpoint(parentExecutor.access(nestedTask, parentExecution))
    parentEndpoint.update("INSERT DATA { <http://s1> <http://p> <http://o1> }")
    parentEndpoint.update("INSERT DATA { <http://s2> <http://p> <http://o2> }")
    parentEndpoint.select(tripleCountQuery).bindings.size mustBe 2

    val childExecution = LocalExecution(false, workflowId = Some(Identifier("childWf")), parentExecution = Some(parentExecution))
    val childExecutor = new InMemoryDatasetExecutor()
    val childEndpoint = sparqlEndpoint(childExecutor.access(nestedTask, childExecution))

    childEndpoint.select(tripleCountQuery).bindings.size mustBe 2
  }

  it should "share the model between parent and child executions" in {
    val nestedDataset = InMemoryDataset(workflowScoped = true)
    val nestedTask = PlainTask("nestedTest", DatasetSpec(nestedDataset))

    val parentExecution = LocalExecution(false, workflowId = Some(Identifier("parentWf")))
    val parentExecutor = new InMemoryDatasetExecutor()
    val parentEndpoint = sparqlEndpoint(parentExecutor.access(nestedTask, parentExecution))
    parentEndpoint.update("INSERT DATA { <http://s1> <http://p> <http://o1> }")

    val childExecution = LocalExecution(false, workflowId = Some(Identifier("childWf")), parentExecution = Some(parentExecution))
    val childExecutor = new InMemoryDatasetExecutor()
    val childEndpoint = sparqlEndpoint(childExecutor.access(nestedTask, childExecution))
    childEndpoint.update("INSERT DATA { <http://s2> <http://p> <http://o2> }")

    childEndpoint.select(tripleCountQuery).bindings.size mustBe 2
    parentEndpoint.select(tripleCountQuery).bindings.size mustBe 2
  }

  it should "only reuse the parent model with the same task id in a nested execution" in {
    val sharedDataset = InMemoryDataset(workflowScoped = true)
    val taskA = PlainTask("datasetA", DatasetSpec(sharedDataset))
    val taskB = PlainTask("datasetB", DatasetSpec(sharedDataset))

    val parentExecution = LocalExecution(false, workflowId = Some(Identifier("parentWf")))

    val parentExecutorA = new InMemoryDatasetExecutor()
    val endpointA = sparqlEndpoint(parentExecutorA.access(taskA, parentExecution))
    endpointA.update("INSERT DATA { <http://a1> <http://p> <http://oA> }")

    val parentExecutorB = new InMemoryDatasetExecutor()
    val endpointB = sparqlEndpoint(parentExecutorB.access(taskB, parentExecution))
    endpointB.update("INSERT DATA { <http://b1> <http://p> <http://oB> }")
    endpointB.update("INSERT DATA { <http://b2> <http://p> <http://oB> }")

    val childExecution = LocalExecution(false, workflowId = Some(Identifier("childWf")), parentExecution = Some(parentExecution))
    val childExecutorA = new InMemoryDatasetExecutor()
    val childEndpointA = sparqlEndpoint(childExecutorA.access(taskA, childExecution))
    childEndpointA.select(tripleCountQuery).bindings.size mustBe 1

    val childExecutorB = new InMemoryDatasetExecutor()
    val childEndpointB = sparqlEndpoint(childExecutorB.access(taskB, childExecution))
    childEndpointB.select(tripleCountQuery).bindings.size mustBe 2
  }

  it should "create a new model in a nested execution if the parent has no matching task id" in {
    val sharedDataset = InMemoryDataset(workflowScoped = true)
    val parentTask = PlainTask("parentOnly", DatasetSpec(sharedDataset))
    val childTask = PlainTask("childOnly", DatasetSpec(sharedDataset))

    val parentExecution = LocalExecution(false, workflowId = Some(Identifier("parentWf")))

    val parentExecutor = new InMemoryDatasetExecutor()
    val parentEndpoint = sparqlEndpoint(parentExecutor.access(parentTask, parentExecution))
    parentEndpoint.update("INSERT DATA { <http://s> <http://p> <http://o> }")

    val childExecution = LocalExecution(false, workflowId = Some(Identifier("childWf")), parentExecution = Some(parentExecution))
    val childExecutor = new InMemoryDatasetExecutor()
    val childEndpoint = sparqlEndpoint(childExecutor.access(childTask, childExecution))
    childEndpoint.select(tripleCountQuery).bindings.size mustBe 0
  }

  it should "drop the execution's references on close() while keeping the data readable through the dataset" in {
    val nestedDataset = InMemoryDataset(workflowScoped = true)
    val nestedTask = PlainTask("cleanupTest", DatasetSpec(nestedDataset))

    val exec = LocalExecution(false, workflowId = Some(Identifier("wf")))
    val executor = new InMemoryDatasetExecutor()
    sparqlEndpoint(executor.access(nestedTask, exec))
      .update("INSERT DATA { <http://s> <http://p> <http://o> }")

    nestedDataset.findEndpoint(exec, nestedTask.id) must not be empty

    executor.close()

    // The execution-scoped sharing entry is gone ...
    nestedDataset.findEndpoint(exec, nestedTask.id) mustBe empty
    // ... but the data the workflow produced is still readable through the dataset itself.
    nestedDataset.sparqlEndpoint.select(tripleCountQuery).bindings.size mustBe 1
  }

  it should "let the parent read data that a nested execution wrote first" in {
    // Regression: the nested workflow accesses and writes to the shared dataset before the parent reads it.
    val nestedDataset = InMemoryDataset(workflowScoped = true)
    val nestedTask = PlainTask("nestedFirst", DatasetSpec(nestedDataset))

    val parentExecution = LocalExecution(false, workflowId = Some(Identifier("parentWf")))
    val childExecution = LocalExecution(false, workflowId = Some(Identifier("childWf")), parentExecution = Some(parentExecution))

    // The nested (child) executor accesses first and writes.
    val childExecutor = new InMemoryDatasetExecutor()
    val childEndpoint = sparqlEndpoint(childExecutor.access(nestedTask, childExecution))
    childEndpoint.update("INSERT DATA { <http://s1> <http://p> <http://o1> }")
    childEndpoint.update("INSERT DATA { <http://s2> <http://p> <http://o2> }")

    // The nested workflow finishes before the parent continues.
    childExecutor.close()

    // The parent then reads and must see the data the nested workflow wrote.
    val parentExecutor = new InMemoryDatasetExecutor()
    val parentEndpoint = sparqlEndpoint(parentExecutor.access(nestedTask, parentExecution))
    parentEndpoint.select(tripleCountQuery).bindings.size mustBe 2
  }

  it should "keep the shared endpoint when a nested execution closes and only drop it when the root closes" in {
    val nestedDataset = InMemoryDataset(workflowScoped = true)
    val nestedTask = PlainTask("cleanupOrder", DatasetSpec(nestedDataset))

    val parentExecution = LocalExecution(false, workflowId = Some(Identifier("parentWf")))
    val childExecution = LocalExecution(false, workflowId = Some(Identifier("childWf")), parentExecution = Some(parentExecution))

    val childExecutor = new InMemoryDatasetExecutor()
    sparqlEndpoint(childExecutor.access(nestedTask, childExecution))
      .update("INSERT DATA { <http://s> <http://p> <http://o> }")

    val parentExecutor = new InMemoryDatasetExecutor()
    parentExecutor.access(nestedTask, parentExecution)

    // A nested execution finishing must not drop the shared endpoint.
    childExecutor.close()
    nestedDataset.findEndpoint(parentExecution, nestedTask.id) must not be empty

    // Only the root execution disposes the shared endpoint.
    parentExecutor.close()
    nestedDataset.findEndpoint(parentExecution, nestedTask.id) mustBe empty
  }

  private def sparqlEndpoint(access: DatasetAccess): SparqlEndpoint =
    access.asInstanceOf[RdfDatasetAccess].sparqlEndpoint
}