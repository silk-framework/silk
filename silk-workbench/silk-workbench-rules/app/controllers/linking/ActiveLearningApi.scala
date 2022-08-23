package controllers.linking

import akka.stream.Materializer
import controllers.core.UserContextActions
import controllers.core.util.JsonUtils
import controllers.linking.activeLearning.JsonFormats._
import controllers.linking.activeLearning.{ActiveLearningInfo, ActiveLearningIterator, ReferenceLinksStatistics}
import controllers.linking.evaluation.LinkageRuleEvaluationResult
import controllers.linking.linkingTask.LinkingTaskApiUtils
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.silkframework.config.Prefixes
import org.silkframework.entity.{FullLink, LinkDecision, ReferenceLink}
import org.silkframework.learning.active.ActiveLearning
import org.silkframework.learning.active.comparisons.{ComparisonPair, ComparisonPairGenerator}
import org.silkframework.rule.evaluation.ReferenceLinks
import org.silkframework.rule.{LinkSpec, LinkageRule}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.serialization.WriteContext
import org.silkframework.runtime.validation.{BadUserInputException, NotFoundException}
import org.silkframework.serialization.json.JsonSerializers.LinkageRuleJsonFormat
import org.silkframework.serialization.json.LinkingSerializers.LinkJsonFormat
import org.silkframework.workbench.Context
import org.silkframework.workspace.WorkspaceFactory
import play.api.libs.json.{JsArray, JsString, JsValue, Json}
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
    implicit val prefixes: Prefixes = project.config.prefixes
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
    val project = WorkspaceFactory().workspace.project(projectId)
    implicit val prefixes: Prefixes = project.config.prefixes
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
    val project = WorkspaceFactory().workspace.project(projectId)
    implicit val prefixes: Prefixes = project.config.prefixes
    val removeComparisonPair = JsonUtils.validateJsonFromRequest[ComparisonPairFormat](request).toComparisonPair.plain
    updateSelectedComparisonPairs(projectId, taskId)(_.filterNot(_.plain == removeComparisonPair))
    NoContent
  }

  @Operation(
    summary = "Status information",
    description = "General status information about the active learning session.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(new Content(
          mediaType = "application/json",
          schema = new Schema(implementation = classOf[ActiveLearningInfo])
        ))
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project or task has not been found."
      )
    )
  )
  def info(@Parameter(
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
           taskId: String): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
    val context = Context.get[LinkSpec](projectId, taskId, request.path)
    Ok(Json.toJson(ActiveLearningInfo(context.task)))
  }

  @Operation(
    summary = "Evaluate reference links",
    description = "The current reference links, evaluated on the top performing linkage rule.",
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
  def referenceLinks(@Parameter(
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
                     taskId: String,
                     @Parameter(
                       name = "includePositiveLinks",
                       description = "If true, positive reference links are included in the result",
                       required = false,
                       in = ParameterIn.QUERY,
                       schema = new Schema(implementation = classOf[Boolean], defaultValue = "true"),
                     )
                     includePositiveLinks: Boolean,
                     @Parameter(
                       name = "includeNegativeLinks",
                       description = "If true, negative reference links are included in the result",
                       required = false,
                       in = ParameterIn.QUERY,
                       schema = new Schema(implementation = classOf[Boolean], defaultValue = "true"),
                     )
                     includeNegativeLinks: Boolean,
                     @Parameter(
                       name = "includeUnlabeledLinks",
                       description = "If true, unlabeled link candidates are included in the result",
                       required = false,
                       in = ParameterIn.QUERY,
                       schema = new Schema(implementation = classOf[Boolean], defaultValue = "false"),
                     )
                     includeUnlabeledLinks: Boolean,
                     @Parameter(
                       name = "offset",
                       description = "Result offset",
                       required = false,
                       in = ParameterIn.QUERY,
                       schema = new Schema(implementation = classOf[Int], defaultValue = "0"),
                     )
                     offset: Int,
                     @Parameter(
                       name = "limit",
                       description = "Maximum number of results",
                       required = false,
                       in = ParameterIn.QUERY,
                       schema = new Schema(implementation = classOf[Int], defaultValue = "100"),
                     )
                     limit: Int,
                     @Parameter(
                       name = "withEntitiesAndSchema",
                       description = "If set to true each link contains the entities and the schema",
                       required = false,
                       in = ParameterIn.QUERY,
                       schema = new Schema(implementation = classOf[Boolean], defaultValue = "false"),
                     )
                     withEntitiesAndSchema: Boolean): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
      val context = Context.get[LinkSpec](projectId, taskId, request.path)
      val activeLearning = context.task.activity[ActiveLearning].value()

      val allReferenceLinks = activeLearning.referenceData.referenceLinks

      // Select Links
      var selectedLinks = Seq[ReferenceLink]()
      selectedLinks ++= allReferenceLinks.filter(l => (includePositiveLinks && l.decision == LinkDecision.POSITIVE) ||
                                                      (includeNegativeLinks && l.decision == LinkDecision.NEGATIVE))
      if(includeUnlabeledLinks) {
        selectedLinks ++= activeLearning.referenceData.linkCandidates
      }
      selectedLinks = selectedLinks.slice(offset, offset + limit)

      // TODO remove evaluation score from this as it has been moved to the bestRule endpoint
      val (bestRule, evaluationScore): (LinkageRule, Option[LinkageRuleEvaluationResult]) = {
        val population = activeLearning.population
        if(population.isEmpty) {
          (LinkageRule(), None)
        } else {
          val bestRule = population.bestIndividual.node.build
          val evaluationResult: LinkageRuleEvaluationResult = LinkingTaskApiUtils.referenceLinkEvaluationScore(bestRule, allReferenceLinks)
          (bestRule, Some(evaluationResult))
        }
      }

    // TODO remove statistics because it has been added to the info endpoint
      val statistics = ReferenceLinksStatistics.compute(context.task.referenceLinks, allReferenceLinks)

      var result =
        Json.obj(
          "statistics" -> Json.toJson(statistics),
          "links" -> serializeLinks(selectedLinks, bestRule, withEntitiesAndSchema, distinctValues = true)
        )
      evaluationScore.foreach(score => {
        result = result ++ Json.obj(
          "evaluationScore" -> Json.toJson(score)
        )
      })

      Ok(result)
  }

  private def serializeLinks(links: Seq[ReferenceLink],
                             linkageRule: LinkageRule,
                             withEntitiesAndSchema: Boolean,
                             distinctValues: Boolean): JsValue = {
    implicit val writeContext: WriteContext[JsValue] = WriteContext[JsValue]()
    JsArray(
      for (link <- links) yield {
        val entities = link.linkEntities
        val fullLink = new FullLink(entities.source.uri, entities.target.uri, linkageRule(entities), entities)
        val linkJson = new LinkJsonFormat(Some(linkageRule), writeEntities = withEntitiesAndSchema,
          writeEntitySchema = withEntitiesAndSchema, distinctValues).write(fullLink)
        linkJson + ("decision" -> JsString(link.decision.getId))
      }
    )
  }

  @Operation(
    summary = "Add a new reference link",
    description = "Adds a new reference link.",
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
  def addReferenceLink(@Parameter(
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
                       taskId: String,
                       @Parameter(
                         name = "linkSource",
                         description = "The URI of the link source.",
                         required = true,
                         in = ParameterIn.QUERY,
                         schema = new Schema(implementation = classOf[String])
                       )
                       linkSource: String,
                       @Parameter(
                         name = "linkTarget",
                         description = "The URI of the link target.",
                         required = true,
                         in = ParameterIn.QUERY,
                         schema = new Schema(implementation = classOf[String])
                       )
                       linkTarget: String,
                       @Parameter(
                         name = "decision",
                         description = "The new label of the link.",
                         required = true,
                         in = ParameterIn.QUERY,
                         schema = new Schema(implementation = classOf[String], allowableValues = Array("positive", "negative", "unlabeled"))
                       )
                       decision: String,
                       @Parameter(
                         name = "synchronous",
                         description = "If true, the endpoint will block until a new iteration has been finished that considers the added reference link.",
                         required = true,
                         in = ParameterIn.QUERY,
                         schema = new Schema(implementation = classOf[Boolean], required = false, defaultValue = "false")
                       )
                       synchronous: Boolean): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
    val context = Context.get[LinkSpec](projectId, taskId, request.path)
    ActiveLearningIterator.commitLink(linkSource, linkTarget, decision, context.task, synchronous)
    NoContent
  }

  @Operation(
    summary = "Next link candidate",
    description = "Retrieves the next link candidate.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success"
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If there are no more link candidates or if the specified project or task has not been found."
      )
    )
  )
  def nextLinkCandidate(@Parameter(
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
                        taskId: String): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
    val context = Context.get[LinkSpec](projectId, taskId, request.path)
    val linkCandidate = ActiveLearningIterator.nextLinkCandidate(context.task)

    implicit val writeContext = WriteContext[JsValue]()
    val format = new LinkJsonFormat(rule = None, writeEntities = true, writeEntitySchema = true, distinctValues = true)
    Ok(format.write(linkCandidate))
  }

  @Operation(
    summary = "Best linkage rule",
    description = "Retrieves the best linkage rule from the population.",
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
  def bestRule(@Parameter(
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
               taskId: String): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
    val context = Context.get[LinkSpec](projectId, taskId, request.path)
    val activeLearning = context.task.activity[ActiveLearning]
    val activeLearningValue = activeLearning.value()
    val population = activeLearningValue.population
    if(population.isEmpty) {
      throw NotFoundException("No rule found.")
    } else {
      val bestRule = population.bestIndividual.node.build
      val links = activeLearningValue.referenceData.referenceLinks
      val score: LinkageRuleEvaluationResult = LinkingTaskApiUtils.referenceLinkEvaluationScore(bestRule, links)

      implicit val writeContext = WriteContext[JsValue]()
      var ruleJson = LinkageRuleJsonFormat.write(bestRule)
      ruleJson += ("evaluationResult" -> Json.toJson(score))
      Ok(ruleJson)
    }
  }

  @Operation(
    summary = "Save result",
    description = "Saves the result of the current active learning session.",
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
  def saveResult(@Parameter(
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
                 taskId: String,
                 @Parameter(
                   name = "saveRule",
                   description = "If true, the current linkage rule will be updated to the best learned rule.",
                   required = true,
                   in = ParameterIn.QUERY,
                   schema = new Schema(implementation = classOf[Boolean])
                 )
                 saveRule: Boolean,
                 @Parameter(
                   name = "saveReferenceLinks",
                   description = "If true, the current reference rules will be updated.",
                   required = true,
                   in = ParameterIn.QUERY,
                   schema = new Schema(implementation = classOf[Boolean])
                 )
                 saveReferenceLinks: Boolean): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
    val context = Context.get[LinkSpec](projectId, taskId, request.path)
    val activeLearning = context.task.activity[ActiveLearning]
    val result = activeLearning.value()

    if(saveRule) {
      if (result.population.isEmpty) {
        throw BadUserInputException("Active learning did not generate a rule")
      }
      context.task.update(context.task.data.copy(rule = result.population.bestIndividual.node.build))
    }

    if(saveReferenceLinks) {
      val referenceLinks =
        ReferenceLinks(
          positive = result.referenceData.positiveLinks.toSet,
          negative = result.referenceData.negativeLinks.toSet
        )
      context.task.update(context.task.data.copy(referenceLinks = referenceLinks))
    }

    NoContent
  }

  @Operation(
    summary = "Reset active learning",
    description = "Wipes the current active learning session.",
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
  def reset(@Parameter(
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
           taskId: String): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
    val context = Context.get[LinkSpec](projectId, taskId, request.path)
    val comparisonPair = context.task.activity[ComparisonPairGenerator]
    val activeLearning = context.task.activity[ActiveLearning]

    comparisonPair.control.reset()
    activeLearning.control.reset()

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
}
