package org.silkframework.rule.plugins.transformer.numeric

import javax.measure.spi.ServiceProvider
import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.rule.plugins.transformer.normalize.UcumUnitTransformer
import systems.uom.ucum.format.UCUMFormat
import systems.uom.ucum.format.UCUMFormat.Variant

class UcumUnitTransformerTest extends FlatSpec with Matchers {

  val config =
    """
      |quantity, unit name , symbol, equals formula (using already registered base units), all additional columns: alternative symbols
      |Area , Are       , are   , 100.m2
      |Energy, Calory    , chal   , 4.1868.J  , Call
      |Energy, denier    , den   , g/(9.km)
      |ElectricCurrent, Coulomb per second, CoulombPerSecond   , C/s
    """.stripMargin

  it should "test parser" in {
    import javax.measure.spi.ServiceProvider
    val parser =ServiceProvider.current.getUnitFormatService.getUnitFormat("CS")

    parser.parse("m").toString shouldBe "m"
    parser.parse("100.m2").toString shouldBe "hone·m²"
    parser.parse("g/(9.km)").toString shouldBe "g/(one*9.0·km)"
    parser.parse("(1000.m)/(60.min)").toString shouldBe "m·kone/(one*60.0·min)"
  }

  it should "testEvaluate" in {

    val transformer = UcumUnitTransformer(charSet = "Unicode", config)

    transformer.evaluate("are") shouldEqual "are"
    transformer.evaluate("den") shouldEqual "den"
    transformer.evaluate("CoulombPerSecond") shouldEqual "CoulombPerSecond"
  }

}
