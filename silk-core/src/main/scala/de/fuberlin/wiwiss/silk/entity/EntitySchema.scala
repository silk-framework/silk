package de.fuberlin.wiwiss.silk.entity

import de.fuberlin.wiwiss.silk.util.Uri

/**
 * Created by andreas on 10/20/15.
 */
case class EntitySchema(typ: Uri, paths: IndexedSeq[Path], filter: Restriction = Restriction.empty) {
  // TODO: Check paths in Restriction filter if they match schema paths, else IllegalArgumentException

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