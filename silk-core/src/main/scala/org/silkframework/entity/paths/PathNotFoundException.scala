package org.silkframework.entity.paths

/**
  * Thrown if a path does not exist on an entity.
  *
  * @param path The path that has not been found
  * @param typeUri The type of the entity on which the path has been requested.
  * @param availablePaths All known available paths.
  */
case class PathNotFoundException(path: Path, typeUri: String, availablePaths: Iterable[Path])
  extends Exception(s"Path '$path' not found for type '$typeUri'. Available paths: ${availablePaths.mkString(", ")}")
