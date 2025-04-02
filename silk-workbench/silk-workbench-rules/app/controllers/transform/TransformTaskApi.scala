package controllers.transform

import controllers.core.UserContextActions
import controllers.transform.TransformTaskApi._
import controllers.transform.doc.TransformTaskApiDoc
import controllers.util.ProjectUtils._
import controllers.util.SerializationUtils._
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, ExampleObject, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.silkframework.config.{MetaData, Prefixes, Task}
import org.silkframework.dataset._
import org.silkframework.entity._
import org.silkframework.rule.TransformSpec.{TargetVocabularyListParameter, TargetVocabularyParameterType}
import org.silkframework.rule._
import org.silkframework.rule.execution.ExecuteTransform
import org.silkframework.rule.util.UriPatternParser.UriPatternParserException
import org.silkframework.runtime.activity.{Activity, UserContext}
import org.silkframework.runtime.resource.ResourceManager
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.runtime.templating.{GlobalTemplateVariables, TemplateVariablesReader}
import org.silkframework.runtime.validation.{BadUserInputException, NotFoundException, ValidationError, ValidationException}
import org.silkframework.serialization.json.JsonParseException
import org.silkframework.serialization.json.JsonSerializers._
import org.silkframework.util.{Identifier, IdentifierGenerator, Uri}
import org.silkframework.workbench.utils.{ErrorResult, UnsupportedMediaTypeException}
import org.silkframework.workspace.activity.transform.TransformPathsCache
import org.silkframework.workspace.{Project, ProjectTask, WorkspaceFactory}
import play.api.libs.json._
import play.api.mvc._

import java.util.logging.{Level, Logger}
import javax.inject.Inject

@Tag(
  name = "Transform",
  description = "Endpoints related to transformation tasks and mapping rules."
)
class TransformTaskApi @Inject() () extends InjectedController with UserContextActions {

  private val log = Logger.getLogger(getClass.getName)

