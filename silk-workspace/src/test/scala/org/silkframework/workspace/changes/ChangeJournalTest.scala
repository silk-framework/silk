package org.silkframework.workspace.changes

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.dataset.{Dataset, DatasetSpec}
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.plugins.dataset.text.TextFileDataset
import org.silkframework.rule._
import org.silkframework.runtime.activity.{SimpleUserContext, TestUserContextTrait, UserExecutionContext}
import org.silkframework.runtime.plugin.{ParameterTemplateValue, ParameterValues, PluginContext, PluginRegistry}
import org.silkframework.runtime.templating.{SimpleSubstitutionTemplateEngine, TemplateVariable, TemplateVariables, VariableScope}
import org.silkframework.runtime.users.DefaultUserManager
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.util.ConfigTestTrait
import org.silkframework.workspace.variables.{DeleteVariableModification, UpdateVariableModification}
import org.silkframework.workspace.{ProjectTask, TestWorkspaceProviderTestTrait}

class ChangeJournalTest extends AnyFlatSpec with Matchers with TestWorkspaceProviderTestTrait with TestUserContextTrait with ConfigTestTrait {

  behavior of "ChangeJournal"

  override def workspaceProviderId: String = "inMemoryWorkspaceProvider"

  // The jinja engine is not on this module's classpath; the simple engine substitutes '{{scope.name}}' references.
  override def propertyMap: Map[String, Option[String]] = Map("config.variables.engine" -> Some(SimpleSubstitutionTemplateEngine.id))

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

  it should "reorder rules and revert the order" in {
    val project = retrieveOrCreateProject("journalReorder")
    val journal = project.changeJournal
    val rules = MappingRules(typeRules = Seq(TypeMapping(id = "type")), propertyRules = Seq(name, age, city))
    val task = project.addTask[TransformSpec]("transform", TransformSpec(mappingRule = RootMappingRule(rules)))
    val reorder = ReorderMappings.of("transform", task.data, "root", Seq("city", "name", "age"))
    reorder shouldBe ReorderMappings("transform", "root", Seq("name", "age", "city"), Seq("city", "name", "age"))
    reorder.describe shouldBe "Reordered mapping rules under 'root' in transform 'transform'"
    task.applyChange(reorder)
    // The type rule stays ahead of the property rules
    ruleIds(task) shouldBe Seq("type", "city", "name", "age")

    // The new order must name each rule once
    a[BadUserInputException] should be thrownBy ReorderMappings.of("transform", task.data, "root", Seq("city", "city", "age"))
    an[IllegalArgumentException] should be thrownBy ReorderMappings("transform", "root", Seq("name"), Seq("age"))

    val reverted = journal.revert(journal.all.last.seq)
    ruleIds(task) shouldBe Seq("type", "name", "age", "city")
    // Redoing the reorder refuses once the rules were reordered again
    task.applyChange(ReorderMappings.of("transform", task.data, "root", Seq("age", "name", "city")))
    a[ChangeConflictException] should be thrownBy journal.revert(reverted.seq)
    ruleIds(task) shouldBe Seq("type", "age", "name", "city")
  }

  it should "attribute entries to the user and origin of the request" in {
    val project = retrieveOrCreateProject("journalOrigin")
    val agent = SimpleUserContext(Some(DefaultUserManager.get("urn:agent")), UserExecutionContext(origin = Some("mcp:test")))
    project.addTask[TransformSpec]("transform", transform(name))(implicitly, agent)

    val entry = project.changeJournal.all.last
    entry.user shouldBe Some("urn:agent")
    entry.origin shouldBe Some("mcp:test")
  }

  private def variable(name: String, value: String, template: Option[String] = None): TemplateVariable = {
    TemplateVariable(name, value, template, scope = VariableScope.project)
  }

  it should "record every variable addition, change and removal" in {
    val project = retrieveOrCreateProject("journalVariables")
    val variables = project.templateVariables
    variables.put(TemplateVariables(Seq(variable("a", "1"), variable("b", "2"))))
    // A reorder is not recorded
    variables.put(TemplateVariables(Seq(variable("b", "2"), variable("a", "1"))))
    variables.put(TemplateVariables(Seq(variable("b", "3"), variable("c", "4"))))
    // The value of a templated variable is derived and not compared
    variables.put(TemplateVariables(Seq(variable("b", "3"), variable("c", "4"), variable("d", "3", Some("{{project.b}}")))))
    variables.put(TemplateVariables(Seq(variable("b", "3"), variable("c", "4"), variable("d", "other", Some("{{project.b}}")))))

    project.changeJournal.all.map(_.change.describe) shouldBe Seq("Added variable 'a'", "Added variable 'b'",
      "Set variable 'b'", "Added variable 'c'", "Removed variable 'a'", "Added variable 'd'")
  }

