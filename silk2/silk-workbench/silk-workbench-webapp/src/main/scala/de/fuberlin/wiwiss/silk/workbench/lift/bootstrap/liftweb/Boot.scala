package bootstrap.liftweb

import _root_.net.liftweb.common._
import _root_.net.liftweb.util._
import _root_.net.liftweb.http._
import _root_.net.liftweb.sitemap._
import _root_.net.liftweb.sitemap.Loc._
import Helpers._
import de.fuberlin.wiwiss.silk.impl.DefaultImplementations
import de.fuberlin.wiwiss.silk.workbench.project.Project
import js.jquery.JQuery14Artifacts
import java.io.ByteArrayOutputStream
import net.liftweb.json.JsonAST._
import de.fuberlin.wiwiss.silk.linkspec.{Aggregator, Metric}
import de.fuberlin.wiwiss.silk.linkspec.input.Transformer
import xml.PrettyPrinter
import de.fuberlin.wiwiss.silk.instance.{Path, InstanceSpecification}
import de.fuberlin.wiwiss.silk.util.strategy.{Parameter, StrategyDefinition, Strategy}
import de.fuberlin.wiwiss.silk.util.SourceTargetPair

/**
  * A class that's instantiated early and run.  It allows the application
  * to modify lift's environment
  */
class Boot
{
  def boot
  {
    DefaultImplementations.register()

    LiftRules.jsArtifacts = JQuery14Artifacts

    // where to search snippet
    LiftRules.addToPackages("de.fuberlin.wiwiss.silk.workbench.lift")

    // Build SiteMap
    val ifProjectOpen = If(() => Project.isOpen, () => RedirectResponse("/index"))

    val entries =
        Menu(Loc("home", List("index"), "Home")) ::
        Menu(Loc("Link Specification", List("linkSpec"), "Link Specification", ifProjectOpen)) ::
        Menu(Loc("Evaluate", List("evaluate"), "Evaluate", ifProjectOpen)) ::
        Menu(Loc("Reference Links", List("alignment"), "Reference Links", ifProjectOpen)) :: Nil
        //Menu(Loc("Learn", List("learn"), "Learn", ifProjectOpen)) :: Nil

    LiftRules.setSiteMap(SiteMap(entries:_*))

    LiftRules.dispatch.prepend(dispatch)
  }

  private def dispatch : LiftRules.DispatchPF =
  {
    case req @ Req(List("project.silk"), "", GetRequest) =>
    {
      val outputStream = new ByteArrayOutputStream()
      Project.save(outputStream)
      () => Full(InMemoryResponse(outputStream.toByteArray, ("Content-Type", "application/zip") :: Nil, Nil, 200))
    }
    case req @ Req(List("config"), "", GetRequest) =>
    {
      val outputStream = new ByteArrayOutputStream()
      val configXml = Project().config.toXML
      val configStr = new PrettyPrinter(140, 2).format(configXml)
      () => Full(InMemoryResponse(configStr.getBytes, ("Content-Type", "application/xml") :: Nil, Nil, 200))
    }
    case req @ Req(List("api", "project", "paths"), "", GetRequest) => () => generatePaths(req)
    case req @ Req(List("api", "project", "operators"), "", GetRequest) => () => generateOperators()
  }

  private def generatePaths(req : Req) =
  {
    val maxPathCount = req.param("max").map(_.toInt).getOrElse(Int.MaxValue)

    val datasets = Project().linkSpec.datasets
    val restrictions = Project().linkSpec.datasets.map(_.restriction)
    val instanceSpecs = Project().cache.instanceSpecs
    val sourcePaths = if(instanceSpecs != null) instanceSpecs.source.paths else List[Path]()
    val targetPaths = if(instanceSpecs != null) instanceSpecs.target.paths else List[Path]()

    val sourceField = JField("source", JObject(JField("id", JString(datasets.source.sourceId)) ::
                                               JField("paths", JArray(generateInstancePaths(sourcePaths, maxPathCount).toList)) ::
                                               JField("availablePaths", JInt(sourcePaths.size)) ::
                                               JField("restrictions", JString(restrictions.source)) :: Nil))
    val targetField = JField("target", JObject(JField("id", JString(datasets.target.sourceId)) ::
                                               JField("paths", JArray(generateInstancePaths(targetPaths, maxPathCount).toList)) ::
                                               JField("availablePaths", JInt(targetPaths.size)) ::
                                               JField("restrictions", JString(restrictions.target)) :: Nil))

    val isLoadingField = JField("isLoading", JBool(instanceSpecs == null))

    val json = JObject(sourceField :: targetField :: isLoadingField :: Nil)

    Full(JsonResponse(json))
  }

  private def generateInstancePaths(paths : Traversable[Path], maxPathCount : Int) =
  {
    for(path <- paths.toSeq.take(maxPathCount)) yield
    {
      JObject(JField("path", JString(path.toString)) :: JField("frequency", JDouble(1.0)) :: Nil)
    }
  }

  private def generateOperators() =
  {
    val transformations = JField("transformations", JArray(generateFactoryOperators(Transformer).toList))
    val comparators = JField("comparators", JArray(generateFactoryOperators(Metric).toList))
    val aggregators = JField("aggregators", JArray(generateFactoryOperators(Aggregator).toList))

    val json = JObject(transformations :: comparators :: aggregators :: Nil)

    Full(JsonResponse(json))
  }

  private def generateFactoryOperators[T <: Strategy](factory : de.fuberlin.wiwiss.silk.util.strategy.Factory[T]) =
  {
    for(strategy <- factory.availableStrategies.toSeq.sortBy(_.label)) yield
    {
      JObject(JField("id", JString(strategy.id)) ::
              JField("label", JString(strategy.label)) ::
              JField("description", JString(strategy.description)) ::
              JField("parameters", JArray(generateFactoryParameters(strategy.parameters).toList)) :: Nil)
    }
  }

  private def generateFactoryParameters(parameters : Traversable[Parameter]) =
  {
    for(parameter <- parameters) yield
    {
      JObject(JField("name", JString(parameter.name)) ::
              JField("type", JString(parameter.dataType.toString)) ::
              /*JField("description", JString(parameter.description)) ::*/ Nil)
    }
  }
}
