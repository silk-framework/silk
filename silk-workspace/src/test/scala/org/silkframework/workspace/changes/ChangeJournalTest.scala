package org.silkframework.workspace.changes

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.rule._
import org.silkframework.runtime.activity.{SimpleUserContext, TestUserContextTrait, UserExecutionContext}
import org.silkframework.runtime.users.DefaultUserManager
import org.silkframework.workspace.{ProjectTask, TestWorkspaceProviderTestTrait}

class ChangeJournalTest extends AnyFlatSpec with Matchers with TestWorkspaceProviderTestTrait with TestUserContextTrait {

  behavior of "ChangeJournal"

  override def workspaceProviderId: String = "inMemoryWorkspaceProvider"

  private def rule(name: String): DirectMapping = {
    DirectMapping(id = name, sourcePath = UntypedPath(name), mappingTarget = MappingTarget("http://example.org/" + name))
  }

  private val name = rule("name")
  private val age = rule("age")
  private val city = rule("city")

  private def transform(rules: TransformRule*): TransformSpec = {
    TransformSpec(mappingRule = RootMappingRule(MappingRules(propertyRules = rules)))
  }

  private def ruleIds(task: ProjectTask[TransformSpec]): Seq[String] = task.data.mappingRule.rules.allRules.map(_.id.toString)

  it should "record every task addition, update and removal" in {
    val project = retrieveOrCreateProject("journalTasks")
    project.addTask[TransformSpec]("transform", transform(name))
    project.updateTask[TransformSpec]("transform", transform(name, age))
    // An update that changes nothing is not recorded
    project.updateTask[TransformSpec]("transform", transform(name, age))
    project.removeTask[TransformSpec]("transform")

    val entries = project.changeJournal.all
    entries.map(_.seq) shouldBe Seq(1, 2, 3)
    entries.map(_.change.describe) shouldBe Seq("Added task 'transform'", "Updated task 'transform'", "Removed task 'transform'")
    entries.map(_.reverts) shouldBe Seq(None, None, None)
  }

  it should "revert whole-task changes while the task is unchanged" in {
    val project = retrieveOrCreateProject("journalRevertTasks")
    val journal = project.changeJournal
    project.addTask[TransformSpec]("transform", transform(name))
    project.updateTask[TransformSpec]("transform", transform(name, age))

    // Revert the update, then revert the revert
    val reverted = journal.revert(2)
    reverted.reverts shouldBe Some(2)
    reverted.change shouldBe a[ReplaceTask]
    ruleIds(project.task[TransformSpec]("transform")) shouldBe Seq("name")
    val redone = journal.revert(reverted.seq)
    ruleIds(project.task[TransformSpec]("transform")) shouldBe Seq("name", "age")
    // An entry is reverted at most once
    a[ChangeConflictException] should be thrownBy journal.revert(2)
    // A whole-task revert refuses once the task changed
    project.updateTask[TransformSpec]("transform", transform(name, age, city))
    a[ChangeConflictException] should be thrownBy journal.revert(redone.seq)
    ruleIds(project.task[TransformSpec]("transform")) shouldBe Seq("name", "age", "city")

    // Reverting an addition removes the task, reverting that adds it back
    project.addTask[TransformSpec]("other", transform(name))
    val added = journal.all.last
    val removed = journal.revert(added.seq)
    removed.change shouldBe a[RemoveTask]
    project.anyTaskOption("other") shouldBe None
    journal.revert(removed.seq)
    project.task[TransformSpec]("other").data shouldBe transform(name)
  }

  it should "apply typed mapping changes and revert them in place" in {
    val project = retrieveOrCreateProject("journalMappings")
    val task = project.addTask[TransformSpec]("transform", transform(name, city))
    task.applyChange(AddMapping("transform", "root", age, index = Some(1)))
    ruleIds(task) shouldBe Seq("name", "age", "city")
    val added = project.changeJournal.all.last
    added.change shouldBe AddMapping("transform", "root", age, Some(1))
    added.change.describe shouldBe "Added mapping rule 'age' under 'root' in transform 'transform'"

    // A later change to another rule does not block the revert, as only the added rule is removed
    task.applyChange(UpdateMapping.of("transform", task.data, "city", city.copy(id = "town")))
    ruleIds(task) shouldBe Seq("name", "age", "town")
    val reverted = project.changeJournal.revert(added.seq)
    reverted.change shouldBe RemoveMapping("transform", "root", age, Some(1))
    ruleIds(task) shouldBe Seq("name", "town")
  }

  it should "restore a removed rule at its position" in {
    val project = retrieveOrCreateProject("journalRemove")
    val task = project.addTask[TransformSpec]("transform", transform(name, age, city))
    task.applyChange(RemoveMapping.of("transform", task.data, "age"))
    ruleIds(task) shouldBe Seq("name", "city")
    project.changeJournal.revert(project.changeJournal.all.last.seq)
    ruleIds(task) shouldBe Seq("name", "age", "city")
  }

  it should "add nested rules and reject conflicting additions without recording them" in {
    val project = retrieveOrCreateProject("journalNested")
    val address = ObjectMapping(id = "address", sourcePath = UntypedPath("address"), rules = MappingRules.empty)
    val task = project.addTask[TransformSpec]("transform", transform(name, address))
    task.applyChange(AddMapping("transform", "address", city))
    task.data.nestedRuleAndSourcePath("city").map(_._1) shouldBe Some(city)

    // Rule ids are unique across the whole tree
    (the[ChangeConflictException] thrownBy task.applyChange(AddMapping("transform", "root", city)))
      .getMessage should include("already exists")
    // A value rule holds no children
    a[ChangeConflictException] should be thrownBy task.applyChange(AddMapping("transform", "name", age))
    // The parent must exist
    a[ChangeConflictException] should be thrownBy task.applyChange(AddMapping("transform", "unknown", age))

    ruleIds(task) shouldBe Seq("name", "address")
    project.changeJournal.all.map(_.change.getClass.getSimpleName) shouldBe Seq("AddTask", "AddMapping")
  }

  it should "refuse to revert a rule update once the rule changed again" in {
    val project = retrieveOrCreateProject("journalUpdate")
    val task = project.addTask[TransformSpec]("transform", transform(name))
    val renamedTarget = name.copy(mappingTarget = MappingTarget("http://example.org/fullName"))
    task.applyChange(UpdateMapping.of("transform", task.data, "name", renamedTarget))
    val update = project.changeJournal.all.last
    task.applyChange(UpdateMapping.of("transform", task.data, "name", renamedTarget.copy(id = "fullName")))
    ruleIds(task) shouldBe Seq("fullName")

    a[ChangeConflictException] should be thrownBy project.changeJournal.revert(update.seq)
    ruleIds(task) shouldBe Seq("fullName")
  }

  it should "attribute entries to the user and origin of the request" in {
    val project = retrieveOrCreateProject("journalOrigin")
    val agent = SimpleUserContext(Some(DefaultUserManager.get("urn:agent")), UserExecutionContext(origin = Some("mcp:test")))
    project.addTask[TransformSpec]("transform", transform(name))(implicitly, agent)

    val entry = project.changeJournal.all.last
    entry.user shouldBe Some("urn:agent")
    entry.origin shouldBe Some("mcp:test")
  }
}
