package de.fuberlin.wiwiss.silk.preprocessing.transformer

/**
 * Ngrams
 * Creates ngrams between two bounds
 *
 * @param lower The lower bound
 * @param upper The upper bound
 */
class Ngrams(lower: Int, upper: Int) extends Transformer{

  def organize(ss: List[String]): String = {
    ss.reduce(_+" "+_)
  }

  def apply(values: List[String]):List[String] = {
      (for( i <- lower to upper) yield values.sliding(i).map(p => organize(p))).flatMap(x => x).toList
    }
}
