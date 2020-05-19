package org.silkframework.plugins.dataset.xml

import org.silkframework.config.CustomTask
import org.silkframework.dataset.DatasetResourceEntitySchema
import org.silkframework.entity.EntitySchema
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}
import org.silkframework.runtime.resource.Resource

@Plugin(
  id = "xsltOperator",
  label = "XSLT",
  description =
      """A task that converts an XML resource via an XSLT script and outputs the transformed XML into a file resource.
      """
)
case class XSLTOperator(@Param("File name inside the resources directory. In the Workbench, this is the '(projectDir)/resources' directory.")
                        file: Resource) extends CustomTask {
  override def inputSchemataOpt: Option[Seq[EntitySchema]] = {
    Some(Seq(DatasetResourceEntitySchema.schema))
  }

  /** Outputs the resulting XML file as a [[org.silkframework.runtime.resource.Resource]] which overwrites the
    * target dataset's resource. */
  override lazy val outputSchemaOpt: Option[EntitySchema] = {
    Some(DatasetResourceEntitySchema.schema)
  }
}
