package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.transformer._
import de.fuberlin.wiwiss.silk.util.{Factory, Strategy}

trait Transformer extends Strategy
{
    def evaluate(strings : Seq[String]) : String
}

object Transformer extends Factory[Transformer]
{
    register("replace", classOf[ReplaceTransformer])
    register("regexReplace", classOf[RegexReplaceTransformer])
    register("concat", classOf[ConcatTransformer])
    register("removeBlanks", classOf[ReplaceTransformer], Map("search" -> " ", "replace" -> ""))
    register("lowerCase", classOf[LowerCaseTransformer])
    register("upperCase", classOf[UpperCaseTransformer])
    register("numReduce", classOf[RegexReplaceTransformer], Map("regex" -> "[^0-9]+", "replace" -> ""))

    register("stem", classOf[StemmerTransformer])
    register("stripPrefix", classOf[StripPrefixTransformer])
    register("stripPostfix", classOf[StripPostfixTransformer])
    register("stripUriPrefix", classOf[StripUriPrefixTransformer])
    register("alphaReduce", classOf[RegexReplaceTransformer], Map("regex" -> "[^\\pL]+", "replace" -> ""))
    register("removeSpecialChars", classOf[RegexReplaceTransformer], Map("regex" -> "[^\\d\\pL\\w]+", "replace" -> ""))

    /*
    TODO
    translateWithDictionary(string str[], filename dictionary.csv) 	Translates a string using a dictionary in the form of comma seperated value pairs.
    */
}
