package models.linking

import org.silkframework.rule.evaluation.{AggregatorConfidence, ComparisonConfidence, Confidence, SimpleConfidence}

object LinkFilter {
  def apply(links: Seq[EvalLink], filter: String): Seq[EvalLink] = {
    val value = filter.trim.toLowerCase

    if (value.isEmpty)
      links
    else
      links.filter(new LinkFilter(value))
  }
}

class LinkFilter(value: String) extends (EvalLink => Boolean) {
  def apply(link: EvalLink): Boolean = {
    link.source.toLowerCase.contains(value) ||
      link.target.toLowerCase.contains(value) ||
      (link.details match {
        case Some(details) => hasValue(details)
        case None => false
      })
  }

  private def hasValue(similarity: Confidence): Boolean = similarity match {
    case AggregatorConfidence(_, _, children) => children.exists(hasValue)
    case ComparisonConfidence(_, _, i1, i2) => {
      i1.values.exists(_.toLowerCase.contains(value)) ||
        i2.values.exists(_.toLowerCase.contains(value))
    }
    case SimpleConfidence(_) => false
  }
}