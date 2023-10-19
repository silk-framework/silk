package controllers.workflowApi.workflow

import org.silkframework.config.{FixedNumberOfInputs, FixedSchemaPort, FlexibleNumberOfInputs, FlexibleSchemaPort, InputPorts, Port, UnknownSchemaPort}
import org.silkframework.entity.EntitySchema
import play.api.libs.json.{Format, JsArray, JsObject, JsResult, JsSuccess, JsValue, Json, OFormat}

/** The port configuration of all nodes in a workflow.
  * This specifies how many input ports each node offers.
  * It defines three config abstractions. The most generic one is by item type.
  * More specific is by a specific task and the most specific one is by workflow node, i.e. the actual usage of a task in a workflow.
  * The most specific configuration for a task should be taken.
  **/
case class WorkflowNodesPortConfig(byItemType: Map[String, WorkflowNodePortConfig],
                                   byPluginId: Map[String, PluginPortConfig],
                                   byTaskId: Map[String, WorkflowNodePortConfig],
                                   byNodeId: Map[String, WorkflowNodePortConfig])

/** Node configuration for a single workflow node. */
case class WorkflowNodePortConfig(minInputPorts: Int,
                                  maxInputPorts: Option[Int],
                                  inputPortsDefinition: PortsDefinition,
                                  outputPortsDefinition: PortsDefinition)

object WorkflowNodePortConfig {
  def apply(inputPorts: InputPorts,
            outputPort: Option[Port]): WorkflowNodePortConfig = {
    val outDefinition: PortsDefinition = outputPort.map(convertPort)
      .map(p => SinglePortPortsDefinition(p))
      .getOrElse(ZeroPortsDefinition)
    inputPorts match {
      case FlexibleNumberOfInputs() =>
        WorkflowNodePortConfig(
          1,
          None,
          MultipleSameTypePortsDefinition(FlexiblePortDefinition),
          outDefinition
        )
      case FixedNumberOfInputs(ports) =>
        WorkflowNodePortConfig(
          ports.size,
          Some(ports.size),
          FixedSizePortsDefinition(
            ports.map(convertPort)
          ),
          outDefinition
        )
    }
  }

  private def convertPort(port: Port): PortDefinition = {
    port match {
      case FixedSchemaPort(schema) =>
        val paths = schema.typedPaths
          .map(p => PortSchemaProperty(p.normalizedSerialization))
        val typeUri = schema.typeUri
        FixedSchemaPortDefinition(PortSchema(
          Some(typeUri),
          paths
        ))
      case FlexibleSchemaPort =>
        FlexiblePortDefinition
      case UnknownSchemaPort =>
        UnknownTypePortDefinition
    }
  }
}

/** A flat schema. From multi entity schemata inputs only the root schema is represented.
  *
  * @param typeUri    The type URI of the entity schema.
  * @param properties The properties of the schema.
  */
case class PortSchema(typeUri: Option[String],
                      properties: Seq[PortSchemaProperty])

/**
  * A single property of a port schema.
  *
  * @param value The value e.g. URI of the property.
  */
case class PortSchemaProperty(value: String)

/**
  * The plugin specific port config.
  * @param configPort The information about the config port.
  */
case class PluginPortConfig(configPort: ConfigPortConfig)

/** The information about the config port of a node. */
case class ConfigPortConfig(inputSchema: PortSchema)

/** The definition of all input/output ports. This can comprise multiple port definitions. */
sealed trait PortsDefinition {
  def portsType: String
}

/**
  * A ports definition where there can be many ports, but each one has the same port definition.
  * @param portDefinition All ports have the same definition.
  */
case class MultipleSameTypePortsDefinition(portDefinition: PortDefinition) extends PortsDefinition {
  override def portsType: String = "multiple"
}

/** Ports definition having a fixed number of ports with their own port definition. */
case class FixedSizePortsDefinition(portDefinitions: Seq[PortDefinition]) extends PortsDefinition {
  override def portsType: String = "fixed"
}

/** A single port. */
case class SinglePortPortsDefinition(portDefinition: PortDefinition) extends PortsDefinition {
  override def portsType: String = "single"
}

