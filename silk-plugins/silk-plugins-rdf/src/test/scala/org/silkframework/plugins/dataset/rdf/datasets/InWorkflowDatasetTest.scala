package org.silkframework.plugins.dataset.rdf.datasets

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.config.{PlainTask, Prefixes}
import org.silkframework.dataset.DatasetSpec
import org.silkframework.dataset.rdf.RdfDataset
import org.silkframework.execution.local.LocalExecution
import org.silkframework.runtime.activity.UserContext

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
}
