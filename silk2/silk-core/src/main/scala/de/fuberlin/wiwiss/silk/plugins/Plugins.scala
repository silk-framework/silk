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

import writer._
import distance._
import characterbased._
import de.fuberlin.wiwiss.silk.datasource.DataSource
import de.fuberlin.wiwiss.silk.linkagerule.input.Transformer
import de.fuberlin.wiwiss.silk.linkagerule.similarity.{Aggregator, DistanceMeasure}
import de.fuberlin.wiwiss.silk.output.{DataWriter, Formatter}
import equality._
import java.io.File
import de.fuberlin.wiwiss.silk.util.Timer
import java.util.logging.Logger
import tokenbased._
import de.fuberlin.wiwiss.silk.plugins.transformer.numeric.NumOperationTransformer
import de.fuberlin.wiwiss.silk.plugins.transformer.substring._
import de.fuberlin.wiwiss.silk.plugins.transformer.tokenization.CamelCaseTokenizer
import de.fuberlin.wiwiss.silk.plugins.transformer.linguistic._
import de.fuberlin.wiwiss.silk.plugins.transformer.normalize._
import de.fuberlin.wiwiss.silk.plugins.transformer.replace.{ReplaceTransformer, RegexReplaceTransformer}
import de.fuberlin.wiwiss.silk.plugins.writer.FileWriter
import de.fuberlin.wiwiss.silk.plugins.distance.characterbased.LevenshteinMetric
import de.fuberlin.wiwiss.silk.plugins.transformer.substring.StripPrefixTransformer
import de.fuberlin.wiwiss.silk.plugins.distance.tokenbased.JaccardDistance
import de.fuberlin.wiwiss.silk.plugins.transformer.normalize.RemoveBlanksTransformer
import de.fuberlin.wiwiss.silk.plugins.aggegrator.QuadraticMeanAggregator
import de.fuberlin.wiwiss.silk.plugins.aggegrator.AverageAggregator
import de.fuberlin.wiwiss.silk.plugins.distance.numeric.GeographicDistanceMetric
import de.fuberlin.wiwiss.silk.plugins.transformer.combine.ConcatMultipleValuesTransformer
import de.fuberlin.wiwiss.silk.plugins.distance.characterbased.QGramsMetric
import de.fuberlin.wiwiss.silk.plugins.aggegrator.MinimumAggregator
import de.fuberlin.wiwiss.silk.plugins.transformer.substring.SubstringTransformer
import de.fuberlin.wiwiss.silk.plugins.transformer.filter.FilterByLength
import de.fuberlin.wiwiss.silk.plugins.transformer.linguistic.NysiisTransformer
import de.fuberlin.wiwiss.silk.plugins.transformer.normalize.LowerCaseTransformer
import de.fuberlin.wiwiss.silk.plugins.transformer.conversion.ConvertCharsetTransformer
import de.fuberlin.wiwiss.silk.plugins.distance.tokenbased.DiceCoefficient
import de.fuberlin.wiwiss.silk.plugins.transformer.filter.RemoveValues
import de.fuberlin.wiwiss.silk.plugins.transformer.combine.MergeTransformer
import de.fuberlin.wiwiss.silk.plugins.datasource.SparqlDataSource
import de.fuberlin.wiwiss.silk.plugins.distance.characterbased.JaroWinklerDistance
import de.fuberlin.wiwiss.silk.plugins.distance.numeric.NumMetric
import de.fuberlin.wiwiss.silk.plugins.transformer.numeric.LogarithmTransformer
import de.fuberlin.wiwiss.silk.plugins.transformer.linguistic.StemmerTransformer
import de.fuberlin.wiwiss.silk.plugins.transformer.linguistic.MetaphoneTransformer
import de.fuberlin.wiwiss.silk.plugins.transformer.normalize.UpperCaseTransformer
import de.fuberlin.wiwiss.silk.plugins.distance.asian.CJKReadingDistance
import de.fuberlin.wiwiss.silk.plugins.transformer.filter.FilterByRegex
import de.fuberlin.wiwiss.silk.plugins.transformer.normalize.CapitalizeTransformer
import de.fuberlin.wiwiss.silk.plugins.writer.SparqlWriter
import de.fuberlin.wiwiss.silk.plugins.distance.numeric.DateTimeMetric
import de.fuberlin.wiwiss.silk.plugins.distance.characterbased.JaroDistanceMetric
import de.fuberlin.wiwiss.silk.plugins.transformer.normalize.RemoveParentheses
import de.fuberlin.wiwiss.silk.plugins.transformer.combine.ConcatTransformer
import de.fuberlin.wiwiss.silk.plugins.transformer.normalize.AlphaReduceTransformer
import de.fuberlin.wiwiss.silk.plugins.transformer.filter.RemoveEmptyValues
import de.fuberlin.wiwiss.silk.plugins.aggegrator.GeometricMeanAggregator
import de.fuberlin.wiwiss.silk.plugins.distance.equality.EqualityMetric
import de.fuberlin.wiwiss.silk.plugins.transformer.tokenization.Tokenizer
import de.fuberlin.wiwiss.silk.plugins.transformer.numeric.NumReduceTransformer
import de.fuberlin.wiwiss.silk.plugins.transformer.substring.StripPostfixTransformer
import de.fuberlin.wiwiss.silk.plugins.datasource.CsvDataSource
import de.fuberlin.wiwiss.silk.plugins.distance.characterbased.LevenshteinDistance
import de.fuberlin.wiwiss.silk.plugins.distance.equality.InequalityMetric
import de.fuberlin.wiwiss.silk.plugins.distance.asian.KoreanTranslitDistance
import de.fuberlin.wiwiss.silk.plugins.distance.tokenbased.TokenwiseStringDistance
import de.fuberlin.wiwiss.silk.plugins.transformer.linguistic.SoundexTransformer
import de.fuberlin.wiwiss.silk.plugins.distance.numeric.DateMetric
import de.fuberlin.wiwiss.silk.plugins.distance.equality.LowerThanMetric
import de.fuberlin.wiwiss.silk.plugins.transformer.substring.StripUriPrefixTransformer
import de.fuberlin.wiwiss.silk.plugins.distance.asian.KoreanPhonemeDistance
import de.fuberlin.wiwiss.silk.plugins.distance.tokenbased.SoftJaccardDistance
import de.fuberlin.wiwiss.silk.plugins.aggegrator.MaximumAggregator
import de.fuberlin.wiwiss.silk.plugins.transformer.date.{DurationInSecondsTransformer, DurationTransformer, DurationInDaysTransformer, TimestampToDateTransformer}
import de.fuberlin.wiwiss.silk.plugins.transformer.spatial.GeometryTransformer
import de.fuberlin.wiwiss.silk.plugins.distance.spatial.CentroidDistanceMetric
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
import de.fuberlin.wiwiss.silk.plugins.transformer.spatial.PointsToCentroidTransformer
import de.fuberlin.wiwiss.silk.plugins.transformer.spatial.SimplifyTransformer
import de.fuberlin.wiwiss.silk.plugins.transformer.spatial.EnvelopeTransformer
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
import de.fuberlin.wiwiss.silk.plugins.transformer.spatial.AreaTransformer


