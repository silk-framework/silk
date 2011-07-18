package de.fuberlin.wiwiss.silk.workbench.evaluation

import de.fuberlin.wiwiss.silk.workbench.workspace.UserData
import de.fuberlin.wiwiss.silk.output.Link

object LinkFilter extends UserData[String]("")
{
  def filter(links : Seq[EvalLink]) : Seq[EvalLink] =
  {
    val value = apply().trim.toLowerCase

    if(value.isEmpty)
    {
     links
    }
    else
    {
      links.filter(new LinkFilter(value))
    }
  }
}

class LinkFilter(value : String) extends (EvalLink => Boolean)
{
  def apply(link : EvalLink) : Boolean =
  {
    link.source.toLowerCase.contains(value) ||
    link.target.toLowerCase.contains(value) ||
    (link.details match
    {
      case Some(details) => hasValue(details)
      case None => false
    })
  }

  private def hasValue(similarity : Link.Confidence) : Boolean = similarity match
  {
    case Link.AggregatorConfidence(_, _, children) => children.exists(hasValue)
    case Link.ComparisonConfidence(_, _, i1, i2) =>
    {
      i1.values.exists(_.toLowerCase.contains(value)) ||
      i2.values.exists(_.toLowerCase.contains(value))
    }
    case Link.SimpleConfidence(_) => false
  }
}