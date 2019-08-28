package org.silkframework.plugins.dataset.rdf.tasks.templating

import org.scalatest.{FlatSpec, MustMatchers}

class SparqlTemplatingEngineVelocityTest extends FlatSpec with MustMatchers {
  behavior of "Velocity SPARQL Templating Engine"

  it should "output the correct input paths of the template" in {
    val templateString =
      """
        |$row.asUri("subject")
        |#if ( $row.exists("somePath") )
        |  Plain: $row.asPlainLiteral("somePath")
        |  Raw: $row.asRawUnsafe("trustedValuePath")
        |#end
        |""".stripMargin
    val engine = SparqlTemplatingEngineVelocity(templateString, 1)
    engine.inputPaths().toSet mustBe Set("subject", "somePath", "trustedValuePath")
  }
}
