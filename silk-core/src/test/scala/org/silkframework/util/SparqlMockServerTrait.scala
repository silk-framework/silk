package org.silkframework.util

/**
  * Add a SPARQL mock server to tests.
  */
trait SparqlMockServerTrait extends MockServerTestTrait {
  /** Empty SPARQL Select example result. If the content does not matter, but a correct format must be returned. */
  val emptySparqlContent: ServedContent = sparqlContent(Seq("s", "p", "o"), Seq.empty)

  /** SPARQL results, plain string values only. */
  def sparqlContent(variables: Seq[String],
                    values: Iterable[Seq[String]],
                    contextPath: String = "/sparql"): ServedContent = ServedContent(content =
      Some(s"""{
        "head": {
          "vars": [${variables.map(v => s"\"$v\"").mkString(", ")}]
        },
        "results": {
          "bindings": [
            ${values.map(vals =>
              "{ " + variables.zip(vals).map { case (varname, value) => s"\"$varname\": {\"type\": \"literal\", \"value\": \"$value\"}" }.mkString(", ") + " }"
            ).mkString(", ")}
          ]
        }
      }""".stripMargin),
      contentType = "application/sparql-results+json",
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
