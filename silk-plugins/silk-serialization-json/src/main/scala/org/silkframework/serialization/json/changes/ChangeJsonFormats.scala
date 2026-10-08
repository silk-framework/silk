package org.silkframework.serialization.json.changes

import org.silkframework.config.{PlainTask, Prefixes, TaskSpec}
import org.silkframework.rule.TransformRule
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.runtime.templating.TemplateVariable
import org.silkframework.serialization.json.JsonHelpers._
import org.silkframework.serialization.json.JsonSerializers.{GenericTaskJsonFormat, TransformRuleJsonFormat}
import org.silkframework.serialization.json.WorkflowSerializers.{WorkflowDatasetJsonFormat, WorkflowOperatorJsonFormat}
import org.silkframework.serialization.json.{JsonFormat, JsonParseException, TemplateVariableJson}
import org.silkframework.util.Identifier
import org.silkframework.workspace.activity.workflow.{WorkflowDataset, WorkflowNode, WorkflowOperator}
import org.silkframework.workspace.changes._
import play.api.libs.json._

import java.time.Instant
import java.util.logging.Logger
import scala.util.control.NonFatal

/**
  * The JSON of change journal entries, as the file store keeps them: an envelope with the header fields and the
  * summary, and the payload under `change` with the fields of the change's case class under their names. A payload
  * that cannot be read, e.g. because its plugin is gone or its format changed, yields an [[UnreadableChange]], so
  * that the entry stays listed and the seqs, the links between entries and the unreviewed count stay intact.
  */
object ChangeJsonFormats {

  /** The envelope version; an envelope of another version is listed as unreadable. */
  final val VERSION = 1

  private val logger = Logger.getLogger(getClass.getName)

  /** The header of an entry: every field of the envelope but `change`. */
  implicit object ChangeHeaderJsonFormat extends JsonFormat[ChangeHeader] {

    /**
      * The header of a stored line without parsing its change: the writer puts `change` last, so the header ends where
      * `,"change":` begins. Inside a JSON string every quote is escaped, so the sequence cannot occur earlier in the line.
      */
    def readLine(line: String): ChangeHeader = {
      val cut = line.indexOf(CHANGE_FIELD)
      read(Json.parse(if(cut >= 0) line.substring(0, cut) + "}" else line))(ReadContext.empty)
    }

    override def read(value: JsValue)(implicit readContext: ReadContext): ChangeHeader = {
      val version = integer(value, "version")
      // Whatever the version: an entry needs its seq and timestamp to be placed, and its type to be listed
      val seq = integer(value, "seq")
      val timestamp = Instant.parse(stringValue(value, "timestamp"))
      val changeType = stringValue(value, "type")
      val summary = stringValueOption(value, "summary").getOrElse(changeType)
      // The fields of this version; an entry of another version is listed as a placeholder, without details and not revertible
      def ofThisVersion[T](field: => Option[T]): Option[T] = if(version == VERSION) field else None
      val details = ofThisVersion(arrayValueOption(value, "details")).map(_.value.toSeq.map(detail =>
        ChangeDetail(stringValue(detail, "label"), stringValueOption(detail, "before"), stringValueOption(detail, "after"))))
      ChangeHeader(seq, timestamp, stringValueOption(value, "user"), stringValueOption(value, "origin"), changeType, summary,
        details.getOrElse(Seq.empty),
        revertible = ofThisVersion(booleanValueOption(value, "revertible")).getOrElse(false),
        proposal = ofThisVersion(booleanValueOption(value, "proposal")).getOrElse(false),
        taskId = ofThisVersion(stringValueOption(value, "taskId")).map(Identifier(_)),
        ruleId = ofThisVersion(stringValueOption(value, "ruleId")).map(Identifier(_)),
        path = ofThisVersion(stringValueOption(value, "path")),
        executionId = ofThisVersion(stringValueOption(value, "executionId")),
        reverts = integerOption(value, "reverts"), fulfils = integerOption(value, "fulfils"))
    }

    override def write(header: ChangeHeader)(implicit writeContext: WriteContext[JsValue]): JsValue = JsObject(fields(header))

    /** The fields in the order they are written; optional ones are left out when unset. */
    def fields(header: ChangeHeader): Seq[(String, JsValue)] = {
      Seq("version" -> JsNumber(VERSION), "seq" -> JsNumber(header.seq), "timestamp" -> JsString(header.timestamp.toString),
        "type" -> JsString(header.changeType)) ++
      field("user", header.user) ++
      field("origin", header.origin) ++
      field("reverts", header.reverts) ++
      field("fulfils", header.fulfils) ++
      Seq("summary" -> JsString(header.summary)) ++
      (if(header.details.nonEmpty) Seq("details" -> JsArray(header.details.map(detailJson))) else Seq.empty) ++
      Seq("revertible" -> JsBoolean(header.revertible)) ++
      (if(header.proposal) Seq("proposal" -> JsBoolean(true)) else Seq.empty) ++
      field("taskId", header.taskId.map(_.toString)) ++
      field("ruleId", header.ruleId.map(_.toString)) ++
      field("path", header.path) ++
      field("executionId", header.executionId)
    }

    private def detailJson(detail: ChangeDetail): JsObject = {
      JsObject(Seq("label" -> JsString(detail.label)) ++ field("before", detail.before) ++ field("after", detail.after))
    }
  }

