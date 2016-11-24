package org.silkframework.workspace.activity.transform

import org.silkframework.entity.Path

/**
  * Inherited by cache values that can suggest mapping properties to the user.
  *
  * @see [[org.silkframework.workspace.activity.transform.VocabularyCache]]
  */
trait MappingCandidates {

  /**
    * Suggests mapping types.
    */
  def suggestTypes: Seq[MappingCandidate]

  /**
    * Suggests mapping properties for a given source path.
    */
  def suggestProperties(sourcePath: Path): Seq[MappingCandidate]

}
