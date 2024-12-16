package org.silkframework.execution.typed

import org.silkframework.entity.paths.UntypedPath
import org.silkframework.util.Uri

/**
 * Creates new types and paths for type entity schemata.
 */
object TypedEntitiesVocab {

  // Namespace prefix to be used
  final val prefix = "entity"

  // Types and paths share this namespace
  final val namespace = "https://vocab.eccenca.com/di/entity/"

  /**
   * Creates a new  entity schema type URI
   */
  def schemaType(suffix: String): Uri = {
    Uri(namespace + suffix)
  }

  /**
   * Creates a new entity schema path.
   */
  def schemaPath(suffix: String): UntypedPath = {
    UntypedPath(namespace + suffix)
  }

}
