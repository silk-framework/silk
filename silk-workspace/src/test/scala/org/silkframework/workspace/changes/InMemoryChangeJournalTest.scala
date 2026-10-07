package org.silkframework.workspace.changes

import org.silkframework.rule.TransformSpec
import org.silkframework.util.ConfigTestTrait

/** The journal against the in-memory store, whose cap is a number of entries. */
class InMemoryChangeJournalTest extends ChangeJournalTestTrait {

  override protected def storeProperties: Map[String, Option[String]] = Map("workspace.changes.plugin" -> Some("inMemoryChangeJournal"))

  it should "count the unreviewed entries the store's cap has dropped" in {
    ConfigTestTrait.withConfig("workspace.changes.inMemoryChangeJournal.maxEntries" -> Some("2")) {
      val project = retrieveOrCreateProject("journalDropped")
      val journal = project.changeJournal
      val agent = agentContext()
      def dropped: Int = journal.droppedUnreviewed(journal.all, journal.reviewedUpTo)
      // A user write before any agent write is passed by the watermark, so the cap drops it uncounted
      project.addTask[TransformSpec]("byUser", transform(name))
      project.addTask[TransformSpec]("first", transform(age))(implicitly, agent)
      project.addTask[TransformSpec]("second", transform(city))(implicitly, agent)
      journal.all.map(_.seq) shouldBe Seq(2, 3)
      dropped shouldBe 0
      // While agent entries wait, the watermark stays: the cap drops the oldest of them, which nobody has reviewed
      project.addTask[TransformSpec]("third", transform(name))(implicitly, agent)
      journal.all.map(_.seq) shouldBe Seq(3, 4)
      dropped shouldBe 1
      // Marking all as reviewed accepts the dropped entry too
      journal.markReviewed(4)
      dropped shouldBe 0
    }
  }
}
