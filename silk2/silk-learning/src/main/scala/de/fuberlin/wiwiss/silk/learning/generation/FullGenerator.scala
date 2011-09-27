package de.fuberlin.wiwiss.silk.learning.generation

import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.linkspec.similarity.DistanceMeasure
import de.fuberlin.wiwiss.silk.learning.LearningConfiguration.Components
import de.fuberlin.wiwiss.silk.entity.Path
import de.fuberlin.wiwiss.silk.learning.individual.FunctionNode

class FullGenerator(components: Components) {

  def apply(paths: DPair[Traversable[Path]]): Traversable[ComparisonGenerator] = {
    for(sourcePath <- paths.source;
        targetPath <- paths.target) yield
        createGenerators(DPair(sourcePath, targetPath))
  }.flatten

  private def createGenerators(pathPair: DPair[Path]) = {
    new ComparisonGenerator(InputGenerator.fromPathPair(pathPair, components.transformations), FunctionNode("levenshteinDistance", Nil, DistanceMeasure), 5.0) ::
    new ComparisonGenerator(InputGenerator.fromPathPair(pathPair, components.transformations), FunctionNode("jaccard", Nil, DistanceMeasure), 1.0) :: Nil
  }
}