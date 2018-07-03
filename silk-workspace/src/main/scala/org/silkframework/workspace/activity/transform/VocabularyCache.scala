package org.silkframework.workspace.activity.transform

import org.silkframework.rule.TransformSpec
import org.silkframework.rule.vocab.VocabularyManager
import org.silkframework.runtime.activity.ActivityContext
import org.silkframework.runtime.resource.WritableResource
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.{CachedActivity, PathsCacheTrait}

/**
 * Holds the target vocabularies.
 */
class VocabularyCache(task: ProjectTask[TransformSpec]) extends CachedActivity[VocabularyCacheValue] with PathsCacheTrait {

  override def name: String = s"Vocabulary cache ${task.id}"

  override def initialValue: Option[VocabularyCacheValue] = Some(new VocabularyCacheValue(Seq.empty))

  override def run(context: ActivityContext[VocabularyCacheValue]): Unit = {
    val transform = task.data
    if(transform.targetVocabularies.nonEmpty) {
      val vocabManager = VocabularyManager()
      val vocabularies = for (vocab <- transform.targetVocabularies) yield vocabManager.get(vocab, task.project.name)
      context.value() = new VocabularyCacheValue(vocabularies.toSeq.sortBy(_.info.uri))
    }
  }

  override def resource: WritableResource = task.project.cacheResources.child("transform").child(task.id).get(s"vocabularyCache.xml")

  val wrappedXmlFormat: WrappedXmlFormat = WrappedXmlFormat()(VocabularyCache.ValueFormat)
}