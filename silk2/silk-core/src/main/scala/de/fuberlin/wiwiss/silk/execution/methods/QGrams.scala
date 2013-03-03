package de.fuberlin.wiwiss.silk.execution.methods

import de.fuberlin.wiwiss.silk.entity.{Path, Index, Entity}
import de.fuberlin.wiwiss.silk.linkagerule.LinkageRule
import de.fuberlin.wiwiss.silk.util.StringUtils._
import math._
import scala.BigInt
import de.fuberlin.wiwiss.silk.execution.ExecutionMethod

/**
 * Q-Grams indexing.
 *
 * @param sourceKey The source blocking key, e.g., rdfs:label
 * @param targetKey The target blocking key, e.g., rdfs:label
 * @param q The size of the q-grams that are indexed, e.g. if q = 2, bigrams will be indexed.
 * @param k The maximum number of q-grams that are indexed for each string.
 */
case class QGrams(sourceKey: Path, targetKey: Path, q: Int, k: Int = 10) extends ExecutionMethod {
  private val minChar = '0'
  private val maxChar = 'z'

  override def indexEntity(entity: Entity, rule: LinkageRule): Index = {
    val key = if(sourceKey.variable == entity.desc.variable) sourceKey else targetKey
    val values = entity.evaluate(key)

    //Collect all q-grams from all values
    val qGrams = values.map(_.qGrams(q).take(k)).fold(Stream[String]())(_ ++ _)
    //Index all q-grams
    val qGramsIndices = qGrams.map(indexQGram).toSet
    //Generate the overall index
    Index.oneDim(qGramsIndices, BigInt(maxChar - minChar + 1).pow(q).toInt)
  }

  /**
   * Generates an index for a single q-gram
   */
  private def indexQGram(qGram: String): Int = {
    def combine(index: Int, char: Char) = {
      val croppedChar = min(max(char, minChar), maxChar)
      index * (maxChar - minChar + 1) + croppedChar - minChar
    }

    qGram.foldLeft(0)(combine)
  }
}
