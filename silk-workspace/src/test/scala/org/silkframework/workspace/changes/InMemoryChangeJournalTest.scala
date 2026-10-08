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

  it should "keep the dropped count until the next review" in {
    ConfigTestTrait.withConfig("workspace.changes.inMemoryChangeJournal.maxEntries" -> Some("2")) {
      val project = retrieveOrCreateProject("journalDroppedKept")
      val journal = project.changeJournal
      def dropped: Int = journal.droppedUnreviewed(journal.all, journal.reviewedUpTo)
      project.addTask[TransformSpec]("byUser", transform(name))
      project.addTask[TransformSpec]("byAgent", transform(age))(implicitly, agentContext())
      // Two user writes drop the agent entry, which nobody has reviewed
      project.addTask[TransformSpec]("third", transform(city))
      project.addTask[TransformSpec]("fourth", transform(name))
      journal.all.map(_.seq) shouldBe Seq(3, 4)
      dropped shouldBe 1
      // The kept entries hold no agent change, but the watermark must not pass the dropped one before a review
      project.addTask[TransformSpec]("fifth", transform(age))
      journal.reviewedUpTo shouldBe 1
      dropped shouldBe 2
      // After the review the watermark follows the journal again
      journal.markReviewed(5)
      dropped shouldBe 0
      project.addTask[TransformSpec]("sixth", transform(city))
      journal.reviewedUpTo shouldBe 6
    }
  }
}
