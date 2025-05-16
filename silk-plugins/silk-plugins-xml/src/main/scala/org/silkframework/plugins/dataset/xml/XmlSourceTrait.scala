package org.silkframework.plugins.dataset.xml

import org.silkframework.dataset.DataSource
import org.silkframework.entity.EntitySchema
import org.silkframework.entity.paths.TypedPath
import org.silkframework.execution.EntityHolder
import org.silkframework.execution.local.EmptyEntityTable
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.util.Uri

/**
  * A trait that all XML data source implementations should implement
  */
trait XmlSourceTrait { this: DataSource =>
  def retrieveXmlPaths(typeUri: Uri, depth: Int, limit: Option[Int], onlyLeafNodes: Boolean, onlyInnerNodes: Boolean): IndexedSeq[TypedPath]

  /**
    * Retrieves a list of entities from this source.
    *
    * @param entitySchema The entity schema
    * @param entities     The URIs of the entities to be retrieved.
    * @return A Traversable over the entities. The evaluation of the Traversable may be non-strict.
    */
  override def retrieveByUri(entitySchema: EntitySchema, entities: Seq[Uri])
                            (implicit context: PluginContext): EntityHolder = {
    if(entities.isEmpty) {
      EmptyEntityTable(underlyingTask)
    } else {
      val uriSet = entities.map(_.uri.toString).toSet
      retrieve(entitySchema).filter(entity => uriSet.contains(entity.uri.toString))
    }
  }
}
