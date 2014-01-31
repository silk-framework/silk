package de.fuberlin.wiwiss.silk.preprocessing.transformer

/**
 * Created with IntelliJ IDEA.
 * User: Petar
 * Date: 21/01/14
 * Time: 14:05
 * To change this template use File | Settings | File Templates.
 */
class Ngrams(lower: Int, upper: Int) extends Transformer{

  def organize(ss: List[String]): String = {
    ss.reduce(_+" "+_)
  }

  def apply(values: List[String]):List[String] = {
      (for( i <- lower to upper) yield values.sliding(i).map(p => organize(p))).flatMap(x => x).toList
    }
}
