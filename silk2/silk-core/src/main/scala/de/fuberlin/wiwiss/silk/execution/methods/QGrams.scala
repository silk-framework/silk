package de.fuberlin.wiwiss.silk.execution.methods

import de.fuberlin.wiwiss.silk.entity.{Path, Index, Entity}
import de.fuberlin.wiwiss.silk.linkagerule.LinkageRule
import de.fuberlin.wiwiss.silk.util.StringUtils._
import math._
import scala.BigInt
import de.fuberlin.wiwiss.silk.execution.ExecutionMethod

/**
 * Q-Grams indexing as described in:
 *
 * Christen, Peter. "Data Matching: Concepts and Techniques for Record Linkage, Entity Resolution,
 * and Duplicate Detection." (2012).
 *
 * @param sourceKey The source blocking key, e.g., rdfs:label
 * @param targetKey The target blocking key, e.g., rdfs:label
 * @param q The size of the q-grams that are indexed, e.g. if q = 2, bigrams will be indexed.
 * @param t The minimum threshold that defines the minimum length of the generated q-gram sub-lists.
 */
case class QGrams(sourceKey: Path, targetKey: Path, q: Int = 2, t: Double = 0.8) extends ExecutionMethod {
  require(q > 0, "q > 0")
  require(0.0 <= t && t < 1.0, "0 <= t < 1")

  override def indexEntity(entity: Entity, rule: LinkageRule): Index = {
    val key = if(sourceKey.variable == entity.desc.variable) sourceKey else targetKey
    val values = entity.evaluate(key)

    Index.blocks(values.flatMap(indexValue))
  }

  @inline
  def indexValue(str: String): Set[Int] = {
    generateSubLists(str).map(_.mkString.hashCode)
  }

  @inline
  def generateSubLists(str: String): Set[Seq[String]] = {
    val qGrams = str.toLowerCase.sliding(q).toSeq
    val minLength = max(1, (qGrams.size * t).toInt)

    generateSubListsRecursive(qGrams, minLength)
  }

  private def generateSubListsRecursive(list: Seq[String], minLength: Int): Set[Seq[String]] = {
    if(list.size > minLength) {
      val subLists = for(i <- 0 until list.size) yield list.patch(i, Seq.empty, 1)
      Set(list) ++ subLists.flatMap(l => generateSubListsRecursive(l, minLength))
    } else {
      Set(list)
    }
  }
}
