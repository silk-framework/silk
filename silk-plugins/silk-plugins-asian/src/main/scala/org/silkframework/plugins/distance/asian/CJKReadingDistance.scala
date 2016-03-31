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

package org.silkframework.plugins.distance.asian

import java.util.{ArrayList, Collections}

import org.silkframework.rule.similarity.SimpleDistanceMeasure
import org.silkframework.runtime.plugin.Plugin

import scala.collection.JavaConverters._
import scala.io.Source

@Plugin(
  id = "cjkReadingDistance",
  categories = Array("Asian"),
  label = "CJK Reading Distance",
  description = "CJK Reading Distance."
)
case class CJKReadingDistance(minChar: Char = '0', maxChar: Char = 'z') extends SimpleDistanceMeasure {
  private val q = 1

  override def evaluate(str1: String, str2: String, limit: Double) = {
    var upd = 0
    var lo_str = ""
    var sh_str = ""
    //to assign long string as source and short string as target for comparison process
    if (str1.length() > str2.length()) {
      lo_str = str1
      sh_str = str2
    } else {
      lo_str = str2
      sh_str = str1
    }
    var level = lo_str.length() - sh_str.length()
    var src = makeStringToArray(lo_str.replaceAllLiterally(" ", ""))
    var tgt = makeStringToArray(sh_str.replaceAllLiterally(" ", ""))
    val text = Source.fromInputStream(getClass.getResourceAsStream("/org/silkframework/plugins/distance/asian/unihanProperties.tsv"))
    val m = for (line <- text.getLines()) yield {
      val splitLine = line.split("\t")
      val key = splitLine.head
      val tail = splitLine.tail
      (key, tail)
    }
    val dicHash = m.toMap
    if (src.equals(tgt)) {
      upd = 0
    }
    else {
      upd = compareWordValueFromDic(src, tgt, dicHash.asJava)
    }
    upd
  }

  // convert string into unicode array 
  private def makeStringToArray(origin: String): ArrayList[String] = {
    var result = new ArrayList[String]()
    var lines = (origin.split("\\r?\\n"))
    for (i <- 0 to lines.length - 1) {
      var aLine = lines(i)
      var strArray = strToArray(origin)
      result = strArray
    }
    result
  }

  //to convert string into its respective Unicode number in array
  private def strToArray(str: String): ArrayList[String] = {
    var result = new ArrayList[String]()
    for (i <- 0 to str.length() - 1) {
      var chr = str.charAt(i)
      var hex = "U+" + Integer.toHexString(chr)
      result.add(hex.toUpperCase())
    }
    result
  }

  private def getReversed(original: ArrayList[String]): ArrayList[String] = {
    var copy = original
    Collections.reverse(copy)
    copy
  }

  private def compareWordValueFromDic(leftOrigin: ArrayList[String], rightOrigin: ArrayList[String], dicHash: java.util.Map[String, Array[String]]): Integer = {
    val left = getReversed(leftOrigin)
    val right = getReversed(rightOrigin)
    var hangul = 0
    var korean = 0
    var mandarin = 0
    var japaneseKun = 0
    var japaneseOn = 0
    var distance = 0
    for (i <- 0 to left.size() - 1) {
      var leftUnimodel = dicHash.get(left.get(i))
      if (leftUnimodel != null) {
        if (right.size() > i) {
          var rightUnimodel = dicHash.get(right.get(i))
          if (rightUnimodel != null) {
            if (leftUnimodel(0).equals(rightUnimodel(0))) {
            }
            else {
              // check hangul
              if (!leftUnimodel(1).equals("null")) {
                if (rightUnimodel(1).equals(leftUnimodel(1))) {
                } else {
                  hangul = hangul + 1
                }
              } else {
                hangul = hangul + 1
              }

              // check korean
              if (!leftUnimodel(2).equals("null")) {
                if (rightUnimodel(2).equals(leftUnimodel(2))) {
                } else {
                  korean = korean + 1
                }
              } else {
                korean = korean + 1
              }

              // check mandarin
              if (!leftUnimodel(3).equals("null")) {
                if (rightUnimodel(3).equals(leftUnimodel(3))) {
                }
                else {
                  mandarin = mandarin + 1
                }
              }
              else {
                mandarin = mandarin + 1
              }

              // check japaneseKun
              if (!leftUnimodel(4).equals("null")) {
                if (rightUnimodel(4).equals(leftUnimodel(4))) {

                } else {
                  japaneseKun = japaneseKun + 1
                }
              } else {
                japaneseKun = japaneseKun + 1
              }


              // check japaneseOn
              if (!leftUnimodel(5).equals("null")) {
                if (rightUnimodel(5).equals(leftUnimodel(5))) {

                } else {
                  japaneseOn = japaneseOn + 1
                }
              } else {
                japaneseOn = japaneseOn + 1
              }
            }
          }
        }
        else {
          hangul = hangul + 1
          korean = korean + 1
          mandarin = mandarin + 1
          japaneseKun = japaneseKun + 1
          japaneseOn = japaneseOn + 1
        }
      }
    }
    distance = hangul + korean + mandarin + japaneseKun + japaneseOn
    distance
  }
}

