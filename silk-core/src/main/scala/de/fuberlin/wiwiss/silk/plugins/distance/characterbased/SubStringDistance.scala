package de.fuberlin.wiwiss.silk.plugins.distance.characterbased

import de.fuberlin.wiwiss.silk.entity.Index
import de.fuberlin.wiwiss.silk.rule.similarity.SimpleDistanceMeasure
import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin

/**
 * Created by IntelliJ IDEA.
 * User: andreas
 * Date: 3/19/12
 * Time: 2:56 PM
 * To change this template use File | Settings | File Templates.
 */

@Plugin(
  id = "substring",
  categories = Array("Characterbased"),
  label = "SubString",
  description = "Return 0 to 1 for strong similarity to weak similarity. Based on the paper: Stoilos, Giorgos, Giorgos Stamou, and Stefanos Kollias. \"A string metric for ontology alignment.\" The Semantic Web-ISWC 2005. Springer Berlin Heidelberg, 2005. 624-637."
)
case class SubStringDistance(granularity: String = "3") extends SimpleDistanceMeasure {
  private val n = granularity.toInt

  override def evaluate(str1: String, str2: String, threshold: Double) = {
    val score = SubStringDistance.score(str1, str2)
    if(score < 0)
      1.0
    else
      1.0 - score
  }

  override def indexValue(str: String, threshold: Double) = {
    val nGrams = SubStringDistance.getNgrams(str, n)
    Index.oneDim(nGrams.map(a => a.hashCode()).toSet)
  }
}

object SubStringDistance {
  def normalizeString(_str: String): String = {
    var str: String = _str
    str = normalizeString(str, '.')
    str = normalizeString(str, '_')
    str = normalizeString(str, ' ')
    str = normalizeString(str, '-')
    return str
  }

  // Find the largest common substring and return the start and stop indexes of both strings and the substring length
  private def findBestSubString(s1: String, s2: String, _best: Int): (Int, Int, Int, Int, Int) = {
    var best = _best
    val str1len = s1.length
    val str2len = s2.length

    var i = 0
    var j = 0

    var startS2 = 0
    var endS2 = 0
    var startS1 = 0
    var endS1 = 0
    var p = 0

    while ((i < str1len) && (str1len - i > best)) {
      j = 0
      while (str2len - j > best) {
        var k = i
        while ((j < str2len) && (s1.charAt(k) != s2.charAt(j)))
          j += 1

        if (j != str2len) {
          p = j
          j += 1
          k += 1
          while ((j < str2len) && (k < str1len) && (s1.charAt(k) == s2.charAt(j))) {
            j += 1
            k += 1
          }
          if (k - i > best) {
            best = k - i
            startS1 = i
            endS1 = k
            startS2 = p
            endS2 = j
          }
        }
      }
      i += 1
    }
    (best, startS1, endS1, startS2, endS2)
  }

  // Remove the string between the start and end index of the given string
  private def removeSubString(_str: String, endSubStr: Int, startSubStr: Int): String = {
    var str: String = _str
    var newString = new Array[Char](str.length - (endSubStr - startSubStr));

    var j = 0
    for (i <- 0 until str.length()) {
      if (!((i >= startSubStr) && (i < endSubStr))) {
        newString(j) = str.charAt(i);
        j += 1
      }
    }

    str = new String(newString);
    str
  }

  // Compute the dissimilarity component
  private def computeDissimilarity(origStr1Len: Int, common: Double, origStr2Len: Int): Double = {
    var dissimilarity = 0.0D;

    val rest1 = origStr1Len - common;
    val rest2 = origStr2Len - common;

    var unmatchedS1 = math.max(rest1, 0.0D);
    var unmatchedS2 = math.max(rest2, 0.0D);
    unmatchedS1 = rest1 / origStr1Len;
    unmatchedS2 = rest2 / origStr2Len;

    val suma = unmatchedS1 + unmatchedS2;
    val product = unmatchedS1 * unmatchedS2;
    val p = 0.6D;
    if (suma - product == 0.0D)
      dissimilarity = 0.0D;
    else {
      dissimilarity = product / (p + (1.0D - p) * (suma - product));
    }
    dissimilarity
  }

