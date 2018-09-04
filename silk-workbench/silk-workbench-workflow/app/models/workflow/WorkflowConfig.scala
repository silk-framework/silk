package models.workflow

import org.silkframework.config.DefaultConfig

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
