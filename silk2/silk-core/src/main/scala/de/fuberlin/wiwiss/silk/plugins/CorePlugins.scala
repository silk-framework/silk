/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fuberlin.wiwiss.silk.plugins

import java.util.logging.{Logger, Level}

import de.fuberlin.wiwiss.silk.dataset.DatasetPlugin
import de.fuberlin.wiwiss.silk.linkagerule.input.Transformer
import de.fuberlin.wiwiss.silk.linkagerule.similarity.{Aggregator, DistanceMeasure}
import de.fuberlin.wiwiss.silk.plugins.aggegrator.{AverageAggregator, GeometricMeanAggregator, MaximumAggregator, MinimumAggregator, QuadraticMeanAggregator}
import de.fuberlin.wiwiss.silk.plugins.dataset.csv.CsvDataset
import de.fuberlin.wiwiss.silk.plugins.dataset.xml.XmlDataset
import de.fuberlin.wiwiss.silk.plugins.distance.asian.{CJKReadingDistance, KoreanPhonemeDistance, KoreanTranslitDistance}
import de.fuberlin.wiwiss.silk.plugins.distance.characterbased._
import de.fuberlin.wiwiss.silk.plugins.distance.equality._
import de.fuberlin.wiwiss.silk.plugins.distance.numeric._
import de.fuberlin.wiwiss.silk.plugins.distance.tokenbased._
import de.fuberlin.wiwiss.silk.plugins.transformer.ConstantTransformer
import de.fuberlin.wiwiss.silk.plugins.transformer.combine.{ConcatMultipleValuesTransformer, ConcatTransformer, MergeTransformer}
import de.fuberlin.wiwiss.silk.plugins.transformer.conversion.ConvertCharsetTransformer
import de.fuberlin.wiwiss.silk.plugins.transformer.date._
import de.fuberlin.wiwiss.silk.plugins.transformer.filter.{FilterByLength, FilterByRegex, RemoveEmptyValues, RemoveValues}
import de.fuberlin.wiwiss.silk.plugins.transformer.linguistic._
import de.fuberlin.wiwiss.silk.plugins.transformer.normalize._
import de.fuberlin.wiwiss.silk.plugins.transformer.numeric._
import de.fuberlin.wiwiss.silk.plugins.transformer.replace.{RegexReplaceTransformer, ReplaceTransformer}
import de.fuberlin.wiwiss.silk.plugins.transformer.substring._
import de.fuberlin.wiwiss.silk.plugins.transformer.tokenization.{CamelCaseTokenizer, Tokenizer}
import de.fuberlin.wiwiss.silk.plugins.transformer.spatial.GeometryTransformer
import de.fuberlin.wiwiss.silk.plugins.distance.spatial.CentroidDistanceMetric
import de.fuberlin.wiwiss.silk.plugins.distance.spatial.MinDistanceMetric
import de.fuberlin.wiwiss.silk.plugins.distance.spatial.ContainsMetric
import de.fuberlin.wiwiss.silk.plugins.distance.spatial.CrossesMetric
import de.fuberlin.wiwiss.silk.plugins.distance.spatial.DisjointMetric
import de.fuberlin.wiwiss.silk.plugins.distance.spatial.EqualsMetric
import de.fuberlin.wiwiss.silk.plugins.distance.spatial.IntersectsMetric
import de.fuberlin.wiwiss.silk.plugins.distance.spatial.OverlapsMetric
import de.fuberlin.wiwiss.silk.plugins.distance.spatial.TouchesMetric
import de.fuberlin.wiwiss.silk.plugins.distance.spatial.WithinMetric
import de.fuberlin.wiwiss.silk.plugins.distance.spatial.CrossesMetric
import de.fuberlin.wiwiss.silk.plugins.distance.spatial.DisjointMetric
import de.fuberlin.wiwiss.silk.plugins.distance.spatial.EqualsMetric
import de.fuberlin.wiwiss.silk.plugins.distance.spatial.IntersectsMetric
import de.fuberlin.wiwiss.silk.plugins.distance.spatial.OverlapsMetric
import de.fuberlin.wiwiss.silk.plugins.distance.spatial.TouchesMetric
import de.fuberlin.wiwiss.silk.plugins.distance.spatial.WithinMetric
import de.fuberlin.wiwiss.silk.plugins.distance.spatial.RelateMetric
import de.fuberlin.wiwiss.silk.plugins.transformer.spatial.PointsToCentroidTransformer
import de.fuberlin.wiwiss.silk.plugins.transformer.spatial.SimplifyTransformer
import de.fuberlin.wiwiss.silk.plugins.transformer.spatial.EnvelopeTransformer
import de.fuberlin.wiwiss.silk.plugins.transformer.spatial.AreaTransformer
import de.fuberlin.wiwiss.silk.plugins.transformer.spatial.BufferTransformer
import de.fuberlin.wiwiss.silk.plugins.distance.temporal.MillisecsDistanceMetric
import de.fuberlin.wiwiss.silk.plugins.distance.temporal.SecsDistanceMetric
import de.fuberlin.wiwiss.silk.plugins.distance.temporal.MinsDistanceMetric
import de.fuberlin.wiwiss.silk.plugins.distance.temporal.HoursDistanceMetric
import de.fuberlin.wiwiss.silk.plugins.distance.temporal.DaysDistanceMetric
import de.fuberlin.wiwiss.silk.plugins.distance.temporal.MonthsDistanceMetric
import de.fuberlin.wiwiss.silk.plugins.distance.temporal.YearsDistanceMetric
import de.fuberlin.wiwiss.silk.plugins.distance.temporal.BeforeMetric
import de.fuberlin.wiwiss.silk.plugins.distance.temporal.AfterMetric
import de.fuberlin.wiwiss.silk.plugins.distance.temporal.MeetsMetric
import de.fuberlin.wiwiss.silk.plugins.distance.temporal.IsMetByMetric
import de.fuberlin.wiwiss.silk.plugins.distance.temporal.OverlapsMetric
import de.fuberlin.wiwiss.silk.plugins.distance.temporal.IsOverlappedByMetric
import de.fuberlin.wiwiss.silk.plugins.distance.temporal.FinishesMetric
import de.fuberlin.wiwiss.silk.plugins.distance.temporal.IsFinishedByMetric
import de.fuberlin.wiwiss.silk.plugins.distance.temporal.ContainsMetric
import de.fuberlin.wiwiss.silk.plugins.distance.temporal.DuringMetric
import de.fuberlin.wiwiss.silk.plugins.distance.temporal.StartsMetric
import de.fuberlin.wiwiss.silk.plugins.distance.temporal.IsStartedByMetric
import de.fuberlin.wiwiss.silk.plugins.distance.temporal.EqualsMetric


