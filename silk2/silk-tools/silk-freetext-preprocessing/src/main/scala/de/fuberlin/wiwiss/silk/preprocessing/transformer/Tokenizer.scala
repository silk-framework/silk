package de.fuberlin.wiwiss.silk.preprocessing.transformer

/**
 * Created with IntelliJ IDEA.
 * User: Petar
 * Date: 25/12/13
 * Time: 15:18
 * To change this template use File | Settings | File Templates.
 */
case class Tokenizer(regex: String = "\\s") extends  Transformer{
  private[this] val compiledRegex = regex.r

  def apply(values: List[String]): List[String] = {
    values.flatMap(compiledRegex.split)
  }
}
