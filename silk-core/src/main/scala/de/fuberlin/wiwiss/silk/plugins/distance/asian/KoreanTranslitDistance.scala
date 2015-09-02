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

package de.fuberlin.wiwiss.silk.plugins.distance.asian

import de.fuberlin.wiwiss.silk.util.StringUtils._
import scala.math.{min, max, abs}
import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin
import de.fuberlin.wiwiss.silk.rule.similarity.SimpleDistanceMeasure
import Array._
import scala.util.control._

@Plugin(
  id = "koreanTranslitDistance",
  categories = Array("Asian"),
  label = "Korean translit distance",
  description = "Transliterated Korean distance."
)
case class KoreanTranslitDistance(minChar: Char = '0', maxChar: Char = 'z') extends SimpleDistanceMeasure {

  private val q = 1

  override def evaluate(str1: String, str2: String, limit: Double) = {
    if (abs(str1.length - str2.length) > limit)
      Double.PositiveInfinity
    else
      evaluateDistance(getKoSoundex(str1),getKoSoundex(str2))
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

  private def getKoSoundex(in: String): String = {

        // 1st pass
	    var out="";
	    var x = in.toLowerCase().toCharArray()
		for(i <- 0 until x.length) {
		  if (x(i) == 'g') out += 'k'
		  else if (x(i) == 'd') out += 't'
		  else if (x(i) == 'b') out += 'p'
		  else if (x(i) == 'l') out += 'r'
		  else out += x(i)
		}
        x = out.toLowerCase().toCharArray()
		x.mkString("").replace(" ","")
	}
}
