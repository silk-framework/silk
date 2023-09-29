package org.silkframework.rule.similarity

import org.silkframework.rule.OperatorExampleValues
import org.silkframework.rule.similarity.DistanceMeasureDescription._
import org.silkframework.runtime.plugin.{CustomPluginDescription, CustomPluginDescriptionGenerator}

case class DistanceMeasureDescription(range: DistanceMeasureRange,
                                      cardinality: Option[DistanceMeasureCardinality],
                                      examples: OperatorExampleValues[DistanceMeasureExampleValue]) extends CustomPluginDescription {

  override def generateDocumentation(sb: StringBuilder): Unit = {
    sb ++= "\n### Characteristics\n"
    sb ++= range.description
    for(c <- cardinality) {
      sb ++= "\n\n"
      sb ++= c.description
    }
    examples.markdownFormatted(sb)
  }

  override def additionalProperties(): Map[String, String] = {
    Map(
      distanceMeasureRangeProperty -> range.id
    )
  }
}

object DistanceMeasureDescription {

  private val distanceMeasureRangeProperty: String = "distanceMeasureRange"

  sealed trait DistanceMeasureRange {
    def id: String
    def description: String
  }

  case object NormalizedMeasure extends DistanceMeasureRange {
    override def id: String = "normalized"
    override def description: String = "This distance measure is normalized, i.e., all distances are between 0 (exact match) and 1 (no similarity)."
  }

  case object UnboundMeasure extends DistanceMeasureRange {
    override def id: String = "unbounded"
    override def description: String = "This distance measure is not normalized, i.e., all distances start at 0 (exact match) and increase the more different the values are."
  }

  case object BooleanMeasure extends DistanceMeasureRange {
    override def id: String = "boolean"
    override def description: String = "This is a boolean distance measure, i.e., all distances are either 0 or 1."
  }

  sealed trait DistanceMeasureCardinality {
    def description: String
  }
  
  case object SingleValueMeasure extends DistanceMeasureCardinality {
    override def description: String =
      "Compares single values (as opposed to sequences of values). " +
      "If multiple values are provided, all values are compared and the lowest distance is returned."
  }

  case object TokenBasedMeasure extends DistanceMeasureCardinality {
    override def description: String =
      "Compares sets of multiple values." +
      "Typically, incoming values are tokenized before being fed into this measure."
  }

}

class DistanceMeasureDescriptionGenerator extends CustomPluginDescriptionGenerator {

  override def generate(pluginClass: Class[_]): Option[CustomPluginDescription] = {
    val range =
      if (classOf[BooleanDistanceMeasure].isAssignableFrom(pluginClass)) {
        BooleanMeasure
      } else if (classOf[NormalizedDistanceMeasure].isAssignableFrom(pluginClass)) {
        NormalizedMeasure
      } else {
        UnboundMeasure
      }

    val cardinality =
      if (classOf[SingleValueDistanceMeasure].isAssignableFrom(pluginClass)) {
        Some(SingleValueMeasure)
      } else if (classOf[TokenBasedDistanceMeasure].isAssignableFrom(pluginClass)) {
        Some(TokenBasedMeasure)
      } else {
        None
      }

    Some(DistanceMeasureDescription(range, cardinality, OperatorExampleValues(DistanceMeasureExampleValue.retrieve(pluginClass))))
  }
}

