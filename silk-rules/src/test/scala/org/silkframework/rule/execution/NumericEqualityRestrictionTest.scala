package org.silkframework.rule.execution


import org.silkframework.entity.paths.UntypedPath
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class NumericEqualityRestrictionTest extends AnyFlatSpec with Matchers {
  behavior of "Numeric Equality Restriction"

  private val oneHopPath = UntypedPath.parse("<http://prop1>")
  private val twoHopPath = UntypedPath.parse("\\<http://prop1>/<http://prop2>")

  it should "serialize correct SPARQL filter expressions for exact precision" in {
    sparqlFilterExpression(oneHopPath, 2.3) mustBe
        "?s <http://prop1> ?generatedVarValue .\n\n" +
        "FILTER (<http://www.w3.org/2001/XMLSchema#double>(?generatedVarValue) = 2.3)"

    sparqlFilterExpression(oneHopPath, 0.00000000001) mustBe
        "?s <http://prop1> ?generatedVarValue .\n\n" +
        "FILTER (<http://www.w3.org/2001/XMLSchema#double>(?generatedVarValue) = 1.0E-11)"
  }

  it should "serialize correct SPARQL filter expressions for slightly loosened precision" in {
    // The precision is decreased (actual value increase of * 0.01) to make up for rounding errors
    // this value does not need to be exact, since query result recall is important, not precision.
    sparqlFilterExpression(oneHopPath, 2.3, precision = 0.01) mustBe
        "?s <http://prop1> ?generatedVarValue .\n\n" +
        "FILTER ((<http://www.w3.org/2001/XMLSchema#double>(?generatedVarValue) <= 2.3101 && <http://www.w3.org/2001/XMLSchema#double>(?generatedVarValue) >= 2.2899))"

    sparqlFilterExpression(twoHopPath, 0.00000000001, precision = 0.000000000001) mustBe
        "?generatedVarInter1 <http://prop1> ?s .\n" +
        "?generatedVarInter1 <http://prop2> ?generatedVarValue .\n\n" +
        "FILTER ((<http://www.w3.org/2001/XMLSchema#double>(?generatedVarValue) <= 1.101E-11 && <http://www.w3.org/2001/XMLSchema#double>(?generatedVarValue) >= 8.99E-12))"
  }

  private def sparqlFilterExpression(path: UntypedPath, value: Double, precision: Double = 0.0): String = {
    NumericEqualityRestriction(path, value, precision).toSparqlFilter("s", "generatedVar").toSparql
  }
}
