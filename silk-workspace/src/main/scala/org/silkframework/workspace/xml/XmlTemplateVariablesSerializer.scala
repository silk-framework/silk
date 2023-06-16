package org.silkframework.workspace.xml

import org.silkframework.config.Prefixes
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.{EmptyResourceManager, ResourceManager}
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.runtime.templating.TemplateVariables
import org.silkframework.runtime.templating.TemplateVariables.TemplateVariablesFormat
import org.silkframework.workspace.TemplateVariablesSerializer

import scala.xml.{Node, PrettyPrinter, XML}

class XmlTemplateVariablesSerializer(resources: ResourceManager) extends TemplateVariablesSerializer {
  /**
    * Reads all variables at this scope.
    */
  override def readVariables()(implicit userContext: UserContext): TemplateVariables = {
    implicit val readContext: ReadContext = ReadContext(EmptyResourceManager(), Prefixes.empty, user = userContext)
    val variablesFile = resources.get("variables.xml")
    if(variablesFile.exists) {
      variablesFile.read(is => TemplateVariablesFormat.read(XML.load(is)))
    } else {
      TemplateVariables.empty
    }
  }

  /**
    * Updates all variables.
    */
  override def putVariables(variables: TemplateVariables)(implicit userContext: UserContext): Unit = {
    implicit val writeContext: WriteContext[Node] = WriteContext[Node](prefixes = Prefixes.empty, user = userContext)
    val variablesFile = resources.get("variables.xml")
    val printer = new PrettyPrinter(Int.MaxValue, 2)
    variablesFile.writeString(printer.format(TemplateVariablesFormat.write(variables)))
  }
}
