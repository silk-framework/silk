package controllers.linking

import akka.stream.Materializer
import controllers.core.UserContextActions
import controllers.core.util.JsonUtils
import controllers.linking.activeLearning.ActiveLearningIterator
import controllers.linking.activeLearning.JsonFormats._
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.silkframework.learning.active.ActiveLearning
import org.silkframework.learning.active.comparisons.{ComparisonPair, ComparisonPairGenerator}
import org.silkframework.rule.LinkSpec
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.serialization.WriteContext
import org.silkframework.runtime.validation.NotFoundException
import org.silkframework.serialization.json.JsonSerializers.LinkageRuleJsonFormat
import org.silkframework.serialization.json.LinkingSerializers.LinkJsonFormat
import org.silkframework.workbench.Context
import org.silkframework.workspace.WorkspaceFactory
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, InjectedController}

import javax.inject.Inject

@Tag(name = "Linking - Active learning", description = "Active learning API.")
class ActiveLearningApi @Inject() (implicit mat: Materializer) extends InjectedController with UserContextActions {

  @Operation(
    summary = "Comparison pairs",
    description = "Retrieves all comparison pairs. The suggested comparison pairs need to be generated first by running the 'ActiveLearning-ComparisonPairs' activity.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success"
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project or task has not been found."
      )
    )
  )
  @RequestBody(
    required = true,
    content = Array(
      new Content(
        mediaType = "application/json",
        schema = new Schema(implementation = classOf[ComparisonPairsFormat]),
      )
    )
  )
  def comparisonPairs(@Parameter(
                        name = "project",
                        description = "The project identifier",
                        required = true,
                        in = ParameterIn.PATH,
                        schema = new Schema(implementation = classOf[String])
                      )
                      projectId: String,
                      @Parameter(
                        name = "task",
                        description = "The task identifier",
                        required = true,
                        in = ParameterIn.PATH,
                        schema = new Schema(implementation = classOf[String])
                      )
                      taskId: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectId)
    val task = project.task[LinkSpec](taskId)
    val activity = task.activity[ComparisonPairGenerator]
    val json = Json.toJson(ComparisonPairsFormat(activity.value()))
    Ok(json)
  }

  @Operation(
    summary = "Add a comparison pair",
    description = "Adds a comparison pair to the list of selected pairs.",
    responses = Array(
      new ApiResponse(
        responseCode = "204",
        description = "Success"
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project or task has not been found."
      )
    )
  )
  @RequestBody(
    required = true,
    content = Array(
      new Content(
        mediaType = "application/json",
        schema = new Schema(implementation = classOf[ComparisonPairFormat]),
      )
    )
  )
  def addComparisonPair(@Parameter(
                          name = "project",
                          description = "The project identifier",
                          required = true,
                          in = ParameterIn.PATH,
                          schema = new Schema(implementation = classOf[String])
                        )
                        projectId: String,
                        @Parameter(
                          name = "task",
                          description = "The task identifier",
                          required = true,
                          in = ParameterIn.PATH,
                          schema = new Schema(implementation = classOf[String])
                        )
                        taskId: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val newComparisonPair = JsonUtils.validateJsonFromRequest[ComparisonPairFormat](request).toComparisonPair
    updateSelectedComparisonPairs(projectId, taskId)(_ :+ newComparisonPair)
    NoContent
  }

  @Operation(
    summary = "Remove a comparison pair",
    description = "Removes a comparison pair from the list of selected pairs.",
    responses = Array(
      new ApiResponse(
        responseCode = "204",
        description = "Success"
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project or task has not been found."
      )
    )
  )
  @RequestBody(
    required = true,
    content = Array(
      new Content(
        mediaType = "application/json",
        schema = new Schema(implementation = classOf[ComparisonPairFormat]),
      )
    )
  )
  def removeComparisonPair(@Parameter(
                             name = "project",
                             description = "The project identifier",
                             required = true,
                             in = ParameterIn.PATH,
                             schema = new Schema(implementation = classOf[String])
                           )
                           projectId: String,
                           @Parameter(
                             name = "task",
                             description = "The task identifier",
                             required = true,
                             in = ParameterIn.PATH,
                             schema = new Schema(implementation = classOf[String])
                           )
                           taskId: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val removeComparisonPair = JsonUtils.validateJsonFromRequest[ComparisonPairFormat](request).toComparisonPair
    updateSelectedComparisonPairs(projectId, taskId)(_.filterNot(_ == removeComparisonPair))
    NoContent
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
    val population = activeLearning.value().population
    if(population.isEmpty) {
      throw NotFoundException("No rule found.")
    } else {
      val bestRule = activeLearning.value().population.bestIndividual.node.build
      implicit val writeContext = WriteContext[JsValue]()
      Ok(LinkageRuleJsonFormat.write(bestRule))
    }
  }
}
