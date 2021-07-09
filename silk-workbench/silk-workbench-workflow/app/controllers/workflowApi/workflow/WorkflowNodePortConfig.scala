package controllers.workflowApi.workflow

import play.api.libs.json.{Format, Json}

/** The port configuration of all nodes in a workflow.
  * This specifies how many input ports each node offers.
  * It defines three config abstractions. The most generic one is by item type.
  * More specific is by a specific task and the most specific one is by workflow node, i.e. the actual usage of a task in a workflow.
  * The most specific configuration for a task should be taken.
  **/
case class WorkflowNodesPortConfig(byItemType: Map[String, WorkflowNodePortConfig],
                                   byTaskId: Map[String, WorkflowNodePortConfig],
                                   byNodeId: Map[String, WorkflowNodePortConfig])

/** Node configuration for a single workflow node. */
case class WorkflowNodePortConfig(minInputPorts: Int,
                                  maxInputPorts: Option[Int])

object WorkflowNodePortConfig {
  def apply(ports: Int): WorkflowNodePortConfig = WorkflowNodePortConfig(ports, Some(ports))
}

object WorkflowNodesPortConfig {
  implicit val workflowNodePortConfigFormat: Format[WorkflowNodePortConfig] = Json.format[WorkflowNodePortConfig]
  implicit val workflowNodesPortConfigFormat: Format[WorkflowNodesPortConfig] = Json.format[WorkflowNodesPortConfig]
}