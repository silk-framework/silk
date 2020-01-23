package org.silkframework.plugins.dataset.rdf

import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.dataset.rdf.{SparqlEndpoint, SparqlParams}
import org.silkframework.entity.{DoubleValueType, IntValueType, StringValueType, UntypedValueType, UriValueType, ValueType}
import org.silkframework.plugins.dataset.rdf.access.SparqlSink

class SparqlSinkTest extends FlatSpec with Matchers with MockitoSugar {
  behavior of "Sparql Sink"

  val SUBJ = "http://a"
  val PROP = "http://b"

  it should "generate valid statements based on the lexical value representation" in {
    val sink = new SparqlSink(SparqlParams(), mock[SparqlEndpoint])
    sink.buildStatementString(SUBJ, PROP, "test", ValueType.UNTYPED) should endWith (" \"test\" .\n")
    sink.buildStatementString(SUBJ, PROP, "123", ValueType.INT) should endWith (" \"123\"^^<http://www.w3.org/2001/XMLSchema#integer> .\n")
    sink.buildStatementString(SUBJ, PROP, "123.45", ValueType.DOUBLE) should endWith (" \"123.45\"^^<http://www.w3.org/2001/XMLSchema#double> .\n")
    sink.buildStatementString(SUBJ, PROP, "http://url.org", ValueType.URI) should endWith (" <http://url.org> .\n")
    sink.buildStatementString(SUBJ, PROP, "http://url.org Some Text", ValueType.UNTYPED) should endWith (" \"http://url.org Some Text\" .\n")
    sink.buildStatementString(SUBJ, PROP, "http://url.org Some Text", ValueType.STRING) should endWith (" \"http://url.org Some Text\" .\n")
  }
}
