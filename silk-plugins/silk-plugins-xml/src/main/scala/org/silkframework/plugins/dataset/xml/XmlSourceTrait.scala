package org.silkframework.plugins.dataset.xml

import org.silkframework.dataset.DataSource
import org.silkframework.entity.{Entity, EntitySchema, Path}
import org.silkframework.util.Uri

/**
  * A trait that all XML data source implementations should implement
  */
trait XmlSourceTrait { this: DataSource =>
  def retrieveXmlPaths(typeUri: Uri, depth: Int, limit: Option[Int], onlyLeafNodes: Boolean, onlyInnerNodes: Boolean): IndexedSeq[Path]

  /**
    * Retrieves a list of entities from this source.
    *
    * @param entitySchema The entity schema
    * @param entities     The URIs of the entities to be retrieved.
    * @return A Traversable over the entities. The evaluation of the Traversable may be non-strict.
    */
  override def retrieveByUri(entitySchema: EntitySchema, entities: Seq[Uri]): Seq[Entity] = {
    val uriSet = entities.map(_.uri).toSet
    retrieve(entitySchema).filter(entity => uriSet.contains(entity.uri)).toSeq
  }

}