  /** The whole entry: the header fields and the change under `change`, which comes last. */
  implicit object ChangeEntryJsonFormat extends JsonFormat[ChangeEntry] {

    override def read(value: JsValue)(implicit readContext: ReadContext): ChangeEntry = {
      val header = ChangeHeaderJsonFormat.read(value)
      val version = integer(value, "version")
      val change = {
        if(version != VERSION) {
          unreadable(header, s"version $version is not supported, this is version $VERSION")
        } else {
          try {
            ChangeJsonFormat.read(objectValue(value, "change"), header.changeType)
          } catch {
            case NonFatal(ex) => unreadable(header, Change.reason(ex))
          }
        }
      }
      ChangeEntry(header, change)
    }

    private def unreadable(header: ChangeHeader, reason: String)(implicit readContext: ReadContext): UnreadableChange = {
      val project = readContext.projectId.map(id => s" of project '$id'").getOrElse("")
      logger.warning(s"Change ${header.seq}$project (${header.changeType}) cannot be read and stands in without its content: $reason")
      UnreadableChange(header.changeType, header.summary)
    }

    /** The payload is written with full URIs, whatever the caller's prefixes: a stored rule must survive a change of the project's prefixes. */
    override def write(entry: ChangeEntry)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      val change = ChangeJsonFormat.write(entry.change)(writeContext.copy(prefixes = Prefixes.empty))
      JsObject(ChangeHeaderJsonFormat.fields(entry.header) :+ ("change" -> change))
    }
  }

  // The start of the change field within a stored line, which the writer puts last
  private val CHANGE_FIELD = ",\"change\":"

  private def integer(json: JsValue, name: String): Int = toInt(name, numberValue(json, name))

  private def integerOption(json: JsValue, name: String): Option[Int] = numberValueOption(json, name).map(toInt(name, _))

  private def toInt(name: String, number: BigDecimal): Int = {
    if(!number.isValidInt) throw JsonParseException(s"'$name' must be an integer, but is $number.")
    number.toInt
  }

  /**
    * The payloads by change type. Reading throws for a type or a field it does not know; the envelope turns that
    * into the placeholder. Writing a type without a format stores an empty payload, which reads back as the placeholder.
    */
  object ChangeJsonFormat {

    def read(value: JsValue, changeType: String)(implicit readContext: ReadContext): Change = changeType match {
      case "AddTask" => AddTask(task(value, "task"))
      case "RemoveTask" => RemoveTask(task(value, "task"))
      case "ReplaceTask" => ReplaceTask(task(value, "before"), task(value, "after"))
      case "AddMapping" => AddMapping(id(value, "taskId"), id(value, "parentId"), rule(value, "rule"), index(value), taskLabel(value))
      case "RemoveMapping" => RemoveMapping(id(value, "taskId"), id(value, "parentId"), rule(value, "rule"), index(value), taskLabel(value))
      case "UpdateMapping" => UpdateMapping(id(value, "taskId"), rule(value, "before"), rule(value, "after"), taskLabel(value))
      case "ReorderMappings" => ReorderMappings(id(value, "taskId"), id(value, "parentId"), ids(value, "before"), ids(value, "after"), taskLabel(value))
      case "AddWorkflowNode" => AddWorkflowNode(id(value, "taskId"), node(value), booleanValue(value, "replaceableInput"), booleanValue(value, "replaceableOutput"), taskLabel(value))
      case "RemoveWorkflowNode" => RemoveWorkflowNode(id(value, "taskId"), node(value), booleanValue(value, "replaceableInput"), booleanValue(value, "replaceableOutput"), taskLabel(value))
      case "ConnectWorkflowNodes" => ConnectWorkflowNodes(id(value, "taskId"), stringValue(value, "sourceNodeId"), stringValue(value, "targetNodeId"), edge(objectValue(value, "edge")), taskLabel(value))
      case "DisconnectWorkflowNodes" => DisconnectWorkflowNodes(id(value, "taskId"), stringValue(value, "sourceNodeId"), stringValue(value, "targetNodeId"), edge(objectValue(value, "edge")), taskLabel(value))
      case "SetVariable" => SetVariable(optionalValue(value, "before").map(variable), variable(requiredValue(value, "after")))
      case "RemoveVariable" => RemoveVariable(variable(requiredValue(value, "variable")))
      case "ResourceCreated" => ResourceCreated(stringValue(value, "path"), fileState(objectValue(value, "after")))
      case "ResourceOverwritten" => ResourceOverwritten(stringValue(value, "path"), fileState(objectValue(value, "before")), fileState(objectValue(value, "after")))
      case "ResourceDeleted" => ResourceDeleted(stringValue(value, "path"), fileState(objectValue(value, "before")))
      case "WorkflowExecuted" => WorkflowExecuted(id(value, "taskId"), stringValueOption(value, "executionId"), booleanValue(value, "failed"), taskLabel(value))
      case "ProposedWorkflowRun" => ProposedWorkflowRun(id(value, "taskId"), taskLabel(value))
      case "DiscardedWorkflowRun" => DiscardedWorkflowRun(id(value, "taskId"), taskLabel(value))
      case other => throw JsonParseException(s"Unknown change type '$other'.")
    }

    def write(change: Change)(implicit writeContext: WriteContext[JsValue]): JsObject = change match {
      case AddTask(task) => Json.obj("task" -> taskJson(task))
      case RemoveTask(task) => Json.obj("task" -> taskJson(task))
      case ReplaceTask(before, after) => Json.obj("before" -> taskJson(before), "after" -> taskJson(after))
      case AddMapping(taskId, parentId, rule, index, taskLabel) =>
        JsObject(Seq("taskId" -> JsString(taskId), "parentId" -> JsString(parentId), "rule" -> ruleJson(rule)) ++ field("index", index) ++ field("taskLabel", taskLabel))
      case RemoveMapping(taskId, parentId, rule, index, taskLabel) =>
        JsObject(Seq("taskId" -> JsString(taskId), "parentId" -> JsString(parentId), "rule" -> ruleJson(rule)) ++ field("index", index) ++ field("taskLabel", taskLabel))
      case UpdateMapping(taskId, before, after, taskLabel) =>
        JsObject(Seq("taskId" -> JsString(taskId), "before" -> ruleJson(before), "after" -> ruleJson(after)) ++ field("taskLabel", taskLabel))
      case ReorderMappings(taskId, parentId, before, after, taskLabel) =>
        JsObject(Seq("taskId" -> JsString(taskId), "parentId" -> JsString(parentId), "before" -> idsJson(before), "after" -> idsJson(after)) ++ field("taskLabel", taskLabel))
      case AddWorkflowNode(taskId, node, replaceableInput, replaceableOutput, taskLabel) =>
        JsObject(Seq("taskId" -> JsString(taskId)) ++ nodeJson(node) ++
          Seq("replaceableInput" -> JsBoolean(replaceableInput), "replaceableOutput" -> JsBoolean(replaceableOutput)) ++ field("taskLabel", taskLabel))
      case RemoveWorkflowNode(taskId, node, replaceableInput, replaceableOutput, taskLabel) =>
        JsObject(Seq("taskId" -> JsString(taskId)) ++ nodeJson(node) ++
          Seq("replaceableInput" -> JsBoolean(replaceableInput), "replaceableOutput" -> JsBoolean(replaceableOutput)) ++ field("taskLabel", taskLabel))
      case ConnectWorkflowNodes(taskId, sourceNodeId, targetNodeId, edge, taskLabel) =>
        JsObject(Seq("taskId" -> JsString(taskId), "sourceNodeId" -> JsString(sourceNodeId), "targetNodeId" -> JsString(targetNodeId), "edge" -> edgeJson(edge)) ++ field("taskLabel", taskLabel))
      case DisconnectWorkflowNodes(taskId, sourceNodeId, targetNodeId, edge, taskLabel) =>
        JsObject(Seq("taskId" -> JsString(taskId), "sourceNodeId" -> JsString(sourceNodeId), "targetNodeId" -> JsString(targetNodeId), "edge" -> edgeJson(edge)) ++ field("taskLabel", taskLabel))
      case SetVariable(before, after) =>
        JsObject(before.map(variable => "before" -> variableJson(variable)).toSeq :+ ("after" -> variableJson(after)))
      case RemoveVariable(variable) => Json.obj("variable" -> variableJson(variable))
      case ResourceCreated(path, after) => Json.obj("path" -> path, "after" -> fileStateJson(after))
      case ResourceOverwritten(path, before, after) => Json.obj("path" -> path, "before" -> fileStateJson(before), "after" -> fileStateJson(after))
      case ResourceDeleted(path, before) => Json.obj("path" -> path, "before" -> fileStateJson(before))
      case WorkflowExecuted(taskId, executionId, failed, taskLabel) =>
        JsObject(Seq("taskId" -> JsString(taskId)) ++ field("executionId", executionId) ++ Seq("failed" -> JsBoolean(failed)) ++ field("taskLabel", taskLabel))
      case ProposedWorkflowRun(taskId, taskLabel) => JsObject(Seq("taskId" -> JsString(taskId)) ++ field("taskLabel", taskLabel))
      case DiscardedWorkflowRun(taskId, taskLabel) => JsObject(Seq("taskId" -> JsString(taskId)) ++ field("taskLabel", taskLabel))
      // It never had a readable payload, so it reads back as the placeholder it is
      case UnreadableChange(_, _) => Json.obj()
      case other =>
        logger.warning(s"Change type ${other.changeType} has no JSON format and is stored without its content: ${other.summary}")
        Json.obj()
    }

    private def id(json: JsValue, name: String): Identifier = Identifier(stringValue(json, name))

    private def ids(json: JsValue, name: String): Seq[Identifier] = arrayValue(json, name).value.map(v => Identifier(v.as[String])).toSeq

    private def idsJson(ids: Seq[Identifier]): JsArray = JsArray(ids.map(id => JsString(id)))

    private def index(json: JsValue): Option[Int] = numberValueOption(json, "index").map(_.toInt)

    private def taskLabel(json: JsValue): Option[String] = stringValueOption(json, "taskLabel")

    // A task that does not load throws, so the entry becomes a placeholder like any other unreadable payload
    private def task(json: JsValue, name: String)(implicit readContext: ReadContext): PlainTask[TaskSpec] = {
      PlainTask.fromTask(GenericTaskJsonFormat.read(objectValue(json, name)))
    }

    private def taskJson(task: PlainTask[TaskSpec])(implicit writeContext: WriteContext[JsValue]): JsValue = GenericTaskJsonFormat.write(task)

    private def rule(json: JsValue, name: String)(implicit readContext: ReadContext): TransformRule = TransformRuleJsonFormat.read(requiredValue(json, name))

    private def ruleJson(rule: TransformRule)(implicit writeContext: WriteContext[JsValue]): JsValue = TransformRuleJsonFormat.write(rule)

    private def node(json: JsValue)(implicit readContext: ReadContext): WorkflowNode = stringValue(json, "nodeType") match {
      case "operator" => WorkflowOperatorJsonFormat.read(objectValue(json, "node"))
      case "dataset" => WorkflowDatasetJsonFormat.read(objectValue(json, "node"))
      case other => throw JsonParseException(s"Unknown workflow node type '$other'.")
    }

    private def nodeJson(node: WorkflowNode)(implicit writeContext: WriteContext[JsValue]): Seq[(String, JsValue)] = node match {
      case operator: WorkflowOperator => Seq("nodeType" -> JsString("operator"), "node" -> WorkflowOperatorJsonFormat.write(operator))
      case dataset: WorkflowDataset => Seq("nodeType" -> JsString("dataset"), "node" -> WorkflowDatasetJsonFormat.write(dataset))
    }

    private def edge(json: JsValue): WorkflowEdge = stringValue(json, "kind") match {
      case "data" => WorkflowEdge.Data(arrayValue(json, "ports").value.map(_.as[Int]).toSeq)
      case "dependency" => WorkflowEdge.Dependency
      case "config" => WorkflowEdge.Config
      case "error" => WorkflowEdge.Error
      case other => throw JsonParseException(s"Unknown workflow edge kind '$other'.")
    }

    private def edgeJson(edge: WorkflowEdge): JsObject = edge match {
      case WorkflowEdge.Data(ports) => Json.obj("kind" -> "data", "ports" -> ports)
      case WorkflowEdge.Dependency => Json.obj("kind" -> "dependency")
      case WorkflowEdge.Config => Json.obj("kind" -> "config")
      case WorkflowEdge.Error => Json.obj("kind" -> "error")
    }

    private def variable(json: JsValue): TemplateVariable = json.as[TemplateVariableJson].convert

    private def variableJson(variable: TemplateVariable): JsValue = Json.toJson(TemplateVariableJson(variable))

    private def fileState(json: JsValue): FileState = FileState(numberValueOption(json, "size").map(_.toLong), instantValueOption(json, "modified"))

    private def fileStateJson(state: FileState): JsObject = {
      JsObject(state.size.map(size => "size" -> JsNumber(size)).toSeq ++ state.modified.map(modified => "modified" -> JsString(modified.toString)))
    }
  }

  /** The field for an optional value: absent when unset. */
  private def field[T](name: String, value: Option[T])(implicit writes: Writes[T]): Seq[(String, JsValue)] = {
    value.map(v => name -> writes.writes(v)).toSeq
  }
}
