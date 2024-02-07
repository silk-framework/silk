package org.silkframework.workspace.activity.transform

import org.silkframework.entity.EntitySchema
import org.silkframework.entity.paths.TypedPath
import org.silkframework.rule.DatasetSelection
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.ParameterValues
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat, XmlSerialization}
import org.silkframework.util.{Identifier, Uri}

import scala.xml.Node

/**
  * The cached schemata of the input tasks
  *
  * @param configuredSchema  The schema of the input as configured in this transform spec
  * @param untypedSchema     The optional schema of the input without type. This is stored for some datasets, currently
  *                          only RDF datasets, in order to make services like auto-completion work in hierarchical mappings.
  * @param inputTaskId       The id of the input task for which this entity schema has been loaded.
  * @param datasetParameters The parameters of the dataset from which this entity schema has been loaded, if any.
  */
case class CachedEntitySchemata(configuredSchema: EntitySchema,
                                untypedSchema: Option[EntitySchema],
                                inputTaskId: Identifier,
                                datasetParameters: Option[ParameterValues]) {
  /**
    * Returns the cached paths. Depending on the provided context either the configured or the untyped
    * cached paths are returned.
    *
    * @param task                The transform task for which the paths are requested
    * @param preferUntypedSchema If the untyped schema from the path cache should be returned if available and appropriate.
    * @return
    */
  def fetchCachedPaths(typeUri: Uri,
                       preferUntypedSchema: Boolean): IndexedSeq[TypedPath] = {
    if(typeUri.uri.nonEmpty && preferUntypedSchema && untypedSchema.isDefined) {
      untypedSchema.get.typedPaths
    } else {
      configuredSchema.typedPaths
    }
  }
}

object CachedEntitySchemata {
  implicit object CachedEntitySchemaXmlFormat extends XmlFormat[CachedEntitySchemata] {
    override def read(value: Node)(implicit readContext: ReadContext): CachedEntitySchemata = {
      val inputTaskId = Identifier((value \ "@inputTaskId").text)
      val configured = XmlSerialization.fromXml[EntitySchema]((value \ "ConfiguredEntitySchema" \ "EntityDescription").head)
      val untyped = (value \ "UnTypedEntitySchema" \ "EntityDescription").headOption.map(XmlSerialization.fromXml[EntitySchema])
      val datasetParams = (value \ "Dataset").headOption.map(XmlSerialization.deserializeParameters)
      CachedEntitySchemata(configured, untyped, inputTaskId, datasetParams)
    }

    override def write(value: CachedEntitySchemata)(implicit writeContext: WriteContext[Node]): Node = {
      <CachedEntitySchemata inputTaskId={value.inputTaskId.toString}>
        <ConfiguredEntitySchema>
          {XmlSerialization.toXml(value.configuredSchema)}
        </ConfiguredEntitySchema>
        {
          for(schema <- value.untypedSchema.toSeq) yield {
            <UnTypedEntitySchema>
              {XmlSerialization.toXml(schema)}
            </UnTypedEntitySchema>
          }
        }
        {
          for(params <- value.datasetParameters) yield {
            <Dataset>{
              XmlSerialization.serializeParameters(params)
            }</Dataset>
          }
        }
      </CachedEntitySchemata>
    }
  }
}