package de.fuberlin.wiwiss.silk.plugins.metric

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import de.fuberlin.wiwiss.silk.plugins.util.approximatelyEqualTo

class JaroDistanceMetricTest extends FlatSpec with ShouldMatchers {
  val metric = new JaroDistanceMetric()

  //Use cases from William E. Winkler : Overview of Record Linkage and Current Research Directions
  //Some tests are disabled because many web sources report different results
  "JaroDistanceMetric" should "pass the original test cases from William E. Winkler" in {
    sim("SHACKLEFORD", "SHACKELFORD") should be(approximatelyEqualTo(0.970))
    sim("DUNNINGHAM", "CUNNIGHAM") should be(approximatelyEqualTo(0.896))
    sim("NICHLESON", "NICHULSON") should be(approximatelyEqualTo(0.926))
    sim("JONES", "JOHNSON") should be(approximatelyEqualTo(0.790))
    sim("MASSEY", "MASSIE") should be(approximatelyEqualTo(0.889))
    sim("ABROMS", "ABRAMS") should be(approximatelyEqualTo(0.889))
    //sim("HARDIN", "MARTINEZ") should be (approximatelyEqualTo (0.722))
    //sim("ITMAN", "SMITH") should be (approximatelyEqualTo (0.000))
    sim("JERALDINE", "GERALDINE") should be(approximatelyEqualTo(0.926))
    sim("MARHTA", "MARTHA") should be(approximatelyEqualTo(0.944))
    sim("MICHELLE", "MICHAEL") should be(approximatelyEqualTo(0.869))
    sim("JULIES", "JULIUS") should be(approximatelyEqualTo(0.889))
    //sim("TANYA", "TONYA") should be (approximatelyEqualTo (0.867))
    sim("DWAYNE", "DUANE") should be(approximatelyEqualTo(0.822))
    sim("SEAN", "SUSAN") should be(approximatelyEqualTo(0.783))
    sim("JON", "JOHN") should be(approximatelyEqualTo(0.917))
    //sim("JON", "JAN") should be (approximatelyEqualTo (0.000))
  }

  "JaroDistanceMetric" should "be commutative" in {
    sim("DIXON", "DICKSONX") should be(approximatelyEqualTo(0.767))
    sim("DICKSONX", "DIXON") should be(approximatelyEqualTo(0.767))
    sim("MARTHA", "MARHTA") should be(approximatelyEqualTo(0.944))
    sim("MARHTA", "MARTHA") should be(approximatelyEqualTo(0.944))
  }

  private def sim(str1: String, str2: String) = 1.0 - metric.evaluate(str1, str2)
}