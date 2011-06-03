package de.fuberlin.wiwiss.silk.impl.metric

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import de.fuberlin.wiwiss.silk.impl.util.approximatelyEqualTo

class JaroWinklerMetricTest extends FlatSpec with ShouldMatchers
{
    val metric = new JaroWinklerSimilarity()
    val t = 0.9

    //Use cases from William E. Winkler : Overview of Record Linkage and Current Research Directions
    //Some tests are disabled because many web sources report different results
    "JaroWinklerSimilarity" should "pass the original test cases from William E. Winkler" in
    {
        //metric.evaluate("SHACKLEFORD", "SHACKELFORD") should be (approximatelyEqualTo (0.982))
        metric.evaluate("DUNNINGHAM", "CUNNIGHAM", t) should be (approximatelyEqualTo (0.896))
        metric.evaluate("NICHLESON", "NICHULSON", t) should be (approximatelyEqualTo (0.956))
        metric.evaluate("JONES", "JOHNSON", t) should be (approximatelyEqualTo (0.832))
        metric.evaluate("MASSEY", "MASSIE", t) should be (approximatelyEqualTo (0.933))
        metric.evaluate("ABROMS", "ABRAMS", t) should be (approximatelyEqualTo (0.922))
        //metric.evaluate("HARDIN", "MARTINEZ", t) should be (approximatelyEqualTo (0.000))
        //metric.evaluate("ITMAN", "SMITH", t) should be (approximatelyEqualTo (0.000))
        metric.evaluate("JERALDINE", "GERALDINE", t) should be (approximatelyEqualTo (0.926))
        metric.evaluate("MARHTA", "MARTHA", t) should be (approximatelyEqualTo (0.961))
        metric.evaluate("MICHELLE", "MICHAEL", t) should be (approximatelyEqualTo (0.921))
        metric.evaluate("JULIES", "JULIUS", t) should be (approximatelyEqualTo (0.933))
        //metric.evaluate("TANYA", "TONYA", t) should be (approximatelyEqualTo (0.880))
        metric.evaluate("DWAYNE", "DUANE", t) should be (approximatelyEqualTo (0.840))
        metric.evaluate("SEAN", "SUSAN", t) should be (approximatelyEqualTo (0.805))
        metric.evaluate("JON", "JOHN", t) should be (approximatelyEqualTo (0.933))
        //metric.evaluate("JON", "JAN", t) should be (approximatelyEqualTo (0.000))
    }

    "JaroWinklerSimilarity" should "be commutative" in
    {
        metric.evaluateDistance("JONES", "JOHNSON") should be (approximatelyEqualTo (0.832))
        metric.evaluateDistance("JOHNSON", "JONES") should be (approximatelyEqualTo (0.832))
    }  
}