package org.silkframework.rule.test

import org.silkframework.rule.similarity.{Aggregator, WeightedSimilarityScore}
import org.silkframework.runtime.plugin.{AggregatorExampleValue, ClassPluginDescription, PluginContext}
import org.silkframework.test.PluginTest

import scala.reflect.ClassTag

/**
  * Can be mixed into an Aggregator test spec.
  * Will iterate through all [[AggregatorExampleValue]] annotations and generate a test case for each.
  *
  * @tparam T The class to be tested.
  */
abstract class AggregatorTest[T <: Aggregator : ClassTag] extends PluginTest {

  /** Numeric values may differ slightly from their expected values. */
  private val epsilon = 0.0001

  /** The class of the aggregator to be tested. */
  private lazy val pluginClass = {
    val clazz = implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]]
    require(clazz != classOf[Nothing], "Type parameter T is no defined")
    clazz
  }

  /** The plugin description of the aggregator to be tested. */
  private lazy val pluginDesc = ClassPluginDescription(pluginClass)

  /** All available aggregator tests. */
  private lazy val aggregatorTests: Seq[AggregatorTest] = {
    val aggregatorExamples = AggregatorExampleValue.retrieve(pluginClass)
    for(example <- aggregatorExamples) yield {
      new AggregatorTest(example)
    }
  }

  // Add all aggregator tests
  aggregatorTests.foreach(_.addTest())

  // Forward one aggregator for general plugin testing.
  override protected lazy val pluginObject: AnyRef = {
    require(aggregatorTests.nonEmpty, s"$pluginClass does not define any AggregatorExample annotation.")
    aggregatorTests.head.aggregator
  }

  private class AggregatorTest(example: AggregatorExampleValue) {
    val aggregator: T = pluginDesc(example.parameters)(PluginContext.empty)

    def addTest(): Unit = {
      val result = aggregator.evaluate(weightedSimilarityScores(example.inputs, example.weights))
      val description = if(example.description.isEmpty) "" else s" (${example.description})"

      it should "fulfill: " + example.formatted + description in {
        (result.score, example.output) match {
          case (Some(resultScore), Some(expectedScore)) =>
            resultScore shouldBe expectedScore +- epsilon
          case (Some(resultScore), None) =>
            fail(s"Aggregation did return a similarity score ($resultScore), although it was expected to return none.")
          case (None, Some(expectedScore)) =>
            fail(s"Aggregation did not return a similarity score, although it was expected to return $expectedScore.")
          case (None, None) =>
            // success
        }
      }
    }

    private def weightedSimilarityScores(scores: Seq[Option[Double]], weights: Seq[Int]): Seq[WeightedSimilarityScore] = {
      for((score, weight) <- scores zip weights) yield {
        WeightedSimilarityScore(score, weight)
      }
    }
  }

}