  @Operation(
    summary = "Retrieve Transform Task",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(
          new Content(
            mediaType = "application/json"
          ),
          new Content(
            mediaType = "application/xml"
        ))
    ))
  )
  def getTransformTask(@Parameter(
                         name = "project",
                         description = "The project identifier",
                         required = true,
                         in = ParameterIn.PATH,
                         schema = new Schema(implementation = classOf[String])
                       )
                       projectName: String,
                       @Parameter(
                         name = "task",
                         description = "The task identifier",
                         required = true,
                         in = ParameterIn.PATH,
                         schema = new Schema(implementation = classOf[String])
                       )
                       taskName: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    implicit val (project, task) = getProjectAndTask[TransformSpec](projectName, taskName)
    implicit val prefixes: Prefixes = project.config.prefixes

    serializeCompileTime[TransformTask](task, Some(project))
  }

  @Operation(
    summary = "Update or create a transform task",
    responses = Array(
      new ApiResponse(
        responseCode = "200"
      )
    )
  )
  def putTransformTask(@Parameter(
                        name = "project",
                        description = "The project identifier",
                        required = true,
                        in = ParameterIn.PATH,
                        schema = new Schema(implementation = classOf[String])
                      )
                      projectName: String,
                      @Parameter(
                        name = "task",
                        description = "The task identifier",
                        required = true,
                        in = ParameterIn.PATH,
                        schema = new Schema(implementation = classOf[String])
                      )
                      taskName: String,
                      @Parameter(
                        name = "createOnly",
                        description = "Always create new transform",
                        required = false,
                        in = ParameterIn.QUERY,
                        schema = new Schema(implementation = classOf[Boolean], defaultValue = "false")
                      )
                      createOnly: Boolean): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val project = getProject(projectName)
    implicit val prefixes: Prefixes = project.config.prefixes
    implicit val readContext: ReadContext = ReadContext.fromProject(project)

    request.body match {
      case AnyContentAsFormUrlEncoded(v) =>
        val values = request.body.asFormUrlEncoded.getOrElse(Map.empty).view.mapValues(_.mkString).toMap
        val input = DatasetSelection(values("source"), Uri.parse(values.getOrElse("sourceType", ""), prefixes),
          Restriction.custom(values.getOrElse("restriction", "")))
        val output = values.get("output").filter(_.nonEmpty).map(Identifier(_))
        val targetVocabularies = values.get("targetVocabularies") match {
          case Some(v) => TargetVocabularyParameterType.fromString(v)
          case None => TargetVocabularyListParameter(Seq.empty)
        }

        project.tasks[TransformSpec].find(_.id.toString == taskName) match {
          //Update existing task
          case Some(oldTask) if !createOnly =>
            val updatedTransformSpec = oldTask.data.copy(selection = input, output = output, targetVocabularies = targetVocabularies)
            project.updateTask(taskName, updatedTransformSpec)
          //Create new task with no rule
          case _ =>
            val rule = RootMappingRule(rules = MappingRules.empty)
            val transformSpec = TransformSpec(input, rule, output, None, targetVocabularies)
            project.addTask(taskName, transformSpec, MetaData.empty)
        }

        Ok
      case _ =>
        catchExceptions {
          deserializeCompileTime[TransformTask]() { task =>
            project.updateTask(task.id, task.data, Some(task.metaData))
            Ok
          }
        }
    }
  }

  @Operation(
    summary = "Delete a transform task",
    responses = Array(
      new ApiResponse(
        responseCode = "200"
      )
    )
  )
  def deleteTransformTask(@Parameter(
                            name = "project",
                            description = "The project identifier",
                            required = true,
                            in = ParameterIn.PATH,
                            schema = new Schema(implementation = classOf[String])
                          )
                          projectName: String,
                          @Parameter(
                            name = "task",
                            description = "The task identifier",
                            required = true,
                            in = ParameterIn.PATH,
                            schema = new Schema(implementation = classOf[String])
                          )
                          taskName: String,
                          @Parameter(
                            name = "removeDependentTasks",
                            description = "If true, transform and linking tasks that directly reference this task are removed as well.",
                            required = true,
                            in = ParameterIn.QUERY,
                            schema = new Schema(implementation = classOf[Boolean], defaultValue = "false")
                          )
                          removeDependentTasks: Boolean): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = getProject(projectName)
    project.removeAnyTask(taskName, removeDependentTasks)

    Ok
  }

  @Operation(
    summary = "Retrieve all mapping rules",
    description = "Get all mapping rules of the transformation task. If no accept header is defined, XML is returned.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(
          new Content(
            mediaType = "application/json",
            examples = Array(new ExampleObject(TransformTaskApiDoc.transformRulesJsonExample))
          ),
          new Content(
            mediaType = "application/xml",
            examples = Array(new ExampleObject(TransformTaskApiDoc.transformRulesXmlExample))
          ))
      ))
  )
  def getRules(@Parameter(
                 name = "project",
                 description = "The project identifier",
                 required = true,
                 in = ParameterIn.PATH,
                 schema = new Schema(implementation = classOf[String])
               )
               projectName: String,
               @Parameter(
                 name = "task",
                 description = "The task identifier",
                 required = true,
                 in = ParameterIn.PATH,
                 schema = new Schema(implementation = classOf[String])
               )
               taskName: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    implicit val (project, task) = getProjectAndTask[TransformSpec](projectName, taskName)
    implicit val prefixes: Prefixes = project.config.prefixes

    serializeCompileTime(task.data.mappingRule, Some(project))
  }

  @Operation(
    summary = "Update all mapping rules",
    description = "Update all rules of a transform specification. As for GET XML and JSON are supported. The format for PUT is exactly the same as the result that is returned by a GET request.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "The rules were successfully updated. There is no response body."
      ),
      new ApiResponse(
        responseCode = "400",
        description = "If the provided rule serialization is invalid."
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If no rule with the given identifier could be found."
      )
    )
  )
  @RequestBody(
    content = Array(
      new Content(
        mediaType = "application/json"
      ),
      new Content(
        mediaType = "application/xml"
      )
    )
  )
  def putRules(@Parameter(
                 name = "project",
                 description = "The project identifier",
                 required = true,
                 in = ParameterIn.PATH,
                 schema = new Schema(implementation = classOf[String])
               )
               projectName: String,
               @Parameter(
                 name = "task",
                 description = "The task identifier",
                 required = true,
                 in = ParameterIn.PATH,
                 schema = new Schema(implementation = classOf[String])
               )
               taskName: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    implicit val (project, task) = getProjectAndTask[TransformSpec](projectName, taskName)
    implicit val prefixes: Prefixes = project.config.prefixes
    implicit val resources: ResourceManager = project.resources
    implicit val readContext: ReadContext = ReadContext(resources, prefixes, validationEnabled = true)

    catchExceptions {
      task.synchronized {
        handleValidationExceptions {
          deserializeCompileTime[RootMappingRule]() { updatedRules =>
            //Update transformation task
            val updatedTask = task.data.copy(mappingRule = updatedRules)
            project.updateTask(taskName, updatedTask)
            Ok
          }
        }
      }
    }
  }

  @Operation(
    summary = "Retrieve mapping rule",
    description = "Retrieve a single mapping rule from a transform task.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(
          new Content(
            mediaType = "application/json",
            examples = Array(new ExampleObject(TransformTaskApiDoc.transformRuleJsonExample))
          ),
          new Content(
            mediaType = "application/xml",
            examples = Array(new ExampleObject(TransformTaskApiDoc.transformRuleXmlExample))
          ))
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project, task or rule has not been found."
      )
    )
  )
  def getRule(@Parameter(
                name = "project",
                description = "The project identifier",
                required = true,
                in = ParameterIn.PATH,
                schema = new Schema(implementation = classOf[String])
              )
              projectName: String,
              @Parameter(
                name = "task",
                description = "The task identifier",
                required = true,
                in = ParameterIn.PATH,
                schema = new Schema(implementation = classOf[String])
              )
              taskName: String,
              @Parameter(
                name = "rule",
                description = "The rule identifier",
                required = true,
                in = ParameterIn.PATH,
                schema = new Schema(implementation = classOf[String])
              )
              ruleId: String,
              @Parameter(
                name = "convertToComplex",
                description = "If set to true then value transform rules will always be converted to complex rules.",
                required = false,
                in = ParameterIn.QUERY,
                schema = new Schema(implementation = classOf[Boolean]))
              convertToComplex: Option[Boolean]): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
      implicit val (project, task) = getProjectAndTask[TransformSpec](projectName, taskName)
      implicit val prefixes: Prefixes = project.config.prefixes
      processRule(task, ruleId) { rule =>
        if (convertToComplex.getOrElse(false)) {
          rule.operator match {
            case valueRule: ValueTransformRule =>
              serializeCompileTime(valueRule.asComplexMapping, Some(project))
            case operator: Operator =>
              serializeCompileTime(operator.asInstanceOf[TransformRule], Some(project))
          }
        } else {
          serializeCompileTime(rule.operator.asInstanceOf[TransformRule], Some(project))
        }
      }
  }

  @Operation(
    summary = "Update mapping rule",
    description = "Updates a rule or parts of a rule. The XML and JSON format is the same as returned by the corresponding GET endpoint. For json payloads, the caller may send a fragment that only specifies the parts of the rule that should be updated. The parts that are not sent in the request will remain unchanged.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(
          new Content(
            mediaType = "application/json",
            schema = new Schema(`type` = "object"),
            examples = Array(new ExampleObject(TransformTaskApiDoc.updateRuleResponseExample))
          ))
      ),
      new ApiResponse(
        responseCode = "400",
        description = "If the provided rule serialization is invalid."
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project, task or rule has not been found."
      )
    )
  )
  @RequestBody(
    content = Array(
      new Content(
        mediaType = "application/json",
        schema = new Schema(`type` = "object"),
        examples = Array(new ExampleObject(TransformTaskApiDoc.updateRuleRequestExample))
      )
    )
  )
  def putRule(@Parameter(
                name = "project",
                description = "The project identifier",
                required = true,
                in = ParameterIn.PATH,
                schema = new Schema(implementation = classOf[String])
              )
              projectName: String,
              @Parameter(
                name = "task",
                description = "The task identifier",
                required = true,
                in = ParameterIn.PATH,
                schema = new Schema(implementation = classOf[String])
              )
              taskName: String,
              @Parameter(
                name = "rule",
                description = "The rule identifier",
                required = true,
                in = ParameterIn.PATH,
                schema = new Schema(implementation = classOf[String])
              )
              ruleId: String): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
    implicit val (project, task) = getProjectAndTask[TransformSpec](projectName, taskName)
    implicit val prefixes: Prefixes = project.config.prefixes
    implicit val resources: ResourceManager = project.resources
    implicit val readContext: ReadContext = ReadContext.fromProject(project).copy(identifierGenerator = identifierGenerator(task), validationEnabled = true)

    task.synchronized {
      processRule(task, ruleId) { currentRule =>
        handleValidationExceptions {
          implicit val writeContext: WriteContext[JsValue] = WriteContext.fromProject[JsValue](project)
          implicit val updatedRequest: Request[AnyContent] = updateJsonRequest(request, currentRule)
          deserializeCompileTime[TransformRule]() { updatedRule =>
            updateRule(currentRule.update(updatedRule))
            serializeCompileTime[TransformRule](updatedRule, Some(project))
          }
        }
      }
    }
  }

  @Operation(
    summary = "Delete mapping rule",
    description = "Delete the rule that is identified by the given id.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(
          new Content(
            mediaType = "application/json"
          ))
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project, task or rule has not been found."
      )
    )
  )
  def deleteRule(@Parameter(
                   name = "project",
                   description = "The project identifier",
                   required = true,
                   in = ParameterIn.PATH,
                   schema = new Schema(implementation = classOf[String])
                 )
                 projectName: String,
                 @Parameter(
                   name = "task",
                   description = "The task identifier",
                   required = true,
                   in = ParameterIn.PATH,
                   schema = new Schema(implementation = classOf[String])
                 )
                 taskName: String,
                 @Parameter(
                   name = "rule",
                   description = "The rule identifier",
                   required = true,
                   in = ParameterIn.PATH,
                   schema = new Schema(implementation = classOf[String])
                 )
                 rule: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    implicit val (project, task) = getProjectAndTask[TransformSpec](projectName, taskName)
    implicit val prefixes: Prefixes = project.config.prefixes

    try {
      task.synchronized {
        val updatedTree = RuleTraverser(task.data.mappingRule).remove(rule)
        task.update(task.data.copy(mappingRule = updatedTree.operator.asInstanceOf[RootMappingRule]))
        Ok
      }
    } catch {
      case ex: NoSuchElementException =>
        ErrorResult(NotFoundException(ex))
    }
  }

  @Operation(
    summary = "Append mapping rule",
    description = "Appends a new child rule to an object mapping rule.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "The rule has been updated successfully. The appended rule is returned. In case the caller did not specify an identifier for the appended rule, the result will contain the generated identifier.",
        content = Array(
          new Content(
            mediaType = "application/json",
            schema = new Schema(`type` = "object"),
            examples = Array(new ExampleObject(TransformTaskApiDoc.appendRuleResponseExample))
          ))
      ),
      new ApiResponse(
        responseCode = "400",
        description = "If the provided rule serialization is invalid."
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project, task or rule has not been found."
      )
    )
  )
  @RequestBody(
    content = Array(
      new Content(
        mediaType = "application/json",
        schema = new Schema(`type` = "object"),
        examples = Array(new ExampleObject(TransformTaskApiDoc.appendRuleRequestExample))
      )
    )
  )
  def appendRule(@Parameter(
                   name = "project",
                   description = "The project identifier",
                   required = true,
                   in = ParameterIn.PATH,
                   schema = new Schema(implementation = classOf[String])
                 )
                 projectName: String,
                 @Parameter(
                   name = "task",
                   description = "The task identifier",
                   required = true,
                   in = ParameterIn.PATH,
                   schema = new Schema(implementation = classOf[String])
                 )
                 taskName: String,
                 @Parameter(
                   name = "rule",
                   description = "The rule identifier",
                   required = true,
                   in = ParameterIn.PATH,
                   schema = new Schema(implementation = classOf[String])
                 )
                 ruleName: String,
                 @Parameter(
                   name = "afterRuleId",
                   description = "Optional parameter that specified after which existing rule the new rule should be inserted.",
                   required = false,
                   in = ParameterIn.QUERY,
                   schema = new Schema(implementation = classOf[String])
                 )
                 afterRuleId: Option[String] = None): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    implicit val (project, task) = getProjectAndTask[TransformSpec](projectName, taskName)
    task.synchronized {
      implicit val readContext: ReadContext = ReadContext(project.resources, project.config.prefixes, identifierGenerator(task), validationEnabled = true)
      processRule(task, ruleName) { parentRule =>
        handleValidationExceptions {
          deserializeCompileTime[TransformRule]() { newChildRule =>
            addRuleToTransformTask(parentRule, newChildRule, afterRuleId)
          }
        }
      }
    }
  }

  // Handles exceptions thrown by transform rule validation
  private def handleValidationExceptions[T](block: => T): T = {
    try {
      block
    } catch {
      case ex: UriPatternParserException =>
        throw BadUserInputException("Invalid URI pattern found. Details: " + ex.getMessage, None)
    }
  }

  private def addRuleToTransformTask(parentRule: RuleTraverser,
                                     newChildRule: TransformRule,
                                     afterRuleId: Option[String])
                                    (implicit request: Request[AnyContent],
                                     task: ProjectTask[TransformSpec],
                                     userContext: UserContext,
                                     project: Project): Result = {
    if (task.data.nestedRuleAndSourcePath(newChildRule.id).isDefined) {
      throw new ValidationException(s"Rule with ID ${newChildRule.id} already exists!")
    }
    val children = parentRule.operator.children
    val newChildren = children.indexWhere(rule => afterRuleId.contains(rule.id.toString)) match {
      case afterRuleIdx: Int if afterRuleIdx >= 0 =>
        val (before, after) = children.splitAt(afterRuleIdx + 1)
        (before :+ newChildRule) ++ after // insert after specified rule
      case -1 => // append
        children :+ newChildRule
    }
    val updatedRule = parentRule.operator.withChildren(newChildren)
    updateRule(parentRule.update(updatedRule))
    serializeCompileTime(newChildRule, Some(project))
  }

  private def assignNewIdsAndLabelToRule(task: ProjectTask[TransformSpec],
                                 ruleToCopy: RuleTraverser): TransformRule = {
    implicit val idGenerator: IdentifierGenerator = identifierGenerator(task)
    ruleToCopy.operator match {
      case t: TransformRule =>
        implicit val prefixes: Prefixes = task.project.config.prefixes
        val newLabel = "Copy of " + t.fullLabel
        val transformRuleCopy = assignNewIdsToRule(t)
        transformRuleCopy.withMetaData(t.metaData.copy(label = Some(newLabel)))
      case other: Operator => throw new RuntimeException("Selected operator was not transform rule. Operator ID: " + other.id)
    }
  }

  private def assignNewIdsToRule(t: TransformRule)
                                (implicit idGenerator: IdentifierGenerator): TransformRule = {
    t match {
      case r: RootMappingRule =>
        val updatedMappingRules = assignNewIdsToMappingRules(r.rules)
        r.copy(id = idGenerator.generate(r.id), rules = updatedMappingRules)
      case c: ComplexMapping => c.copy(id = idGenerator.generate(c.id))
      case c: ComplexUriMapping => c.copy(id = idGenerator.generate(c.id))
      case d: DirectMapping => d.copy(id = idGenerator.generate(d.id))
      case o: ObjectMapping =>
        val updatedMappingRules = assignNewIdsToMappingRules(o.rules)
        o.copy(id = idGenerator.generate(o.id), rules = updatedMappingRules)
      case typeMapping: TypeMapping => assignNewIdsToRule(typeMapping)
      case uriMapping: UriMapping => assignNewIdsToRule(uriMapping)
    }
  }

  private def assignNewIdsToMappingRules(mappingRules: MappingRules)
                                        (implicit identifierGenerator: IdentifierGenerator): MappingRules = {
    mappingRules.copy(
      uriRule = mappingRules.uriRule.map(assignNewIdsToRule),
      typeRules = mappingRules.typeRules.map(assignNewIdsToRule),
      propertyRules = mappingRules.propertyRules.map(assignNewIdsToRule)
    )
  }

  private def assignNewIdsToRule(typeMapping: TypeMapping)
                                (implicit idGenerator: IdentifierGenerator): TypeMapping = {
    typeMapping.copy(id = idGenerator.generate(typeMapping.id))
  }

  private def assignNewIdsToRule(uriMapping: UriMapping)
                                (implicit idGenerator: IdentifierGenerator): UriMapping = {
    uriMapping match {
      case c: ComplexUriMapping =>
        c.copy(id = idGenerator.generate(c.id))
      case p: PatternUriMapping =>
        p.copy(id = idGenerator.generate(p.id))
    }
  }

  /** Converts a root mapping rule to an object mapping rule. */
  private def convertRootMappingRule(rule: TransformRule): TransformRule = {
    rule match {
      case RootMappingRule(rules, id, target, metaData) =>
        ObjectMapping(id, rules = rules, metaData = metaData, target = Some(target.copy(propertyUri = ROOT_COPY_TARGET_PROPERTY)))
      case other: TransformRule =>
        other
    }
  }

  @Operation(
    summary = "Copy mapping rule",
    description = "Copy a rule from the source transformation task specified by the query parameters and inserts it into the given target transform task specified by the path parameters.",
    responses = Array(
      new ApiResponse(
        responseCode = "200"
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project, task or rule has not been found."
      )
    )
  )
  def copyRule(@Parameter(
                 name = "project",
                 description = "The project identifier",
                 required = true,
                 in = ParameterIn.PATH,
                 schema = new Schema(implementation = classOf[String])
               )
               projectName: String,
               @Parameter(
                 name = "task",
                 description = "The task identifier",
                 required = true,
                 in = ParameterIn.PATH,
                 schema = new Schema(implementation = classOf[String])
               )
               taskName: String,
               @Parameter(
                 name = "rule",
                 description = "The rule identifier",
                 required = true,
                 in = ParameterIn.PATH,
                 schema = new Schema(implementation = classOf[String])
               )
               ruleName: String,
               @Parameter(
                 name = "sourceProject",
                 description = "The identifier of the source project from the workspace that contains the source transform task from which a rule should be copied from.",
                 required = true,
                 in = ParameterIn.QUERY,
                 schema = new Schema(implementation = classOf[String])
               )
               sourceProject: String,
               @Parameter(
                 name = "sourceTask",
                 description = "The identifier of the source task the rule should be copied from.",
                 required = true,
                 in = ParameterIn.QUERY,
                 schema = new Schema(implementation = classOf[String])
               )
               sourceTask: String,
               @Parameter(
                 name = "sourceRule",
                 description = "The identifier of the source rule that should be copied to the target transform task.",
                 required = true,
                 in = ParameterIn.QUERY,
                 schema = new Schema(implementation = classOf[String])
               )
               sourceRule: String,
               @Parameter(
                 name = "afterRuleId",
                 description = "Optional parameter that specified after which existing rule the new rule should be inserted.",
                 required = false,
                 in = ParameterIn.QUERY,
                 schema = new Schema(implementation = classOf[String])
               )
               afterRuleId: Option[String] = None): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    implicit val (project, task) = getProjectAndTask[TransformSpec](projectName, taskName)
    val (_, fromTask) = getProjectAndTask[TransformSpec](sourceProject, sourceTask)
    implicit val prefixes: Prefixes = project.config.prefixes
    task.synchronized {
      implicit val readContext: ReadContext = ReadContext(project.resources, project.config.prefixes, identifierGenerator(task))
      processRule(fromTask, sourceRule) { ruleToCopy =>
        processRule(task, ruleName) { parentRule =>
          val newChildRule = convertRootMappingRule(assignNewIdsAndLabelToRule(task, ruleToCopy))
          addRuleToTransformTask(parentRule, newChildRule, afterRuleId)
        }
      }
    }
  }

  @Operation(
    summary = "Reorder mapping rules",
    description = "Reorder all child rules of an object mapping.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "The rules have been successfully reordered. The new ordered list of rules is returned.",
        content = Array(new Content(
          mediaType = "application/json",
          schema = new Schema(`type` = "object"),
          examples = Array(new ExampleObject("[ \"objectRule\", \"directRule\" ]"))
        ))
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project, task or rule has not been found."
      )
    )
  )
  @RequestBody(
    content = Array(
      new Content(
        mediaType = "application/json",
        schema = new Schema(`type` = "object"),
        examples = Array(new ExampleObject("[ \"objectRule\", \"directRule\" ]"))
      )
    )
  )
  def reorderRules(@Parameter(
                     name = "project",
                     description = "The project identifier",
                     required = true,
                     in = ParameterIn.PATH,
                     schema = new Schema(implementation = classOf[String])
                   )
                   projectName: String,
                   @Parameter(
                     name = "task",
                     description = "The task identifier",
                     required = true,
                     in = ParameterIn.PATH,
                     schema = new Schema(implementation = classOf[String])
                   )
                   taskName: String,
                   @Parameter(
                     name = "rule",
                     description = "The rule identifier",
                     required = true,
                     in = ParameterIn.PATH,
                     schema = new Schema(implementation = classOf[String])
                   )
                   ruleName: String): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
    implicit val (project, task) = getProjectAndTask[TransformSpec](projectName, taskName)
    implicit val prefixes: Prefixes = project.config.prefixes

    task.synchronized {
      processRule(task, ruleName) { parentRule =>
        request.body.asJson match {
          case Some(json) =>
            val currentRules = parentRule.operator.asInstanceOf[TransformRule].rules
            val currentOrder = currentRules.propertyRules.map(_.id.toString).toList
            val newOrder = json.as[JsArray].value.map(_.as[JsString].value).toList
            if (newOrder.toSet == currentOrder.toSet) {
              val newPropertyRules =
                for (id <- newOrder) yield {
                  parentRule.operator.children.find(_.id == id).get
                }
              val newRules = currentRules.uriRule.toSeq ++ currentRules.typeRules ++ newPropertyRules
              updateRule(parentRule.update(parentRule.operator.withChildren(newRules)))
              Ok(JsArray(newPropertyRules.map(r => JsString(r.id))))
            } else {
              ErrorResult(BadUserInputException(s"Provided list $newOrder does not contain the same elements as current list $currentOrder."))
            }
          case None =>
            ErrorResult(UnsupportedMediaTypeException.supportedFormats("application/json."))
        }
      }
    }
  }

  /**
    * Processes a rule a catches relevant exceptions
    */
  private def processRule(task: Task[TransformSpec], ruleId: String)(processFunc: RuleTraverser => Result): Result = {
    RuleTraverser(task.data.mappingRule).find(ruleId) match {
      case Some(rule) =>
        catchExceptions(processFunc(rule))
      case None =>
        ErrorResult(NotFoundException(s"No rule with id '$ruleId' found!"))
    }
  }

  /**
    * Catches relevant exceptions and returns appropriate error codes.
    */
  private def catchExceptions(func: => Result): Result = {
    try {
      func
    } catch {
      case ex: BadUserInputException =>
        log.log(Level.FINE, "Invalid transformation rule", ex)
        ErrorResult.validation(BAD_REQUEST, ex.getMessage, ValidationError(ex.getMessage) :: Nil)
      case ex: ValidationException =>
        log.log(Level.INFO, "Invalid transformation rule", ex)
        ErrorResult.validation(BAD_REQUEST, "Invalid transformation rule", ex.errors)
      case ex: JsonParseException =>
        log.log(Level.INFO, "Invalid transformation rule JSON", ex)
        ErrorResult(BadUserInputException(ex))
      case ex: Exception =>
        log.log(Level.WARNING, "Failed process mapping rule", ex)
        ErrorResult.validation(INTERNAL_SERVER_ERROR, "Failed to process mapping rule", ValidationError("Error in back end: " + ex.getMessage) :: Nil)
    }
  }

  private def identifierGenerator(transformTask: Task[TransformSpec]): IdentifierGenerator = {
    TransformSpec.identifierGenerator(transformTask.data)
  }

  private def updateJsonRequest(request: Request[AnyContent], rule: RuleTraverser)
                               (implicit writeContext: WriteContext[JsValue]): Request[AnyContent] = {
    request.body.asJson match {
      case Some(requestJson) =>
        val ruleJson = toJson(rule.operator.asInstanceOf[TransformRule]).as[JsObject]
        val updatedJson = ruleJson.deepMerge(requestJson.as[JsObject])
        request.map(_ => AnyContentAsJson(updatedJson))
      case None => request
    }
  }

  private def updateRule(ruleTraverser: RuleTraverser)
                        (implicit task: ProjectTask[TransformSpec],
                         userContext: UserContext): Unit = {
    val updatedRoot = ruleTraverser.root.operator.asInstanceOf[RootMappingRule]
    val updatedTask = task.data.copy(mappingRule = updatedRoot)
    task.project.updateTask(task.id, updatedTask)
  }

  def reloadTransformCache(projectName: String, taskName: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.task[TransformSpec](taskName)
    task.activity[TransformPathsCache].control.reset()
    task.activity[TransformPathsCache].control.start()
    Ok
  }

  def executeTransformTask(projectName: String, taskName: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.task[TransformSpec](taskName)
    val activity = task.activity[ExecuteTransform].control
    activity.start()
    Ok
  }

  @Operation(
    summary = "Execute transform with payload",
    description = "Execute a specific transformation task against input data from the POST body.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "If no sink is specified in the request then return results in N-Triples format with the response, else write triples to defined data sink.",
        content = Array(new Content(
          mediaType = "application/n-triples",
          examples = Array(new ExampleObject("<http://uri1> <http://xmlns.com/foaf/0.1/name> \"John Doe\"@en ."))
        ))
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project or task has not been found."
      )
    )
  )
  @RequestBody(
    content = Array(
      new Content(
        mediaType = "application/xml",
        schema = new Schema(implementation = classOf[String]),
        examples = Array(new ExampleObject(TransformTaskApiDoc.transformInputExample))
      ),
    )
  )
  def postTransformInput(@Parameter(
                           name = "project",
                           description = "The project identifier",
                           required = true,
                           in = ParameterIn.PATH,
                           schema = new Schema(implementation = classOf[String])
                         )
                         projectName: String,
                         @Parameter(
                           name = "task",
                           description = "The task identifier",
                           required = true,
                           in = ParameterIn.PATH,
                           schema = new Schema(implementation = classOf[String])
                         )
                         taskName: String): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
    val (_, task) = projectAndTask(projectName, taskName)
    request.body match {
      case AnyContentAsXml(xmlRoot) =>
        implicit val (resourceManager, _) = createInMemoryResourceManagerForResources(xmlRoot, projectName, withProjectResources = true, None)
        val dataSource = createDataSource(xmlRoot, None)
        val (model, entitySink) = createEntitySink(xmlRoot)
        executeTransform(task, entitySink, dataSource, errorEntitySinkOpt = None)
        val acceptedContentType = request.acceptedTypes.headOption.map(_.toString()).getOrElse("application/n-triples")
        result(model, acceptedContentType, "Data transformed successfully!")
      case _ =>
        throw UnsupportedMediaTypeException.supportedFormats("application/xml")
    }
  }

  private def executeTransform(task: ProjectTask[TransformSpec],
                               entitySink: EntitySink,
                               dataSource: DataSource,
                               errorEntitySinkOpt: Option[EntitySink])
                              (implicit userContext: UserContext): Unit = {
    implicit val prefixes: Prefixes = task.project.config.prefixes
    implicit val variables: TemplateVariablesReader = task.project.combinedTemplateVariables
    val inputTask = task.project.anyTask (task.selection.inputId)
    val transform = new ExecuteTransform(task, (_) => inputTask, (_) => dataSource, (_) => entitySink, (_) => errorEntitySinkOpt)
    Activity(transform).startBlocking()
  }

  private def projectAndTask(projectName: String, taskName: String)
                            (implicit userContext: UserContext): (Project, ProjectTask[TransformSpec]) = {
    getProjectAndTask[TransformSpec](projectName, taskName)
  }


}

object TransformTaskApi {

  // The property that is set when copying a root mapping rule that will be converted into an object mapping rule
  final val ROOT_COPY_TARGET_PROPERTY = "urn:temp:child"
}

