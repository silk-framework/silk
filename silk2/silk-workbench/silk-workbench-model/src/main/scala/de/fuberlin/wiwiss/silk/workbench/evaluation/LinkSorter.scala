package de.fuberlin.wiwiss.silk.workbench.evaluation

import de.fuberlin.wiwiss.silk.workbench.workspace.TaskData

object LinkSorter extends TaskData[LinkSorter](NoSorter)
{
  def sort(links : Seq[EvalLink]) : Seq[EvalLink] =
  {
    apply()(links)
  }
}

trait LinkSorter extends (Seq[EvalLink] => Seq[EvalLink])
{
  def apply(links : Seq[EvalLink]) : Seq[EvalLink]
}

object NoSorter extends LinkSorter
{
  def apply(links : Seq[EvalLink]) = links
}

object ConfidenceSorterAscending extends LinkSorter
{
  def apply(links : Seq[EvalLink]) : Seq[EvalLink] =
  {
    links.sortBy(_.confidence.getOrElse(-1.0))
  }
}

object ConfidenceSorterDescending extends LinkSorter
{
  def apply(links : Seq[EvalLink]) : Seq[EvalLink] =
  {
    links.sortBy(-_.confidence.getOrElse(-1.0))
  }
}
