package org.silkframework.rule.test

import org.silkframework.config.Prefixes
import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.plugin.{PluginDescription, TransformExample, TransformExampleValue}
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
    val transformExamples = TransformExampleValue.retrieve(pluginClass)
    for(example <- transformExamples) yield {
      new TransformTest(example)
    }
  }

  // Add all transform tests
  assert(transformTests.nonEmpty, s"$pluginClass does not define any TransformExample annotation")
  transformTests.foreach(_.addTest())

  // Forward one transformer for general plugin testing.
  override protected lazy val pluginObject: AnyRef = transformTests.head.transformer

  private class TransformTest(example: TransformExampleValue) {
    val transformer: T = pluginDesc(example.parameters)(Prefixes.empty)

    private val generatedOutput =
      try {
        transformer(example.input)
      } catch {
        case NonFatal(ex) =>
          List()
      }

    def addTest(): Unit = {
      it should s"return ${format(example.input)} for parameters ${format(example.parameters)} and input values ${format(example.input.map(format))}" in {
        generatedOutput should have size example.output.size
        for ((value, expected) <- generatedOutput zip example.output) {
          (value, expected) match {
            case (DoubleLiteral(doubleValue), DoubleLiteral(doubleExpected)) =>
              doubleValue shouldEqual doubleExpected +- epsilon
            case _ =>
              value shouldEqual expected
          }
        }
      }
    }

    private def format(traversable: Traversable[_]): String = {
      traversable.mkString("[", ", ", "]")
    }
  }

}
