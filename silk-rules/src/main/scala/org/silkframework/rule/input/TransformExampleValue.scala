package org.silkframework.rule.input

import org.silkframework.rule.OperatorExampleValue
import org.silkframework.rule.OperatorExampleValues.code
import org.silkframework.rule.annotations.TransformExample

import scala.collection.immutable.ArraySeq

case class TransformExampleValue(description: Option[String],
                                 parameters: Map[String, String],
                                 input: Seq[Seq[String]],
                                 output: Seq[String],
                                 throwsException: Option[Class[_]]) extends OperatorExampleValue {

  def formatted: String = {
    throwsException match {
      case Some(exClass) =>
        s"Fails validation with exception `${exClass.getSimpleName}` and thus returns ${format(output)} for parameters ${format(parameters)} and input values ${format(input.map(format))}."
      case None =>
        s"Returns ${format(output)} for parameters ${format(parameters)} and input values ${format(input.map(format))}."
    }
  }

  def markdownFormatted(sb: StringBuilder): Unit = {
    if(input.nonEmpty) {
      sb ++= "* Input values:\n"
      for((input, idx) <- input.zipWithIndex) {
        sb ++= s"    ${idx + 1}. ${code(format(input))}\n"
      }
      sb ++= "\n"
    }
    sb ++= s"* Returns: ${code(format(output))}\n"
    for(exceptionClass <- throwsException) {
      sb ++= s"* **Throws error:** `${exceptionClass.getSimpleName}`\n"
    }
  }

  private def format(traversable: Iterable[_]): String = {
    traversable.mkString("[", ", ", "]")
  }

}

object TransformExampleValue {

  def retrieve(transformer: Class[_]): Seq[TransformExampleValue] = {
    val transformExamples = ArraySeq.unsafeWrapArray(transformer.getAnnotationsByType(classOf[TransformExample]))
    for(example <- transformExamples) yield {
      TransformExampleValue(
        description = Option(example.description()).filter(_.nonEmpty),
        parameters = retrieveParameters(example),
        input = retrieveInputs(example),
        output = example.output().toList,
        throwsException = Option(example.throwsException()).filterNot(_ == classOf[Object])
      )
    }
  }

  private def retrieveInputs(example: TransformExample): Seq[Seq[String]] = {
    val allValues = Seq(example.input1(), example.input2(), example.input3(), example.input4(), example.input5())
    val definedValues = allValues.filter(_.toSeq != Seq("  __UNINITIALIZED__  "))
    definedValues.map(_.toSeq)
  }

  private def retrieveParameters(transformExample: TransformExample): Map[String, String] = {
    assert(transformExample.parameters().length % 2 == 0, "TransformExample.parameters must have an even number of values")
    transformExample.parameters().grouped(2).map(group => (group(0), group(1))).toMap
  }


}