/** No output port. */
object ZeroPortsDefinition extends PortsDefinition {
  override def portsType: String = "none"
}

/** A single port definition. */
sealed trait PortDefinition {
  /** The type of the port, flexible, fixed or unknown. */
  def portsType: String
}

case class FixedSchemaPortDefinition(schema: PortSchema) extends PortDefinition {
  override def portsType: String = "fixedSchemaType"
}

/** A flexible port can take or output various kinds of schemata.  */
case object FlexiblePortDefinition extends PortDefinition {
  override def portsType: String = "flexibleType"
}

/** Usually has a fixed schema at runtime that is however not known without runtime or context information. */
case object UnknownTypePortDefinition extends PortDefinition {
  override def portsType: String = "unknownType"
}

object WorkflowNodesPortConfig {
  implicit val portSchemaProperty: Format[PortSchemaProperty] = Json.format[PortSchemaProperty]
  implicit val portSchemaFormat: Format[PortSchema] = Json.format[PortSchema]
  implicit val configPortConfigFormat: Format[ConfigPortConfig] = Json.format[ConfigPortConfig]
  implicit val pluginPortConfigFormat: Format[PluginPortConfig] = Json.format[PluginPortConfig]
  implicit val portDefinitionFormat: Format[PortDefinition] = new Format[PortDefinition] {
    override def reads(json: JsValue): JsResult[PortDefinition] = {
      (json \ "portsType").as[String] match {
        case "fixedSchemaType" =>
          val schemaJson = (json \ "schema").as[JsObject]
            Json.fromJson[PortSchema](schemaJson).map(schema => {
              FixedSchemaPortDefinition(schema)
            })
        case "flexibleType" =>
          JsSuccess(FlexiblePortDefinition)
        case "unknownType" =>
          JsSuccess(UnknownTypePortDefinition)
      }
    }

    override def writes(o: PortDefinition): JsValue = {
      val baseObject = Json.obj("portsType" -> o.portsType)
      o match {
        case FixedSchemaPortDefinition(schema) =>
          baseObject + ("schema" -> Json.toJson(schema))
        case _ =>
          baseObject
      }
    }
  }
  implicit val portsDefinitionFormat: Format[PortsDefinition] = new Format[PortsDefinition] {
    override def reads(json: JsValue): JsResult[PortsDefinition] = {
      (json \ "portsType").as[String] match {
        case "single" =>
          val portDefinition = Json.fromJson[PortDefinition]((json \ "portDefinition").as[JsObject])
          portDefinition.map(pd => SinglePortPortsDefinition(pd))
        case "multiple" =>
          val portDefinition = Json.fromJson[PortDefinition]((json \ "portDefinition").as[JsObject])
          portDefinition.map(pd => MultipleSameTypePortsDefinition(pd))
        case "fixed" =>
          val portDefinitions = Json.fromJson[Seq[PortDefinition]]((json \ "portDefinitions").as[JsArray])
          portDefinitions.map(pds => FixedSizePortsDefinition(pds))
        case "none" =>
          JsSuccess(ZeroPortsDefinition)
      }
    }

    override def writes(o: PortsDefinition): JsValue = {
      val baseObject = Json.obj("portsType" -> o.portsType)
      o match {
        case FixedSizePortsDefinition(portDefinitions) =>
          baseObject + ("portDefinitions" -> Json.toJson(portDefinitions))
        case SinglePortPortsDefinition(portDefinition) =>
          baseObject + ("portDefinition" -> Json.toJson(portDefinition))
        case MultipleSameTypePortsDefinition(portDefinition) =>
          baseObject + ("portDefinition" -> Json.toJson(portDefinition))
        case ZeroPortsDefinition =>
          baseObject
      }
    }
  }
  implicit val workflowNodePortConfigFormat: Format[WorkflowNodePortConfig] = Json.format[WorkflowNodePortConfig]
  implicit val workflowNodesPortConfigFormat: Format[WorkflowNodesPortConfig] = Json.format[WorkflowNodesPortConfig]
}