package org.silkframework.workspace.xml

import org.silkframework.config.{Prefixes, Task}
import org.silkframework.rule.RuleBlockSpec
import org.silkframework.runtime.plugin.{PluginContext, TaskResolver}
import org.silkframework.runtime.resource.{ResourceLoader, ResourceManager}
import org.silkframework.runtime.serialization.{WriteContext, XmlSerialization}
import org.silkframework.util.Identifier
import org.silkframework.util.XMLUtils._
import org.silkframework.workspace.LoadedTask

import scala.xml.Node

private class RuleBlockXmlSerializer extends XmlSerializer[RuleBlockSpec] {

  override def prefix: String = "ruleBlock"

  override def writeTask(task: Task[RuleBlockSpec], resources: ResourceManager, projectResourceManager: ResourceManager): Unit = {
    implicit val writeContext: WriteContext[Node] = WriteContext[Node](
      resources = projectResourceManager,
      prefixes = Prefixes.empty,
      taskResolver = TaskResolver.empty
    )
    val taskXml = XmlSerialization.toXml(task)
    resources.get(task.id.toString + ".xml").write() { os =>
      taskXml.write(os)
    }
  }

  override def removeTask(name: Identifier, resources: ResourceManager): Unit = {
    resources.delete(name.toString + ".xml")
  }

  override def loadTasks(resources: ResourceLoader)
                        (implicit context: PluginContext): Seq[LoadedTask[RuleBlockSpec]] = {
    val names = resources.list.filter(_.endsWith(".xml"))
    for(name <- names) yield {
      loadTaskSafelyFromXML(name, Identifier(name.stripSuffix(".xml")), resources)
    }
  }
}
