package controllers.sparqlapi

import org.scalatest.{FlatSpec, MustMatchers}
import org.scalatestplus.mockito.MockitoSugar

class SparqlQueryTypeTest extends FlatSpec with MustMatchers with MockitoSugar {

  behavior of "SparqlQueryTypeTest"

  it should "determine SparqlQueryType SELECT" in {
    val correctSelect1 = s"""prefix ex:	<http://www.example.org/schema#>
                        prefix in:	<http://www.example.org/instance#>

                        select ?x ?l (datatype(?l) as ?dt) where {
                          ?x ex:p ?l
                        }"""
    val correctSelect2 = s"""prefix ex:	<http://www.example.org/schema#>
                        prefix in:	<http://www.example.org/instance#>

                        select distinct ?x ?l (datatype(?l) as ?dt) where {
                          ?x ex:p ?l
                        }"""
    val correctSelect3 = s"""
                        SELECT REDUCED ?x ?l (datatype(?l) as ?dt) where {
                          ?x ex:p ?l
                        }"""
    val incorrectSelect1 = s"""prefix ex:	<http://www.example.org/schema#>
                        prefix in:	<http://www.example.org/instance#>

                        Select ?x ?l (datatype(?l) as ?dt) where {
                          ?x ex:p ?l
                        }"""
    val incorrectSelect2 = s"""prefix ex:	<http://www.example.org/schema#>
                        prefix in:	<http://www.example.org/instance#>

                        SELECT x l (datatype(?l) as ?dt) where {
                          ?x ex:p ?l
                        }"""

    SparqlQueryType.determineSparqlQueryType(correctSelect1) mustBe SparqlQueryType.SELECT
    SparqlQueryType.determineSparqlQueryType(correctSelect2) mustBe SparqlQueryType.SELECT
    SparqlQueryType.determineSparqlQueryType(correctSelect3) mustBe SparqlQueryType.SELECT

    intercept[IllegalArgumentException]{
      SparqlQueryType.determineSparqlQueryType(incorrectSelect1)
    }
    intercept[IllegalArgumentException]{
      SparqlQueryType.determineSparqlQueryType(incorrectSelect2)
    }
  }

  it should "determine SparqlQueryType ASK" in {
    val correctAsk1 = s"""	ASK {
		GRAPH <http://example.org/protocol-update-dataset-test/> {
			<http://kasei.us/2009/09/sparql/data/data1.rdf> a <http://purl.org/dc/terms/BibliographicResource>
		}
	}"""
    val correctAsk2 = s"""prefix ex:	<http://www.example.org/schema#>
                        prefix in:	<http://www.example.org/instance#>

    ASK WHERE {
		GRAPH <http://example.org/protocol-update-dataset-test/> {
			<http://kasei.us/2009/09/sparql/data/data1.rdf> a <http://purl.org/dc/terms/BibliographicResource>
		}
	}"""
    val incorrectAsk1 = s"""    ASK ?g {
		GRAPH ?g {
			<http://kasei.us/2009/09/sparql/data/data1.rdf> a <http://purl.org/dc/terms/BibliographicResource>
		}"""
    val incorrectAsk2 = s"""    ASKS {
		GRAPH <http://example.org/protocol-update-dataset-test/> {
			<http://kasei.us/2009/09/sparql/data/data1.rdf> a <http://purl.org/dc/terms/BibliographicResource>
		}"""

    SparqlQueryType.determineSparqlQueryType(correctAsk1) mustBe SparqlQueryType.ASK
    SparqlQueryType.determineSparqlQueryType(correctAsk2) mustBe SparqlQueryType.ASK

    intercept[IllegalArgumentException]{
      SparqlQueryType.determineSparqlQueryType(incorrectAsk1)
    }
    intercept[IllegalArgumentException]{
      SparqlQueryType.determineSparqlQueryType(incorrectAsk2)
    }
  }

  it should "determine SparqlQueryType DESCRIBE" in {
    val correctDescribe1 = s"""PREFIX foaf:   <http://xmlns.com/foaf/0.1/>
          DESCRIBE ?x
          WHERE    { ?x foaf:mbox <mailto:alice@org> }"""
    val correctDescribe2 = s"""describe ?x
          WHERE    { ?x foaf:mbox <mailto:alice@org> }"""
    val incorrectDescribe1 = s"""PREFIX foaf:   <http://xmlns.com/foaf/0.1/>
          DESCRIBE x
          WHERE    { ?x foaf:mbox <mailto:alice@org> }"""

    SparqlQueryType.determineSparqlQueryType(correctDescribe1) mustBe SparqlQueryType.DESCRIBE
    SparqlQueryType.determineSparqlQueryType(correctDescribe2) mustBe SparqlQueryType.DESCRIBE

    intercept[IllegalArgumentException]{
      SparqlQueryType.determineSparqlQueryType(incorrectDescribe1)
    }
  }

  it should "determine SparqlQueryType CONSTRUCT" in {
    val correctConstruct1 = s"""PREFIX foaf:   <http://xmlns.com/foaf/0.1/>
          PREFIX org:    <http://example.com/ns#>
          CONSTRUCT { ?x foaf:name ?name }
          WHERE  { ?x org:employeeName ?name }"""
    val correctConstruct2 = s"""
          construct {  }
          WHERE  {  }"""
    val incorrectConstruct1 = s"""
          construct { ?x foaf:name ?name }"""
    val incorrectConstruct2 = s"""PREFIX foaf:   <http://xmlns.com/foaf/0.1/>
          PREFIX org:    <http://example.com/ns#>
          Construct {  }
          Where  { ?x org:employeeName ?name }"""

    SparqlQueryType.determineSparqlQueryType(correctConstruct1) mustBe SparqlQueryType.CONSTRUCT
    SparqlQueryType.determineSparqlQueryType(correctConstruct2) mustBe SparqlQueryType.CONSTRUCT

    intercept[IllegalArgumentException]{
      SparqlQueryType.determineSparqlQueryType(incorrectConstruct1)
    }
    intercept[IllegalArgumentException]{
      SparqlQueryType.determineSparqlQueryType(incorrectConstruct2)
    }
  }
}
