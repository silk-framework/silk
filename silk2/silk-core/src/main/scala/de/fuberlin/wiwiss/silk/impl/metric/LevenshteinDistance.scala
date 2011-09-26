package de.fuberlin.wiwiss.silk.impl.metric

import de.fuberlin.wiwiss.silk.util.StringUtils._
import scala.math.{min, max, abs}
import de.fuberlin.wiwiss.silk.util.plugin.Plugin
import de.fuberlin.wiwiss.silk.linkspec.similarity.SimpleDistanceMeasure

@Plugin(id = "levenshteinDistance", label = "Levenshtein distance", description = "Levenshtein distance.")
case class LevenshteinDistance(minChar: Char = '0', maxChar: Char = 'z') extends SimpleDistanceMeasure {
  /**The size of the q-Grams to be indexed */
  private val q = 2

  override def evaluate(str1: String, str2: String, limit: Double) = {
    if (abs(str1.length - str2.length) > limit) {
      Double.PositiveInfinity
    }
    else {
      evaluateDistance(str1, str2)
    }
  }

  override def indexValue(str: String, limit: Double): Set[Seq[Int]] = {
    val qGrams = str.qGrams(q)
    val qGramsReordered = qGrams.drop(q - 1) ++ qGrams.take(q - 1)

    val index = qGramsReordered.take(limit.toInt * q + 1).map(indexQGram).toSet

    index
  }

  private def indexQGram(qGram: String): Seq[Int] = {
    def combine(index: Int, char: Char) = {
      val croppedChar = min(max(char, minChar), maxChar)
      index * (maxChar - minChar + 1) + croppedChar - minChar
    }

    Seq(qGram.foldLeft(0)(combine))
  }

  override def blockCounts(threshold: Double): Seq[Int] = {
    Seq(BigInt(maxChar - minChar + 1).pow(q).toInt)
  }

  /**
   * Fast implementation of the levenshtein distance.
   * Based on: Gonzalo Navarro - A guided tour to approximate string matching
   */
  private def evaluateDistance(row: String, col: String): Double = {
    //Handle trivial cases when one string is empty
    if (row.isEmpty) return col.size;
    if (col.isEmpty) return row.size;

    //Create two row vectors
    var r0 = new Array[Int](row.size + 1)
    var r1 = new Array[Int](row.size + 1)

    // Initialize the first row
    for (rowIdx <- 1 to row.size) r0(rowIdx) = rowIdx;

    //Build the matrix
    var rowC: Char = 0
    var colC: Char = 0
    for (iCol <- 1 to col.size) {
      //Set the first element to the column number
      r1(0) = iCol;

      colC = col(iCol - 1)

      //Compute current column
      for (iRow <- 1 to row.size) {
        rowC = row(iRow - 1)

        // Find minimum cost
        val cost = if (rowC == colC) 0 else 1

        val min = min3(
          r0(iRow) + 1, // deletion
          r1(iRow - 1) + 1, // insertion
          r0(iRow - 1) + cost) // substitution

        //Update row
        r1(iRow) = min;
      }

      //Swap the rows
      val vTmp = r0;
      r0 = r1;
      r1 = vTmp;
    }

    r0(row.size)
  }

  @inline
  private def min3(x: Int, y: Int, z: Int) = min(min(x, y), z)

  //Original levenshtein implementation
  //  private def evaluateDistanceOld(str1 : String, str2 : String, limit : Int) : Double =
  //  {
  //    val lenStr1 = str1.length
  //    val lenStr2 = str2.length
  //
  //    val d: Array[Array[Int]] = Array.ofDim(lenStr1 + 1, lenStr2 + 1)
  //
  //    for (i <- 0 to lenStr1) d(i)(0) = i
  //    for (j <- 0 to lenStr2) d(0)(j) = j
  //
  //    for (i <- 1 to lenStr1; val j <- 1 to lenStr2) {
  //      val cost = if (str1(i - 1) == str2(j - 1)) 0 else 1
  //
  //      d(i)(j) = math.min(
  //        d(i - 1)(j) + 1, // deletion
  //        math.min(d(i)(j - 1) + 1, // insertion
  //          d(i - 1)(j - 1) + cost) // substitution
  //      )
  //    }
  //
  //    d(lenStr1)(lenStr2)
  //  }
}
