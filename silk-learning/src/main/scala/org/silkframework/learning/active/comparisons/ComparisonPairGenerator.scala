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

package org.silkframework.learning.active.comparisons

import org.silkframework.config.Prefixes
import org.silkframework.dataset.DataSource
import org.silkframework.entity.ValueType
import org.silkframework.entity.paths.{TypedPath, UntypedPath}
import org.silkframework.learning.active.LinkCandidate
import org.silkframework.learning.{LearningConfiguration, LearningException}
import org.silkframework.rule.LinkSpec
import org.silkframework.rule.input.{Input, PathInput, TransformInput}
import org.silkframework.rule.similarity.{Aggregation, Comparison, SimilarityOperator}
import org.silkframework.runtime.activity.{Activity, ActivityContext, UserContext}
import org.silkframework.util.{DPair, Timer}
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.linking.LinkingPathsCache
import org.silkframework.workspace.activity.linking.LinkingTaskUtils._

import scala.collection.mutable
import scala.util.Random

/**
  * Generates comparison pairs for the current link specification.
  */
class ComparisonPairGenerator(task: ProjectTask[LinkSpec],
                              config: LearningConfiguration,
                              initialState: ComparisonPairs) extends Activity[ComparisonPairs] {

  override def initialValue: Option[ComparisonPairs] = Some(initialState)

  override def run(context: ActivityContext[ComparisonPairs])
                  (implicit userContext: UserContext): Unit = {

    val linkSpec = task.data
    val datasets = task.dataSources
    val paths = getPaths()
    implicit val prefixes: Prefixes = task.project.config.prefixes

    // Update random seed
    val newRandomSeed = new Random(context.value().randomSeed).nextLong()
    context.value() = context.value().copy(randomSeed = newRandomSeed)
    implicit val random: Random = new Random(newRandomSeed)

    // Update unlabeled pool
    updatePool(linkSpec, datasets, context, paths)
  }

  // Retrieves available paths
  private def getPaths() = {
    val pathsCache = task.activity[LinkingPathsCache].control
    pathsCache.waitUntilFinished()

    // Check if we got any paths
    val paths = pathsCache.value().map(_.typedPaths)
    assert(paths.source.nonEmpty, "No paths have been found in the source dataset (in LinkingPathsCache).")
    assert(paths.target.nonEmpty, "No paths have been found in the target dataset (in LinkingPathsCache).")

    // Remove URI paths
    val filteredPaths = paths.map(_.filterNot(_.valueType == ValueType.URI))

    filteredPaths
  }

  private def updatePool(linkSpec: LinkSpec,
                         datasets: DPair[DataSource],
                         context: ActivityContext[ComparisonPairs],
                         paths: DPair[IndexedSeq[TypedPath]])
                        (implicit userContext: UserContext, prefixes: Prefixes, random: Random): Unit = Timer("Generating Pool") {
    // Build unlabeled pool
    context.status.updateMessage("Generating pool")
    val pathPairs = generatePathPairs(paths)
    val generator = config.active.linkPoolGenerator.generator(datasets, linkSpec, pathPairs, random.nextLong())
    val pool = context.child(generator, 0.5).startBlockingAndGetValue()

    if (pool.links.isEmpty) {
      throw new LearningException("Could not find any matches.")
    }

    // Find matching paths
    context.value.updateWith(_.copy(suggestedPairs = ComparisonPairGenerator(pool.links, linkSpec)))
  }

  private def generatePathPairs(paths: DPair[Seq[TypedPath]]): Seq[ComparisonPair] = {
    if(paths.source.toSet.diff(paths.target.toSet).size <= paths.source.size.toDouble * 0.1) {
      // If both sources share most path, assume that the schemata are equal and generate direct pairs
      for((source, target) <- paths.source zip paths.target) yield ComparisonPair(source, target)
    } else {
      // If both source have different paths, generate the complete cartesian product
      for (sourcePath <- paths.source; targetPath <- paths.target) yield ComparisonPair(sourcePath, targetPath)
    }
  }
}

object ComparisonPairGenerator {

  def apply(linkCandidates: Traversable[LinkCandidate], linkSpec: LinkSpec): Seq[ComparisonPair] = {
    val comparisonPairs = (fromLinkCandidates(linkCandidates) ++ fromLinkSpec(linkSpec)).distinct
    if(comparisonPairs.isEmpty) {
      throw new Exception("Did not find any matching paths from the current linkage rule and by matching the source data.")
    }
    comparisonPairs
  }

  private def fromLinkCandidates(linkCandidates: Traversable[LinkCandidate]): Seq[ComparisonPair] = {
    val pathScores = mutable.HashMap[ComparisonPair, Double]()
    for {
      linkCandidate <- linkCandidates
      matchingPair <- linkCandidate.matchingValues
    } {
      val paths = ComparisonPair(matchingPair.sourcePath(linkCandidate.sourceEntity), matchingPair.targetPath(linkCandidate.targetEntity))
      pathScores.put(paths, pathScores.getOrElse(paths, 0.0) + matchingPair.score)
    }
    pathScores.toSeq.sortBy(-_._2).map(_._1)
  }

  private def fromLinkSpec(linkSpec: LinkSpec): Seq[ComparisonPair] = {
    linkSpec.rule.operator.toSeq.flatMap(fromSimilarityOperator)
  }

  private def fromSimilarityOperator(similarityOperator: SimilarityOperator): Seq[ComparisonPair] = {
    similarityOperator match {
      case agg: Aggregation =>
        agg.operators.flatMap(fromSimilarityOperator)
      case cmp: Comparison =>
        // Comparing multiple paths cannot be expressed at the moment, so we just generate the complete Cartesian product
        for {
          sourcePath <- collectPaths(cmp.inputs.source)
          targetPath <- collectPaths(cmp.inputs.target)
        } yield ComparisonPair(sourcePath, targetPath)
    }
  }

  private def collectPaths(input: Input): Seq[TypedPath] = {
    input match {
      case PathInput(_, path: TypedPath) =>
        Seq(path)
      case PathInput(_, path: UntypedPath) =>
        Seq(path.asStringTypedPath)
      case transform: TransformInput =>
        transform.inputs.flatMap(collectPaths)

    }
  }

}
