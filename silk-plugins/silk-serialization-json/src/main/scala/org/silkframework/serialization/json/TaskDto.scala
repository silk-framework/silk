package org.silkframework.serialization.json

import io.swagger.v3.oas.annotations.media.Schema.{AccessMode, RequiredMode}
import io.swagger.v3.oas.annotations.media.{ArraySchema, Schema}
import org.silkframework.config.{FixedNumberOfInputs, Task, TaskSpec}
import org.silkframework.entity.EntitySchema
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.serialization.WriteContext
import org.silkframework.runtime.validation.{BadUserInputException, ValidationException}
import org.silkframework.serialization.json.MetaDataSerializers.{MetaDataPlain, metaDataFormat}
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Canonical task wire format, produced and accepted by [[JsonSerializers.TaskJsonFormat]]
  * (see dev-wiki/feature-specs/2026-07-14-typed-task-json.md). This model is the single
  * definition of the task JSON structure and provides the OpenAPI schema for the task CRUD
  * endpoints. The interior of `data.parameters`/`data.templates` is plugin-defined and stays
  * free-form by design.
  */
@Schema(description = "A project task in its canonical JSON format.")
case class TaskDto(@Schema(
                     description = "The task identifier. Always present in responses. Optional on task creation, in which case it is generated from the label.",
                     requiredMode = RequiredMode.NOT_REQUIRED
                   )
                   id: String,
                   @Schema(
                     description = "The project this task belongs to. Only present in responses.",
                     requiredMode = RequiredMode.NOT_REQUIRED,
                     accessMode = AccessMode.READ_ONLY,
                     implementation = classOf[String]
                   )
                   project: Option[String],
                   @Schema(
                     description = "The task meta data, such as label and description.",
                     requiredMode = RequiredMode.NOT_REQUIRED,
                     implementation = classOf[MetaDataPlain]
                   )
                   metadata: Option[MetaDataPlain],
                   @ArraySchema(schema = new Schema(
                     description = "Execution variables of this task. Omitted if empty. On task updates, omitting this field keeps the stored variables, " +
                       "while an empty array removes them.",
                     requiredMode = RequiredMode.NOT_REQUIRED,
                     implementation = classOf[TemplateVariableJson]
                   ))
                   executionVariables: Option[Seq[TemplateVariableJson]],
                   @Schema(
                     description = "The task type. Duplicated from 'data.taskType' for convenience. Ignored on requests.",
                     requiredMode = RequiredMode.NOT_REQUIRED,
                     accessMode = AccessMode.READ_ONLY,
                     implementation = classOf[String]
                   )
                   taskType: Option[String],
                   @Schema(
                     description = "The task specification. Required on requests, except on updates of selected properties, which take a partial task. " +
                       "Only omitted in responses if requested via the format options.",
                     requiredMode = RequiredMode.REQUIRED,
                     implementation = classOf[TaskDataDto]
                   )
                   data: Option[TaskDataDto],
                   @ArraySchema(schema = new Schema(
                     description = "The task parameters as displayable key-value pairs. Only present in responses if requested.",
                     requiredMode = RequiredMode.NOT_REQUIRED,
                     accessMode = AccessMode.READ_ONLY,
                     implementation = classOf[TaskPropertyDto]
                   ))
                   properties: Option[Seq[TaskPropertyDto]],
                   @Schema(
                     description = "Relations of this task to other tasks. Only present in responses if requested.",
                     requiredMode = RequiredMode.NOT_REQUIRED,
                     accessMode = AccessMode.READ_ONLY,
                     implementation = classOf[TaskRelationsDto]
                   )
                   relations: Option[TaskRelationsDto],
                   @Schema(
                     description = "The input and output schemata of this task. Only present in responses if requested.",
                     requiredMode = RequiredMode.NOT_REQUIRED,
                     accessMode = AccessMode.READ_ONLY,
                     implementation = classOf[TaskSchemataDto]
                   )
                   schemata: Option[TaskSchemataDto])

object TaskDto {
  implicit val taskDtoWrites: OWrites[TaskDto] = Json.writes[TaskDto]

