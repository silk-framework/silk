package org.silkframework.rule.similarity

import org.silkframework.rule.OperatorExampleValue
import org.silkframework.rule.annotations.AggregatorExample
import scala.collection.immutable.ArraySeq

case class AggregatorExampleValue(description: Option[String],
                                  parameters: Map[String, String],
                                  inputs: Seq[Option[Double]],
                                  weights: Seq[Int],
                                  output: Option[Double],
                                  throwsException: String) extends OperatorExampleValue {

  def formatted: String = {
    if (throwsException.trim != "") {
      s"Fails validation and thus returns ${format(output)} for parameters ${format(parameters)} and input scores ${format(inputs.map(formatScore))}."
    } else {
      s"Returns ${formatScore(output)} for parameters ${format(parameters)}, input scores ${format(inputs.map(formatScore))} and weights ${format(weights)}."
    }
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

  private def format(iterable: Iterable[_]): String = {
    iterable.mkString("[", ", ", "]")
  }
}

object AggregatorExampleValue {

  def retrieve(transformer: Class[_]): Seq[AggregatorExampleValue] = {
    val aggregatorExamples = ArraySeq.unsafeWrapArray(transformer.getAnnotationsByType(classOf[AggregatorExample]))
    for(example <- aggregatorExamples) yield {
      AggregatorExampleValue(
        description = Option(example.description()).filter(_.nonEmpty),
        parameters = retrieveParameters(example),
        inputs = ArraySeq.unsafeWrapArray(example.inputs.map(convertScore)),
        weights = ArraySeq.unsafeWrapArray(example.weights()),
        output = convertScore(example.output()),
        throwsException = example.throwsException()
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
