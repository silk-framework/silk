package org.silkframework.plugins.dataset.rdf

import org.scalatest.mock.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.dataset.rdf.{SparqlEndpoint, SparqlParams}

/**
  * Created on 4/25/16.
  */
class SparqlSinkTest extends FlatSpec with Matchers with MockitoSugar {
  behavior of "Sparql Sink"

  it should "generate valid statements based on the lexical value representation" in {
    val sink = new SparqlSink(SparqlParams(), mock[SparqlEndpoint])
    sink.buildStatementString("http://a", "http://b", "test") should endWith (" \"test\" .")
    sink.buildStatementString("http://a", "http://b", "123") should endWith (" \"123\"^^<http://www.w3.org/2001/XMLSchema#integer> .")
    sink.buildStatementString("http://a", "http://b", "123.45") should endWith (" \"123.45\"^^<http://www.w3.org/2001/XMLSchema#double> .")
    sink.buildStatementString("http://a", "http://b", "http://url.org") should endWith (" <http://url.org> .")
    sink.buildStatementString("http://a", "http://b", "http://url.org Some Text") should endWith (" \"http://url.org Some Text\" .")
  }
}