  /**
    * Strict reads of the canonical task JSON envelope. Response-only fields ('project',
    * top-level 'taskType', 'properties', 'relations', 'schemata') and unknown fields are
    * ignored. An absent or empty id is represented as an empty string; the caller generates
    * the id from the label in that case.
    */
  implicit val taskDtoReads: Reads[TaskDto] = (
    (__ \ JsonSerializers.ID).readNullable[String] and
    (__ \ JsonSerializers.METADATA).readNullable[MetaDataPlain] and
    (__ \ JsonSerializers.TaskJsonFormat.EXECUTION_VARIABLES).readNullable[Seq[TemplateVariableJson]] and
    (__ \ JsonSerializers.DATA).read[TaskDataDto]
  ) { (id, metadata, executionVariables, data) =>
    TaskDto(
      id = id.map(_.trim).filter(_.nonEmpty).getOrElse(""),
      project = None,
      metadata = metadata,
      executionVariables = executionVariables,
      taskType = data.taskType,
      data = Some(data),
      properties = None,
      relations = None,
      schemata = None
    )
  }

  /**
    * Parses the canonical task JSON, mapping parse errors to [[BadUserInputException]]s with
    * readable messages that include the JSON path.
    */
  def parseTaskJson(value: JsValue): TaskDto = {
    value.validate[TaskDto] match {
      case JsSuccess(dto, _) =>
        dto
      case JsError(errors) =>
        val errorStrings = errors.map { case (path, validationErrors) =>
          val messages = validationErrors.map(readableMessage).mkString(", ")
          jsonPath(path).map(p => s"At '$p': $messages").getOrElse(messages)
        }
        throw BadUserInputException("The task JSON is invalid. " + errorStrings.mkString("; "))
    }
  }

  /**
    * The path of an attribute as the client sees it, e.g. 'data.parameters'.
    * Not using JsPath.toJsonString, which prefixes the path with Play's internal 'obj' root.
    */
  private def jsonPath(path: JsPath): Option[String] = {
    Some(path.path.map(_.toJsonString).mkString.stripPrefix(".")).filter(_.nonEmpty)
  }

  private def readableMessage(error: JsonValidationError): String = {
    error.message match {
      case "error.path.missing" => "attribute is missing"
      case "error.expected.jsobject" => "attribute must be a JSON object"
      case "error.expected.jsstring" => "attribute must be a string"
      case "error.expected.jsarray" => "attribute must be an array"
      case "error.expected.jsboolean" => "attribute must be a boolean"
      case other => other
    }
  }
}

@Schema(description = "A task specification, i.e., the task type and its parameters.")
case class TaskDataDto(@Schema(
                         description = "The task type. Always present in responses. On requests to endpoints with a known task type, it may be omitted.",
                         requiredMode = RequiredMode.REQUIRED,
                         implementation = classOf[String],
                         allowableValues = Array("Dataset", "Transform", "Linking", "Workflow", "CustomTask", "RuleBlock")
                       )
                       taskType: Option[String],
                       @Schema(
                         description = "Whether this dataset is read-only. Only used by dataset tasks.",
                         requiredMode = RequiredMode.NOT_REQUIRED,
                         implementation = classOf[Boolean]
                       )
                       readOnly: Option[Boolean],
                       @Schema(
                         description = "The property that stores the entity URIs. Only used by dataset tasks.",
                         requiredMode = RequiredMode.NOT_REQUIRED,
                         implementation = classOf[String]
                       )
                       uriProperty: Option[String],
                       @Schema(
                         description = "The plugin identifier, e.g., 'csv' for a CSV dataset. Always present in responses. On requests to endpoints with a " +
                           "known plugin type, it may be omitted.",
                         requiredMode = RequiredMode.REQUIRED,
                         implementation = classOf[String]
                       )
                       `type`: Option[String],
                       @Schema(
                         description = "The plugin parameters. The available keys are defined by the plugin (see the plugin description endpoints). " +
                           "Each value is either a string (scalar parameter), a nested parameters object (nested plugin) or " +
                           "a plugin-defined JSON structure (object parameter, e.g., a mapping rule tree).",
                         requiredMode = RequiredMode.REQUIRED,
                         `type` = "object"
                       )
                       parameters: JsObject,
                       @Schema(
                         description = "Template expressions for plugin parameters. Mirrors the structure of 'parameters' with template strings as leaves. " +
                           "A templated parameter appears in both objects: evaluated under 'parameters' and as expression under 'templates'.",
                         requiredMode = RequiredMode.NOT_REQUIRED,
                         `type` = "object"
                       )
                       templates: Option[JsObject])

object TaskDataDto {

  final val READ_ONLY = "readOnly"
  final val URI_PROPERTY = "uriProperty"

  private val knownFields = Set(JsonSerializers.TASKTYPE, JsonSerializers.TYPE, JsonSerializers.PARAMETERS,
    JsonSerializers.TEMPLATES, READ_ONLY, URI_PROPERTY)

