package org.silkframework.rule.similarity

import org.silkframework.rule.OperatorExampleValue
import org.silkframework.rule.annotations.AggregatorExample

case class AggregatorExampleValue(description: Option[String],
                                  parameters: Map[String, String],
                                  inputs: Seq[Option[Double]],
                                  weights: Seq[Int],
                                  output: Option[Double]) extends OperatorExampleValue {

  def formatted: String = {
    s"Returns ${formatScore(output)} for parameters ${format(parameters)}, input scores ${format(inputs.map(formatScore))} and weights ${format(weights)}."
  }

  /**
    * Format this example as markdown.
    */
  override def markdownFormatted(sb: StringBuilder): Unit = {
    if (weights.nonEmpty) {
      sb ++= "* Weights: "
      sb ++= format(weights)
      sb ++= "\n"
    }
    if (inputs.nonEmpty) {
      sb ++= "* Input values: "
      sb ++= format(inputs.map(formatScore))
      sb ++= "\n"
    }
    sb ++= s"* Returns: `${formatScore(output)}`\n"
  }

  private def formatScore(score: Option[Double]): String = {
    score match {
      case Some(s) => s.toString
      case None => "(none)"
    }
  }

  private def format(traversable: Traversable[_]): String = {
    traversable.mkString("[", ", ", "]")
  }
}

object AggregatorExampleValue {

  def retrieve(transformer: Class[_]): Seq[AggregatorExampleValue] = {
    val aggregatorExamples = transformer.getAnnotationsByType(classOf[AggregatorExample])
    for(example <- aggregatorExamples) yield {
      AggregatorExampleValue(
        description = Option(example.description()).filter(_.nonEmpty),
        parameters = retrieveParameters(example),
        inputs = example.inputs.map(convertScore),
        weights = example.weights(),
        output = convertScore(example.output())
      )
    }
  }

  private def convertScore(score: Double): Option[Double] = {
    if(score.isNaN) {
      None
    } else {
      Some(score)
    }
  }

  private def retrieveParameters(aggregatorExample: AggregatorExample): Map[String, String] = {
    assert(aggregatorExample.parameters().length % 2 == 0, "AggregatorExample.parameters must have an even number of values")
    aggregatorExample.parameters().grouped(2).map(group => (group(0), group(1))).toMap
  }
}
