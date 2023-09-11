package org.silkframework.rule.plugins.transformer.linguistic

import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.runtime.plugin.annotations.Plugin

@Plugin(
  id = "NYSIIS",
  categories = Array("Linguistic"),
  label = "NYSIIS",
  description = "NYSIIS phonetic encoding."
)
case class NysiisTransformer(refined: Boolean = true) extends SimpleTransformer {

  override def evaluate(value: String) = {
    if(refined)
      RefinedNysiisAlgorithm.compute(value).getOrElse("")
    else
      NysiisAlgorithm.compute(value).getOrElse("")
  }
}

// Based on code of the StringMetric library: http://rockymadden.com/stringmetric/.
private object RefinedNysiisAlgorithm {
  def compute(a: Array[Char]): Option[Array[Char]] =
    if (a.length == 0 || !(Alpha.isAlpha(a.head))) {
      None
    } else {
      val lca = a.map(_.toLower)
      val tlh = (transcodeHead andThen transcodeLast)(lca.head +: cleanLast(lca.tail, Set('s', 'z')))
      val t = transcode(Array.empty[Char], tlh.head, tlh.tail, Array.empty[Char])

      if (t.length == 1) Some(t)
      else Some(deduplicate(
        t.head +: (cleanLast.tupled andThen cleanTerminal)(t.tail, Set('a'))
      ))
    }

  def compute(string: String): Option[String] = compute(string.toCharArray).map(_.mkString)

  private val cleanLast: ((Array[Char], Set[Char]) => Array[Char]) = (ca, s) =>
    if (ca.length == 0) ca
    else if (s.contains(ca.last)) ca.dropRight(ca.reverseIterator.takeWhile(c => s.contains(c)).length)
    else ca

  private val cleanTerminal: (Array[Char] => Array[Char]) = (ca) =>
    if (ca.length >= 2 && ca.last == 'y' && ca(ca.length - 2) == 'a') ca.dropRight(2) :+ 'y'
    else ca

  private val deduplicate: (Array[Char] => Array[Char]) = (ca) =>
    if (ca.length <= 1) ca
    else ca.sliding(2).withFilter(a => a(0) != a(1)).map(a => a(0)).toArray[Char] :+ ca.last
  
  private val transcode: ((Array[Char], Char, Array[Char], Array[Char]) => Array[Char]) = (l, c, r, o) =>
    if (c == '\u0000' && r.length == 0) o
    else {
      def shift(d: Int, ca: Array[Char]) = {
        val sca = r.splitAt(d - 1)

        (
          if (sca._1.length > 0) (l :+ c) ++ sca._1 else l :+ c,
          if (sca._2.length > 0) sca._2.head else '\u0000',
          if (sca._2.length > 1) sca._2.tail else Array.empty[Char],
          ca
        )
      }

      val t = {
        (c: @annotation.switch) match {
          case 'a' | 'i' | 'o' | 'u' =>
            if (l.length == 0) shift(1, o :+ c)
            else shift(1, o :+ 'a')
          case 'b' | 'c' | 'f' | 'j' | 'l' | 'n' | 'r' | 't' | 'v' | 'x' => shift(1, o :+ c)
          case 'd' =>
            if (r.length >= 1 && r.head == 'g') shift(2, o :+ 'g') else shift(1, o :+ c)
          case 'e' =>
            if (l.length == 0) shift(1, o :+ c)
            else if (r.length >= 1 && r.head == 'v') shift(2, o ++ Array('a', 'f'))
            else shift(1, o :+ 'a')
          case 'g' =>
            if (r.length >= 2 && r.head == 'h' && r(1) == 't') shift(3, o ++ Array('g', 't'))
            else shift(1, o :+ c)
          case 'h' =>
            if (l.length == 0) shift(1, o :+ c)
            else if (!(Alpha.isLowercaseVowel(l.last)) || (r.length >= 1 && !(Alpha.isLowercaseVowel(r.head))))
              shift(1, o)
            else shift(1, o :+ c)
          case 'k' => if (r.length >= 1 && r.head == 'n') shift(2, o :+ 'n') else shift(1, o :+ 'c')
          case 'm' => if (l.length == 0) shift(1, o :+ c) else shift(1, o :+ 'n')
          case 'p' => if (r.length >= 1 && r.head == 'h') shift(2, o :+ 'f') else shift(1, o :+ c)
          case 'q' => if (l.length == 0) shift(1, o :+ c) else shift(1, o :+ 'g')
          case 's' =>
            if (r.length >= 2 && r.head == 'c' && r(1) == 'h') shift(3, o :+ c)
            else if (r.length >= 1 && r.head == 'h') shift(2, o :+ c)
            else shift(1, o :+ c)
          case 'w' =>
            if (l.length >= 1 && (Alpha.isLowercaseVowel(l.last))) shift(1, o)
            else if (r.length >= 1 && r.head == 'r') shift(2, o :+ 'r')
            else shift(1, o :+ c)
          case 'y' =>
            if (l.length >= 1 && r.length >= 2 && r.head == 'w') shift(2, o :+ 'a')
            else if (r.length >= 1 && r.head == 'w') shift(2, o :+ c)
            else if (l.length >= 1 && r.length >= 1) shift(1, o :+ 'a')
            else shift(1, o :+ c)
          case 'z' => if (l.length == 0) shift(1, o :+ c) else shift(1, o :+ 's')
          case _ => shift(1, o)
        }
      }

      transcode(t._1, t._2, t._3, t._4)
    }

