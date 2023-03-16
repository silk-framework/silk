package org.silkframework.runtime.plugin

import org.silkframework.runtime.plugin.annotations.DistanceMeasureExample
import org.silkframework.util.DPair

case class DistanceMeasureExampleValue(description: Option[String],
                                       parameters: Map[String, String],
                                       inputs: DPair[Seq[String]],
                                       output: Double,
                                       throwsException: Option[Class[_]]) {

  def formatted: String = {
    s"Returns $output for parameters ${format(parameters)} and input values ${format(inputs.map(format))}."
  }

  def markdownFormatted: String = {
    val sb = new StringBuilder()
    if (parameters.nonEmpty) {
      sb ++= "* Parameters\n"
      for ((param, paramValue) <- parameters) {
        sb ++= s"  * *$param*: `$paramValue`\n"
      }
      sb ++= "\n"
    }
    sb ++= "* Input values:\n"
    sb ++= s"  - Source: `${format(inputs.source)}`\n"
    sb ++= s"  - Target: `${format(inputs.target)}`\n"
    sb ++= "\n"
    sb ++= s"* Returns: → `${output}`\n"
    sb.toString()
  }

  private def format(traversable: Traversable[_]): String = {
    traversable.mkString("[", ", ", "]")
  }

}

object DistanceMeasureExampleValue {

  def retrieve(distanceMeasureClass: Class[_]): Seq[DistanceMeasureExampleValue] = {
    val distanceMeasureExamples = distanceMeasureClass.getAnnotationsByType(classOf[DistanceMeasureExample])
    for(example <- distanceMeasureExamples) yield {
      DistanceMeasureExampleValue(
        description = Option(example.description()).filter(_.nonEmpty),
        parameters = retrieveParameters(example),
        inputs = DPair(example.input1(), example.input2()),
        output = example.output(),
        throwsException = Option(example.throwsException()).filterNot(_ == classOf[Object])
      )
    }
  }

  private def retrieveParameters(distanceMeasureExample: DistanceMeasureExample): Map[String, String] = {
    assert(distanceMeasureExample.parameters().length % 2 == 0, "DistanceMeasureExample.parameters must have an even number of values")
    distanceMeasureExample.parameters().grouped(2).map(group => (group(0), group(1))).toMap
  }
}