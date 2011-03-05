package de.fuberlin.wiwiss.silk.impl

import datasource._
import transformer._
import aggegrator._
import writer._
import metric._
import de.fuberlin.wiwiss.silk.datasource.DataSource
import de.fuberlin.wiwiss.silk.linkspec.input.Transformer
import de.fuberlin.wiwiss.silk.linkspec.condition.{Aggregator, Metric}
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

        Transformer.register(classOf[ReplaceTransformer])
        Transformer.register(classOf[RegexReplaceTransformer])
        Transformer.register(classOf[ConcatTransformer])
        Transformer.register(classOf[RemoveBlanksTransformer])
        Transformer.register(classOf[LowerCaseTransformer])
        Transformer.register(classOf[UpperCaseTransformer])
        Transformer.register(classOf[NumReduceTransformer])
        Transformer.register(classOf[StemmerTransformer])
        Transformer.register(classOf[StripPrefixTransformer])
        Transformer.register(classOf[StripPostfixTransformer])
        Transformer.register(classOf[StripUriPrefixTransformer])
        Transformer.register(classOf[AlphaReduceTransformer])
        Transformer.register(classOf[RemoveSpecialCharsTransformer])
        Transformer.register(classOf[LogarithmTransformer])

        Metric.register(classOf[LevenshteinMetric])
        Metric.register(classOf[JaroDistanceMetric])
        Metric.register(classOf[JaroWinklerMetric])
        Metric.register(classOf[QGramsMetric])
        Metric.register(classOf[EqualityMetric])
        Metric.register(classOf[InequalityMetric])
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