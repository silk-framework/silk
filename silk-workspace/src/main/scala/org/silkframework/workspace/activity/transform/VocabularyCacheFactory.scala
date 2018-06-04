package org.silkframework.workspace.activity.transform

import org.silkframework.rule.TransformSpec
import org.silkframework.runtime.plugin.Plugin
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.TaskActivityFactory

@Plugin(
  id = "VocabularyCache",
  label = "Target Vocabulary Cache",
  categories = Array("TransformSpecification"),
  description = "Holds the target vocabularies"
)
case class VocabularyCacheFactory() extends TaskActivityFactory[TransformSpec, VocabularyCache] {

  override def autoRun: Boolean = true

  def apply(task: ProjectTask[TransformSpec]): VocabularyCache = {
    new VocabularyCache(task)
  }
}
