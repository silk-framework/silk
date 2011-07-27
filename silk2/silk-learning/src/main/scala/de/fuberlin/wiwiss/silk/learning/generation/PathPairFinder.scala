package de.fuberlin.wiwiss.silk.learning.generation

import de.fuberlin.wiwiss.silk.evaluation.ReferenceInstances
import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import de.fuberlin.wiwiss.silk.instance.{Instance, Path}

object PathPairFinder {
  def apply(instances: ReferenceInstances): Traversable[SourceTargetPair[Path]] = {
    //Get all paths except sameAs paths
    val paths = instances.positive.values.head.map(_.spec.paths).map(_.filterNot(_.toString.contains("sameAs")))

    val distinctPaths =
      SourceTargetPair(
        source = removeDuplicatePaths(instances.positive.values.map(_.source) ++ instances.negative.values.map(_.source), paths.source),
        target = removeDuplicatePaths(instances.positive.values.map(_.target) ++ instances.negative.values.map(_.target), paths.target)
      )

    val pathPairs = for(sourcePath <- distinctPaths.source; targetPath <- distinctPaths.target) yield SourceTargetPair(sourcePath, targetPath)

    pathPairs.filter(pathValuesMatch(instances, _))
  }

  def removeDuplicatePaths(instances: Traversable[Instance], paths: Traversable[Path]): Traversable[Path] = {
    for(path :: tail <- paths.toList.tails.toTraversable
        if !tail.exists(p => pathsMatch(instances, SourceTargetPair(path, p)))) yield path
  }

  def pathsMatch(instances: Traversable[Instance], pathPair: SourceTargetPair[Path]): Boolean = {
    instances.forall(pathsMatch(_, pathPair))
  }

  def pathsMatch(instance: Instance, pathPair: SourceTargetPair[Path]): Boolean = {
    val values = pathPair.map(instance.evaluate)

    val valuePairs = for(v1 <- values.source; v2 <- values.target) yield SourceTargetPair(v1, v2)

    valuePairs.exists(p => p.source.toLowerCase == p.target.toLowerCase)
  }

  def pathValuesMatch(instances: ReferenceInstances, pathPair: SourceTargetPair[Path]): Boolean = {
    val positiveMatches = instances.positive.values.filter(i => matches(i, pathPair)).size
    val negativeMatches = instances.negative.values.filter(i => matches(i, pathPair)).size

    positiveMatches > negativeMatches
  }

  def matches(instancePair: SourceTargetPair[Instance], pathPair: SourceTargetPair[Path]): Boolean = {
    val sourceValues = instancePair.source.evaluate(pathPair.source)
    val targetValues = instancePair.target.evaluate(pathPair.target)

    val valuePairs = for(sourceValue <- sourceValues; targetValue <- targetValues) yield valuesMatch(sourceValue, targetValue)

    valuePairs.exists(identity)
  }

  def valuesMatch(value1: String, value2: String): Boolean = {
    val tokens1 = value1.toLowerCase.split("\\s")
    val tokens2 = value2.toLowerCase.split("\\s")

    val valuePairs = for(t1 <- tokens1; t2 <- tokens2) yield t1 == t2

    valuePairs.exists(identity)
  }
}