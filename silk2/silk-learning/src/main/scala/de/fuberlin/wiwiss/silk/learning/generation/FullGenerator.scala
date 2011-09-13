package de.fuberlin.wiwiss.silk.learning.generation

import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import de.fuberlin.wiwiss.silk.instance.Path
import de.fuberlin.wiwiss.silk.learning.individual.StrategyNode
import de.fuberlin.wiwiss.silk.linkspec.similarity.DistanceMeasure
import de.fuberlin.wiwiss.silk.learning.LearningConfiguration.Components

class FullGenerator(components: Components) {

  def apply(paths: SourceTargetPair[Traversable[Path]]): Traversable[ComparisonGenerator] = {
    for(sourcePath <- paths.source;
        targetPath <- paths.target) yield
        createGenerators(SourceTargetPair(sourcePath, targetPath))
  }.flatten

  private def createGenerators(pathPair: SourceTargetPair[Path]) = {
    new ComparisonGenerator(InputGenerator.fromPathPair(pathPair, components.transformations), StrategyNode("levenshteinDistance", Nil, DistanceMeasure), 5.0) ::
    new ComparisonGenerator(InputGenerator.fromPathPair(pathPair, components.transformations), StrategyNode("jaccard", Nil, DistanceMeasure), 1.0) :: Nil
  }
}