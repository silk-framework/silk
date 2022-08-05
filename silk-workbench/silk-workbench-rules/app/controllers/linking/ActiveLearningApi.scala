package controllers.linking

import akka.stream.Materializer
import controllers.core.UserContextActions
import controllers.core.util.JsonUtils
import controllers.linking.activeLearning.ActiveLearningIterator
import controllers.linking.activeLearning.JsonFormats.ComparisonPairFormat
import org.silkframework.learning.active.ActiveLearning
import org.silkframework.learning.active.comparisons.{ComparisonPair, ComparisonPairGenerator}
import org.silkframework.rule.LinkSpec
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.serialization.WriteContext
import org.silkframework.serialization.json.JsonSerializers.LinkageRuleJsonFormat
import org.silkframework.serialization.json.LinkingSerializers.LinkJsonFormat
import org.silkframework.workbench.Context
import org.silkframework.workspace.WorkspaceFactory
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent, InjectedController}

import javax.inject.Inject

class ActiveLearningApi @Inject() (implicit mat: Materializer) extends InjectedController with UserContextActions {

  def addComparisonPair(project: String, task: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val newComparisonPair = JsonUtils.validateJsonFromRequest[ComparisonPairFormat](request).toComparisonPair
    updateSelectedComparisonPairs(project, task)(_ :+ newComparisonPair)
    Ok
  }

  def removeComparisonPair(project: String, task: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val removeComparisonPair = JsonUtils.validateJsonFromRequest[ComparisonPairFormat](request).toComparisonPair
    updateSelectedComparisonPairs(project, task)(_.filterNot(_ == removeComparisonPair))
    Ok
  }

  private def updateSelectedComparisonPairs(projectId: String, taskId: String)
                                           (updateFunc: Seq[ComparisonPair] => Seq[ComparisonPair])
                                           (implicit user: UserContext): Unit = {
    val project = WorkspaceFactory().workspace.project(projectId)
    val task = project.task[LinkSpec](taskId)
    val activity = task.activity[ComparisonPairGenerator]
    activity.updateValue(activity.value().copy(selectedPairs = updateFunc(activity.value().selectedPairs)))
  }

  def iterate(project: String, task: String, decision: String,
              linkSource: String, linkTarget: String, synchronous: Boolean = false): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
    val context = Context.get[LinkSpec](project, task, request.path)
    val linkCandidate = ActiveLearningIterator.nextActiveLearnCandidate(decision, linkSource, linkTarget, context.task, synchronous)
    linkCandidate match {
      case Some(candidate) =>
        implicit val writeContext = WriteContext[JsValue]()
        val format = new LinkJsonFormat(rule = None, writeEntities = true, writeEntitySchema = true)
        Ok(format.write(candidate))
      case None =>
        NoContent
    }
  }

  def bestRule(project: String, task: String): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
    val context = Context.get[LinkSpec](project, task, request.path)
    val activeLearning = context.task.activity[ActiveLearning]
    val bestRule = activeLearning.value().population.bestIndividual.node.build
    implicit val writeContext = WriteContext[JsValue]()
    Ok(LinkageRuleJsonFormat.write(bestRule))
  }
}
