package bootstrap.liftweb

import xml.PrettyPrinter
import de.fuberlin.wiwiss.silk.entity.Path
import de.fuberlin.wiwiss.silk.config.Prefixes
import de.fuberlin.wiwiss.silk.linkspec.input.Transformer
import de.fuberlin.wiwiss.silk.linkspec.similarity.{Aggregator, DistanceMeasure}
import de.fuberlin.wiwiss.silk.util.strategy.{Parameter, Strategy}
import de.fuberlin.wiwiss.silk.workbench.workspace.io.{ProjectExporter, SilkConfigExporter}
import net.liftweb.common.Full
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import net.liftweb.json.JsonAST._
import net.liftweb.http._
import de.fuberlin.wiwiss.silk.util.task.TaskFinished

/**
 * The Silk Workbench REST API.
 */
object Api {
  def dispatch : LiftRules.DispatchPF = {
    case req @ Req(List("project"), "xml", GetRequest) => {
      val projectXml = ProjectExporter(User().project)
      val projectStr = new PrettyPrinter(140, 2).format(projectXml)
      () => Full(InMemoryResponse(projectStr.getBytes, ("Content-Type", "application/xml") :: ("Content-Disposition", "attachment") :: Nil, Nil, 200))
    }
    case req @ Req(List("config"), "xml", GetRequest) => {
      val configXml = SilkConfigExporter.build().toXML
      val configStr = new PrettyPrinter(140, 2).format(configXml)
      () => Full(InMemoryResponse(configStr.getBytes, ("Content-Type", "application/xml") :: ("Content-Disposition", "attachment") :: Nil, Nil, 200))
    }
    case req @ Req(List("referenceLinks"), "xml", GetRequest) => {
      val alignmentXml = User().linkingTask.referenceLinks.toXML
      val configStr = new PrettyPrinter(140, 2).format(alignmentXml)
      () => Full(InMemoryResponse(configStr.getBytes, ("Content-Type", "application/xml") :: ("Content-Disposition", "attachment") :: Nil, Nil, 200))
    }
    case req @ Req(List("api", "project", "paths"), "", GetRequest) => () => generatePaths(req)
    case req @ Req(List("api", "project", "operators"), "", GetRequest) => () => generateOperators()
  }

  private def generatePaths(req : Req) = {
    val maxPathCount = req.param("max").map(_.toInt).getOrElse(Int.MaxValue)

    implicit val prefixes = User().project.config.prefixes
    val linkingTask = User().linkingTask
    val datasets = linkingTask.linkSpec.datasets
    val restrictions = linkingTask.linkSpec.datasets.map(_.restriction)
    val entityDescs = linkingTask.cache.entityDescs
    val sourcePaths = if(entityDescs != null) entityDescs.source.paths else List[Path]()
    val targetPaths = if(entityDescs != null) entityDescs.target.paths else List[Path]()

    val sourceField = JField("source", JObject(JField("id", JString(datasets.source.sourceId)) ::
                                               JField("paths", JArray(generateEntityPaths(sourcePaths, maxPathCount).toList)) ::
                                               JField("availablePaths", JInt(sourcePaths.size)) ::
                                               JField("restrictions", JString(restrictions.source.toString)) ::
                                               JField("variable", JString(datasets.source.variable)) :: Nil))
    val targetField = JField("target", JObject(JField("id", JString(datasets.target.sourceId)) ::
                                               JField("paths", JArray(generateEntityPaths(targetPaths, maxPathCount).toList)) ::
                                               JField("availablePaths", JInt(targetPaths.size)) ::
                                               JField("restrictions", JString(restrictions.target.toString)) ::
                                               JField("variable", JString(datasets.target.variable)) :: Nil))

    var errorMsg : Option[String] = linkingTask.cache.status match {
      case TaskFinished(_, _, _, Some(ex)) => Some(ex.getMessage)
      case _ => None
    }

    val isLoadingField = JField("isLoading", JBool(errorMsg.isEmpty && entityDescs == null))

    val json = errorMsg match {
      case None => JObject(sourceField :: targetField :: isLoadingField :: Nil)
      case Some(error) => JObject(sourceField :: targetField :: isLoadingField :: JField("error", JString(error)) :: Nil)
    }

    Full(JsonResponse(json))
  }

  private def generateEntityPaths(paths : Traversable[Path], maxPathCount : Int)(implicit prefixes : Prefixes) = {
    for(path <- paths.toSeq.take(maxPathCount)) yield {
      JObject(JField("path", JString(path.serialize)) :: JField("frequency", JDouble(1.0)) :: Nil)
    }
  }

  private def generateOperators() = {
    val transformations = JField("transformations", JArray(generateFactoryOperators(Transformer).toList))
    val comparators = JField("comparators", JArray(generateFactoryOperators(DistanceMeasure).toList))
    val aggregators = JField("aggregators", JArray(generateFactoryOperators(Aggregator).toList))

    val json = JObject(transformations :: comparators :: aggregators :: Nil)

    Full(JsonResponse(json))
  }

  private def generateFactoryOperators[T <: Strategy](factory : de.fuberlin.wiwiss.silk.util.strategy.Factory[T]) = {
    for(strategy <- factory.availableStrategies.toSeq.sortBy(_.label)) yield {
      JObject(JField("id", JString(strategy.id)) ::
              JField("label", JString(strategy.label)) ::
              JField("description", JString(strategy.description)) ::
              JField("parameters", JArray(generateFactoryParameters(strategy.parameters).toList)) :: Nil)
    }
  }

  private def generateFactoryParameters(parameters : Traversable[Parameter]) = {
    for(parameter <- parameters) yield {
      JObject(JField("name", JString(parameter.name)) ::
              JField("type", JString(parameter.dataType.toString)) ::
              JField("optional", JBool(parameter.defaultValue.isDefined)) ::
              parameter.defaultValue.map(value => JField("defaultValue", JString(value.toString))).toList)
    }
  }
}