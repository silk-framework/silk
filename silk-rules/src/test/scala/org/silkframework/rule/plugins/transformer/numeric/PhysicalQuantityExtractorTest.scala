package org.silkframework.rule.plugins.transformer.numeric

import org.scalatest.{FlatSpec, Matchers}

class PhysicalQuantityExtractorTest extends FlatSpec with Matchers {

  behavior of "Physical Quantity Extractor"

  it should "extract isolated physical quantities" in {
    extract("0.1F", "F", "en") shouldBe Some(0.1)
    extract("230V", "V", "en") shouldBe Some(230)
    extract("-100C", "C", "en") shouldBe Some(-100)
  }

  it should "extract isolated physical quantities with unit prefixes" in {
    extract("50km", "m", "en") shouldBe Some(50000)
    extract("500mV", "V", "en") shouldBe Some(0.5)
  }

  it should "support different localities" in {
    extract("10.5m", "m", "en") shouldBe Some(10.5)
    extract("10,5m", "m", "de") shouldBe Some(10.5)
    extract("10,000.5m", "m", "en") shouldBe Some(10000.5)
    extract("10.000,5m", "m", "de") shouldBe Some(10000.5)
  }

  it should "extract physical quantities from texts" in {
    extract("Capacitor 10000pF 10V ### durable", "F", "en") shouldBe Some(0.00000001)
    extract("Capacitor 10000pF 10V ### durable", "V", "en") shouldBe Some(10)
    extract("74LVC387xxx/f50_5.4V/3.45V_XXX", "V", "en") shouldBe Some(5.4)
  }

  it should "extract multiple physical quantities from texts" in {
    extract("2.7V/5.5V", "V", "en", 0) shouldBe Some(2.7)
    extract("2.7V/5.5V", "V", "en", 1) shouldBe Some(5.5)
    extract("2.7V/5.5V", "V", "en", 2) shouldBe None
  }

  private def extract(value: String, symbol: String, numberFormat: String, index: Int = 0): Option[Double] = {
    PhysicalQuantityExtractor(symbol, numberFormat, "", index).evaluate(value).map(_.toDouble)
  }

}
