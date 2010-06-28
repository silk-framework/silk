package de.fuberlin.wiwiss.silk.impl.metric

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import de.fuberlin.wiwiss.silk.impl.util.approximatelyEqualTo

class JaroDistanceMetricTest extends FlatSpec with ShouldMatchers
{
    val metric = new JaroDistanceMetric()

    //Use cases from William E. Winkler : Overview of Record Linkage and Current Research Directions
    //Some tests are disabled because many web sources report different results
    "JaroDistanceMetric" should "pass the original test cases from William E. Winkler" in
    {
        metric.evaluate("SHACKLEFORD", "SHACKELFORD") should be (approximatelyEqualTo (0.970))
        metric.evaluate("DUNNINGHAM", "CUNNIGHAM") should be (approximatelyEqualTo (0.896))
        metric.evaluate("NICHLESON", "NICHULSON") should be (approximatelyEqualTo (0.926))
        metric.evaluate("JONES", "JOHNSON") should be (approximatelyEqualTo (0.790))
        metric.evaluate("MASSEY", "MASSIE") should be (approximatelyEqualTo (0.889))
        metric.evaluate("ABROMS", "ABRAMS") should be (approximatelyEqualTo (0.889))
        //metric.evaluate("HARDIN", "MARTINEZ") should be (approximatelyEqualTo (0.722))
        //metric.evaluate("ITMAN", "SMITH") should be (approximatelyEqualTo (0.000))
        metric.evaluate("JERALDINE", "GERALDINE") should be (approximatelyEqualTo (0.926))
        metric.evaluate("MARHTA", "MARTHA") should be (approximatelyEqualTo (0.944))
        metric.evaluate("MICHELLE", "MICHAEL") should be (approximatelyEqualTo (0.869))
        metric.evaluate("JULIES", "JULIUS") should be (approximatelyEqualTo (0.889))
        //metric.evaluate("TANYA", "TONYA") should be (approximatelyEqualTo (0.867))
        metric.evaluate("DWAYNE", "DUANE") should be (approximatelyEqualTo (0.822))
        metric.evaluate("SEAN", "SUSAN") should be (approximatelyEqualTo (0.783))
        metric.evaluate("JON", "JOHN") should be (approximatelyEqualTo (0.917))
        //metric.evaluate("JON", "JAN") should be (approximatelyEqualTo (0.000))
    }

    "JaroDistanceMetric" should "be commutative" in
    {
        metric.evaluate("DIXON", "DICKSONX") should be (approximatelyEqualTo (0.767))
        metric.evaluate("DICKSONX", "DIXON") should be (approximatelyEqualTo (0.767))
        metric.evaluate("MARTHA", "MARHTA") should be (approximatelyEqualTo (0.944))
        metric.evaluate("MARHTA", "MARTHA") should be (approximatelyEqualTo (0.944))
    }
}