  private val transcodeHead: (Array[Char] => Array[Char]) = (ca) =>
    if (ca.length == 0) ca
    else
      (ca.head: @annotation.switch) match {
        case 'm' if ca.length >= 3 && ca(1) == 'a' && ca(2) == 'c' =>
          Array('m', 'c') ++ ca.takeRight(ca.length - 3)
        case 'p' if ca.length >= 2 && ca(1) == 'f' => 'f' +: ca.takeRight(ca.length - 2)
        case _ => ca
      }

  private val transcodeLast: (Array[Char] => Array[Char]) = (ca) =>
    if (ca.length >= 2) {
      val lc = ca(ca.length - 1)
      val lcm1 = ca(ca.length - 2)
      lazy val t2 = ca.take(ca.length - 2)

      (lc: @annotation.switch) match {
        case 'd' if lcm1 == 'n' || lcm1 == 'r' => t2 :+ 'd'
        case 'e' if lcm1 == 'e' || lcm1 == 'i' || lcm1 == 'y' => t2 :+ 'y'
        case 't' if lcm1 == 'd' || lcm1 == 'n' || lcm1 == 'r' => t2 :+ 'd'
        case 'x' if lcm1 == 'e' => t2 ++ Array('e', 'c')
        case 'x' if lcm1 == 'i' => t2 ++ Array('i', 'c')
        case _ => ca
      }
    } else ca
}

// Based on code of the StringMetric library: http://rockymadden.com/stringmetric/.
private object NysiisAlgorithm {

  def compute(a: Array[Char]): Option[Array[Char]] =
    if (a.length == 0 || !(Alpha.isAlpha(a.head))) None
    else {
      val tr = transcodeRight(a.map(_.toLower))
      val tl = transcodeLeft(tr._1)
      val t =
        if (tl._2.length == 0) tl._1 ++ tr._2
        else tl._1 ++ transcodeCenter(
          Array.empty[Char],
          tl._2.head,
          if (tl._2.length > 1) tl._2.tail else Array.empty[Char],
          Array.empty[Char]
        ) ++ tr._2

      if (t.length == 1) Some(t)
      else Some(t.head +: (cleanLast andThen cleanTerminal andThen deduplicate)(t.tail))
    }

  def compute(a: String): Option[String] = compute(a.toCharArray).map(_.mkString)

  private val cleanLast: (Array[Char] => Array[Char]) = (ca) =>
    if (ca.length == 0) ca
    else if (ca.last == 'a' || ca.last == 's')
      ca.dropRight(ca.reverseIterator.takeWhile(c => c == 'a' || c == 's').length)
    else ca

  private val cleanTerminal: (Array[Char] => Array[Char]) = (ca) =>
    if (ca.length >= 2 && ca.last == 'y' && ca(ca.length - 2) == 'a') ca.dropRight(2) :+ 'y'
    else ca

