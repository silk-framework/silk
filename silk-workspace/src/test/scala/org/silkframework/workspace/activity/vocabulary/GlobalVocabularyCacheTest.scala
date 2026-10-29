package org.silkframework.workspace.activity.vocabulary

import org.scalatest.concurrent.Eventually
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.rule.vocab.{Vocabulary, VocabularyManager}
import org.silkframework.runtime.activity.{Activity, Status, TestUserContextTrait, UserContext}
import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.util.{ConfigTestTrait, Identifier}

class GlobalVocabularyCacheTest extends AnyFlatSpec with Matchers with Eventually with ConfigTestTrait with TestUserContextTrait {

  behavior of "GlobalVocabularyCache"

  PluginRegistry.registerPlugin(classOf[GlobalVocabularyCacheTest.TestVocabularyManager])

  import GlobalVocabularyCacheTest._

  it should "re-queue force-reload requests if the run fails" in {
    GlobalVocabularyCache.clearAndGetVocabularies // Start with an empty queue
    installed = Seq("http://vocabA")
    failOnGet = true
    blockOnGet = false
    GlobalVocabularyCache.putVocabularyInQueue("http://vocabA")
    val activity = Activity(GlobalVocabularyCache())
    intercept[RuntimeException] {
      activity.startBlocking()
    }
    // The failed run must not lose the request
    GlobalVocabularyCache.clearAndGetVocabularies mustBe Set("http://vocabA")
  }

  it should "re-queue force-reload requests that were skipped because the run was cancelled" in {
    GlobalVocabularyCache.clearAndGetVocabularies // Start with an empty queue
    val vocabularies = Set("http://vocab1", "http://vocab2")
    installed = vocabularies.toSeq
    failOnGet = false
    blockOnGet = true
    released = false
    getEntered = false
    vocabularies.foreach(GlobalVocabularyCache.putVocabularyInQueue)
    val activity = Activity(GlobalVocabularyCache())
    activity.start()
    eventually { getEntered mustBe true } // the first installation is in progress
    activity.cancel()
    released = true
    eventually { activity.status() mustBe a[Status.Finished] }
    // The request whose installation was skipped by the cancellation must be re-queued
    val requeued = GlobalVocabularyCache.clearAndGetVocabularies
    requeued.size mustBe 1
    vocabularies must contain allElementsOf requeued
  }

  override def propertyMap: Map[String, Option[String]] = Map(
    "vocabulary.manager.plugin" -> Some("testGlobalVocabularyCacheManager")
  )
}

object GlobalVocabularyCacheTest {

  @volatile var installed: Seq[String] = Seq.empty
  @volatile var failOnGet: Boolean = false
  @volatile var blockOnGet: Boolean = false
  @volatile var released: Boolean = false
  @volatile var getEntered: Boolean = false

  /** Vocabulary manager whose get() can be made to fail or to block until released. */
  @Plugin(id = "testGlobalVocabularyCacheManager", label = "Test vocabulary manager")
  case class TestVocabularyManager() extends VocabularyManager {

    override def get(uri: String, project: Option[Identifier])(implicit userContext: UserContext): Option[Vocabulary] = {
      getEntered = true
      if (failOnGet) {
        throw new RuntimeException("Intentional test failure while loading a vocabulary")
      }
      while (blockOnGet && !released) {
        try Thread.sleep(10) catch { case _: InterruptedException => } // Keep blocking until released, even when cancelled
      }
      None // Counts as processed; the cache logs a warning that no vocabulary was found
    }

    override def retrieveGlobalVocabularies()(implicit userContext: UserContext): Option[Iterable[String]] = Some(installed)
  }
}
