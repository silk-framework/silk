package org.silkframework.rule.test

import org.silkframework.config.Prefixes
import org.silkframework.rule.input.{TransformExample, Transformer}
import org.silkframework.runtime.plugin.PluginDescription
import org.silkframework.test.PluginTest
import org.silkframework.util.StringUtils.DoubleLiteral

import scala.reflect.ClassTag
import scala.util.control.NonFatal

abstract class TransformerTest[T <: Transformer : ClassTag] extends PluginTest {

  /** Numeric values may differ slightly from their expected values. */
  private val epsilon = 0.0001

  /** The class of the transformation to be tested. */
  private lazy val pluginClass = implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]]

  /** The plugin description of the transformation to be tested. */
  private lazy val pluginDesc = PluginDescription(pluginClass)

  /** All available transform tests. */
  private lazy val transformTests: Seq[TransformTest] = {
    val transformExamples = pluginClass.getAnnotationsByType(classOf[TransformExample])
    for(example <- transformExamples) yield {
      new TransformTest(example)
    }
  }

  // Add all transform tests
  assert(transformTests.nonEmpty, s"$pluginClass does not define any TransformExample annotation")
  transformTests.foreach(_.addTest())

  // Forward one transformer for general plugin testing.
  override protected lazy val pluginObject: AnyRef = transformTests.head.transformer

  private class TransformTest(example: TransformExample) {
    private val parameters = retrieveParameters(example)
    private val inputValues = Seq(example.input1(), example.input2(), example.input3(), example.input4(), example.input5()).map(_.toList).filter(_.nonEmpty)
    private val expectedOutput = example.output().toList

    val transformer: T = pluginDesc(parameters)(Prefixes.empty)

    private val generatedOutput =
      try {
        transformer(inputValues)
      } catch {
        case NonFatal(ex) =>
          List()
      }

    def addTest(): Unit = {
      it should s"return $expectedOutput for parameters $parameters and input values $inputValues" in {
        generatedOutput should have size expectedOutput.size
        for ((value, expected) <- generatedOutput zip expectedOutput) {
          (value, expected) match {
            case (DoubleLiteral(doubleValue), DoubleLiteral(doubleExpected)) =>
              doubleValue shouldEqual doubleExpected +- epsilon
            case _ =>
              value shouldEqual expected
          }
        }
      }
    }

    private def retrieveParameters(transformExample: TransformExample): Map[String, String] = {
      assert(transformExample.parameters().length % 2 == 0, "TransformExample.parameters must have an even number of values")
      transformExample.parameters().grouped(2).map(group => (group(0), group(1))).toMap
    }
  }

}
