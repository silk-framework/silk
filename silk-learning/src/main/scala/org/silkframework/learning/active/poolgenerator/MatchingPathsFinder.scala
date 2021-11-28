package org.silkframework.learning.active.poolgenerator

import org.silkframework.entity.paths.TypedPath
import org.silkframework.learning.active.LinkCandidate
import org.silkframework.util.DPair

import scala.collection.mutable

object MatchingPathsFinder {

  def apply(linkCandidates: Traversable[LinkCandidate]): Seq[DPair[TypedPath]] = {
    val pathScores = mutable.HashMap[DPair[TypedPath], Double]()
    for {
      linkCandidate <- linkCandidates
      matchingPair <- linkCandidate.matchingValues
    } {
      val paths = DPair(matchingPair.sourcePath(linkCandidate.sourceEntity), matchingPair.targetPath(linkCandidate.targetEntity))
      pathScores.put(paths, pathScores.getOrElse(paths, 0.0) + matchingPair.score)
    }

    if(pathScores.isEmpty) {
      throw new Exception("Did not find any matching paths")
    }

    pathScores.toSeq.sortBy(-_._2).map(_._1)
  }

}
