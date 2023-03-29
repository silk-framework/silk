package org.silkframework.rule.test

import org.silkframework.rule.similarity.{DistanceMeasure, DistanceMeasureExampleValue}
import org.silkframework.runtime.plugin.{AnyPlugin, ClassPluginDescription, ParameterValues, PluginContext}
import org.silkframework.test.PluginTest

import java.util.logging.Logger
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

/**
  * Can be mixed into a DistanceMeasure test spec.
  * Will iterate through all [[DistanceMeasureExampleValue]] annotations and generate a test case for each.
  *
  * @tparam T The class to be tested.
  */
abstract class DistanceMeasureTest[T <: DistanceMeasure : ClassTag] extends PluginTest {

  /** Numeric values may differ slightly from their expected values. */
  private val epsilon = 0.0001

  private val log: Logger = Logger.getLogger(getClass.getName)

  /** The class of the distance measure to be tested. */
  private lazy val pluginClass = {
    val clazz = implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]]
    require(clazz != classOf[Nothing], "Type parameter T is no defined")
    clazz
  }

  /** The plugin description of the distance measure to be tested. */
  private lazy val pluginDesc = ClassPluginDescription(pluginClass)

  /** All available distance measure tests. */
  private lazy val distanceMeasureTests: Seq[DistanceMeasureTest] = {
    val distanceMeasureExamples = DistanceMeasureExampleValue.retrieve(pluginClass)
    for(example <- distanceMeasureExamples) yield {
      new DistanceMeasureTest(example)
    }
  }

  // Add all distance measure tests
  distanceMeasureTests.foreach(_.addTest())

  // Forward one distance measure for general plugin testing.
  override protected lazy val pluginObject: AnyPlugin = {
    require(distanceMeasureTests.nonEmpty, s"$pluginClass does not define any DistanceMeasureExample annotation.")
    distanceMeasureTests.head.distanceMeasure
  }

  private class DistanceMeasureTest(example: DistanceMeasureExampleValue) {
    val distanceMeasure: T = pluginDesc(ParameterValues.fromStringMap(example.parameters))(PluginContext.empty)

    def addTest(): Unit = {
      val result = Try(distanceMeasure(example.inputs.source, example.inputs.target))
      val configurationString = s" for parameters ${format(example.parameters)} and input values ${format(example.inputs.map(format))}."
      val descriptionString = example.description.filter(_.nonEmpty).map(desc => s" ($desc)").getOrElse("")

      example.throwsException match {
        case Some(expectedException) =>
          it should s"throw exception ${expectedException.getName} for $configurationString$descriptionString" in {
            result match {
              case Success(_) =>
                fail(s"Expected exception $expectedException has not been thrown")
              case Failure(thrownException) =>
                if(!expectedException.isAssignableFrom(thrownException.getClass)) {
                  throw new RuntimeException(s"Another exception was thrown: $thrownException. Expected: s$expectedException")
                }
            }
          }
        case None =>
          it should s"return ${example.output} for $configurationString$descriptionString" in {
            result match {
              case Success(value) =>
                if(example.output.isPosInfinity) {
                  // Positive Infinity and Double.Max value is used interchangeability to signal that no distance has been computed
                  value should be >= Double.MaxValue
                } else {
                  value shouldBe example.output +- epsilon
                }
              case Failure(thrownException) =>
                throw thrownException
            }
          }
      }


    }

    private def format(traversable: Traversable[_]): String = {
      traversable.mkString("[", ", ", "]")
    }
  }

}
