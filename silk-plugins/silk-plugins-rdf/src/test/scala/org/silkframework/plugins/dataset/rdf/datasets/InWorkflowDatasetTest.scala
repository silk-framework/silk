package org.silkframework.plugins.dataset.rdf.datasets

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.config.{PlainTask, Prefixes}
import org.silkframework.dataset.DatasetSpec
import org.silkframework.dataset.rdf.RdfDataset
import org.silkframework.execution.local.LocalExecution
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.Identifier

class InWorkflowDatasetTest extends AnyFlatSpec with Matchers {

  private implicit val userContext: UserContext = UserContext.Empty
  private implicit val prefixes: Prefixes = Prefixes.empty

  private val dataset = InWorkflowDataset()
  private val task = PlainTask("test", DatasetSpec(dataset))
  private val execution = LocalExecution()

  private val tripleCountQuery = "SELECT * WHERE {?s ?p ?o}"

  behavior of "InWorkflowDataset"

  it should "store data in the executor, not in the dataset itself" in {
    val executor = new InWorkflowDatasetExecutor()
    val executorEndpoint = executor.access(task, execution).asInstanceOf[RdfDataset].sparqlEndpoint

    executorEndpoint.update("INSERT DATA { <http://s1> <http://p> <http://o1> }")

    // The executor's model contains the written data
    executorEndpoint.select(tripleCountQuery).bindings.size mustBe 1
    // After access() the dataset's sparqlEndpoint reflects the executor's model
    dataset.sparqlEndpoint.select(tripleCountQuery).bindings.size mustBe 1
  }

  it should "retain data after close() is called" in {
    val executor = new InWorkflowDatasetExecutor()
    val executorEndpoint = executor.access(task, execution).asInstanceOf[RdfDataset].sparqlEndpoint

    executorEndpoint.update("INSERT DATA { <http://s1> <http://p> <http://o1> }")
    executorEndpoint.select(tripleCountQuery).bindings.size mustBe 1

    executor.close()

    executorEndpoint.select(tripleCountQuery).bindings.size mustBe 1
  }

  it should "update the dataset sparqlEndpoint to the latest executor's model" in {
    val dataset2 = InWorkflowDataset()
    val task2 = PlainTask("test2", DatasetSpec(dataset2))
    val executor1 = new InWorkflowDatasetExecutor()
    val executor2 = new InWorkflowDatasetExecutor()

    val endpoint1 = executor1.access(task2, execution).asInstanceOf[RdfDataset].sparqlEndpoint
    endpoint1.update("INSERT DATA { <http://s1> <http://p> <http://o1> }")
    // dataset2 now points to executor1's model — one triple visible
    dataset2.sparqlEndpoint.select(tripleCountQuery).bindings.size mustBe 1

    // executor2.access() replaces the endpoint — dataset2 now sees executor2's (empty) model
    executor2.access(task2, execution)
    dataset2.sparqlEndpoint.select(tripleCountQuery).bindings.size mustBe 0
  }

  it should "isolate data between concurrent executions" in {
    val executor1 = new InWorkflowDatasetExecutor()
    val executor2 = new InWorkflowDatasetExecutor()
    val endpoint1 = executor1.access(task, execution).asInstanceOf[RdfDataset].sparqlEndpoint
    val endpoint2 = executor2.access(task, execution).asInstanceOf[RdfDataset].sparqlEndpoint

    endpoint1.update("INSERT DATA { <http://s1> <http://p> <http://o> }")
    endpoint2.update("INSERT DATA { <http://s2> <http://p> <http://o> }")
    endpoint2.update("INSERT DATA { <http://s3> <http://p> <http://o> }")

    // Each executor only sees data from its own model
    endpoint1.select(tripleCountQuery).bindings.size mustBe 1
    endpoint2.select(tripleCountQuery).bindings.size mustBe 2
  }

  it should "use parent execution data in the nested executor" in {
    val nestedDataset = InWorkflowDataset()
    val nestedTask = PlainTask("nestedTest", DatasetSpec(nestedDataset))

    // Parent execution writes data
    val parentExecution = LocalExecution(false, workflowId = Some(Identifier("parentWf")))
    val parentExecutor = new InWorkflowDatasetExecutor()
    val parentEndpoint = parentExecutor.access(nestedTask, parentExecution).asInstanceOf[RdfDataset].sparqlEndpoint
    parentEndpoint.update("INSERT DATA { <http://s1> <http://p> <http://o1> }")
    parentEndpoint.update("INSERT DATA { <http://s2> <http://p> <http://o2> }")
    parentEndpoint.select(tripleCountQuery).bindings.size mustBe 2

    // Child execution with parent reference
    val childExecution = LocalExecution(false, workflowId = Some(Identifier("childWf")), parentExecution = Some(parentExecution))
    val childExecutor = new InWorkflowDatasetExecutor()
    val childEndpoint = childExecutor.access(nestedTask, childExecution).asInstanceOf[RdfDataset].sparqlEndpoint

    // Child sees the parent's data
    childEndpoint.select(tripleCountQuery).bindings.size mustBe 2
  }

