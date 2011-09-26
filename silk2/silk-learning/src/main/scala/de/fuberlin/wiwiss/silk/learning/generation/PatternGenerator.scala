package de.fuberlin.wiwiss.silk.learning.generation

import de.fuberlin.wiwiss.silk.config.Prefixes
import de.fuberlin.wiwiss.silk.evaluation.ReferenceEntities
import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import de.fuberlin.wiwiss.silk.entity.Path
import de.fuberlin.wiwiss.silk.linkspec.similarity.DistanceMeasure
import de.fuberlin.wiwiss.silk.learning.individual._
import de.fuberlin.wiwiss.silk.linkspec.input.Transformer
import de.fuberlin.wiwiss.silk.learning.LearningConfiguration.Components

class PatternGenerator(components: Components) {

  implicit val prefixes = Prefixes.empty

  private val handlers = LabelHandler :: Wgs84Handler :: Nil

  def apply(paths: SourceTargetPair[Traversable[Path]]): Traversable[ComparisonGenerator] = {
    handlers.flatMap(_.apply(paths))
  }

  private trait Handler extends (SourceTargetPair[Traversable[Path]] => Option[ComparisonGenerator]) {
    protected def getProperty(paths : SourceTargetPair[Traversable[Path]], property : String) : Option[SourceTargetPair[Path]] = {
      (paths.source.find(_.serialize.contains(property)), paths.target.find(_.serialize.contains(property))) match {
        case (Some(sourcePath), Some(targetPath)) => Some(SourceTargetPair(sourcePath, targetPath))
        case _ => None
      }
    }
  }

  private object LabelHandler extends Handler {
    private val labelProperty = "http://www.w3.org/2000/01/rdf-schema#label"

    def apply(paths : SourceTargetPair[Traversable[Path]]) = {
      getProperty(paths, labelProperty).map(createGenerator)
    }

    //TODO create measures other than levensthein
    private def createGenerator(pathPair : SourceTargetPair[Path]) = {
      new ComparisonGenerator(
        inputGenerators = InputGenerator.fromPathPair(pathPair, components.transformations),
        measure = StrategyNode("levenshteinDistance", Nil, DistanceMeasure),
        maxThreshold = 10.0
      )
    }
  }

  private object Wgs84Handler extends Handler
  {
    private val latProperty = "http://www.w3.org/2003/01/geo/wgs84_pos#lat"
    private val longProperty = "http://www.w3.org/2003/01/geo/wgs84_pos#long"

    def apply(paths : SourceTargetPair[Traversable[Path]]) = {
      (getProperty(paths, latProperty), getProperty(paths, longProperty)) match {
        case (Some(latPaths), Some(longPaths)) => Some(createGenerator(latPaths, longPaths))
        case _ => None
      }
    }

    private def createGenerator(latPaths : SourceTargetPair[Path], longPaths : SourceTargetPair[Path]) = {
      val inputs =
        SourceTargetPair(
          TransformNode(
            isSource = true,
            inputs = PathInputNode(latPaths.source, true) :: PathInputNode(longPaths.source, true) :: Nil,
            transformer = StrategyNode("concat", ParameterNode("glue", " ") :: Nil, Transformer)
          ),
          TransformNode(
            isSource = false,
            inputs = PathInputNode(latPaths.target, false) :: PathInputNode(longPaths.target, false) :: Nil,
            transformer = StrategyNode("concat", ParameterNode("glue", " ") :: Nil, Transformer)
          )
        )

      new ComparisonGenerator(
        inputGenerators = InputGenerator.fromInputPair(inputs, components.transformations),
        measure = StrategyNode("wgs84", ParameterNode("unit", "km") :: Nil, DistanceMeasure),
        maxThreshold = 50.0
      )
    }
  }
}