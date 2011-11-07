package de.fuberlin.wiwiss.silk.plugins.transformer

import de.fuberlin.wiwiss.silk.util.plugin.Plugin
import de.fuberlin.wiwiss.silk.linkagerule.input.SimpleTransformer

@Plugin(id = "capitalize", label = "Capitalize", description = "Capitalizes the string i.e. converts the first character to upper case. " +
    "If 'allWords' is set to true, all words are capitalized and not only the first character.")
case class CapitalizeTransformer(allWords: Boolean = false) extends SimpleTransformer {
  override def evaluate(value: String) = {
    if(allWords)
      value.capitalize
    else
      value.split("\\s+").map(_.capitalize).mkString
  }
}