  /**
    * Extracts the DTO from the data JSON emitted by the task type serializers.
    * Fails loudly on fields that are not covered by this DTO, so that the DTO stays the single
    * definition of the wire format.
    */
  def fromDataJson(dataJson: JsObject): TaskDataDto = {
    val unknownFields = dataJson.keys.toSet -- knownFields
    if(unknownFields.nonEmpty) {
      throw new ValidationException(s"Task data JSON contains field(s) not covered by TaskDataDto: ${unknownFields.mkString(", ")}. " +
        s"TaskDataDto needs to be extended to cover all fields emitted by the task type serializers.")
    }
    TaskDataDto(
      taskType = Some((dataJson \ JsonSerializers.TASKTYPE).as[String]),
      readOnly = (dataJson \ READ_ONLY).asOpt[Boolean],
      uriProperty = (dataJson \ URI_PROPERTY).asOpt[String],
      `type` = Some((dataJson \ JsonSerializers.TYPE).as[String]),
      parameters = (dataJson \ JsonSerializers.PARAMETERS).as[JsObject],
      templates = (dataJson \ JsonSerializers.TEMPLATES).asOpt[JsObject]
    )
  }

  implicit val taskDataDtoWrites: OWrites[TaskDataDto] = OWrites { data =>
    var json = Json.obj()
    for(taskType <- data.taskType) {
      json += JsonSerializers.TASKTYPE -> JsString(taskType)
    }
    for(readOnly <- data.readOnly) {
      json += READ_ONLY -> JsBoolean(readOnly)
    }
    for(uriProperty <- data.uriProperty) {
      json += URI_PROPERTY -> JsString(uriProperty)
    }
    for(pluginType <- data.`type`) {
      json += JsonSerializers.TYPE -> JsString(pluginType)
    }
    json += JsonSerializers.PARAMETERS -> data.parameters
    for(templates <- data.templates) {
      json += JsonSerializers.TEMPLATES -> templates
    }
    json
  }

  /**
    * Accepts booleans as well as their string representation, since clients commonly transport all
    * parameter values as strings. Matches [[JsonHelpers.booleanValueOption]], which the dataset
    * format uses to read the same attribute.
    */
  private val lenientBooleanReads: Reads[Boolean] = Reads {
    case JsBoolean(value) => JsSuccess(value)
    case JsString(value) if value.equalsIgnoreCase("true") => JsSuccess(true)
    case JsString(value) if value.equalsIgnoreCase("false") => JsSuccess(false)
    case _ => JsError("error.expected.jsboolean")
  }

  /** Rejects unknown attributes, mirroring [[fromDataJson]]. They would be dropped silently otherwise. */
  private val rejectUnknownFields: Reads[JsObject] = Reads {
    case dataJson: JsObject =>
      val unknownFields = (dataJson.keys.toSet -- knownFields).toSeq.sorted
      if(unknownFields.isEmpty) {
        JsSuccess(dataJson)
      } else {
        JsError(s"unknown attribute(s): ${unknownFields.mkString(", ")}. " +
          s"Plugin parameters must be provided in the '${JsonSerializers.PARAMETERS}' object.")
      }
    case _ =>
      JsError("error.expected.jsobject")
  }

  private val fieldReads: Reads[TaskDataDto] = (
    (__ \ JsonSerializers.TASKTYPE).readNullable[String] and
    (__ \ READ_ONLY).readNullable[Boolean](lenientBooleanReads) and
    (__ \ URI_PROPERTY).readNullable[String] and
    (__ \ JsonSerializers.TYPE).readNullable[String] and
    (__ \ JsonSerializers.PARAMETERS).read[JsObject] and
    (__ \ JsonSerializers.TEMPLATES).readNullable[JsObject]
  )(TaskDataDto.apply _)

  /**
    * Strict reads of the canonical task data. 'taskType' and 'type' stay optional here because
    * endpoints with a known task/plugin type accept payloads without them; the generic task
    * dispatch enforces 'taskType' itself.
    */
  implicit val taskDataDtoReads: Reads[TaskDataDto] = rejectUnknownFields.flatMap(_ => fieldReads)
}

@Schema(description = "A task parameter as a displayable key-value pair.")
case class TaskPropertyDto(key: String, value: String)

object TaskPropertyDto {

