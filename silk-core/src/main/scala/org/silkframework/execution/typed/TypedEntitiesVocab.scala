package org.silkframework.execution.typed

import org.silkframework.entity.paths.UntypedPath
import org.silkframework.util.Uri

/**
 * Creates new types and paths for type entity schemata.
 */
object TypedEntitiesVocab {

  // Types and paths share this prefix
  private val prefix = "https://vocab.eccenca.com/di/entity/"

  // Types are prefixed with this URI
  private val typePrefix = prefix + "type/"

  // Paths are prefixed with this URI
  private val pathPrefix = prefix + "path/"

  /**
   * Creates a new  entity schema type URI
   */
  def schemaType(suffix: String): Uri = {
    Uri(typePrefix + suffix)
  }

  /**
   * Creates a new entity schema path.
   */
  def schemaPath(suffix: String): UntypedPath = {
    UntypedPath(pathPrefix + suffix)
  }

}
