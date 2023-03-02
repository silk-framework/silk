package org.silkframework.workbench.workflow

import org.silkframework.runtime.plugin.{AnyPlugin, PluginModule}
import org.silkframework.workbench.workflow.WorkflowOutput.{WorkflowOutputJsonFormat, WorkflowOutputXmlFormat}

class WorkflowPlugins extends PluginModule {

  override def pluginClasses: Seq[Class[_ <: AnyPlugin]] =
    Seq(
      classOf[WorkflowPlugin],
      classOf[WorkflowWithPayloadExecutorFactory],
      WorkflowOutputJsonFormat.getClass,
      WorkflowOutputXmlFormat.getClass
    )
}