  /**
   * Calculates the similarity score [-1..1]
   */
  def score(str1: String, str2: String): Double = {
    if (invalidStrings(str1, str2)) {
      return -1.0D
    }

    var s1 = str1.toLowerCase
    var s2 = str2.toLowerCase

    if (s1.equals(s2)) {
      return 1.0
    }
    s1 = normalizeString(s1)
    s2 = normalizeString(s2)

    val origStr1Len = s1.length()
    val origStr2Len = s2.length()

    if ((origStr1Len == 0) && (origStr2Len == 0))
      return 0.0
    if ((origStr1Len == 0) || (origStr2Len == 0)) {
      return 1.0
    }

    val common = computeCommonSubStrings(s1, s2)

    val commonality = computeCommonality(common, origStr1Len, origStr2Len)

    val winklerImprov: Double = winklerImprovement(str1, str2, commonality);

    val dissimilarity: Double = computeDissimilarity(origStr1Len, common, origStr2Len)

    return commonality - dissimilarity + winklerImprov;
  }

  private def computeCommonSubStrings(str1: String, str2: String): Double = {
    var s1 = str1
    var s2 = str2
    var common = 0.0
    var best = 2
    while ((s1.length() > 0) && (s2.length() > 0) && (best != 0)) {
      best = 0

      val r = findBestSubString(s1, s2, best)
      best = r._1
      var startS1: Int = r._2
      var endS1: Int = r._3
      var startS2: Int = r._4
      var endS2: Int = r._5

      s1 = removeSubString(s1, endS1, startS1)
      s2 = removeSubString(s2, endS2, startS2)

      if (best > 2)
        common += best;
      else {
        best = 0;
      }
    }
    return common
  }

  private def computeCommonality(common: Double, str1Len: Int, str2Len:Int):Double = {
    return 2.0D * common / (str1Len + str2Len);
  }

  private def invalidStrings(str1: String, str2: String): Boolean = {
    (str1 == null) || (str2 == null) ||
      (str1.length() == 0) || (str2.length() == 0)
  }

  private def winklerImprovement(s1: String, s2: String, commonality: Double): Double = {
    val diffIndex = indexOfDifference(s1, s2)
    val commonPrefixLength = math.min(4, diffIndex)

    return commonPrefixLength * 0.1D * (1.0D - commonality)
  }

  private def indexOfDifference(s1: String, s2: String): Int = {
    var i = 0
    val n = math.min(s1.length(), s2.length());
    while (i < n) {
      if ((s1(i) != s2(i)))
        return i
      i += 1
    }
    return n-1
  }

  def normalizeString(str: String, removeChar: Char): String = {
    val strBuf = new StringBuilder();

    for (character <- str)
      if (character != removeChar)
        strBuf.append(character)

    return strBuf.toString();
  }

  def getNgrams(str: String, n: Int = 3): Seq[String] = {
    val normString = normalizeString(str).toLowerCase
    if(normString.length <= n)
      return Seq(normString)
    for(i <- 0 to normString.length-n)
      yield normString.substring(i, i+n)
  }

  def main(args: Array[String]) {
    println(score("aaabbb", "aaaBBB"))
    println(score("numPersons", "personNumber"))
    println(score("trythis", "trythat"))
    println(score("aaaccc", "aaaBBB"))
    println(score("aaabbbccc", "cccbbbaaa"))
    println(score("gsdhgsdihgaaageigerer", "vmwnbifvsdaaafgsdhje"))
    println(score("aopqrst", "rtpaopqs"))
    println(score("NrPersons", "NumberPers"))
    println(score("aabb", "bbaa"))
    println(score("aaaaork", "efghaaaa"))
    val lev = new LevenshteinDistance
    println(lev.indexValue("aaaa", 0.0))
    println('a'.toInt)
    val isub = new SubStringDistance
    println(isub.indexValue("abcdef",0.0))
  }
}