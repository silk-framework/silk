package org.silkframework.rule.test

import org.silkframework.entity.{Entity, Index}
import org.silkframework.rule.Operator
import org.silkframework.rule.similarity.{Aggregator, AggregatorExampleValue, SimilarityOperator}
import org.silkframework.runtime.plugin.{AnyPlugin, ClassPluginDescription, ParameterValues, PluginContext, TestPluginContext}
import org.silkframework.test.PluginTest
import org.silkframework.util.{DPair, Identifier}

import scala.reflect.ClassTag
import scala.util.control.NonFatal

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
  override protected lazy val pluginObject: AnyPlugin = {
    require(aggregatorTests.nonEmpty, s"$pluginClass does not define any AggregatorExample annotation.")
    aggregatorTests.head.aggregator
  }

  private class AggregatorTest(example: AggregatorExampleValue) {
    val aggregator: T = pluginDesc(ParameterValues.fromStringMap(example.parameters))(TestPluginContext())

    def addTest(): Unit = {
      val weights = if (example.weights.nonEmpty) example.weights else Seq.fill(example.inputs.size)(1)
      val description = if (example.description.isEmpty) "" else s" (${example.description})"

      it should "fulfill: " + example.formatted + description in {
        example.throwsException match {
          case Some(expectedException) =>
            var expectedExceptionThrown = false
            try {
              aggregator(operators(example.inputs, weights), DPair.fill(Entity.empty("dummy")), 0.0)
            } catch {
              case NonFatal(ex) =>
                if (!expectedException.isAssignableFrom(ex.getClass)) {
                  fail("Another exception was thrown: " + ex.getClass.getName + ". Expected: " + example.throwsException)
                } else {
                  expectedExceptionThrown = true
                }
            }
            if(!expectedExceptionThrown) {
              fail("Exception " + example.throwsException + " has not been thrown!")
            }
          case None =>
            val result = aggregator(operators(example.inputs, weights), DPair.fill(Entity.empty("dummy")), 0.0)
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
