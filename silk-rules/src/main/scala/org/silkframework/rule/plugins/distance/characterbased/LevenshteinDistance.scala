/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.silkframework.rule.plugins.distance.characterbased

import org.silkframework.entity.Index
import org.silkframework.rule.similarity.SimpleDistanceMeasure
import org.silkframework.runtime.plugin.annotations.{DistanceMeasureExample, DistanceMeasureExamples, DistanceMeasurePlugin, DistanceMeasureRange, Param, Plugin}
import org.silkframework.runtime.plugin.PluginCategories
import org.silkframework.util.StringUtils._

import scala.math.{abs, max, min}

@Plugin(
  id = "levenshteinDistance",
  categories = Array("Characterbased", PluginCategories.recommended),
  label = "Levenshtein distance",
  description = "Levenshtein distance. Returns a distance value between zero and the size of the string."
)
@DistanceMeasurePlugin(
  range = DistanceMeasureRange.UNBOUND
)
@DistanceMeasureExamples(Array(
  new DistanceMeasureExample(
    description = "Returns 0 for equal strings.",
    input1 = Array("John"),
    input2 = Array("John"),
    output = 0.0
  ),
  new DistanceMeasureExample(
    description = "Returns 1 for strings that differ by one edit operation.",
    input1 = Array("John"),
    input2 = Array("Jxhn"),
    output = 1.0
  ),
  new DistanceMeasureExample(
    description = "Returns 3 for strings that differ by three edit operations.",
    input1 = Array("Saturday"),
    input2 = Array("Sunday"),
    output = 3.0
  )
))
case class LevenshteinDistance(
  @Param(label = "Q-grams size", value = "The size of the q-grams to be indexed. Setting this to zero will disable indexing.", advanced = true)
  qGramsSize: Int = 2,
  @Param(value = "The minimum character that is used for indexing", advanced = true)
  minChar: Char = '0',
  @Param(value = "The maximum character that is used for indexing", advanced = true)
  maxChar: Char = 'z') extends SimpleDistanceMeasure {

  assert(qGramsSize >= 0, "Q-grams size cannot be negative")
  assert(qGramsSize <= 4, "Q-grams size is not allowed to be larger than 4")

  private val indexingEnabled = qGramsSize != 0

  private val indexSize = BigInt(maxChar - minChar + 1).pow(qGramsSize).toInt

  override def evaluate(str1: String, str2: String, limit: Double): Double = {
    if (abs(str1.length - str2.length) > limit) {
      Double.PositiveInfinity
    } else {
      evaluateDistance(str1, str2)
    }
  }

  override def indexValue(str: String, limit: Double, sourceOrTarget: Boolean): Index = {
    if(indexingEnabled) {
      val qGrams = str.qGrams(qGramsSize)
      val qGramsReordered = qGrams.drop(qGramsSize - 1) ++ qGrams.take(qGramsSize - 1)

      val numberOfIndices = limit.toInt * qGramsSize + 1
      val indices = qGramsReordered.take(numberOfIndices).map(indexQGram).toSet

      Index.oneDim(indices, indexSize)
    } else {
      Index.default
    }
  }

  override def emptyIndex(limit: Double): Index = {
    if(indexingEnabled) {
      Index.oneDim(Set.empty, indexSize)
    } else {
      Index.empty
    }
  }

  private def indexQGram(qGram: String): Int = {
    def combine(index: Int, char: Char) = {
      val croppedChar = min(max(char, minChar), maxChar)
      index * (maxChar - minChar + 1) + croppedChar - minChar
    }

    qGram.foldLeft(0)(combine)
  }

  /**
   * Fast implementation of the levenshtein distance.
   * Based on: Gonzalo Navarro - A guided tour to approximate string matching
   */
  private def evaluateDistance(row: String, col: String): Double = {
    //Handle trivial cases when one string is empty
    if (row.isEmpty) return col.length
    if (col.isEmpty) return row.length

    //Create two row vectors
    var r0 = new Array[Int](row.length + 1)
    var r1 = new Array[Int](row.length + 1)

    // Initialize the first row
    var rowIdx = 1
    while(rowIdx <= row.length) {
      r0(rowIdx) = rowIdx
      rowIdx += 1
    }

    //Build the matrix
    var rowC: Char = 0
    var colC: Char = 0
    var iCol = 1
    while(iCol <= col.length) {
      //Set the first element to the column number
      r1(0) = iCol;

      colC = col.charAt(iCol - 1)

      //Compute current column
      var iRow = 1
      while(iRow <= row.length) {
        rowC = row.charAt(iRow - 1)

        // Find minimum cost
        val cost = if (rowC == colC) 0 else 1

        val min = min3(
          r0(iRow) + 1, // deletion
          r1(iRow - 1) + 1, // insertion
          r0(iRow - 1) + cost) // substitution

        //Update row
        r1(iRow) = min

        iRow += 1
      }

      //Swap the rows
      val vTmp = r0;
      r0 = r1;
      r1 = vTmp;

      iCol += 1
    }

    r0(row.length)
  }

  @inline
  private def min3(x: Int, y: Int, z: Int) = {
    if(x <= y)
      if(z <= x) z else x
    else
      if(z <= y) z else y
  }

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
