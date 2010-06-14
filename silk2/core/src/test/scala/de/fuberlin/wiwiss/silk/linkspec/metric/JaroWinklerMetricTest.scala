package de.fuberlin.wiwiss.silk.linkspec.metric

import org.scalatest.FlatSpec
import de.fuberlin.wiwiss.silk.metric.JaroWinklerMetric
import org.scalatest.matchers.{BeMatcher, MatchResult, ShouldMatchers}
import de.fuberlin.wiwiss.silk.linkspec.util.approximatelyEqualTo

class JaroWinklerMetricTest extends FlatSpec with ShouldMatchers
{
    val metric = new JaroWinklerMetric()

    //Use cases from William E. Winkler : Overview of Record Linkage and Current Research Directions
    //Some tests are disabled because many web sources report different results
    "JaroWinklerMetric" should "pass the original test cases from William E. Winkler" in
    {
        //metric.evaluate("SHACKLEFORD", "SHACKELFORD") should be (approximatelyEqualTo (0.982))
        metric.evaluate("DUNNINGHAM", "CUNNIGHAM") should be (approximatelyEqualTo (0.896))
        metric.evaluate("NICHLESON", "NICHULSON") should be (approximatelyEqualTo (0.956))
        metric.evaluate("JONES", "JOHNSON") should be (approximatelyEqualTo (0.832))
        metric.evaluate("MASSEY", "MASSIE") should be (approximatelyEqualTo (0.933))
        metric.evaluate("ABROMS", "ABRAMS") should be (approximatelyEqualTo (0.922))
        //metric.evaluate("HARDIN", "MARTINEZ") should be (approximatelyEqualTo (0.000))
        //metric.evaluate("ITMAN", "SMITH") should be (approximatelyEqualTo (0.000))
        metric.evaluate("JERALDINE", "GERALDINE") should be (approximatelyEqualTo (0.926))
        metric.evaluate("MARHTA", "MARTHA") should be (approximatelyEqualTo (0.961))
        metric.evaluate("MICHELLE", "MICHAEL") should be (approximatelyEqualTo (0.921))
        metric.evaluate("JULIES", "JULIUS") should be (approximatelyEqualTo (0.933))
        //metric.evaluate("TANYA", "TONYA") should be (approximatelyEqualTo (0.880))
        metric.evaluate("DWAYNE", "DUANE") should be (approximatelyEqualTo (0.840))
        metric.evaluate("SEAN", "SUSAN") should be (approximatelyEqualTo (0.805))
        metric.evaluate("JON", "JOHN") should be (approximatelyEqualTo (0.933))
        //metric.evaluate("JON", "JAN") should be (approximatelyEqualTo (0.000))
    }

    "JaroWinklerMetric" should "be commutative" in
    {
        metric.evaluateDistance("JONES", "JOHNSON") should be (approximatelyEqualTo (0.832))
        metric.evaluateDistance("JOHNSON", "JONES") should be (approximatelyEqualTo (0.832))
    }  
}
