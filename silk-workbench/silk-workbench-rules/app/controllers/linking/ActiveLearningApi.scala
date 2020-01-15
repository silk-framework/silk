package controllers.linking

import akka.stream.Materializer
import controllers.core.{RequestUserContextAction, UserContextAction}
import javax.inject.Inject
import org.silkframework.learning.active.ActiveLearning
import org.silkframework.rule.LinkSpec
import org.silkframework.runtime.serialization.WriteContext
import org.silkframework.serialization.json.JsonSerializers.LinkageRuleJsonFormat
import org.silkframework.serialization.json.LinkingSerializers
import org.silkframework.serialization.json.LinkingSerializers.LinkJsonFormat
import org.silkframework.workbench.Context
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent, InjectedController}

class ActiveLearningApi @Inject() (implicit mat: Materializer) extends InjectedController {

  def iterate(project: String, task: String, decision: String,
              linkSource: String, linkTarget: String): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
    val context = Context.get[LinkSpec](project, task, request.path)
    val linkCandidate = ActiveLearningIterator.nextActiveLearnCandidate(decision, linkSource, linkTarget, context.task)
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
