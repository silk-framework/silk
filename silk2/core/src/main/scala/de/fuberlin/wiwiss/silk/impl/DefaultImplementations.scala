package de.fuberlin.wiwiss.silk.impl

import datasource._
import transformer._
import aggegrator._
import blockingfunction._
import writer._
import metric._
import de.fuberlin.wiwiss.silk.datasource.DataSource
import de.fuberlin.wiwiss.silk.linkspec.input.Transformer
import de.fuberlin.wiwiss.silk.linkspec.{Metric, Aggregator, BlockingFunction}
import de.fuberlin.wiwiss.silk.output.{AlignmentWriter, Formatter, Output}

object DefaultImplementations
{
    def register()
    {
        DataSource.register("sparqlEndpoint", classOf[SparqlDataSource])

        BlockingFunction.register("numeric", classOf[NumBlockingFunction])
        BlockingFunction.register("alphanumeric", classOf[AlphaNumBlockingFunction])

        Transformer.register("replace", classOf[ReplaceTransformer])
        Transformer.register("regexReplace", classOf[RegexReplaceTransformer])
        Transformer.register("concat", classOf[ConcatTransformer])
        Transformer.register("removeBlanks", classOf[ReplaceTransformer], Map("search" -> " ", "replace" -> ""))
        Transformer.register("lowerCase", classOf[LowerCaseTransformer])
        Transformer.register("upperCase", classOf[UpperCaseTransformer])
        Transformer.register("numReduce", classOf[RegexReplaceTransformer], Map("regex" -> "[^0-9]+", "replace" -> ""))
        Transformer.register("stem", classOf[StemmerTransformer])
        Transformer.register("stripPrefix", classOf[StripPrefixTransformer])
        Transformer.register("stripPostfix", classOf[StripPostfixTransformer])
        Transformer.register("stripUriPrefix", classOf[StripUriPrefixTransformer])
        Transformer.register("alphaReduce", classOf[RegexReplaceTransformer], Map("regex" -> "[^\\pL]+", "replace" -> ""))
        Transformer.register("removeSpecialChars", classOf[RegexReplaceTransformer], Map("regex" -> "[^\\d\\pL\\w]+", "replace" -> ""))

        Metric.register("levenshtein", classOf[LevenshteinMetric])
        Metric.register("jaro", classOf[JaroDistanceMetric])
        Metric.register("jaroWinkler", classOf[JaroWinklerMetric])
        Metric.register("qGrams", classOf[QGramsMetric])
        Metric.register("equality", classOf[EqualityMetric])
        Metric.register("num", classOf[NumMetric])
        Metric.register("date", classOf[DateMetric])
        Metric.register("wgs84", classOf[GeographicDistanceMetric])

        Aggregator.register("average", classOf[AverageAggregator])
        Aggregator.register("max", classOf[MaximumAggregator])
        Aggregator.register("min", classOf[MinimumAggregator])
        Aggregator.register("quadraticMean", classOf[QuadraticMeanAggregator])
        Aggregator.register("geometricMean", classOf[GeometricMeanAggregator])

        AlignmentWriter.register("file", classOf[FileWriter])
        AlignmentWriter.register("memory", classOf[MemoryWriter])

        Formatter.register("ntriples", classOf[NTriplesFormatter])
        Formatter.register("alignment", classOf[AlignmentFormatter])
    }
}