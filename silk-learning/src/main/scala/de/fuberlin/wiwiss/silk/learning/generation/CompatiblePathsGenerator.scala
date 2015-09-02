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

package de.fuberlin.wiwiss.silk.learning.generation

import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.learning.individual.FunctionNode
import de.fuberlin.wiwiss.silk.rule.similarity.DistanceMeasure
import de.fuberlin.wiwiss.silk.learning.LearningConfiguration.Components
import de.fuberlin.wiwiss.silk.evaluation.ReferenceEntities
import de.fuberlin.wiwiss.silk.entity.{Entity, Path}
import de.fuberlin.wiwiss.silk.plugins.transformer.substring.StripUriPrefixTransformer
import de.fuberlin.wiwiss.silk.plugins.transformer.tokenization.Tokenizer
import de.fuberlin.wiwiss.silk.plugins.transformer.normalize.LowerCaseTransformer

/**
 * Analyses the reference entities and generates pairs of paths.
 */
class CompatiblePathsGenerator(components: Components) {
  
  private val minFrequency = 0.01
  
  def apply(entities: ReferenceEntities, seed: Boolean): Traversable[ComparisonGenerator] = {
    if(entities.positive.isEmpty) {
      Traversable.empty
    } else {
      //Get all paths except sameAs paths
      val paths = PathsRetriever(entities)

      if(seed) {
        //Remove paths which hold the same values (e.g. rdfs:label and drugbank:drugName)
        val distinctPaths = DuplicateRemover(paths, entities)
        //Return all path pairs
        val pathPairs = PairGenerator(distinctPaths, entities)

        //pathPairs.foreach(p => printLink(p, instances))
        //pathPairs.foreach(println)

        pathPairs.flatMap(createGenerators)
      }
      else {
        for(s <- paths.source;
            t <- paths.target;
            generator <- createGenerators(DPair(s, t)))
          yield generator
      }
    }
  }

//TODO remove
//  private def printLink(pathPair: DPair[Path], instances: ReferenceEntities) {
//    println("-------------------------------------")
//    println(pathPair.mkString(" - "))
//    println("P")
//    for(instancePair <- instances.positive.values) {
//      println(instancePair.source.evaluate(pathPair.source).mkString(", ") + "\n---\n" + instancePair.target.evaluate(pathPair.target).mkString(", "))
//    }
//    println("N")
//    for(instancePair <- instances.negative.values) {
//      println(instancePair.source.evaluate(pathPair.source).mkString(", ") + "\n---\n" + instancePair.target.evaluate(pathPair.target).mkString(", "))
//    }
//  }

  private def createGenerators(pathPair: DPair[Path]) = {
    new ComparisonGenerator(InputGenerator.fromPathPair(pathPair, components.transformations), FunctionNode("levenshteinDistance", Nil, DistanceMeasure), 2.0) ::
    new ComparisonGenerator(InputGenerator.fromPathPair(pathPair, components.transformations), FunctionNode("jaccard", Nil, DistanceMeasure), 1.0) ::
    new ComparisonGenerator(InputGenerator.fromPathPair(pathPair, components.transformations), FunctionNode("date", Nil, DistanceMeasure), 1000.0) :: Nil
  }

  /**
   * Retrieves all paths except sameAs paths
   */
  private object PathsRetriever {
    def apply(entities: ReferenceEntities) = {
      val pair = entities.positive.values.head
      val allPaths = pair.map(e => Path(e.desc.variable, Nil) +: e.desc.paths)
      allPaths.map(_.filterNot(_.toString.contains("sameAs"))).map(_.filterNot(_.toString.contains("abstract"))).map(_.filterNot(_.toString.contains("comment")))
    }
  }

  /**
   * Removes paths which hold the same values (e.g. rdfs:label and drugbank:drugName)
   */
  private object DuplicateRemover {
    def apply(paths: DPair[Traversable[Path]], entities: ReferenceEntities) = {
      val sourceValues = entities.positive.values.map(_.source) ++ entities.negative.values.map(_.source)
      val targetValues = entities.positive.values.map(_.target) ++ entities.negative.values.map(_.target)
      
      DPair(
        source = removeDuplicatePaths(sourceValues, paths.source),
        target = removeDuplicatePaths(targetValues, paths.target)
      )
    }

    private def removeDuplicatePaths(entities: Traversable[Entity], paths: Traversable[Path]): Traversable[Path] = {
      val pathTails = paths.toList.tails.toTraversable.par
      val distinctPaths = for(path :: tail <- pathTails if !tail.exists(p => pathsMatch(entities, DPair(path, p)))) yield path
      distinctPaths.seq
    }

    private def pathsMatch(entities: Traversable[Entity], pathPair: DPair[Path]): Boolean = {
      entities.forall(pathsMatch(_, pathPair))
    }

    private def pathsMatch(entities: Entity, pathPair: DPair[Path]): Boolean = {
      val values = pathPair.map(entities.evaluate)

      val valuePairs = for(v1 <- values.source; v2 <- values.target) yield DPair(v1, v2)

      valuePairs.exists(p => p.source.toLowerCase == p.target.toLowerCase)
    }
  }

  /**
   * Generates all pair of paths with overlapping values.
   */
  private object PairGenerator {
    private val transformers = Tokenizer() :: StripUriPrefixTransformer() :: LowerCaseTransformer() :: Nil
    
    def apply(paths: DPair[Traversable[Path]], entities: ReferenceEntities) = {
      val pathPairs = for(sourcePath <- paths.source; targetPath <- paths.target) yield DPair(sourcePath, targetPath)
      val posEntities = entities.positive.values.map(transformEntities)
      val negEntities = entities.negative.values.map(transformEntities)

      pathPairs.par.filter(pathValuesMatch(posEntities, negEntities, _)).seq
    }
    
    @inline private def transformEntities(entities: DPair[Entity]) = {
      for(entity <- entities) yield {
        new Entity(
          uri = transformValues(Set(entity.uri)).head,
          values = for(values <- entity.values) yield transformValues(values),
          desc = entity.desc
        )
      }
    }

    @inline private def transformValues(values: Set[String]) = {
      transformers.foldLeft(values)((v, trans) => trans(Seq(v)))
    }

    private def pathValuesMatch(posEntities: Traversable[DPair[Entity]], negEntities: Traversable[DPair[Entity]], pathPair: DPair[Path]): Boolean = {
      val positive = posEntities.count(i => matches(i, pathPair)).toDouble / posEntities.size

//      if(!negEntities.isEmpty) {
//        val negative = negEntities.count(i => matches(i, pathPair)).toDouble / negEntities.size
//        positive > minFrequency && positive * 2.0 >= negative
//      }
//      else {
        positive > minFrequency
//      }
    }

    private def matches(entityPair: DPair[Entity], pathPair: DPair[Path]): Boolean = {
      val sourceValues = entityPair.source.evaluate(pathPair.source)
      val targetValues = entityPair.target.evaluate(pathPair.target)

      sourceValues.exists(targetValues)
    }
  }
}