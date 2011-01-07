package de.fuberlin.wiwiss.silk.impl

import datasource._
import transformer._
import aggegrator._
import writer._
import metric._
import de.fuberlin.wiwiss.silk.datasource.DataSource
import de.fuberlin.wiwiss.silk.linkspec.input.Transformer
import de.fuberlin.wiwiss.silk.linkspec.{Metric, Aggregator}
import de.fuberlin.wiwiss.silk.output.{LinkWriter, Formatter}

/**
 * Registers all default implementations.
 */
object DefaultImplementations
{
    def register()
    {
        DataSource.register(classOf[SparqlDataSource])
        DataSource.register(classOf[CacheDataSource])

//        Transformer.register("replace", classOf[ReplaceTransformer])
//        Transformer.register("regexReplace", classOf[RegexReplaceTransformer])
//        Transformer.register("concat", classOf[ConcatTransformer])
//        Transformer.register("removeBlanks", classOf[ReplaceTransformer], Map("search" -> " ", "replace" -> ""))
        Transformer.register(classOf[LowerCaseTransformer])
//        Transformer.register("upperCase", classOf[UpperCaseTransformer])
//        Transformer.register("numReduce", classOf[RegexReplaceTransformer], Map("regex" -> "[^0-9]+", "replace" -> ""))
//        Transformer.register("stem", classOf[StemmerTransformer])
//        Transformer.register("stripPrefix", classOf[StripPrefixTransformer])
//        Transformer.register("stripPostfix", classOf[StripPostfixTransformer])
//        Transformer.register("stripUriPrefix", classOf[StripUriPrefixTransformer])
//        Transformer.register("alphaReduce", classOf[RegexReplaceTransformer], Map("regex" -> "[^\\pL]+", "replace" -> ""))
//        Transformer.register("removeSpecialChars", classOf[RegexReplaceTransformer], Map("regex" -> "[^\\d\\pL\\w]+", "replace" -> ""))

        Metric.register(classOf[LevenshteinMetric])
        Metric.register(classOf[JaroDistanceMetric])
        Metric.register(classOf[JaroWinklerMetric])
        Metric.register(classOf[QGramsMetric])
        Metric.register(classOf[EqualityMetric])
        Metric.register(classOf[NumMetric])
        Metric.register(classOf[DateMetric])
        Metric.register(classOf[GeographicDistanceMetric])

        Aggregator.register(classOf[AverageAggregator])
        Aggregator.register(classOf[MaximumAggregator])
        Aggregator.register(classOf[MinimumAggregator])
        Aggregator.register(classOf[QuadraticMeanAggregator])
        Aggregator.register(classOf[GeometricMeanAggregator])

        LinkWriter.register(classOf[FileWriter])
        LinkWriter.register(classOf[MemoryWriter])

        Formatter.register(classOf[NTriplesFormatter])
        Formatter.register(classOf[AlignmentFormatter])
    }
}