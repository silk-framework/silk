package org.silkframework.plugins.dataset.xml

import org.silkframework.dataset.{DataSource, DataSourceCharacteristics, SpecialPathInfo, SupportedPathExpressions}
import org.silkframework.entity.paths.TypedPath
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.execution.EntityHolder
import org.silkframework.execution.local.EmptyEntityTable
import org.silkframework.runtime.activity.UserContext
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
  override def retrieveByUri(entitySchema: EntitySchema, entities: Seq[Uri])(implicit userContext: UserContext): EntityHolder = {
    if(entities.isEmpty) {
      EmptyEntityTable(underlyingTask)
    } else {
      val uriSet = entities.map(_.uri.toString).toSet
      retrieve(entitySchema).filter(entity => uriSet.contains(entity.uri.toString))
    }
  }
}

object XmlSourceTrait {
  object SpecialXmlPaths {
    final val ID = "#id"
    final val TAG = "#tag"
    final val TEXT = "#text"
    final val ALL_CHILDREN = "*"
    final val ALL_CHILDREN_RECURSIVE = "**"
    final val BACKWARD_PATH = "\\.."
  }
  import SpecialXmlPaths._
  final val characteristics = DataSourceCharacteristics(
    SupportedPathExpressions(
      multiHopPaths = true,
      propertyFilter = true,
      specialPaths = Seq(
        SpecialPathInfo(BACKWARD_PATH, Some("Navigate to parent element.")),
        SpecialPathInfo(ID, Some("A document-wide unique ID of the entity.")),
        SpecialPathInfo(TAG, Some("The element tag of the entity.")),
        SpecialPathInfo(TEXT, Some("The concatenated text inside an element.")),
        SpecialPathInfo(ALL_CHILDREN, Some("Selects all direct children of the entity.")),
        SpecialPathInfo(ALL_CHILDREN_RECURSIVE, Some("Selects all children nested below the entity at any depth."))
      )
    )
  )
}