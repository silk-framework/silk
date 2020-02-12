package org.silkframework.runtime.plugin

import org.silkframework.runtime.plugin.annotations.DistanceMeasureExample
import org.silkframework.util.DPair

case class DistanceMeasureExampleValue(description: String, parameters: Map[String, String], inputs: DPair[Seq[String]], output: Double) {

  def formatted: String = {
    s"Returns $output for parameters ${format(parameters)} and input values ${format(inputs.map(format))}."
  }

  private def format(traversable: Traversable[_]): String = {
    traversable.mkString("[", ", ", "]")
  }

}

object DistanceMeasureExampleValue {

  def retrieve(transformer: Class[_]): Seq[DistanceMeasureExampleValue] = {
    val transformExamples = transformer.getAnnotationsByType(classOf[DistanceMeasureExample])
    for(example <- transformExamples) yield {
      DistanceMeasureExampleValue(
        description = example.description(),
        parameters = retrieveParameters(example),
        inputs = DPair(example.input1(), example.input2()),
        output = example.output()
      )
    }
  }

  private def retrieveParameters(distanceMeasureExample: DistanceMeasureExample): Map[String, String] = {
    assert(distanceMeasureExample.parameters().length % 2 == 0, "DistanceMeasureExample.parameters must have an even number of values")
    distanceMeasureExample.parameters().grouped(2).map(group => (group(0), group(1))).toMap
  }
}