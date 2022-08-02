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

package org.silkframework.learning.generation

import org.silkframework.config.Prefixes
import org.silkframework.entity.Entity
import org.silkframework.entity.paths.{TypedPath, UntypedPath}
import org.silkframework.learning.LearningConfiguration.Components
import org.silkframework.learning.individual.FunctionNode
import org.silkframework.rule.evaluation.ReferenceEntities
import org.silkframework.rule.plugins.transformer.normalize.LowerCaseTransformer
import org.silkframework.rule.plugins.transformer.substring.StripUriPrefixTransformer
import org.silkframework.rule.plugins.transformer.tokenization.Tokenizer
import org.silkframework.rule.similarity.DistanceMeasure
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.resource.ResourceManager
import org.silkframework.util.DPair

/**
  * Analyses the reference entities and generates pairs of paths.
  */
class CompatiblePathsGenerator(components: Components)
                              (implicit pluginContext: PluginContext){

  private val minFrequency = 0.01

  def apply(entities: ReferenceEntities, seed: Boolean): Traversable[ComparisonGenerator] = {
    if (entities.positiveLinks.isEmpty) {
      Traversable.empty
    } else {
      //Get all paths except sameAs paths
      val paths = PathsRetriever(entities)

      if (seed) {
        //Remove paths which hold the same values (e.g. rdfs:label and drugbank:drugName)
        val distinctPaths = DuplicateRemover(paths, entities)
        //Return all path pairs
        val pathPairs = PairGenerator(distinctPaths, entities).map(_.map(_.toUntypedPath))

        pathPairs.flatMap(createGenerators)
      }
      else {
        for (s <- paths.source;
             t <- paths.target;
             generator <- createGenerators(DPair(s.toUntypedPath, t.toUntypedPath)))
          yield generator
      }
    }
  }

  private def createGenerators(pathPair: DPair[UntypedPath]): Seq[ComparisonGenerator] = {
    ComparisonGenerator(InputGenerator.fromPathPair(pathPair, components.transformations), FunctionNode("levenshteinDistance", Nil, DistanceMeasure), 3.0) ::
    ComparisonGenerator(InputGenerator.fromPathPair(pathPair, components.transformations), FunctionNode("jaccard", Nil, DistanceMeasure), 1.0) ::
    // Substring is currently too slow ComparisonGenerator(InputGenerator.fromPathPair(pathPair, components.transformations), FunctionNode("substring", Nil, DistanceMeasure), 0.6) ::
    ComparisonGenerator(InputGenerator.fromPathPair(pathPair, components.transformations), FunctionNode("date", Nil, DistanceMeasure), 1000.0) :: Nil
  }

  /**
    * Retrieves all paths except sameAs paths
    */
  private object PathsRetriever {
    def apply(entities: ReferenceEntities): DPair[Traversable[TypedPath]] = {
      val pair = entities.positiveEntities.head
      val allPaths = pair.map(e => e.schema.typedPaths)
      allPaths.
          map(_.filterNot(_.toString.contains("sameAs"))).
          map(_.filterNot(_.toString.contains("abstract"))).
          map(_.filterNot(_.toString.contains("comment")))
    }
  }

  /**
    * Removes paths which hold the same values (e.g. rdfs:label and drugbank:drugName)
    */
  private object DuplicateRemover {
    def apply(paths: DPair[Traversable[TypedPath]], entities: ReferenceEntities): DPair[Traversable[TypedPath]] = {
      val sourceValues = entities.positiveEntities.map(_.source) ++ entities.negativeEntities.map(_.source)
      val targetValues = entities.positiveEntities.map(_.target) ++ entities.negativeEntities.map(_.target)

      DPair(
        source = removeDuplicatePaths(sourceValues, paths.source),
        target = removeDuplicatePaths(targetValues, paths.target)
      )
    }

    private def removeDuplicatePaths(entities: Traversable[Entity], paths: Traversable[TypedPath]): Traversable[TypedPath] = {
      val pathTails = paths.toList.tails.toTraversable.par
      val distinctPaths = for (path :: tail <- pathTails if !tail.exists(p => pathsMatch(entities, DPair(path, p)))) yield path
      distinctPaths.seq
    }

    private def pathsMatch(entities: Traversable[Entity], pathPair: DPair[TypedPath]): Boolean = {
      entities.forall(pathsMatch(_, pathPair))
    }

    private def pathsMatch(entities: Entity, pathPair: DPair[TypedPath]): Boolean = {
      val values = pathPair.map(entities.evaluate)

      val valuePairs = for (v1 <- values.source; v2 <- values.target) yield DPair(v1, v2)

      valuePairs.exists(p => p.source.toLowerCase == p.target.toLowerCase)
    }
  }

  /**
    * Generates all pair of paths with overlapping values.
    */
  private object PairGenerator {
    private val transformers = Tokenizer() :: StripUriPrefixTransformer() :: LowerCaseTransformer() :: Nil

    def apply(paths: DPair[Traversable[TypedPath]], entities: ReferenceEntities): Iterable[DPair[TypedPath]] = {
      val pathPairs = for (sourcePath <- paths.source; targetPath <- paths.target) yield DPair(sourcePath, targetPath)
      val posEntities = entities.positiveEntities.map(transformEntities)
      val negEntities = entities.negativeEntities.map(transformEntities)

      pathPairs.par.filter(pathValuesMatch(posEntities, negEntities, _)).seq
    }

    @inline private def transformEntities(entities: DPair[Entity]) = {
      for (entity <- entities) yield {
        Entity(
          uri = transformValues(Seq(entity.uri)).head,
          values = for (values <- entity.values) yield transformValues(values),
          schema = entity.schema
        )
      }
    }

    @inline private def transformValues(values: Seq[String]) = {
      transformers.foldLeft(values)((v, trans) => trans(Seq(v)))
    }

    private def pathValuesMatch(posEntities: Traversable[DPair[Entity]], negEntities: Traversable[DPair[Entity]], pathPair: DPair[TypedPath]): Boolean = {
      val positive = posEntities.count(i => matches(i, pathPair)).toDouble / posEntities.size

      positive > minFrequency
    }

    private def matches(entityPair: DPair[Entity], pathPair: DPair[TypedPath]): Boolean = {
      val sourceValues = entityPair.source.evaluate(pathPair.source)
      val targetValues = entityPair.target.evaluate(pathPair.target).toSet

      sourceValues.exists(targetValues)
    }
  }

}