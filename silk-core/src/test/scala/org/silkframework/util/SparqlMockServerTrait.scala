package org.silkframework.util

/**
  * Add a SPARQL mock server to tests.
  */
trait SparqlMockServerTrait extends MockServerTestTrait {
  /** Empty SPARQL Select example result. If the content does not matter, but a correct format must be returned. */
  val emptySparqlContent: ServedContent = sparqlContent(Seq("s", "p", "o"), Seq.empty)

  /** SPARQL results, plain string values only. */
  def sparqlContent(variables: Seq[String],
                    values: Traversable[Seq[String]],
                    contextPath: String = "/sparql"): ServedContent = ServedContent(content =
      Some(s"""<?xml version="1.0"?>
             |<sparql xmlns="http://www.w3.org/2005/sparql-results#">
             |  <head>
             |    ${variables.map(v => s"""<variable name="$v"/>""").mkString("    \n")}
             |  </head>
             |  <results>
             |    ${values.map(vals => serializeResult(vals, variables)).mkString(s"${" " * 4}\n")}
             |  </results>
             |</sparql>""".stripMargin),
    contentType = "application/sparql-results+xml",
    statusCode = OK)

  private def serializeResult(values: Seq[String], variables: Seq[String]): String = {
    if(values.length != variables.size) {
      throw new IllegalArgumentException("Number of values must be the same as number of variables")
    }
    s"""<result>
      |      ${values.zip(variables).map { case (value, varname) => s"""<binding name="$varname"><literal>$value</literal></binding>"""}.mkString(s"${" " * 6}\n")}
      |    </result>""".stripMargin
  }
}
