package org.silkframework.rule.test

import org.silkframework.rule.input.{TransformExampleValue, Transformer}
import org.silkframework.runtime.plugin.{AnyPlugin, ClassPluginDescription, ParameterValues, PluginContext, TestPluginContext}
import org.silkframework.test.PluginTest

import java.util.logging.Logger
import scala.reflect.ClassTag
import scala.util.control.NonFatal

abstract class TransformerTest[T <: Transformer : ClassTag] extends PluginTest {

  /** Numeric values may differ slightly from their expected values. */
  private val epsilon = 0.0001

  private val log: Logger = Logger.getLogger(getClass.getName)

  /** The class of the transformation to be tested. */
  private lazy val pluginClass = {
    val clazz = implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]]
    require(clazz != classOf[Nothing], "Type parameter T is no defined")
    clazz
  }

  /** The plugin description of the transformation to be tested. */
  private lazy val pluginDesc = ClassPluginDescription(pluginClass)

  /** All available transform tests. */
  private lazy val transformTests: Seq[TransformTest] = {
    val transformExamples = TransformExampleValue.retrieve(pluginClass)
    for(example <- transformExamples) yield {
      new TransformTest(example)
    }
  }

  // Add all transform tests
  transformTests.foreach(_.addTest())

  // Forward one transformer for general plugin testing.
  override protected lazy val pluginObject: T = {
    require(transformTests.nonEmpty, s"$pluginClass does not define any TransformExample annotation.")
    transformTests.head.transformer
  }

  private class TransformTest(example: TransformExampleValue) {
    lazy val transformer: T = pluginDesc(ParameterValues.fromStringMap(example.parameters))(TestPluginContext())

    private lazy val (generatedOutput, throwableOpt): (Seq[String], Option[Throwable]) =
      try {
        (transformer(example.input), None)
      } catch {
        case NonFatal(ex) =>
          (List(), Some(ex))
      }

    def addTest(): Unit = {
      if(example.throwsException != "") {
        it should s"throw ${example.throwsException} for parameters ${format(example.parameters)} and input values ${format(example.input.map(format))}" in {
          generatedOutput should have size 0
          val expectedException = Class.forName(example.throwsException)
          throwableOpt match {
            case Some(ex) =>
              if(!expectedException.isAssignableFrom(ex.getClass)) {
                throw new RuntimeException("Another exception was thrown: " + ex.getClass.getName + ". Expected: " + example.throwsException)
              }
            case None =>
              throw new RuntimeException("Exception " + example.throwsException + " has not been thrown!")
          }

        }
      } else {
        it should s"return ${format(example.output)} for parameters ${format(example.parameters)} and input values ${format(example.input.map(format))}" in {
          throwableOpt foreach { ex =>
            log.warning("Exception was thrown: " + ex.getMessage)
            ex.printStackTrace()
          }
          generatedOutput should have size example.output.size
          for ((value, expected) <- generatedOutput zip example.output) {
            value shouldEqual expected
          }
        }
      }
    }

    private def format(traversable: Iterable[_]): String = {
      traversable.mkString("[", ", ", "]")
    }
  }

}
