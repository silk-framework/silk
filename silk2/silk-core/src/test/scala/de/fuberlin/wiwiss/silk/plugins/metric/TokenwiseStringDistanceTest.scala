package de.fuberlin.wiwiss.silk.plugins.metric

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class TokenwiseStringDistanceTest extends FlatSpec with ShouldMatchers {
  val metric = new TokenwiseStringDistance(metricName = "levenshtein", stopwords = "and or in on the a from thy mr mrs who", nonStopwordWeight = 0.1, stopwordWeight = 0.001)

  "TokenwiseStringDistance" should "return distance 0 (several seditious scribes, several seditious scribes)" in {
    metric.evaluate("several seditious scribes", "several seditious scribes", 1.0) should equal(0.0)
  }

  "TokenwiseStringDistance" should "return distance 0 (several seditious scribes, scribes seditious several)" in {
    metric.evaluate("several seditious scribes", "scribes seditious several", 1.0) should equal(0.0)
  }


  "TokenwiseStringDistance" should "return distance 0.251 (several seditious scribes, several seditious scribes from caesarea)" in {
    metric.evaluate("several seditious scribes", "several seditious scribes from caesarea", 1.0) should be(0.251 plusOrMinus 0.001)
    metric.evaluate("several seditious scribes from caesarea", "several seditious scribes", 1.0) should be(0.251 plusOrMinus 0.001)
  }

  "TokenwiseStringDistance" should "return distance 0.09091 (several seditious scribes, several seditious scibes)" in {
    metric.evaluate("several seditious scibes", "several seditious scribes", 1.0) should be(0.091 plusOrMinus 0.001)
    metric.evaluate("several seditious scribes", "several seditious scibes", 1.0) should be(0.091 plusOrMinus 0.001)
  }

  "TokenwiseStringDistance" should "return distance 1.0 (,)" in {
    metric.evaluate("", "", 1.0) should equal(1.0)
  }

  "TokenwiseStringDistance" should "return distance 1.0 (Anything,)" in {
    metric.evaluate("Anything", "", 1.0) should equal(1.0)
    metric.evaluate("", "Anything", 1.0) should equal(1.0)
  }

  "TokenwiseStringDistance" should "return distance 0.001 (Hotel Hilton in Manhattan, hotel hilton manhattan)" in {
    metric.evaluate("Hotel Hilton in Manhattan", "hotel hilton manhattan", 1.0) should be(0.001 plusOrMinus 0.001)
  }

  "TokenwiseStringDistance" should "return different distances for similar terms if stopwords are involved" in {
    //both of these are stopwords - match score is high
    metric.evaluate("the", "thy", 1.0) should be(0.5 plusOrMinus 0.001)
    //one of these is a stopword - match score is low
    metric.evaluate("and", "any", 1.0) should be(0.9804 plusOrMinus 0.001)
    //none of these is a stopword - match score is high
    metric.evaluate("war", "was", 1.0) should be(0.5 plusOrMinus 0.001)
    //a match where the stopwords in both strings are matched - they don't contribute much to the result
    metric.evaluate("Mr Doe", "Mrs Dow", 1.0) should be(0.5 plusOrMinus 0.001)
    //a match where the stopwords in both strings are matched - they don't contribute much to the result
    metric.evaluate("Mr John Doe", "Mrs John Doe", 1.0) should be(0.0 plusOrMinus 0.0001)
    //identical match containing stopwords:
    metric.evaluate("Mr John Doe", "Mr John Doe", 1.0) should equal(0.0)
    //all-stopwords matches in comparison (try this with normal stopword processing!):
    metric.evaluate("the who", "the who", 1.0) should equal(0.0)
    metric.evaluate("the the", "the who", 1.0) should equal(0.5)
  }

  "TokenwiseStringDistance" should "return distance 0.5 (Hotel Hotel, Hotel)" in {
    //test if only one of two identical tokens is matched
    metric.evaluate("Hotel Hotel", "Hotel", 1.0) should be(0.5 plusOrMinus 0.001)
  }

  "TokenwiseStringDistance" should "return distance 0.5 (Hotel Hotel, Hotel) if token length is taken into account" in {
    val myMetric = new TokenwiseStringDistance(metricName = "levenshtein", stopwords = "and or in on the a from thy mr mrs", nonStopwordWeight = 0.1, stopwordWeight = 0.001, adjustByTokenLength = true)
    myMetric.evaluate("Hotel Hotel", "Hotel", 1.0) should be(0.5 plusOrMinus 0.001)
  }

  "TokenwiseStringDistance" should "return distance 0.667 (Hotel California, Hotel) if token length is taken into account" in {
    val myMetric = new TokenwiseStringDistance(metricName = "levenshtein", stopwords = "and or in on the a from thy mr mrs", nonStopwordWeight = 0.1, stopwordWeight = 0.001, adjustByTokenLength = true)
    myMetric.evaluate("Hotel California", "Hotel", 1.0) should be(0.667 plusOrMinus 0.001)
  }

  "TokenwiseStringDistance" should "return distance 0.5 (several seditious scribes, scribes seditious several) with orderingImpact of 0.5" in {
    val myMetric = new TokenwiseStringDistance(metricName = "levenshtein", stopwords = "and or in on the a from thy mr mrs", nonStopwordWeight = 0.1, stopwordWeight = 0.001, orderingImpact = 0.5)
    myMetric.evaluate("several seditious scribes", "scribes seditious several", 1.0) should equal(0.5)
  }

  "TokenwiseStringDistance" should "return different distances for when matchThreshold is used (several seditious scribes, several sedated scribes)" in {
    val myMetric = new TokenwiseStringDistance(metricName = "levenshtein", stopwords = "and or in on the a from thy mr mrs", nonStopwordWeight = 0.1, stopwordWeight = 0.001, matchThreshold = 0.85)
    metric.evaluate("several seditious scribes", "several sedated scribes", 1.0) should be(0.313 plusOrMinus 0.001)
    myMetric.evaluate("several seditious scribes", "several sedated scribes", 1.0) should be(0.5 plusOrMinus 0.001)
  }

  "TokenwiseStringDistance" should "return 1.0 in (Sirenia + Niobeth, ould Sirenia and for Niobeth) with special settings" in {
    val myMetric = new TokenwiseStringDistance(metricName = "levenshtein", stopwords = "and for ould", nonStopwordWeight = 1.0, stopwordWeight = 0.0)
    myMetric.evaluate("Sirenia + Niobeth", "ould Sirenia and for Niobeth", 1.0) should equal(0.0)
  }

  "TokenwiseStringDistance" should "return 1.0 in (Hotel California, California) with ONLY one stopword 'Hotel'" in {
    val myMetric = new TokenwiseStringDistance(metricName = "levenshtein", stopwords = "Hotel", nonStopwordWeight = 1.0, stopwordWeight = 0.0)
    myMetric.evaluate("Hotel California", "California", 1.0) should equal(0.0)
  }

  "TokenwiseStringDistance" should "return the same value as JaccardSimilarity with the right settings" in {
    val myMetric = new TokenwiseStringDistance(metricName = "levenshtein", stopwords = "", nonStopwordWeight = 1.0, stopwordWeight = 1.0, matchThreshold = 1.0)
    val jaccardMetric = new JaccardDistance()
    val tokenWiseScore = myMetric.evaluate("several seditious scribes", "several seditious scribes from caesarea", 1.0)
    val jaccardScore = jaccardMetric.apply(Set("several", "seditious", "scribes"), Set("several", "seditious", "scribes", "from", "caesarea"), 1.0)
    tokenWiseScore should equal(jaccardScore)

  }



  ////// test for metric timing
  //    "TokenwiseStringDistance" should "return 1.0 in (The expansion of chains like Abercrombie and Gap to Europe is based on a major shift in how young Europeans think about American fashion. , The expansion of chains like Gap and Abercrombie to Europe is fueled by a major shift in how young Europeans think about American fashion. ) with special settings" in
  //    {
  //        val myMetric = new TokenwiseStringDistance(metricName = "levenshtein", stopwords="and or in on the a from thy mr mrs",nonStopwordWeight = 0.1, stopwordWeight=0.001, orderingImpact = 0.2)
  //        var time = System.nanoTime();
  //        for (i <- 1 to 1000) {
  //          myMetric.evaluate("The expansion of chains like Abercrombie and Gap to Europe is based on a major shift in how young Europeans think about American fashion.", "The expansion of chains like Gap and Abercrombie to Europe is fueled by a major shift in how young Europeans think about American fashion.", 0.0)
  //        }
  //        println("comparison of long strings took " +  ((System.nanoTime() - time).toDouble / 1000000.0) + " + millis")
  //
  //        time = System.nanoTime();
  //        for (i <- 1 to 1000) {
  //          myMetric.evaluate("Sirenia + Niobeth", "ould Sirenia and for Niobeth", 0.0)
  //        }
  //        println("comparison of short strings took " +  ((System.nanoTime() - time).toDouble / 1000000.0) + " + millis")
  //
  //    }
}