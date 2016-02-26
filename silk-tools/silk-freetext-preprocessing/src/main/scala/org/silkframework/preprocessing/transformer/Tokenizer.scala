package org.silkframework.preprocessing.transformer

/**
 * Ngrams
 * Creates ngrams between two bounds
 *
 * @param regex Represent a delimiter
 */
case class Tokenizer(regex: String = "\\s") extends  Transformer{

  //TODO: Borrow the Rapidminer english and german tokenizers


  private[this] val compiledRegex = regex.r

  def apply(values: List[String]): List[String] = {
    values.flatMap(compiledRegex.split)
  }
}
