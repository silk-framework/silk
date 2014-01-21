package controllers.api

import play.api.mvc.{Action, Controller}
import play.api.libs.json.{JsString, JsObject, JsArray, JsNumber, JsBoolean}
import de.fuberlin.wiwiss.silk.workspace.{Project, User}
import de.fuberlin.wiwiss.silk.workspace.modules.linking.LinkingTask
import de.fuberlin.wiwiss.silk.entity.Path
import de.fuberlin.wiwiss.silk.util.task.TaskFinished
import de.fuberlin.wiwiss.silk.linkagerule.input.Transformer
import de.fuberlin.wiwiss.silk.linkagerule.similarity.{Aggregator, DistanceMeasure}
import de.fuberlin.wiwiss.silk.util.plugin.{Parameter, AnyPlugin}
import de.fuberlin.wiwiss.silk.util.Identifier._
import de.fuberlin.wiwiss.silk.evaluation.LinkageRuleEvaluator._
import de.fuberlin.wiwiss.silk.entity.Link
import de.fuberlin.wiwiss.silk.config.{LinkingConfig, LinkSpecification, Prefixes}
import de.fuberlin.wiwiss.silk.workspace.io.SilkConfigImporter._
import de.fuberlin.wiwiss.silk.evaluation.{ReferenceLinks, LinkageRuleEvaluator}
import de.fuberlin.wiwiss.silk.linkagerule.LinkageRule
import de.fuberlin.wiwiss.silk.util.{ValidationException, CollectLogs}
import java.util.logging.{Logger, Level}
import de.fuberlin.wiwiss.silk.util.ValidationException.ValidationError

object LinkingTaskApi extends Controller {

  private val log = Logger.getLogger(getClass.getName)

  def getRule(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.linkingModule.task(taskName)
    implicit val prefixes = project.config.prefixes
    val ruleXml = task.linkSpec.rule.toXML

    Ok(ruleXml)
  }

  def putRule(projectName: String, taskName: String) = Action { request => {
    val project = User().workspace.project(projectName)
    val task = project.linkingModule.task(taskName)
    implicit val prefixes = project.config.prefixes
    implicit val globalThreshold = None

    request.body.asXml match {
      case Some(xml) => {
        val rule = LinkageRule.fromXML(xml.head)

        //Update linking task
        val updatedTask = task.updateLinkSpec(task.linkSpec.copy(rule = rule), project)
        project.linkingModule.update(updatedTask)

        Ok
      }
      case None => {
        BadRequest("Expecting text/xml request body")
      }
    }
  }}

  def getLinkSpec(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.linkingModule.task(taskName)
    implicit val prefixes = project.config.prefixes
    val linkSpecXml = task.linkSpec.toXML

    Ok(linkSpecXml)
  }

  def putLinkSpec(projectName: String, taskName: String) = Action { request => {
    val project = User().workspace.project(projectName)
    val task = project.linkingModule.task(taskName)
    val prefixes = project.config.prefixes

    request.body.asXml match {
      case Some(xml) => {
        try {
          //Collect warnings while saving link spec
          val warnings = CollectLogs(Level.WARNING, "de.fuberlin.wiwiss.silk.linkspec") {
            //Load link specification
            val newLinkSpec = LinkSpecification.load(prefixes)(xml.head)

            //Update linking task
            val updatedTask = task.updateLinkSpec(newLinkSpec, project)
            project.linkingModule.update(updatedTask)
          }

          Ok(statusJson(warnings = warnings.map(_.getMessage)))
        } catch {
          case ex: ValidationException => {
            log.log(Level.INFO, "Invalid linkage rule")
            BadRequest(statusJson(errors = ex.errors))
          }
          case ex: Exception => {
            log.log(Level.INFO, "Failed to save linkage rule", ex)
            InternalServerError(statusJson(errors = ValidationError("Error in back end: " + ex.getMessage) :: Nil))
          }
        }
      }
      case None => BadRequest("Expecting text/xml request body")
    }
  }}

  private def statusJson(errors: Seq[ValidationError] = Nil, warnings: Seq[String] = Nil, infos: Seq[String] = Nil) = {
    /**Generates a Json expression from an error */
    def errorToJsExp(error: ValidationError) = JsObject(("message", JsString(error.toString)) :: ("id", JsString(error.id.map(_.toString).getOrElse(""))) :: Nil)

    JsObject(
      ("error", JsArray(errors.map(errorToJsExp))) ::
      ("warning", JsArray(warnings.map(JsString(_)))) ::
      ("info", JsArray(infos.map(JsString(_)))) :: Nil
    )
  }

  def getReferenceLinks(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.linkingModule.task(taskName)
    val referenceLinksXml = task.referenceLinks.toXML

    Ok(referenceLinksXml)
  }

  def putReferenceLinks(projectName: String, taskName: String) = Action { implicit request => {
    val project = User().workspace.project(projectName)
    val task = project.linkingModule.task(taskName)

    for(data <- request.body.asMultipartFormData;
        file <- data.files) {
      val referenceLinks = ReferenceLinks.fromXML(scala.xml.XML.loadFile(file.ref.file))
      project.linkingModule.update(task.updateReferenceLinks(referenceLinks, project))
    }
    Ok
  }}
  
