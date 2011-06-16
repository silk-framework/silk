package de.fuberlin.wiwiss.silk.impl

import datasource._
import transformer._
import aggegrator._
import writer._
import metric._
import de.fuberlin.wiwiss.silk.datasource.DataSource
import de.fuberlin.wiwiss.silk.linkspec.input.Transformer
import de.fuberlin.wiwiss.silk.linkspec.condition.{Aggregator, DistanceMeasure}
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
        Transformer.register(classOf[ConvertCharsetTransformer])
        Transformer.register(classOf[RemoveEmptyValues])
        Transformer.register(classOf[Tokenizer])

        DistanceMeasure.register(classOf[LevenshteinMetric])
        DistanceMeasure.register(classOf[LevenshteinDistance])
        DistanceMeasure.register(classOf[JaroDistanceMetric])
        DistanceMeasure.register(classOf[JaroWinklerDistance])
        DistanceMeasure.register(classOf[QGramsMetric])
        DistanceMeasure.register(classOf[EqualityMetric])
        DistanceMeasure.register(classOf[InequalityMetric])
        DistanceMeasure.register(classOf[NumMetric])
        DistanceMeasure.register(classOf[DateMetric])
        DistanceMeasure.register(classOf[DateTimeMetric])
        DistanceMeasure.register(classOf[GeographicDistanceMetric])
        DistanceMeasure.register(classOf[JaccardDistance])
        DistanceMeasure.register(classOf[DiceCoefficient])

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