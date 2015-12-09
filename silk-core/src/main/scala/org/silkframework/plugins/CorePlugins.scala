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

package org.silkframework.plugins

import java.util.logging.Logger
import org.silkframework.plugins.aggegrator.{AverageAggregator, GeometricMeanAggregator, MaximumAggregator, MinimumAggregator, QuadraticMeanAggregator}
import org.silkframework.plugins.dataset.InternalDataset
import org.silkframework.plugins.distance.asian.{CJKReadingDistance, KoreanPhonemeDistance, KoreanTranslitDistance}
import org.silkframework.plugins.distance.characterbased._
import org.silkframework.plugins.distance.equality._
import org.silkframework.plugins.distance.numeric._
import org.silkframework.plugins.distance.tokenbased._
import org.silkframework.plugins.transformer.combine.{ConcatMultipleValuesTransformer, ConcatTransformer, MergeTransformer}
import org.silkframework.plugins.transformer.conditional.{IfContains, IfExists}
import org.silkframework.plugins.transformer.conversion.ConvertCharsetTransformer
import org.silkframework.plugins.transformer.date._
import org.silkframework.plugins.transformer.filter._
import org.silkframework.plugins.transformer.linguistic._
import org.silkframework.plugins.transformer.normalize._
import org.silkframework.plugins.transformer.numeric._
import org.silkframework.plugins.transformer.replace.{RegexReplaceTransformer, ReplaceTransformer}
import org.silkframework.plugins.transformer.substring._
import org.silkframework.plugins.transformer.tokenization.{CamelCaseTokenizer, Tokenizer}
import org.silkframework.plugins.transformer.value.{ConstantTransformer, RandomNumberTransformer}
import org.silkframework.runtime.plugin.PluginModule

/**
 * Registers all default plugins.
 */
class CorePlugins extends PluginModule {

  override def pluginClasses = datasets ++ transformers ++ measures ++ aggregators

  private def datasets = {
    classOf[InternalDataset] :: Nil
  }

  private def transformers =
    classOf[RemoveDuplicates] ::
    classOf[ReplaceTransformer] ::
    classOf[RegexReplaceTransformer] ::
    classOf[ConcatTransformer] ::
    classOf[RemoveBlanksTransformer] ::
    classOf[LowerCaseTransformer] ::
    classOf[UpperCaseTransformer] ::
    classOf[CapitalizeTransformer] ::
    classOf[StemmerTransformer] ::
    classOf[StripPrefixTransformer] ::
    classOf[StripPostfixTransformer] ::
    classOf[StripUriPrefixTransformer] ::
    classOf[AlphaReduceTransformer] ::
    classOf[RemoveSpecialCharsTransformer] ::
    classOf[ConvertCharsetTransformer] ::
    classOf[RemoveValues] ::
    classOf[RemoveStopwords] ::
    classOf[RemoveEmptyValues] ::
    classOf[RemoveParentheses] ::
    classOf[TrimTransformer] ::
    classOf[Tokenizer] ::
    classOf[ConcatMultipleValuesTransformer] ::
    classOf[MergeTransformer] ::
    classOf[SpotlightTextVectorTransformer] ::
    classOf[CamelCaseTokenizer] ::
    classOf[NormalizeCharsTransformer] ::
    classOf[FilterByLength] ::
    classOf[FilterByRegex] ::
    classOf[UntilCharacterTransformer] ::
    classOf[SubstringTransformer] ::
    classOf[SoundexTransformer] ::
    classOf[NysiisTransformer] ::
    classOf[MetaphoneTransformer] ::
    classOf[ConstantTransformer] ::
    classOf[RandomNumberTransformer] ::
    // Conditional
    classOf[IfContains] ::
    classOf[IfExists] ::
    // Numeric
    classOf[NumReduceTransformer] ::
    classOf[NumOperationTransformer] ::
    classOf[LogarithmTransformer] ::
    classOf[AggregateNumbersTransformer] ::
    classOf[CompareNumbersTransformer] ::
    classOf[CountTransformer] ::
    // Date
    classOf[TimestampToDateTransformer] ::
    classOf[DateToTimestampTransformer] ::
    classOf[DurationTransformer] ::
    classOf[DurationInSecondsTransformer] ::
    classOf[DurationInDaysTransformer] ::
    classOf[CompareDatesTransformer] ::
    classOf[NumberToDurationTransformer] ::
    classOf[ParseDateTransformer] :: Nil

  private def measures =
    classOf[LevenshteinMetric] ::
    classOf[LevenshteinDistance] ::
    classOf[JaroDistanceMetric] ::
    classOf[JaroWinklerDistance] ::
    classOf[InsideNumericInterval] ::
    classOf[QGramsMetric] ::
    classOf[SubStringDistance] ::
    classOf[EqualityMetric] ::
    classOf[InequalityMetric] ::
    classOf[LowerThanMetric] ::
    classOf[NumMetric] ::
    classOf[DateMetric] ::
    classOf[DateTimeMetric] ::
    classOf[GeographicDistanceMetric] ::
    classOf[JaccardDistance] ::
    classOf[DiceCoefficient] ::
    classOf[SoftJaccardDistance] ::
    classOf[TokenwiseStringDistance] ::
    classOf[RelaxedEqualityMetric] ::
    classOf[CosineDistanceMetric] ::
    classOf[KoreanPhonemeDistance] ::
    classOf[KoreanTranslitDistance] ::
    classOf[CJKReadingDistance] ::
    classOf[ConstantMetric] :: Nil

  private def aggregators =
    classOf[AverageAggregator] ::
    classOf[MaximumAggregator] ::
    classOf[MinimumAggregator] ::
    classOf[QuadraticMeanAggregator] ::
    classOf[GeometricMeanAggregator] :: Nil
}
