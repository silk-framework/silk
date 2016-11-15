package org.silkframework.workspace.activity.transform

import org.silkframework.rule.TransformSpec
import org.silkframework.runtime.plugin.Plugin
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.{CachedActivity, TaskActivityFactory}

@Plugin(
  id = "VocabularyCache",
  label = "Target Vocabulary Cache",
  categories = Array("TransformSpecification"),
  description = "Holds the target vocabularies"
)
case class VocabularyCacheFactory() extends TaskActivityFactory[TransformSpec, VocabularyCache] {

  override def autoRun = true

  def apply(task: ProjectTask[TransformSpec]) = {
    new CachedActivity(
      activity = new VocabularyCache(task),
      resource = task.project.cacheResources.child("transform").child(task.id).get(s"vocabularyCache.xml")
    )
  }
}
