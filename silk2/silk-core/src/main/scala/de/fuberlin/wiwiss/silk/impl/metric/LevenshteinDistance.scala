package de.fuberlin.wiwiss.silk.impl.metric

import de.fuberlin.wiwiss.silk.util.StringUtils._
import scala.math.{min, max, abs}
import de.fuberlin.wiwiss.silk.util.strategy.StrategyAnnotation
import de.fuberlin.wiwiss.silk.linkspec.condition.SimpleDistanceMeasure

@StrategyAnnotation(id = "levenshteinDistance", label = "Levenshtein distance", description = "Levenshtein distance.")
case class LevenshteinDistance(minChar : Char = '0', maxChar : Char = 'z') extends SimpleDistanceMeasure
{
  private val q = 1

  override def evaluate(str1 : String, str2 : String, limit : Double) =
  {
    if(abs(str1.length - str2.length) >= limit)
    {
      Double.PositiveInfinity
    }
    else
    {
      evaluateDistance(str1, str2).toDouble
    }
  }

  override def index(str : String, limit : Double) : Set[Seq[Int]] =
  {
    val qGrams = str.qGrams(q)
    val qGramsReordered = qGrams.drop(q - 1) ++ qGrams.take(q - 1)

    val index = qGramsReordered.take(limit.toInt * q + 1).map(indexQGram).toSet

    index
  }

  private def indexQGram(qGram : String) =
  {
    def combine(index : Int, char : Char) =
    {
      val croppedChar = min(max(char, minChar), maxChar)
      index * (maxChar - minChar + 1) + croppedChar - minChar
    }

    Seq(qGram.foldLeft(0)(combine))
  }

  override def blockCounts(threshold : Double) : Seq[Int] =
  {
    Seq(BigInt(maxChar - minChar + 1).pow(q).toInt)
  }

  private def evaluateDistance(str1 : String, str2 : String): Int =
  {
    val lenStr1 = str1.length
    val lenStr2 = str2.length

    val d: Array[Array[Int]] = Array.ofDim(lenStr1 + 1, lenStr2 + 1)

    for (i <- 0 to lenStr1) d(i)(0) = i
    for (j <- 0 to lenStr2) d(0)(j) = j

    for (i <- 1 to lenStr1; val j <- 1 to lenStr2) {
      val cost = if (str1(i - 1) == str2(j - 1)) 0 else 1

      d(i)(j) = math.min(
        d(i - 1)(j) + 1, // deletion
        math.min(d(i)(j - 1) + 1, // insertion
          d(i - 1)(j - 1) + cost) // substitution
      )
    }
    return d(lenStr1)(lenStr2)
  }

  //from [Gonzalo Navarro - A guided tour to approximate string matching]
  private def evaluateDistance2(p : String, t : String) : Int =
  {
    var min = Integer.MAX_VALUE

    //Previous column in the matrix
    var pc = new Array[Int](p.length + 1)

    //Current column in the matrix
    var c = new Array[Int](p.length + 1)

    //Substring matching: The empty pattern matches with zero errors at any text position
    //        for (int i = 0; i <= p.length(); i++)
    //            c[i] = i;

    //Build the matrix
    for(j <- 1 to t.length())
    {
      //Swap the current and the previous column
      val temp = pc;
      pc = c;
      c = temp;

      //Compute current column
      for(i <- 1 to p.length())
      {
        if (p.charAt(i - 1) == t.charAt(j - 1))
          c(i) = pc(i - 1)
        else
          c(i) = 1 + min3(c(i - 1), pc(i), pc(i - 1))
      }

      if (c(p.length()) < min)
        min = c(p.length())
    }

    return min;
  }

  private def min3(x : Int, y : Int, z : Int) = math.min(x, math.min(y, z))
}
