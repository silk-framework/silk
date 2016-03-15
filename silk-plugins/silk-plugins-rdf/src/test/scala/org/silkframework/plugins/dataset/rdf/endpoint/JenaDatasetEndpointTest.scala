package org.silkframework.plugins.dataset.rdf.endpoint

import com.hp.hpl.jena.query.DatasetFactory
import org.scalatest.{FlatSpec, Matchers}

/**
  * Created on 3/14/16.
  */
class JenaDatasetEndpointTest extends FlatSpec with Matchers {
  behavior of "JenaDatasetEndpoint"

  def endpoint = {
    val dataset = DatasetFactory.createMem()
    new JenaDatasetEndpoint(dataset)
  }

  val graph = "http://graph1"

  it should "store RDF via graph store endpoint" in {
    val ep = endpoint
    val os = ep.postDataToGraph(graph)
    os.write("<a> <b> <c> .".getBytes("UTF-8"))
    os.close()
    val results = ep.select(s"SELECT * FROM <$graph> FROM NAMED <$graph> WHERE { ?s ?p ?o}")
    for(result <- results.bindings) {
      /* No results generated here, this seems to be a limitation of Jena:
       * http://answers.semanticweb.com/questions/24274/fuseki-specifying-default-graph-using-from-in-sparql
       */
    }

    val results2 = ep.select(s"SELECT * WHERE { graph <$graph> {?s ?p ?o}}")
    results2.bindings.size shouldBe > (0)
  }
}
