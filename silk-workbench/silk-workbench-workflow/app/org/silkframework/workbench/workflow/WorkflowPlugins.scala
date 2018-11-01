package org.silkframework.workbench.workflow

import org.silkframework.runtime.plugin.PluginModule
import org.silkframework.workbench.workflow.WorkflowPayload.{WorkflowPayloadJsonFormat, WorkflowPayloadXmlFormat}

class WorkflowPlugins extends PluginModule {

  override def pluginClasses: Seq[Class[_]] =
    Seq(
      classOf[WorkflowPlugin],
      classOf[WorkflowWithPayloadExecutorFactory],
      WorkflowPayloadJsonFormat.getClass,
      WorkflowPayloadXmlFormat.getClass
    )
}
