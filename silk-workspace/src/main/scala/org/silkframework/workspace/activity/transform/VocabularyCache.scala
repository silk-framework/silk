package org.silkframework.workspace.activity.transform

import org.silkframework.entity.Path
import org.silkframework.rule.TransformSpec
import org.silkframework.rule.vocab.{Vocabularies, Vocabulary, VocabularyManager}
import org.silkframework.runtime.activity.{Activity, ActivityContext}
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.PathsCacheTrait
import org.silkframework.workspace.activity.transform.VocabularyCache.Value

import scala.xml.Node

/**
 * Holds the target vocabularies.
 */
class VocabularyCache(task: ProjectTask[TransformSpec]) extends Activity[Value] with PathsCacheTrait {

  override def name: String = s"Vocabulary cache ${task.id}"

  override def initialValue: Option[Value] = Some(new Value(Seq.empty))

  override def run(context: ActivityContext[Value]): Unit = {
    val transform = task.data
    if(transform.targetVocabularies.nonEmpty) {
      val vocabManager = VocabularyManager()
      val vocabularies = for (vocab <- transform.targetVocabularies) yield vocabManager.get(vocab)
      context.value() = new Value(vocabularies.toSeq.sortBy(_.info.uri))
    }
  }
}

object VocabularyCache {

  /**
    * The value of the vocabulary cache.
    * Holds the target vocabularies of the transformation and suggests types and properties from it.
    */
  class Value(vocabularies: Seq[Vocabulary]) extends Vocabularies(vocabularies) with MappingCandidates {
    /**
      * Suggests mapping types.
      */
    override def suggestTypes: Seq[MappingCandidate] = {
      for(vocab <- vocabularies; clazz <- vocab.classes) yield
        MappingCandidate(clazz.info.uri, 0.0)
    }

    /**
      * Suggests mapping properties.
      */
    override def suggestProperties(sourcePath: Path): Seq[MappingCandidate] = {
      for(vocab <- vocabularies; prop <- vocab.properties) yield
        MappingCandidate(prop.info.uri, 0.0)
    }
  }

  /**
    * XML serialization format for a the cache value.
    */
  implicit object ValueFormat extends XmlFormat[Value] {

    def read(node: Node)(implicit readContext: ReadContext) = {
      new Value(Vocabularies.VocabulariesFormat.read(node).vocabularies)
    }

    def write(desc: Value)(implicit writeContext: WriteContext[Node]): Node = {
      Vocabularies.VocabulariesFormat.write(desc)
    }
  }

}