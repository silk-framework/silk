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

package de.fuberlin.wiwiss.silk.plugins.metric

import de.fuberlin.wiwiss.silk.util.StringUtils._
import scala.math.{min, max, abs, floor}
import de.fuberlin.wiwiss.silk.util.plugin.Plugin
import de.fuberlin.wiwiss.silk.linkagerule.similarity.SimpleDistanceMeasure
import de.fuberlin.wiwiss.silk.entity.Index
import Array._
import scala.collection.mutable.HashMap
import scala.collection.JavaConversions._

@Plugin(id = "korean PhonemeDistance", label = "Korean phoneme distance", description = "Korean phoneme distance.")
case class KoreanPhonemeDistance(minChar: Char = '0', maxChar: Char = 'z') extends SimpleDistanceMeasure {
	private val q = 1

  override def evaluate(str1: String, str2: String, limit: Double) = {

    var kpd=0.0
    var src = str1.replaceAllLiterally(" ","")
    var tgt = str2.replaceAllLiterally(" ","")

    var qu = (limit/3).toInt
    var th=0
    if(limit%3==0) th=qu else th=qu+1

    if (abs(str1.length - str2.length) > th)
      kpd = Double.PositiveInfinity
    else
      kpd = getPhonemeDistance(src, tgt)

    kpd
  }
	private def getPhonemeDistance(str1:String, str2:String) : Double = {

		var minKpd=Double.PositiveInfinity
		var lo_str =""
		var sh_str =""
		if (str1.length()>str2.length()) {
			lo_str = str1.replaceAll(" ","")
			sh_str = str2.replaceAll(" ","")
		} else {
			lo_str = str2.replaceAll(" ","")
			sh_str = str1.replaceAll(" ","")
		}

		var lo_chAr = lo_str.toCharArray()
		var sh_chAr = sh_str.toCharArray()
		var level=lo_str.length()-sh_str.length();
		var lo_strlen = lo_str.length();

		var shMatrix=Array.ofDim[String](1)
		var pairDistMat= Array.ofDim[Double](0,0)

		/*1. build phoneme matrix */
		if (level==0) {
			shMatrix(0) = sh_chAr.toString()
		} else {
			val list = new java.util.ArrayList[String]()
			getBlankPosition(lo_strlen, 0, "", level, list);
			shMatrix = buildShStrLongMatrix(list, lo_strlen, sh_chAr);
		}

		/*2. build pairDistance matrix */
		pairDistMat = Array.ofDim[Double](shMatrix.length,lo_strlen)
		for(i <- 0 to shMatrix.length-1) {
			var mat_chAr = shMatrix(i).toCharArray();
			for (j <- 0 to lo_strlen-1) {
				pairDistMat(i)(j) = evaluateDistance(getKoreanPhonemes(Character.toString(lo_chAr(j))),
									getKoreanPhonemes(Character.toString(mat_chAr(j))));
			}
		}

		/*3. calc phoneme distance */
		var i=0
		var zcnt=0
		var minPd=Double.PositiveInfinity
		var kpd=Double.PositiveInfinity
		for(i <- 0 to pairDistMat.length-1) {
			for(j <- 0 to pairDistMat(i).length-1) {
				if (pairDistMat(i)(j) == 0) {
					zcnt = zcnt+1
				} else {
					if (minPd > pairDistMat(i)(j).toInt) {
						minPd = pairDistMat(i)(j).toInt
					}
				}
			}

			var sd = lo_strlen-zcnt;
			if (sd == 0) {
				kpd = 0 ;
			} else {
				kpd = (sd-1)*3+minPd;
			}

			if (minKpd > kpd) {
				minKpd = kpd;
			}

			zcnt=0
			minPd=Double.PositiveInfinity
			kpd=Double.PositiveInfinity
		}
		minKpd
	}
	private def getSyllablePositions(str: String): Array[Int] = {
		var prev=0
		var pos = ofDim[Int](str.length())

		for(i <- 0 to str.length-1) {
			var pstr = getKoreanPhonemes(str.substring(i, i+1))
			pos(i) = pstr.length()+prev;
			prev = pos(i);
		}
		pos
	}

	private def getBlankPosition(len: Int, prevI: Int, prefix: String, level: Int, list: java.util.ArrayList[String] ): Int= {
		for(i <- prevI+1 to len) {
			if (level <1) {
				0
			} else if (level == 1) {
				if (prefix.length()==0) list.add(""+i); else list.add(prefix+","+i);
			} else {
				if (prefix.length()==0) {
					getBlankPosition(len, i, ""+i, level-1, list);
				} else {
					getBlankPosition(len, i, prefix+","+i, level-1, list);
				}
			}
		}
		level-1
	}
	private def buildShStrLongMatrix( list: java.util.ArrayList[String], str_len:Int, sh_chAr:Array[Char]): Array[String]  = {

		//initialization
		var chArr = new Array[Array[Char]](list.length)
		for (i <- (0 to list.length - 1)) {
		   chArr(i) = new Array[Char](str_len)
		}

		var strArr = new Array[String](list.length)
		var I = list.listIterator()
		var o=0;
		while(I.hasNext()) {
			var pos = I.next()
			var posA = pos.split(",")

			//one row-data filling
			var j=0
			for(i <- 1 to chArr(o).size) {
				if (!posA.contains(i.toString())) {
					chArr(o)(i-1) = sh_chAr(j)
					j = j+1
				}
			}
			strArr(o) = chArr(o).mkString
			o = o+1
		}
		return strArr
	}

	private def getKoreanPhonemes(in: String): String = {
		//19
		val choseong = Array[Char](
				'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ',
				'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ' );

		//21
		val joongseong = Array[Char](
				'ㅏ', 'ㅐ', 'ㅑ', 'ㅒ', 'ㅓ', 'ㅔ', 'ㅕ', 'ㅖ', 'ㅗ', 'ㅘ',
				'ㅙ', 'ㅚ', 'ㅛ', 'ㅜ', 'ㅝ', 'ㅞ', 'ㅟ', 'ㅠ', 'ㅡ', 'ㅢ',
				'ㅣ');

		//28
		val jongseong = Array[Char](
				' ', 'ㄱ', 'ㄲ', 'ㄳ', 'ㄴ', 'ㄵ', 'ㄶ', 'ㄷ', 'ㄹ', 'ㄺ',
				'ㄻ', 'ㄼ', 'ㄽ', 'ㄾ', 'ㄿ', 'ㅀ', 'ㅁ', 'ㅂ', 'ㅄ', 'ㅅ',
				'ㅆ', 'ㅇ', 'ㅈ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ');

	  var out=""
		for(syllable <- in) {
			if (syllable >= 0XAC00 && syllable <= 0XD7A3) {
			  var	uniValue = syllable - 0xAC00;
			  var	Jong = uniValue % 28;
			  var	Jung = ( ( uniValue - Jong ) / 28 ) % 21;
			  var	Cho = (( ( uniValue - Jong ) / 28 ) / 21);
			  out += choseong(Cho).toString()+joongseong(Jung).toString()+jongseong(Jong).toString()
			} else {
			  out+= syllable
			}
	    }
		out = out.replaceAll(" ","")
		out
	}

    /**
     * Fast implementation of the levenshtein distance.
     * Based on: Gonzalo Navarro - A guided tour to approximate string matching
     */
	override def indexValue(str: String, limit: Double): Index = {
      val qGrams = str.qGrams(q)
      val qGramsReordered = qGrams.drop(q - 1) ++ qGrams.take(q - 1)

      val indices = qGramsReordered.take(limit.toInt * q + 1).map(indexQGram).toSet

      Index.oneDim(indices, BigInt(maxChar - minChar + 1).pow(q).toInt)
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
}