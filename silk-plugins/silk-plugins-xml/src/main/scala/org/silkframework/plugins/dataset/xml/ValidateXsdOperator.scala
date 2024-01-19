package org.silkframework.plugins.dataset.xml

import org.silkframework.config._
import org.silkframework.dataset.DatasetResourceEntitySchema
import org.silkframework.entity.{EntitySchema, ValueType}
import org.silkframework.entity.paths.TypedPath
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}
import org.silkframework.runtime.resource.Resource
import org.silkframework.workspace.resources.ResourceAutoCompletionProvider

@Plugin(
  id = "validateXsdOperator",
  label = "Validate XML",
  description =
      """Validates an XML dataset against a provided XML schema (XSD) file.
         Any errors are written to the output. Can be used in conjunction with the `Cancel Workflow` operator in order to stop the workflow if errors have been found."
      """
)
case class ValidateXsdOperator(@Param(value = "The XSD file to be used for validating the XML.",
                               autoCompletionProvider = classOf[ResourceAutoCompletionProvider], allowOnlyAutoCompletedValues = true)
                               file: Resource) extends CustomTask {

  override def inputPorts: InputPorts = {
    FixedNumberOfInputs(Seq(FixedSchemaPort(DatasetResourceEntitySchema.schema)))
  }

  override lazy val outputPort: Option[Port] = {
    Some(FixedSchemaPort(ValidateXsdOperator.outputSchema))
  }

  override def referencedResources: Seq[Resource] = Seq(file)

}

object ValidateXsdOperator {

  val outputSchema: EntitySchema = {
    EntitySchema(
      typeUri = "urn:instance:XsdValidationResult",
      typedPaths =
        IndexedSeq(
          path("severity"),
          path("message"),
          path("lineNumber"),
          path("columnNumber")
        )
    )
  }

  private def path(name: String): TypedPath = {
    TypedPath(name, ValueType.STRING)
  }

}