  it should "revert variable changes while the variable is unchanged" in {
    val project = retrieveOrCreateProject("journalRevertVariables")
    val journal = project.changeJournal
    def value(name: String): Option[String] = project.templateVariables.all.map.get(name).map(_.value)
    UpdateVariableModification(project, variable("a", "1")).execute()
    UpdateVariableModification(project, variable("a", "2")).execute()
    val set = journal.all.last
    set.change shouldBe SetVariable(Some(variable("a", "1")), variable("a", "2"))

    // Revert the change, then revert the revert
    val reverted = journal.revert(set.seq)
    reverted.reverts shouldBe Some(set.seq)
    reverted.change shouldBe SetVariable(Some(variable("a", "2")), variable("a", "1"))
    value("a") shouldBe Some("1")
    val redone = journal.revert(reverted.seq)
    value("a") shouldBe Some("2")
    // Refused once the variable changed again
    UpdateVariableModification(project, variable("a", "3")).execute()
    a[ChangeConflictException] should be thrownBy journal.revert(redone.seq)
    value("a") shouldBe Some("3")

    // Reverting an addition removes the variable, reverting that adds it back, unless the name is taken again
    UpdateVariableModification(project, variable("b", "1")).execute()
    val added = journal.all.last
    val removed = journal.revert(added.seq)
    removed.change shouldBe RemoveVariable(variable("b", "1"))
    value("b") shouldBe None
    UpdateVariableModification(project, variable("b", "9")).execute()
    a[ChangeConflictException] should be thrownBy journal.revert(removed.seq)
    DeleteVariableModification(project, "b").execute()
    journal.revert(removed.seq)
    value("b") shouldBe Some("1")
  }

  it should "let the tasks that use a variable follow it without recording them" in {
    val project = retrieveOrCreateProject("journalVariableTasks")
    val journal = project.changeJournal
    UpdateVariableModification(project, variable("fileName", "a.csv")).execute()
    implicit val pluginContext: PluginContext = PluginContext.fromProject(project)
    val dataset = PluginRegistry.create[Dataset]("text", ParameterValues(Map("file" -> ParameterTemplateValue("{{project.fileName}}"))))
    project.addTask[GenericDatasetSpec]("dataset", DatasetSpec(dataset))
    def file: String = project.task[GenericDatasetSpec]("dataset").data.plugin.asInstanceOf[TextFileDataset].file.name
    file shouldBe "a.csv"
    val recorded = journal.all.size

    // The dataset follows the variable; only the variable change is recorded
    UpdateVariableModification(project, variable("fileName", "b.csv")).execute()
    file shouldBe "b.csv"
    journal.all.drop(recorded).map(_.change.describe) shouldBe Seq("Set variable 'fileName'")
    val reverted = journal.revert(journal.all.last.seq)
    file shouldBe "a.csv"
    reverted.reverts shouldBe Some(journal.all(recorded).seq)
    journal.all.drop(recorded + 1).map(_.change.describe) shouldBe Seq("Set variable 'fileName'")

    // A variable that a task uses cannot be removed by reverting its addition
    a[ChangeConflictException] should be thrownBy journal.revert(journal.all.head.seq)
    file shouldBe "a.csv"
    project.templateVariables.all.map("fileName").value shouldBe "a.csv"
  }

  it should "record file writes and deletions and revert a creation while the file is unchanged" in {
    val project = retrieveOrCreateProject("journalFiles")
    val journal = project.changeJournal
    val file = project.resources.get("data.txt")
    file.writeString("first")
    file.writeString("second!")
    file.delete()
    journal.all.map(_.change.describe) shouldBe Seq("Added file 'data.txt'", "Overwrote file 'data.txt'", "Deleted file 'data.txt'")
    // The previous content is not kept, so only a creation has an inverse
    journal.all.map(_.change.inverse.isDefined) shouldBe Seq(true, false, false)
    (the[ChangeConflictException] thrownBy journal.revert(journal.all.last.seq)).getMessage should include("cannot be reverted")

    // Reverting a creation refuses once the file changed
    file.writeString("third")
    val created = journal.all.last
    created.change shouldBe a[ResourceCreated]
    file.writeString("third, changed")
    a[ChangeConflictException] should be thrownBy journal.revert(created.seq)
    file.loadAsString() shouldBe "third, changed"

    // Reverting a creation deletes the file
    file.delete()
    file.writeString("fourth")
    val recreated = journal.all.last
    journal.revert(recreated.seq).change shouldBe a[ResourceDeleted]
    file.exists shouldBe false
  }
}