  def putReferenceLink(projectName: String, taskName: String, linkType: String, source: String, target: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.linkingModule.task(taskName)
    val link = new Link(source, target)
    
    linkType match {
      case "positive" => {
        val updatedTask = task.updateReferenceLinks(task.referenceLinks.withPositive(link), project)
        project.linkingModule.update(updatedTask)
      }
      case "negative" => {
        val updatedTask = task.updateReferenceLinks(task.referenceLinks.withNegative(link), project)
        project.linkingModule.update(updatedTask)
      }
    }
    
    Ok
  }
  
  def deleteReferenceLink(projectName: String, taskName: String, source: String, target: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.linkingModule.task(taskName)
    val link = new Link(source, target)
    
    val updatedTask = task.updateReferenceLinks(task.referenceLinks.without(link), project)
    project.linkingModule.update(updatedTask)
    
    Ok
  }

  def types(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.linkingModule.task(taskName)
    Ok(generateTypes(project, task))
  }

  def paths(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.linkingModule.task(taskName)
    Ok(generatePaths(project, task))
  }

  def operators(projectName: String, taskName: String) = Action {
    Ok(generateOperators())
  }

  private def generateTypes(project: Project, task: LinkingTask) = {
    val datasets = task.linkSpec.datasets
    val sources = datasets.map(_.sourceId).map(project.sourceModule.task(_))

    sources.map(_.source.retrieveTypes()).toString
  }

  private def generatePaths(project: Project, task: LinkingTask) = {
    implicit val prefixes = project.config.prefixes
    val datasets = task.linkSpec.datasets
    val restrictions = task.linkSpec.datasets.map(_.restriction)
    val entityDescs = task.cache.entityDescs
    val sourcePaths = if(entityDescs != null) entityDescs.source.paths else List[Path]()
    val targetPaths = if(entityDescs != null) entityDescs.target.paths else List[Path]()

    val sourceField = ("source", JsObject(("id", JsString(datasets.source.sourceId)) ::
                                          ("paths", JsArray(generateEntityPaths(sourcePaths).toList)) ::
                                          ("availablePaths", JsNumber(sourcePaths.size)) ::
                                          ("restrictions", JsString(restrictions.source.toString)) ::
                                          ("variable", JsString(datasets.source.variable)) :: Nil))

    val targetField = ("target", JsObject(("id", JsString(datasets.target.sourceId)) ::
                                          ("paths", JsArray(generateEntityPaths(targetPaths).toList)) ::
                                          ("availablePaths", JsNumber(targetPaths.size)) ::
                                          ("restrictions", JsString(restrictions.target.toString)) ::
                                          ("variable", JsString(datasets.target.variable)) :: Nil))

    var errorMsg : Option[String] = task.cache.pathCache.status match {
      case TaskFinished(_, _, _, Some(ex)) => Some(ex.getMessage)
      case _ => None
    }

    val isLoadingField = ("isLoading", JsBoolean(errorMsg.isEmpty && entityDescs == null))

    val json = errorMsg match {
      case None => JsObject(sourceField :: targetField :: isLoadingField :: Nil)
      case Some(error) => JsObject(sourceField :: targetField :: isLoadingField :: ("error", JsString(error)) :: Nil)
    }

    json
  }

  private def generateEntityPaths(paths : Traversable[Path])(implicit prefixes : Prefixes) = {
    for(path <- paths.toSeq) yield {
      JsObject(("path", JsString(path.serialize)) :: ("frequency", JsNumber(1.0)) :: Nil)
    }
  }

  private def generateOperators() = {
    val transformations = ("transformations", JsArray(generateFactoryOperators(Transformer).toList))
    val comparators = ("comparators", JsArray(generateFactoryOperators(DistanceMeasure).toList))
    val aggregators = ("aggregators", JsArray(generateFactoryOperators(Aggregator).toList))

    val json = JsObject(transformations :: comparators :: aggregators :: Nil)

    json
  }

  private def generateFactoryOperators[T <: AnyPlugin](factory : de.fuberlin.wiwiss.silk.util.plugin.PluginFactory[T]) = {
    for(plugin <- factory.availablePlugins.toSeq.sortBy(_.label)) yield {
      JsObject(("id", JsString(plugin.id)) ::
               ("categories", JsArray(plugin.categories.toSeq.map(JsString(_)))) ::
               ("label", JsString(plugin.label)) ::
               ("description", JsString(plugin.description)) ::
               ("parameters", JsArray(generateFactoryParameters(plugin.parameters).toList)) :: Nil)
    }
  }

  private def generateFactoryParameters(parameters : Traversable[Parameter]) = {
    for(parameter <- parameters) yield {
      JsObject(("name", JsString(parameter.name)) ::
               ("type", JsString(parameter.dataType.toString)) ::
               ("optional", JsBoolean(parameter.defaultValue.isDefined)) ::
               parameter.defaultValue.map(value => ("defaultValue", JsString(value.toString))).toList)
    }
  }
}
