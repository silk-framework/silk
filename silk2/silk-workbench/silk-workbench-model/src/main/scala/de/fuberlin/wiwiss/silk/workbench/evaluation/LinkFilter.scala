package de.fuberlin.wiwiss.silk.workbench.evaluation

import de.fuberlin.wiwiss.silk.workbench.workspace.UserData

object CurrentLinkFilter extends UserData[LinkFilter](NoFilter)

trait LinkFilter extends (Seq[EvalLink] => Seq[EvalLink])
{
  def apply(links : Seq[EvalLink]) : Seq[EvalLink]
}

object NoFilter extends LinkFilter
{
  def apply(links : Seq[EvalLink]) = links
}

object ConfidenceSorterAscending extends LinkFilter
{
  def apply(links : Seq[EvalLink]) : Seq[EvalLink] =
  {
    links.sortBy(_.confidence)
  }
}

object ConfidenceSorterDescending extends LinkFilter
{
  def apply(links : Seq[EvalLink]) : Seq[EvalLink] =
  {
    links.sortBy(-_.confidence)
  }
}

