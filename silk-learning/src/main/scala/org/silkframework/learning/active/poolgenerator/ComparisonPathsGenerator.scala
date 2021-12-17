package org.silkframework.learning.active.poolgenerator

import org.silkframework.entity.paths.{TypedPath, UntypedPath}
import org.silkframework.learning.active.LinkCandidate
import org.silkframework.rule.LinkSpec
import org.silkframework.rule.input.{Input, PathInput, TransformInput}
import org.silkframework.rule.similarity.{Aggregation, Comparison, SimilarityOperator}
import org.silkframework.util.DPair

import scala.collection.mutable

object ComparisonPathsGenerator {

  def apply(linkCandidates: Traversable[LinkCandidate], linkSpec: LinkSpec): Seq[DPair[TypedPath]] = {
    val comparisonPairs = (fromLinkCandidates(linkCandidates) ++ fromLinkSpec(linkSpec)).distinct
    if(comparisonPairs.isEmpty) {
      throw new Exception("Did not find any matching paths from the current linkage rule and by matching the source data.")
    }
    comparisonPairs
  }

  private def fromLinkCandidates(linkCandidates: Traversable[LinkCandidate]): Seq[DPair[TypedPath]] = {
    val pathScores = mutable.HashMap[DPair[TypedPath], Double]()
    for {
      linkCandidate <- linkCandidates
      matchingPair <- linkCandidate.matchingValues
    } {
      val paths = DPair(matchingPair.sourcePath(linkCandidate.sourceEntity), matchingPair.targetPath(linkCandidate.targetEntity))
      pathScores.put(paths, pathScores.getOrElse(paths, 0.0) + matchingPair.score)
    }
    pathScores.toSeq.sortBy(-_._2).map(_._1)
  }

  private def fromLinkSpec(linkSpec: LinkSpec): Seq[DPair[TypedPath]] = {
    linkSpec.rule.operator.toSeq.flatMap(fromSimilarityOperator)
  }

  private def fromSimilarityOperator(similarityOperator: SimilarityOperator): Seq[DPair[TypedPath]] = {
    similarityOperator match {
      case agg: Aggregation =>
        agg.operators.flatMap(fromSimilarityOperator)
      case cmp: Comparison =>
        // Comparing multiple paths cannot be expressed at the moment, so we just generate the complete Cartesian product
        for {
          sourcePath <- collectPaths(cmp.inputs.source)
          targetPath <- collectPaths(cmp.inputs.target)
        } yield DPair(sourcePath, targetPath)
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