  private val deduplicate: (Array[Char] => Array[Char]) = (ca) =>
    if (ca.length <= 1) ca
    else ca.sliding(2).withFilter(a => a(0) != a(1)).map(_(0)).toArray[Char] :+ ca.last
  
  private val transcodeCenter: ((Array[Char], Char, Array[Char], Array[Char]) => Array[Char]) = (l, c, r, o) =>
    if (c == '\u0000' && r.length == 0) o
    else {
      def shift(d: Int, ca: Array[Char]) = {
        val sca = r.splitAt(d - 1)

        (
          if (sca._1.length > 0) (l :+ c) ++ sca._1 else l :+ c,
          if (sca._2.length > 0) sca._2.head else '\u0000',
          if (sca._2.length > 1) sca._2.tail else Array.empty[Char],
          ca
        )
      }

      val t = {
        (c: @annotation.switch) match {
          case 'a' | 'i' | 'o' | 'u' => shift(1, o :+ 'a')
          case 'b' | 'c' | 'd' | 'f' | 'g' | 'j' | 'l' | 'n' | 'r' | 't' | 'v' | 'x' | 'y' => shift(1, o :+ c)
          case 'e' =>
            if (r.length >= 1 && r.head == 'v') shift(2, o ++ Array('a', 'f'))
            else shift(1, o :+ 'a')
          case 'h' =>
            if (l.length >= 1 && (!(Alpha.isLowercaseVowel(l.last)) || (r.length >= 1 && !(Alpha.isLowercaseVowel(r.head)))))
              shift(1, o)
            else shift(1, o :+ c)
          case 'k' => if (r.length >= 1 && r.head == 'n') shift(2, o :+ 'n') else shift(1, o :+ 'c')
          case 'm' => shift(1, o :+ 'n')
          case 'p' => if (r.length >= 1 && r.head == 'h') shift(2, o :+ 'f') else shift(1, o :+ c)
          case 'q' => shift(1, o :+ 'g')
          case 's' =>
            if (r.length >= 2 && r.head == 'c' && r(1) == 'h') shift(3, o :+ c)
            else shift(1, o :+ c)
          case 'w' =>
            if (l.length >= 1 && (Alpha.isLowercaseVowel(l.last))) shift(1, o)
            else shift(1, o :+ c)
          case 'z' => shift(1, o :+ 's')
          case _ => shift(1, o)
        }
      }

      transcodeCenter(t._1, t._2, t._3, t._4)
    }

  private val transcodeLeft: (Array[Char] => (Array[Char], Array[Char])) = (ca) =>
    if (ca.length == 0) (Array.empty[Char], ca)
    else {
      lazy val tr2 = ca.takeRight(ca.length - 2)
      lazy val tr3 = ca.takeRight(ca.length - 3)

      (ca.head: @annotation.switch) match {
        case 'k' if ca.length >= 2 && ca(1) == 'n' => (Array('n', 'n'), tr2)
        case 'k' => (Array('c'), ca.tail)
        case 'm' if ca.length >= 3 && (ca(1) == 'a' && ca(2) == 'c') => (Array('m', 'c'), tr3)
        case 'p' if ca.length >= 2 && (ca(1) == 'h' || ca(1) == 'f') => (Array('f', 'f'), tr2)
        case 's' if ca.length >= 3 && (ca(1) == 'c' && ca(2) == 'h') => (Array('s', 's'), tr3)
        case _ => (Array(ca.head), ca.tail)
      }
    }

  private val transcodeRight: (Array[Char] => (Array[Char], Array[Char])) = (ca) =>
    if (ca.length >= 2) {
      val lc = ca(ca.length - 1)
      val lcm1 = ca(ca.length - 2)
      lazy val t2 = ca.take(ca.length - 2)

      (lc: @annotation.switch) match {
        case 'd' if lcm1 == 'n' || lcm1 == 'r' => (t2, Array('d'))
        case 'e' if lcm1 == 'e' || lcm1 == 'i' => (t2, Array('y'))
        case 't' if lcm1 == 'd' || lcm1 == 'n' || lcm1 == 'r' => (t2, Array('d'))
        case _ => (ca, Array.empty[Char])
      }
    } else (ca, Array.empty[Char])
}