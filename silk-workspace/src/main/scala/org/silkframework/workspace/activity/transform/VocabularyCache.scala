package org.silkframework.workspace.activity.transform

import org.silkframework.dataset.rdf.SparqlEndpoint
import org.silkframework.rule.TransformSpec
import org.silkframework.rule.vocab.{Vocabularies, Vocabulary, VocabularyManager}
import org.silkframework.runtime.activity.{Activity, ActivityContext}
import org.silkframework.workspace.{ProjectTask, RdfWorkspaceProvider, User}
import org.silkframework.workspace.activity.PathsCacheTrait

/**
 * Holds the target vocabularies.
 */
class VocabularyCache(task: ProjectTask[TransformSpec]) extends Activity[Vocabularies] with PathsCacheTrait {

  override def name: String = s"Vocabulary cache ${task.id}"

  override def initialValue: Option[Vocabularies] = Some(Vocabularies.empty)

  override def run(context: ActivityContext[Vocabularies]): Unit = {
    val transform = task.data
    if(transform.targetVocabularies.nonEmpty) {
      val vocabManager = VocabularyManager()
      val vocabularies = for (vocab <- transform.targetVocabularies) yield vocabManager.get(vocab)
      context.value() = Vocabularies(vocabularies)
    }
  }



}
