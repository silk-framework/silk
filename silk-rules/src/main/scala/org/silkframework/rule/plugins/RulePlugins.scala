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

package org.silkframework.rule.plugins

import org.silkframework.rule.LinkSpec.LinkSpecificationFormat
import org.silkframework.rule.MappingRules.MappingRulesFormat
import org.silkframework.rule.RootMappingRule.RootMappingRuleFormat
import org.silkframework.rule.TransformRule.TransformRuleFormat
import org.silkframework.rule.TransformSpec.{TransformSpecFormat, TransformTaskXmlFormat}
import org.silkframework.rule.plugins.aggegrator._
import org.silkframework.rule.plugins.distance.characterbased._
import org.silkframework.rule.plugins.distance.equality._
import org.silkframework.rule.plugins.distance.numeric._
import org.silkframework.rule.plugins.distance.tokenbased._
import org.silkframework.rule.plugins.transformer.combine.{ConcatMultipleValuesTransformer, ConcatTransformer, MergeTransformer}
import org.silkframework.rule.plugins.transformer.conditional._
import org.silkframework.rule.plugins.transformer.conversion.ConvertCharsetTransformer
import org.silkframework.rule.plugins.transformer.date._
import org.silkframework.rule.plugins.transformer.extraction.RegexExtractionTransformer
import org.silkframework.rule.plugins.transformer.filter._
import org.silkframework.rule.plugins.transformer.linguistic._
import org.silkframework.rule.plugins.transformer.normalize._
import org.silkframework.rule.plugins.transformer.numeric._
import org.silkframework.rule.plugins.transformer.replace.{MapTransformer, MapTransformerWithDefaultInput, RegexReplaceTransformer, ReplaceTransformer}
import org.silkframework.rule.plugins.transformer.selection.RegexSelectTransformer
import org.silkframework.rule.plugins.transformer.sequence.{GetValueByIndexTransformer, ValuesToIndexesTransformer}
import org.silkframework.rule.plugins.transformer.substring._
import org.silkframework.rule.plugins.transformer.tokenization.{CamelCaseTokenizer, Tokenizer}
import org.silkframework.rule.plugins.transformer.validation._
import org.silkframework.rule.plugins.transformer.value._
import org.silkframework.runtime.plugin.PluginModule

import scala.language.existentials

/**
  * Registers all default plugins.
  */
class RulePlugins extends PluginModule {

  override def pluginClasses = transformers ++ measures ++ aggregators ++ serializers

  private def transformers =
    classOf[RemoveDuplicates] ::
        classOf[ReplaceTransformer] ::
        classOf[RegexReplaceTransformer] ::
        classOf[RegexExtractionTransformer] ::
        classOf[MapTransformer] ::
        classOf[MapTransformerWithDefaultInput] ::
        classOf[ConcatTransformer] ::
        classOf[RemoveBlanksTransformer] ::
        classOf[LowerCaseTransformer] ::
        classOf[UpperCaseTransformer] ::
        classOf[CapitalizeTransformer] ::
        classOf[UrlEncodeTransformer] ::
        classOf[StemmerTransformer] ::
        classOf[StripPrefixTransformer] ::
        classOf[StripPostfixTransformer] ::
        classOf[StripUriPrefixTransformer] ::
        classOf[UriFixTransformer] ::
        classOf[AlphaReduceTransformer] ::
        classOf[RemoveSpecialCharsTransformer] ::
        classOf[ConvertCharsetTransformer] ::
        classOf[RemoveValues] ::
        classOf[RemoveRemoteStopwords] ::
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
        classOf[ConstantUriTransformer] ::
        classOf[RandomNumberTransformer] ::
        classOf[EmptyValueTransformer] ::
        // Conditional
        classOf[IfContains] ::
        classOf[IfExists] ::
        classOf[IfMatchesRegexTransformer] ::
        classOf[ContainsAllOf] ::
        classOf[ContainsAnyOf] ::
        classOf[Negate] ::
        // Numeric
        classOf[NumReduceTransformer] ::
        classOf[NumOperationTransformer] ::
        classOf[LogarithmTransformer] ::
        classOf[AggregateNumbersTransformer] ::
        classOf[CompareNumbersTransformer] ::
        classOf[CountTransformer] ::
        classOf[PhysicalQuantityExtractor] ::
        classOf[FormatNumber] ::
        // Date
        classOf[TimestampToDateTransformer] ::
        classOf[DateToTimestampTransformer] ::
        classOf[DurationTransformer] ::
        classOf[DurationInSecondsTransformer] ::
        classOf[DurationInDaysTransformer] ::
        classOf[DurationInYearsTransformer] ::
        classOf[CompareDatesTransformer] ::
        classOf[NumberToDurationTransformer] ::
        classOf[ParseDateTransformer] ::
        classOf[CurrentDateTransformer] ::
        // Validation
        classOf[ValidateDateRange] ::
        classOf[ValidateNumericRange] ::
        classOf[ValidateDateAfter] ::
        classOf[ValidateRegex] ::
        classOf[ValidateNumberOValues] ::
        // Sequence
        classOf[GetValueByIndexTransformer] ::
        classOf[ValuesToIndexesTransformer] ::
        // Selection
        classOf[RegexSelectTransformer] ::
        Nil

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
        classOf[GreaterThanMetric] ::
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
        classOf[ConstantMetric] ::
        classOf[StartsWithDistance] ::
        classOf[IsSubstringDistance] ::
        classOf[NumericEqualityMetric] ::
        Nil

  private def aggregators =
    classOf[AverageAggregator] ::
        classOf[MaximumAggregator] ::
        classOf[MinimumAggregator] ::
        classOf[QuadraticMeanAggregator] ::
        classOf[GeometricMeanAggregator] ::
      classOf[NegationAggregator] :: Nil

  private def serializers =
    TransformSpecFormat.getClass ::
    TransformTaskXmlFormat.getClass ::
    TransformRuleFormat.getClass ::
    MappingRulesFormat.getClass ::
    RootMappingRuleFormat.getClass ::
    LinkSpecificationFormat.getClass ::
    Nil
}