/**
 * Registers all default plugins.
 */
object CorePlugins {

  private val logger = Logger.getLogger("CorePlugins")

  /**
   * Registers all default plugins.
   * For performance reasons, this is done manually instead of using automatic classpath lookup.
   */
  def register() {
    logger.log(Level.FINE, "Registering core plugins.")

    DatasetPlugin.register(classOf[CsvDataset])
    DatasetPlugin.register(classOf[XmlDataset])

    Transformer.register(classOf[ReplaceTransformer])
    Transformer.register(classOf[RegexReplaceTransformer])
    Transformer.register(classOf[ConcatTransformer])
    Transformer.register(classOf[RemoveBlanksTransformer])
    Transformer.register(classOf[LowerCaseTransformer])
    Transformer.register(classOf[UpperCaseTransformer])
    Transformer.register(classOf[CapitalizeTransformer])
    Transformer.register(classOf[StemmerTransformer])
    Transformer.register(classOf[StripPrefixTransformer])
    Transformer.register(classOf[StripPostfixTransformer])
    Transformer.register(classOf[StripUriPrefixTransformer])
    Transformer.register(classOf[AlphaReduceTransformer])
    Transformer.register(classOf[RemoveSpecialCharsTransformer])
    Transformer.register(classOf[ConvertCharsetTransformer])
    Transformer.register(classOf[RemoveValues])
    Transformer.register(classOf[RemoveEmptyValues])
    Transformer.register(classOf[RemoveParentheses])
    Transformer.register(classOf[Tokenizer])
    Transformer.register(classOf[ConcatMultipleValuesTransformer])
    Transformer.register(classOf[MergeTransformer])
    Transformer.register(classOf[SpotlightTextVectorTransformer])
    Transformer.register(classOf[CamelCaseTokenizer])
    Transformer.register(classOf[NormalizeCharsTransformer])
    Transformer.register(classOf[FilterByLength])
    Transformer.register(classOf[FilterByRegex])
    Transformer.register(classOf[UntilCharacterTransformer])
    Transformer.register(classOf[SubstringTransformer])
    Transformer.register(classOf[SoundexTransformer])
    Transformer.register(classOf[NysiisTransformer])
    Transformer.register(classOf[MetaphoneTransformer])
    Transformer.register(classOf[ConstantTransformer])
    // Numeric
    Transformer.register(classOf[NumReduceTransformer])
    Transformer.register(classOf[NumOperationTransformer])
    Transformer.register(classOf[LogarithmTransformer])
    Transformer.register(classOf[AggregateNumbersTransformer])
    Transformer.register(classOf[CompareNumbersTransformer])
    // Date
    Transformer.register(classOf[TimestampToDateTransformer])
    Transformer.register(classOf[DateToTimestampTransformer])
    Transformer.register(classOf[DurationTransformer])
    Transformer.register(classOf[DurationInSecondsTransformer])
    Transformer.register(classOf[DurationInDaysTransformer])
    Transformer.register(classOf[CompareDatesTransformer])
    //Spatial Transformers
    Transformer.register(classOf[GeometryTransformer])
    Transformer.register(classOf[PointsToCentroidTransformer])
    Transformer.register(classOf[SimplifyTransformer])
    Transformer.register(classOf[EnvelopeTransformer])
    Transformer.register(classOf[AreaTransformer])
    Transformer.register(classOf[BufferTransformer])

    
    DistanceMeasure.register(classOf[LevenshteinMetric])
    DistanceMeasure.register(classOf[LevenshteinDistance])
    DistanceMeasure.register(classOf[JaroDistanceMetric])
    DistanceMeasure.register(classOf[JaroWinklerDistance])
    DistanceMeasure.register(classOf[InsideNumericInterval])
    DistanceMeasure.register(classOf[QGramsMetric])
    DistanceMeasure.register(classOf[SubStringDistance])
    DistanceMeasure.register(classOf[EqualityMetric])
    DistanceMeasure.register(classOf[InequalityMetric])
    DistanceMeasure.register(classOf[LowerThanMetric])
    DistanceMeasure.register(classOf[NumMetric])
    DistanceMeasure.register(classOf[DateMetric])
    DistanceMeasure.register(classOf[DateTimeMetric])
    DistanceMeasure.register(classOf[GeographicDistanceMetric])
    DistanceMeasure.register(classOf[JaccardDistance])
    DistanceMeasure.register(classOf[DiceCoefficient])
    DistanceMeasure.register(classOf[SoftJaccardDistance])
    DistanceMeasure.register(classOf[TokenwiseStringDistance])
    DistanceMeasure.register(classOf[RelaxedEqualityMetric])
    DistanceMeasure.register(classOf[CosineDistanceMetric])
    DistanceMeasure.register(classOf[KoreanPhonemeDistance])
    DistanceMeasure.register(classOf[KoreanTranslitDistance])
    DistanceMeasure.register(classOf[CJKReadingDistance])
    DistanceMeasure.register(classOf[ConstantMetric])
    //Spatial and Temporal Distance Metrics
    DistanceMeasure.register(classOf[CentroidDistanceMetric])
    DistanceMeasure.register(classOf[MinDistanceMetric])
    DistanceMeasure.register(classOf[de.fuberlin.wiwiss.silk.plugins.distance.spatial.ContainsMetric])
    DistanceMeasure.register(classOf[CrossesMetric])
    DistanceMeasure.register(classOf[DisjointMetric])
    DistanceMeasure.register(classOf[de.fuberlin.wiwiss.silk.plugins.distance.spatial.EqualsMetric])
    DistanceMeasure.register(classOf[IntersectsMetric])
    DistanceMeasure.register(classOf[de.fuberlin.wiwiss.silk.plugins.distance.spatial.OverlapsMetric])
    DistanceMeasure.register(classOf[TouchesMetric])
    DistanceMeasure.register(classOf[WithinMetric])
    DistanceMeasure.register(classOf[RelateMetric])
    DistanceMeasure.register(classOf[MillisecsDistanceMetric])
    DistanceMeasure.register(classOf[SecsDistanceMetric])
    DistanceMeasure.register(classOf[MinsDistanceMetric])
    DistanceMeasure.register(classOf[HoursDistanceMetric])
    DistanceMeasure.register(classOf[DaysDistanceMetric])
    DistanceMeasure.register(classOf[MonthsDistanceMetric])
    DistanceMeasure.register(classOf[YearsDistanceMetric])
    DistanceMeasure.register(classOf[BeforeMetric])
    DistanceMeasure.register(classOf[AfterMetric])
    DistanceMeasure.register(classOf[MeetsMetric])
    DistanceMeasure.register(classOf[IsMetByMetric])
    DistanceMeasure.register(classOf[de.fuberlin.wiwiss.silk.plugins.distance.temporal.OverlapsMetric])
    DistanceMeasure.register(classOf[IsOverlappedByMetric])
    DistanceMeasure.register(classOf[FinishesMetric])
    DistanceMeasure.register(classOf[IsFinishedByMetric])
    DistanceMeasure.register(classOf[de.fuberlin.wiwiss.silk.plugins.distance.temporal.ContainsMetric])
    DistanceMeasure.register(classOf[DuringMetric])
    DistanceMeasure.register(classOf[StartsMetric])
    DistanceMeasure.register(classOf[IsStartedByMetric])
    DistanceMeasure.register(classOf[de.fuberlin.wiwiss.silk.plugins.distance.temporal.EqualsMetric])
    
    Aggregator.register(classOf[AverageAggregator])
    Aggregator.register(classOf[MaximumAggregator])
    Aggregator.register(classOf[MinimumAggregator])
    Aggregator.register(classOf[QuadraticMeanAggregator])
    Aggregator.register(classOf[GeometricMeanAggregator])
  }
}
