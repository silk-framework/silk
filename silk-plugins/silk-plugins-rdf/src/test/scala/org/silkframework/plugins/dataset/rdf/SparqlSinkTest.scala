package org.silkframework.plugins.dataset.rdf

import org.scalatest.mock.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.dataset.rdf.{SparqlEndpoint, SparqlParams}
import org.silkframework.entity.UntypedValueType
import org.silkframework.plugins.dataset.rdf.access.SparqlSink

/**
  * Created on 4/25/16.
  */
class SparqlSinkTest extends FlatSpec with Matchers with MockitoSugar {
  behavior of "Sparql Sink"
  val SUBJ = "http://a"
  val PROP = "http://b"

  it should "generate valid statements based on the lexical value representation" in {
    val sink = new SparqlSink(SparqlParams(), mock[SparqlEndpoint])
    sink.buildStatementString(SUBJ, PROP, "test", UntypedValueType) should endWith (" \"test\" .\n")
    sink.buildStatementString(SUBJ, PROP, "123", UntypedValueType) should endWith (" \"123\"^^<http://www.w3.org/2001/XMLSchema#integer> .\n")
    sink.buildStatementString(SUBJ, PROP, "123.45", UntypedValueType) should endWith (" \"123.45\"^^<http://www.w3.org/2001/XMLSchema#double> .\n")
    sink.buildStatementString(SUBJ, PROP, "http://url.org", UntypedValueType) should endWith (" <http://url.org> .\n")
    sink.buildStatementString(SUBJ, PROP, "http://url.org Some Text", UntypedValueType) should endWith (" \"http://url.org Some Text\" .\n")
  }
}
