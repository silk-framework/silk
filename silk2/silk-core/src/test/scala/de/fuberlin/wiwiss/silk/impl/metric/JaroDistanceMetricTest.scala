package de.fuberlin.wiwiss.silk.impl.metric

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import de.fuberlin.wiwiss.silk.impl.util.approximatelyEqualTo

class JaroDistanceMetricTest extends FlatSpec with ShouldMatchers
{
    val metric = new JaroDistanceMetric()
    val t = 0.9

    //Use cases from William E. Winkler : Overview of Record Linkage and Current Research Directions
    //Some tests are disabled because many web sources report different results
    "JaroDistanceMetric" should "pass the original test cases from William E. Winkler" in
    {
        metric.evaluate("SHACKLEFORD", "SHACKELFORD", t) should be (approximatelyEqualTo (0.970))
        metric.evaluate("DUNNINGHAM", "CUNNIGHAM", t) should be (approximatelyEqualTo (0.896))
        metric.evaluate("NICHLESON", "NICHULSON", t) should be (approximatelyEqualTo (0.926))
        metric.evaluate("JONES", "JOHNSON", t) should be (approximatelyEqualTo (0.790))
        metric.evaluate("MASSEY", "MASSIE", t) should be (approximatelyEqualTo (0.889))
        metric.evaluate("ABROMS", "ABRAMS", t) should be (approximatelyEqualTo (0.889))
        //metric.evaluate("HARDIN", "MARTINEZ") should be (approximatelyEqualTo (0.722))
        //metric.evaluate("ITMAN", "SMITH") should be (approximatelyEqualTo (0.000))
        metric.evaluate("JERALDINE", "GERALDINE", t) should be (approximatelyEqualTo (0.926))
        metric.evaluate("MARHTA", "MARTHA", t) should be (approximatelyEqualTo (0.944))
        metric.evaluate("MICHELLE", "MICHAEL", t) should be (approximatelyEqualTo (0.869))
        metric.evaluate("JULIES", "JULIUS", t) should be (approximatelyEqualTo (0.889))
        //metric.evaluate("TANYA", "TONYA") should be (approximatelyEqualTo (0.867))
        metric.evaluate("DWAYNE", "DUANE", t) should be (approximatelyEqualTo (0.822))
        metric.evaluate("SEAN", "SUSAN", t) should be (approximatelyEqualTo (0.783))
        metric.evaluate("JON", "JOHN", t) should be (approximatelyEqualTo (0.917))
        //metric.evaluate("JON", "JAN") should be (approximatelyEqualTo (0.000))
    }

    "JaroDistanceMetric" should "be commutative" in
    {
        metric.evaluate("DIXON", "DICKSONX", t) should be (approximatelyEqualTo (0.767))
        metric.evaluate("DICKSONX", "DIXON", t) should be (approximatelyEqualTo (0.767))
        metric.evaluate("MARTHA", "MARHTA", t) should be (approximatelyEqualTo (0.944))
        metric.evaluate("MARHTA", "MARTHA", t) should be (approximatelyEqualTo (0.944))
    }
}