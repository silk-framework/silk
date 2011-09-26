package de.fuberlin.wiwiss.silk.util.sparql

import de.fuberlin.wiwiss.silk.util.Timer
import java.net.URI
import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.entity.SparqlRestriction

/**
 * Compares the performance of the different path collectors.
 */
object SparqlPathsCollectorTest {
  implicit val logger = Logger.getLogger(SparqlPathsCollectorTest.getClass.getName)

  private val tests = {
    Test(
      name = "Sider",
      uri = "http://www4.wiwiss.fu-berlin.de/sider/sparql",
      restriction = "?a <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www4.wiwiss.fu-berlin.de/sider/resource/sider/drugs>"
    ) :: Test(
      name = "DBpedia-Drugs",
      uri = "http://dbpedia.org/sparql",
      restriction = "?a <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/ontology/Drug>"
    ) :: Nil
  }

  def main(args: Array[String]) {
    for(test <- tests) test.execute()
  }

  private case class Test(name: String, uri: String, restriction: String)
  {
    def execute() {
      logger.info("Executing " + name + " test")

      val endpoint = new RemoteSparqlEndpoint(uri = new URI(uri), retryCount = 100)
      val sparqlRestriction = SparqlRestriction.fromSparql(restriction)
      val limit = Some(50)

      Timer("SparqlAggregatePathsCollector") {
        SparqlAggregatePathsCollector(endpoint, sparqlRestriction, limit).toList
      }

      Timer("SparqlSamplePathsCollector") {
        SparqlSamplePathsCollector(endpoint, sparqlRestriction, limit).toList
      }
    }
  }
}