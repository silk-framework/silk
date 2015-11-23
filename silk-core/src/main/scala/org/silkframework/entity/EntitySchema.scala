package org.silkframework.entity

import org.silkframework.util.Uri

/**
 * An entity schema.
 *
 * @param typ The entity type
 * @param paths The list of paths
 * @param filter A filter for restricting the entity set
 */
case class EntitySchema(typ: Uri, paths: IndexedSeq[Path], filter: Restriction) {
  require(filter.paths.forall(paths.contains), "All paths that are used in restriction must be contained in paths list.")

  /**
   * Retrieves the index of a given path.
   */
  def pathIndex(path: Path) = {
    var index = 0
    while (path != paths(index)) {
      index += 1
      if (index >= paths.size)
        throw new NoSuchElementException(s"Path $path not found on entity. Available paths: ${paths.mkString(", ")}.")
    }
    index
  }
}