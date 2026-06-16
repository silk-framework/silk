package org.silkframework.plugins.dataset.rdf.access

import org.apache.jena.query.{Dataset, DatasetFactory}
import org.apache.jena.riot.RDFLanguages
import org.silkframework.config.Prefixes
import org.silkframework.dataset.rdf.GraphStoreFileUploadTrait
import org.silkframework.entity.ValueType
import org.silkframework.plugins.dataset.rdf.endpoint.JenaDatasetEndpoint
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.ConfigTestTrait
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

import java.io.{File, FileInputStream}

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

  it should "use file upload when useStreaming is false and graphStore supports file upload" in {
    val graph = "urn:graph:fileupload"
    val dataset = DatasetFactory.createTxnMem()
    val endpoint = new FileUploadJenaEndpoint(dataset)
    val sink = GraphStoreSink(endpoint, graph, None, None, dropGraphOnClear = false, useStreaming = false)
    sink.init()
    sink.writeStatement("urn:subj:1", "urn:prop:p", "value", ValueType.UNTYPED)
    sink.close()
    endpoint.uploadFileToGraphCalled mustBe true
    dataset.getNamedModel(graph).size() mustBe 1
  }

  it should "use streaming when useStreaming is true even when graphStore supports file upload" in {
    val graph = "urn:graph:streaming"
    val dataset = DatasetFactory.createTxnMem()
    val endpoint = new FileUploadJenaEndpoint(dataset)
    val sink = GraphStoreSink(endpoint, graph, None, None, dropGraphOnClear = false, useStreaming = true)
    sink.init()
    sink.writeStatement("urn:subj:1", "urn:prop:p", "value", ValueType.UNTYPED)
    sink.close()
    endpoint.uploadFileToGraphCalled mustBe false
    dataset.getNamedModel(graph).size() mustBe 1
  }

  /** The properties that should be changed.
    * If the value is [[None]] then the property value is removed,
    * else it is set to the new value.
    */
  override def propertyMap: Map[String, Option[String]] = Map(
    "graphstore.default.max.request.size" -> Some((STATEMENT_SIZE * 10).toString)
  )
}

/** A JenaDatasetEndpoint that also implements GraphStoreFileUploadTrait, for testing the file upload vs streaming paths. */
class FileUploadJenaEndpoint(val jenaDataset: Dataset)
    extends JenaDatasetEndpoint(jenaDataset) with GraphStoreFileUploadTrait {

  var uploadFileToGraphCalled: Boolean = false
  var uploadCount: Int = 0

  override def uploadFileToGraph(graph: String,
                                 file: File,
                                 contentType: String,
                                 comment: Option[String])
                                (implicit userContext: UserContext): Unit = {
    uploadFileToGraphCalled = true
    uploadCount += 1
    val model = jenaDataset.getNamedModel(graph)
    val lang = RDFLanguages.contentTypeToLang(contentType)
    val inputStream = new FileInputStream(file)
    try {
      model.read(inputStream, null, lang.getName)
    } finally {
      inputStream.close()
    }
  }
}