/**
 * Registers all default plugins as well as external plugins found in the provided directory.
 */
object Plugins {
  /** Indicates if register() has already been called */
  private var registered = false

  private implicit val logger = Logger.getLogger(Plugins.getClass.getName)

  /**
   * Registers all default plugins as well as external plugins found in the provided directory.
   */
  def register(pluginsDir: File = new File(System.getProperty("user.home") + "/.silk/plugins/")): Unit = synchronized {
    if(!registered) {
      registerDefaultPlugins()
      registerExternalPlugins(pluginsDir)
      registered = true
    }
  }

  /**
   * Registers all default plugins.
   * For performance reasons, this is done manually instead of using automatic classpath lookup.
   */
  private def registerDefaultPlugins() {
    DataSource.register(classOf[SparqlDataSource])
    DataSource.register(classOf[CsvDataSource])
    //DataSource.register(classOf[CacheDataSource])

    Transformer.register(classOf[ReplaceTransformer])
    Transformer.register(classOf[RegexReplaceTransformer])
    Transformer.register(classOf[ConcatTransformer])
    Transformer.register(classOf[RemoveBlanksTransformer])
    Transformer.register(classOf[LowerCaseTransformer])
    Transformer.register(classOf[UpperCaseTransformer])
    Transformer.register(classOf[CapitalizeTransformer])
    Transformer.register(classOf[NumReduceTransformer])
    Transformer.register(classOf[StemmerTransformer])
    Transformer.register(classOf[StripPrefixTransformer])
    Transformer.register(classOf[StripPostfixTransformer])
    Transformer.register(classOf[StripUriPrefixTransformer])
    Transformer.register(classOf[AlphaReduceTransformer])
    Transformer.register(classOf[RemoveSpecialCharsTransformer])
    Transformer.register(classOf[LogarithmTransformer])
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
    Transformer.register(classOf[NumOperationTransformer])
    Transformer.register(classOf[UntilCharacterTransformer])
    Transformer.register(classOf[SubstringTransformer])
    Transformer.register(classOf[SoundexTransformer])
    Transformer.register(classOf[NysiisTransformer])
    Transformer.register(classOf[MetaphoneTransformer])
    // Date
    Transformer.register(classOf[TimestampToDateTransformer])
    Transformer.register(classOf[DurationTransformer])
    Transformer.register(classOf[DurationInSecondsTransformer])
    Transformer.register(classOf[DurationInDaysTransformer])
    Transformer.register(classOf[GeometryTransformer])
    Transformer.register(classOf[PointsToCentroidTransformer])
    Transformer.register(classOf[SimplifyTransformer])
    Transformer.register(classOf[EnvelopeTransformer])
    Transformer.register(classOf[AreaTransformer])

    
    DistanceMeasure.register(classOf[LevenshteinMetric])
    DistanceMeasure.register(classOf[LevenshteinDistance])
    DistanceMeasure.register(classOf[JaroDistanceMetric])
    DistanceMeasure.register(classOf[JaroWinklerDistance])
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
    DistanceMeasure.register(classOf[spatial.CentroidDistanceMetric])
    DistanceMeasure.register(classOf[spatial.ContainsMetric])
    DistanceMeasure.register(classOf[spatial.CrossesMetric])
    DistanceMeasure.register(classOf[spatial.DisjointMetric])
    DistanceMeasure.register(classOf[spatial.EqualsMetric])
    DistanceMeasure.register(classOf[spatial.IntersectsMetric])
    DistanceMeasure.register(classOf[spatial.OverlapsMetric])
    DistanceMeasure.register(classOf[spatial.TouchesMetric])
    DistanceMeasure.register(classOf[spatial.WithinMetric])
    DistanceMeasure.register(classOf[temporal.MillisecsDistanceMetric])
    DistanceMeasure.register(classOf[temporal.SecsDistanceMetric])
    DistanceMeasure.register(classOf[temporal.MinsDistanceMetric])
    DistanceMeasure.register(classOf[temporal.HoursDistanceMetric])
    DistanceMeasure.register(classOf[temporal.DaysDistanceMetric])
    DistanceMeasure.register(classOf[temporal.MonthsDistanceMetric])
    DistanceMeasure.register(classOf[temporal.YearsDistanceMetric])
    DistanceMeasure.register(classOf[temporal.BeforeMetric])
    DistanceMeasure.register(classOf[temporal.AfterMetric])
    DistanceMeasure.register(classOf[temporal.MeetsMetric])
    DistanceMeasure.register(classOf[temporal.IsMetByMetric])
    DistanceMeasure.register(classOf[temporal.OverlapsMetric])
    DistanceMeasure.register(classOf[temporal.IsOverlappedByMetric])
    DistanceMeasure.register(classOf[temporal.FinishesMetric])
    DistanceMeasure.register(classOf[temporal.IsFinishedByMetric])
    DistanceMeasure.register(classOf[temporal.ContainsMetric])
    DistanceMeasure.register(classOf[temporal.DuringMetric])
    DistanceMeasure.register(classOf[temporal.StartsMetric])
    DistanceMeasure.register(classOf[temporal.IsStartedByMetric])
    DistanceMeasure.register(classOf[temporal.EqualsMetric])
    
    Aggregator.register(classOf[AverageAggregator])
    Aggregator.register(classOf[MaximumAggregator])
    Aggregator.register(classOf[MinimumAggregator])
    Aggregator.register(classOf[QuadraticMeanAggregator])
    Aggregator.register(classOf[GeometricMeanAggregator])

    DataWriter.register(classOf[FileWriter])
    DataWriter.register(classOf[SparqlWriter])
    //LinkWriter.register(classOf[MemoryWriter])

    Formatter.register(classOf[NTriplesFormatter])
    Formatter.register(classOf[AlignmentFormatter])
  }

  /**
   * Registers external plugins.
   */
  private def registerExternalPlugins(pluginsDir: File) {
    Timer("Registering external plugins") {
      if(pluginsDir.isDirectory) {
        DataSource.registerJars(pluginsDir)
        Transformer.registerJars(pluginsDir)
        DistanceMeasure.registerJars(pluginsDir)
        Aggregator.registerJars(pluginsDir)
      }
      else {
       logger.info("No plugins loaded because the plugin directory " + pluginsDir + " has not been found.")
      }
    }
  }
}
