package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.transformer.ReplaceTransformer

trait Transformer
{
    val params : Map[String, String]

    def evaluate(strings : Seq[String]) : String
}

object Transformer
{
    def apply(transformFunction : String, params : Map[String, String]) : Transformer =
    {
        transformFunction match
        {
            case "replace" => new ReplaceTransformer(params)
            case _ => throw new IllegalArgumentException("Transform function unknown: " + transformFunction)
        }
    }
}