package org.silkframework.plugins.dataset.rdf.access

import org.apache.jena.query.DatasetFactory
import org.apache.jena.rdf.model.ModelFactory
import org.silkframework.config.Prefixes
import org.silkframework.entity.ValueType
import org.silkframework.plugins.dataset.rdf.endpoint.JenaDatasetEndpoint
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.ConfigTestTrait
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class GraphStoreSinkTest extends AnyFlatSpec with Matchers with ConfigTestTrait {
  behavior of "Graph store sink"

  implicit private val userContext: UserContext = UserContext.Empty
  implicit private val prefixes: Prefixes = Prefixes.empty
  private val STATEMENT_SIZE = 37

  it should "chunk the triples according to the config" in {
    val graph = "urn:graph:default"
    val dataset = DatasetFactory.createTxnMem()
    val sink = GraphStoreSink(new JenaDatasetEndpoint(dataset), graph, None, None, dropGraphOnClear = false)
    sink.init()
    for(i <- 10 to 19) {
      // Every statement takes 37 bytes in the upload file
      sink.writeStatement(s"urn:subj:$i", "urn:prop:p", "value", ValueType.UNTYPED)
    }
    dataset.getNamedModel(graph).size() mustBe 0
    sink.writeStatement("s", "p", "v", ValueType.UNTYPED)
    // The first 10 statements, the last statement is part of the next transaction
    dataset.getNamedModel(graph).size() mustBe 10
    sink.close()
    dataset.getNamedModel(graph).size() mustBe 11
  }

  /** The properties that should be changed.
    * If the value is [[None]] then the property value is removed,
    * else it is set to the new value.
    */
  override def propertyMap: Map[String, Option[String]] = Map(
    "graphstore.default.max.request.size" -> Some((STATEMENT_SIZE * 10).toString)
  )
}
