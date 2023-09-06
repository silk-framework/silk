package controllers.workflowApi.workflow

import play.api.libs.json.{Format, Json, OFormat}

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
                                  maxInputPorts: Option[Int])

object WorkflowNodePortConfig {
  def apply(ports: Int): WorkflowNodePortConfig = WorkflowNodePortConfig(ports, Some(ports))
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

object WorkflowNodesPortConfig {
  implicit val portSchemaProperty: Format[PortSchemaProperty] = Json.format[PortSchemaProperty]
  implicit val portSchemaFormat: Format[PortSchema] = Json.format[PortSchema]
  implicit val configPortConfigFormat: Format[ConfigPortConfig] = Json.format[ConfigPortConfig]
  implicit val pluginPortConfigFormat: Format[PluginPortConfig] = Json.format[PluginPortConfig]
  implicit val workflowNodePortConfigFormat: Format[WorkflowNodePortConfig] = Json.format[WorkflowNodePortConfig]
  implicit val workflowNodesPortConfigFormat: Format[WorkflowNodesPortConfig] = Json.format[WorkflowNodesPortConfig]
}