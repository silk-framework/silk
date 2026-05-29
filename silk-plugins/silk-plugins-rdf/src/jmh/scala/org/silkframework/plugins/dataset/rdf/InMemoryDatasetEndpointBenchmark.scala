package org.silkframework.plugins.dataset.rdf

import org.openjdk.jmh.annotations._
import org.silkframework.dataset.rdf.SparqlEndpoint
import org.silkframework.plugins.dataset.rdf.datasets.InMemoryDataset
import org.silkframework.runtime.activity.UserContext

import java.util.concurrent.TimeUnit

/** Diagnoses how repeated public sparqlEndpoint creation affects update cost on the same dataset model. */
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(1)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
class InMemoryDatasetEndpointBenchmark {

  @Benchmark
  def updateAndClear(state: EndpointAccumulationState): Unit = {
    implicit val userContext: UserContext = state.userContext
    state.writerEndpoint.update(state.insertQuery)
    state.writerEndpoint.update(state.clearQuery)
  }
}

@State(Scope.Thread)
class EndpointAccumulationState {

  @Param(Array("0", "1", "10", "100", "1000", "10000"))
  var publicEndpointCount: Int = _

  val userContext: UserContext = UserContext.Empty

  private var dataset: InMemoryDataset = _
  private var publicEndpoints: Array[SparqlEndpoint] = Array.empty
  var writerEndpoint: SparqlEndpoint = _

  val insertQuery: String = "INSERT DATA { <http://bench/s> <http://bench/p> <http://bench/o> }"
  val clearQuery: String = "DROP SILENT DEFAULT"

  @Setup(Level.Iteration)
  def setup(): Unit = {
    dataset = InMemoryDataset(workflowScoped = false)
    publicEndpoints = Array.fill(publicEndpointCount)(dataset.sparqlEndpoint)
    writerEndpoint = dataset.sparqlEndpoint
  }

  @TearDown(Level.Iteration)
  def tearDown(): Unit = {
    writerEndpoint = null
    publicEndpoints = Array.empty
    dataset = null
  }
}
