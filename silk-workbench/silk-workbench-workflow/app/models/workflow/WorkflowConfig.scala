package models.workflow

import org.silkframework.config.{DefaultConfig, ProductionConfig}
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.rule.{LinkSpec, TransformSpec}
import org.silkframework.util.Identifier
import org.silkframework.workbench.WorkbenchPlugin.TaskActions
import org.silkframework.workspace.activity.workflow.Workflow

object WorkflowConfig {

  /**
    * Returns the workflow execution activity that corresponds to the configured execution manager.
    */
  def executorName: String = {
    DefaultConfig.instance().getString("execution.manager.plugin") match {
      case "LocalExecutionManager" => "ExecuteLocalWorkflow"
      case "SparkExecutionManager" => "ExecuteSparkWorkflow"
      case manager: String =>
        throw new RuntimeException("Unknown execution manager: " + manager)
    }
  }

}
