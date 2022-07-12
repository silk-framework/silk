package org.silkframework.rule.test

import org.silkframework.config.Prefixes
import org.silkframework.entity.{Entity, Index}
import org.silkframework.rule.Operator
import org.silkframework.rule.similarity.{Aggregator, SimilarityOperator}
import org.silkframework.runtime.plugin.{AggregatorExampleValue, ClassPluginDescription}
import org.silkframework.test.PluginTest
import org.silkframework.util.{DPair, Identifier}

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
    val aggregator: T = pluginDesc(example.parameters)(Prefixes.empty)

    def addTest(): Unit = {
      val result = aggregator(operators(example.inputs, example.weights), DPair.fill(Entity.empty("dummy")), 0.0)
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

    private def operators(scores: Seq[Option[Double]], weights: Seq[Int]): Seq[DummyOperator] = {
      for((score, weight) <- scores zip weights) yield {
        DummyOperator(score, weight)
      }
    }
  }

  private case class DummyOperator(score: Option[Double], weight: Int) extends SimilarityOperator {

    override def id: Identifier = "dummy"

    override def indexing: Boolean = true

    override def apply(entities: DPair[Entity], limit: Double): Option[Double] = score

    override def index(entity: Entity, sourceOrTarget: Boolean, limit: Double): Index = Index.default

    override def children: Seq[Operator] = Seq.empty

    override def withChildren(newChildren: Seq[Operator]): Operator = this
  }

}