  it should "share the model between parent and child executions" in {
    val nestedDataset = InWorkflowDataset()
    val nestedTask = PlainTask("nestedTest", DatasetSpec(nestedDataset))

    // Parent execution writes one triple
    val parentExecution = LocalExecution(false, workflowId = Some(Identifier("parentWf")))
    val parentExecutor = new InWorkflowDatasetExecutor()
    val parentEndpoint = parentExecutor.access(nestedTask, parentExecution).asInstanceOf[RdfDataset].sparqlEndpoint
    parentEndpoint.update("INSERT DATA { <http://s1> <http://p> <http://o1> }")

    // Child execution references the same model and writes more
    val childExecution = LocalExecution(false, workflowId = Some(Identifier("childWf")), parentExecution = Some(parentExecution))
    val childExecutor = new InWorkflowDatasetExecutor()
    val childEndpoint = childExecutor.access(nestedTask, childExecution).asInstanceOf[RdfDataset].sparqlEndpoint
    childEndpoint.update("INSERT DATA { <http://s2> <http://p> <http://o2> }")

    // Both see the same data since they share the model
    childEndpoint.select(tripleCountQuery).bindings.size mustBe 2
    parentEndpoint.select(tripleCountQuery).bindings.size mustBe 2
  }

  it should "only reuse the parent model with the same task id in a nested execution" in {
    val sharedDataset = InWorkflowDataset()
    val taskA = PlainTask("datasetA", DatasetSpec(sharedDataset))
    val taskB = PlainTask("datasetB", DatasetSpec(sharedDataset))

    val parentExecution = LocalExecution(false, workflowId = Some(Identifier("parentWf")))

    // Parent registers models for both taskA and taskB
    val parentExecutorA = new InWorkflowDatasetExecutor()
    val endpointA = parentExecutorA.access(taskA, parentExecution).asInstanceOf[RdfDataset].sparqlEndpoint
    endpointA.update("INSERT DATA { <http://a1> <http://p> <http://oA> }")

    val parentExecutorB = new InWorkflowDatasetExecutor()
    val endpointB = parentExecutorB.access(taskB, parentExecution).asInstanceOf[RdfDataset].sparqlEndpoint
    endpointB.update("INSERT DATA { <http://b1> <http://p> <http://oB> }")
    endpointB.update("INSERT DATA { <http://b2> <http://p> <http://oB> }")

    // Child execution for taskA — must see only taskA's data (1 triple), not taskB's (2 triples)
    val childExecution = LocalExecution(false, workflowId = Some(Identifier("childWf")), parentExecution = Some(parentExecution))
    val childExecutorA = new InWorkflowDatasetExecutor()
    val childEndpointA = childExecutorA.access(taskA, childExecution).asInstanceOf[RdfDataset].sparqlEndpoint
    childEndpointA.select(tripleCountQuery).bindings.size mustBe 1

    // Child execution for taskB — must see only taskB's data (2 triples)
    val childExecutorB = new InWorkflowDatasetExecutor()
    val childEndpointB = childExecutorB.access(taskB, childExecution).asInstanceOf[RdfDataset].sparqlEndpoint
    childEndpointB.select(tripleCountQuery).bindings.size mustBe 2
  }

  it should "create a new model in a nested execution if the parent has no matching task id" in {
    val sharedDataset = InWorkflowDataset()
    val parentTask = PlainTask("parentOnly", DatasetSpec(sharedDataset))
    val childTask = PlainTask("childOnly", DatasetSpec(sharedDataset))

    val parentExecution = LocalExecution(false, workflowId = Some(Identifier("parentWf")))

    // Parent registers a model for parentOnly
    val parentExecutor = new InWorkflowDatasetExecutor()
    val parentEndpoint = parentExecutor.access(parentTask, parentExecution).asInstanceOf[RdfDataset].sparqlEndpoint
    parentEndpoint.update("INSERT DATA { <http://s> <http://p> <http://o> }")

    // Child execution for a different task id — must NOT see parent data
    val childExecution = LocalExecution(false, workflowId = Some(Identifier("childWf")), parentExecution = Some(parentExecution))
    val childExecutor = new InWorkflowDatasetExecutor()
    val childEndpoint = childExecutor.access(childTask, childExecution).asInstanceOf[RdfDataset].sparqlEndpoint
    childEndpoint.select(tripleCountQuery).bindings.size mustBe 0
  }

  it should "clean up model on close()" in {
    val nestedDataset = InWorkflowDataset()
    val nestedTask = PlainTask("cleanupTest", DatasetSpec(nestedDataset))

    val exec = LocalExecution(false, workflowId = Some(Identifier("wf")))
    val executor = new InWorkflowDatasetExecutor()
    executor.access(nestedTask, exec).asInstanceOf[RdfDataset].sparqlEndpoint
      .update("INSERT DATA { <http://s> <http://p> <http://o> }")

    // Model is registered
    nestedDataset.findModel(exec, nestedTask.id) must not be empty

    // After close, model is removed from the dataset's map
    executor.close()
    nestedDataset.findModel(exec, nestedTask.id) mustBe empty
  }
}
