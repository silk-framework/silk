package org.silkframework.rule.plugins.transformer.numeric

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class PhysicalQuantityExtractorTest extends AnyFlatSpec with Matchers {

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
    extract("74LVC387xxx/f50_5.4V_3.45V_XXX", "V", "en") shouldBe Some(5.4)
  }

  it should "extract multiple physical quantities from texts" in {
    extract("2.7V/5.5V", "V", "en", 0) shouldBe Some(2.7)
    extract("2.7V/5.5V", "V", "en", 1) shouldBe Some(5.5)
    extract("2.7V/5.5V", "V", "en", 2) shouldBe None
  }

  it should "parse correctly when evaluated from multiple threads" in {
    import java.util.concurrent.{ConcurrentLinkedQueue, CyclicBarrier}
    import scala.util.control.NonFatal
    val extractor = PhysicalQuantityExtractor(symbol = "V")
    val inputs = IndexedSeq("1.5V" -> "1.5", "987654.125V" -> "987654.125")
    val barrier = new CyclicBarrier(8)
    val errors = new ConcurrentLinkedQueue[String]()
    val threads = for(t <- 0 until 8) yield new Thread(() => {
      val (input, expected) = inputs(t % 2)
      barrier.await()
      for(_ <- 1 to 50000) {
        val output = try extractor.evaluate(input).getOrElse("<none>") catch { case NonFatal(ex) => ex.toString }
        if(output != expected) {
          errors.add(s"$input -> $output")
        }
      }
    })
    threads.foreach(_.start())
    threads.foreach(_.join())
    errors shouldBe empty
  }

  private def extract(value: String, symbol: String, numberFormat: String, index: Int = 0): Option[Double] = {
    PhysicalQuantityExtractor(symbol, numberFormat, "", index).evaluate(value).map(_.toDouble)
  }

}
