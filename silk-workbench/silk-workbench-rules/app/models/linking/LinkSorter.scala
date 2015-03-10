package models.linking

import EvalLink.{Unknown, Incorrect, Correct}

object LinkSorter {
  private val sorters = {
    Seq(
      NoSorter,
      SourceUriSorterAscending,
      SourceUriSorterDescending,
      TargetUriSorterAscending,
      TargetUriSorterDescending,
      ConfidenceSorterAscending,
      ConfidenceSorterDescending,
      CorrectnessSorterAscending,
      CorrectnessSorterDescending
    ).map(s => (s.id -> s)).toMap
  }

  def fromId(id: String) = {
    sorters(id)
  }
}

case class LinkSorter(name: String, ascending: Boolean) extends (Seq[EvalLink] => Seq[EvalLink]) {
  def id = name + "-" + ascending
  def apply(links: Seq[EvalLink]) = links
}

object NoSorter extends LinkSorter("unsorted", true) {
  override def id = "unsorted"
  override def apply(links: Seq[EvalLink]) = links
}

object SourceUriSorterAscending extends LinkSorter("source", true) {
  override def apply(links: Seq[EvalLink]) = links.sortBy(_.source)
}

object SourceUriSorterDescending extends LinkSorter("source", false) {
  override def apply(links: Seq[EvalLink]) = links.sortBy(_.source).reverse
}

object TargetUriSorterAscending extends LinkSorter("target", true) {
  override def apply(links: Seq[EvalLink]) = links.sortBy(_.target)
}

object TargetUriSorterDescending extends LinkSorter("target", false) {
  override def apply(links: Seq[EvalLink]) = links.sortBy(_.target).reverse
}

object ConfidenceSorterAscending extends LinkSorter("confidence", true){
  override def apply(links: Seq[EvalLink]): Seq[EvalLink] = {
    links.sortBy(_.confidence.getOrElse(-1.0))
  }
}

object ConfidenceSorterDescending extends LinkSorter("confidence", false) {
  override def apply(links: Seq[EvalLink]): Seq[EvalLink] = {
    links.sortBy(-_.confidence.getOrElse(-1.0))
  }
}

object CorrectnessSorterAscending extends LinkSorter("correctness", true) {
  override def apply(links: Seq[EvalLink]) = {
    links.sortBy{ _.correct match {
      case Correct => 0
      case Incorrect => 1
      case Unknown => 2
    }}
  }
}

object CorrectnessSorterDescending extends LinkSorter("correctness", false) {
  override def apply(links: Seq[EvalLink]) = {
    links.sortBy{ _.correct match {
      case Unknown => 0
      case Correct => 1
      case Incorrect => 2
    }}
  }
}
