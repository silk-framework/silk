package org.silkframework.workspace.activity.transform

import org.silkframework.entity.paths.UntypedPath

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
  def suggestProperties(sourcePath: UntypedPath): Seq[MappingCandidate]

}
