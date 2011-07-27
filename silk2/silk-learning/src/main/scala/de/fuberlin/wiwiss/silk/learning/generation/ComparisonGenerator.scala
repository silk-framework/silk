//package de.fuberlin.wiwiss.silk.learning.generation
//
//import de.fuberlin.wiwiss.silk.config.Prefixes
//import de.fuberlin.wiwiss.silk.util.SourceTargetPair
//import de.fuberlin.wiwiss.silk.instance.Path
//import util.Random
//import de.fuberlin.wiwiss.silk.linkspec.input.Transformer
//import de.fuberlin.wiwiss.silk.linkspec.similarity.DistanceMeasure
//import de.fuberlin.wiwiss.silk.workbench.learning.LearningConfiguration
//import de.fuberlin.wiwiss.silk.workbench.learning.tree._
//

//TODO
//object ComparisonGenerator
//{
//  implicit val prefixes = Prefixes.empty
//
//  private val handlers = IndexedSeq(LabelHandler, Wgs84Handler)
//
//  def generate(config : GenerationConfiguration): Option[ComparisonNode] =
//  {
//    val handler = handlers(Random.nextInt(handlers.size))
//
//    handler(config.paths)
//  }
//
//  private trait Handler extends (SourceTargetPair[Traversable[Path]] => Option[ComparisonNode])
//  {
//    protected def getProperty(paths : SourceTargetPair[Traversable[Path]], property : String) : Option[SourceTargetPair[Path]] =
//    {
//      (paths.source.find(_.serialize.contains(property)), paths.target.find(_.serialize.contains(property))) match
//      {
//        case (Some(sourcePath), Some(targetPath)) => Some(SourceTargetPair(sourcePath, targetPath))
//        case _ => None
//      }
//    }
//  }
//
//  private object LabelHandler extends Handler
//  {
//    private val labelProperty = "http://www.w3.org/2000/01/rdf-schema#label"
//
//    def apply(paths : SourceTargetPair[Traversable[Path]]) =
//    {
//      getProperty(paths, labelProperty) match
//      {
//        case Some(labelPaths) => Some(generateNode(labelPaths))
//        case _ => None
//      }
//    }
//
//    private def generateNode(paths : SourceTargetPair[Path]) =
//    {
//      val inputs = SourceTargetPair(PathInputNode(true, paths.source),
//                                    PathInputNode(false, paths.target))
//
//      ComparisonNode(inputs, Random.nextInt(10), StrategyNode("levenshtein", Nil, DistanceMeasure))
//    }
//  }
//
//  private object Wgs84Handler extends Handler
//  {
//    private val latProperty = "http://www.w3.org/2003/01/geo/wgs84_pos#lat"
//    private val longProperty = "http://www.w3.org/2003/01/geo/wgs84_pos#long"
//
//    def apply(paths : SourceTargetPair[Traversable[Path]]) =
//    {
//      (getProperty(paths, latProperty), getProperty(paths, longProperty)) match
//      {
//        case (Some(latPaths), Some(longPaths)) => Some(generateNode(latPaths, longPaths))
//        case _ => None
//      }
//    }
//
//    private def generateNode(latPaths : SourceTargetPair[Path], longPaths : SourceTargetPair[Path]) =
//    {
//      val sourceInput =
//        TransformNode(
//          isSource = true,
//          inputs = PathInputNode(true, latPaths.source) :: PathInputNode(true, longPaths.source) :: Nil,
//          transformer = StrategyNode("concat", ParameterNode("glue", " ") :: Nil, Transformer))
//
//      val targetInput =
//        TransformNode(
//          isSource = false,
//          inputs = PathInputNode(false, latPaths.target) :: PathInputNode(false, longPaths.target) :: Nil,
//          transformer = StrategyNode("concat", ParameterNode("glue", " ") :: Nil, Transformer))
//
//      ComparisonNode(
//        inputs = SourceTargetPair(sourceInput, targetInput),
//        threshold = Random.nextInt(50),
//        metric = StrategyNode("wgs84", ParameterNode("unit", "km") :: Nil, DistanceMeasure))
//    }
//  }
//}