  // The write context provides the plugin context that parameter templates are evaluated against.
  def fromTask(task: Task[_ <: TaskSpec])(implicit writeContext: WriteContext[JsValue]): Seq[TaskPropertyDto] = {
    for((key, value) <- task.data.parameters.toStringMap.toSeq) yield {
      TaskPropertyDto(key, value)
    }
  }

  implicit val taskPropertyDtoWrites: OWrites[TaskPropertyDto] = Json.writes[TaskPropertyDto]
}

@Schema(description = "Relations of a task to other tasks.")
case class TaskRelationsDto(@ArraySchema(schema = new Schema(description = "The tasks that are inputs to this task.", implementation = classOf[String]))
                            inputTasks: Seq[String],
                            @ArraySchema(schema = new Schema(description = "The tasks that this task writes to.", implementation = classOf[String]))
                            outputTasks: Seq[String],
                            @ArraySchema(schema = new Schema(description = "All tasks that are referenced by this task.", implementation = classOf[String]))
                            referencedTasks: Seq[String],
                            @ArraySchema(schema = new Schema(description = "The tasks that directly depend on this task. Task identifiers by default; " +
                              "endpoints may return richer objects instead."))
                            dependentTasksDirect: Seq[JsValue],
                            @ArraySchema(schema = new Schema(description = "All tasks that directly or transitively depend on this task. Task identifiers " +
                              "by default; endpoints may return richer objects instead."))
                            dependentTasksAll: Seq[JsValue])

object TaskRelationsDto {

  def fromTask(task: Task[_ <: TaskSpec],
               dependentTaskFormatter: Option[String => JsValue])
              (implicit userContext: UserContext): TaskRelationsDto = {
    def dependentTasks(recursive: Boolean): Seq[JsValue] = {
      val tasks = task.findDependentTasks(recursive)
      dependentTaskFormatter match {
        case Some(jsonFormatter) =>
          tasks.map(t => jsonFormatter(t)).toSeq
        case None =>
          tasks.map(t => JsString(t)).toSeq
      }
    }
    TaskRelationsDto(
      inputTasks = task.data.inputTasks.toSeq.map(_.toString),
      outputTasks = task.data.outputTasks.toSeq.map(_.toString),
      referencedTasks = task.data.referencedTasks.toSeq.map(_.toString),
      dependentTasksDirect = dependentTasks(recursive = false),
      dependentTasksAll = dependentTasks(recursive = true)
    )
  }

  implicit val taskRelationsDtoWrites: OWrites[TaskRelationsDto] = Json.writes[TaskRelationsDto]
}

@Schema(description = "An entity schema of a task input or output.")
case class TaskSchemaDto(@ArraySchema(schema = new Schema(description = "The paths of the schema in their normalized serialization.", implementation = classOf[String]))
                         paths: Seq[String])

object TaskSchemaDto {

  def fromSchema(schema: EntitySchema): TaskSchemaDto = {
    TaskSchemaDto(schema.typedPaths.map(_.toUntypedPath.normalizedSerialization))
  }

  implicit val taskSchemaDtoWrites: OWrites[TaskSchemaDto] = Json.writes[TaskSchemaDto]
}

@Schema(description = "The input and output schemata of a task.")
case class TaskSchemataDto(@ArraySchema(schema = new Schema(description = "The schemata of the task inputs. Null if the task does not have a fixed number of inputs.",
                             implementation = classOf[TaskSchemaDto], nullable = true))
                           input: Option[Seq[TaskSchemaDto]],
                           @Schema(description = "The schema of the task output. Null if the task does not provide an output schema.",
                             implementation = classOf[TaskSchemaDto], nullable = true)
                           output: Option[TaskSchemaDto])

object TaskSchemataDto {

  def fromTask(task: Task[_ <: TaskSpec]): TaskSchemataDto = {
    val input = task.data.inputPorts match {
      case FixedNumberOfInputs(ports) => Some(ports.flatMap(_.schemaOpt).map(TaskSchemaDto.fromSchema))
      case _ => None
    }
    val output = task.data.outputPort.flatMap(_.schemaOpt).map(TaskSchemaDto.fromSchema)
    TaskSchemataDto(input, output)
  }

  // 'input' and 'output' are always present and explicitly null if undefined.
  implicit val taskSchemataDtoWrites: OWrites[TaskSchemataDto] = OWrites { schemata =>
    Json.obj(
      "input" -> schemata.input.map(schemas => JsArray(schemas.map(Json.toJson(_)))).getOrElse[JsValue](JsNull),
      "output" -> schemata.output.map(Json.toJson(_)).getOrElse[JsValue](JsNull)
    )
  }
}
