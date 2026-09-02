package org.silkframework.workspace.changes

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.config.MetaData
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.dataset.{Dataset, DatasetSpec}
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.plugins.dataset.text.TextFileDataset
import org.silkframework.rule._
import org.silkframework.rule.input.PathInput
import org.silkframework.runtime.activity.{SimpleUserContext, TestUserContextTrait, UserContext, UserExecutionContext}
import org.silkframework.runtime.plugin.{ParameterStringValue, ParameterTemplateValue, ParameterValues, PluginContext, PluginRegistry}
import org.silkframework.runtime.templating.{SimpleSubstitutionTemplateEngine, TemplateVariable, TemplateVariables, VariableScope}
import org.silkframework.runtime.users.DefaultUserManager
import org.silkframework.runtime.validation.{BadUserInputException, NotFoundException}
import org.silkframework.util.{ConfigTestTrait, Uri}
import org.silkframework.workspace.variables.{DeleteVariableModification, UpdateVariableModification}
import org.silkframework.workspace.{ProjectTask, TestWorkspaceProviderTestTrait, WorkspaceFactory}

import java.util.concurrent.CyclicBarrier
import scala.util.{Failure, Try}

class ChangeJournalTest extends AnyFlatSpec with Matchers with TestWorkspaceProviderTestTrait with TestUserContextTrait with ConfigTestTrait {

  behavior of "ChangeJournal"

  override def workspaceProviderId: String = "inMemoryWorkspaceProvider"

  // The jinja engine is not on this module's classpath; the simple engine substitutes '{{scope.name}}' references.
  override def propertyMap: Map[String, Option[String]] = Map(
    "config.variables.engine" -> Some(SimpleSubstitutionTemplateEngine.id),
    // No store is configured by default, which records nothing.
    "workspace.changes.plugin" -> Some("inMemoryChangeJournal")
  )

  /** An agent's user context; without an origin the write does not queue for review. */
  private def agentContext(origin: Option[String] = Some("mcp:test")): UserContext = {
    SimpleUserContext(Some(DefaultUserManager.get("urn:agent")), UserExecutionContext(origin = origin))
  }

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
    entries.map(_.change.describe) shouldBe Seq("Added transform 'transform'", "Updated transform 'transform'", "Removed transform 'transform'")
    entries.map(_.reverts) shouldBe Seq(None, None, None)
    // The task parameters may be sensitive, so a change never prints the task data
    entries.map(_.change.toString) shouldBe Seq("AddTask(transform)", "ReplaceTask(transform)", "RemoveTask(transform)")
  }

  it should "not record an update of a file based dataset that changes nothing" in {
    val project = retrieveOrCreateProject("journalDatasetUpdate")
    implicit val pluginContext: PluginContext = PluginContext.fromProject(project)
    def dataset: GenericDatasetSpec = {
      DatasetSpec(PluginRegistry.create[Dataset]("text", ParameterValues(Map("file" -> ParameterStringValue("data.txt")))))
    }
    project.addTask[GenericDatasetSpec]("dataset", dataset)
    project.updateTask[GenericDatasetSpec]("dataset", dataset)
    project.changeJournal.all.map(_.change.describe) shouldBe Seq("Added Text dataset 'dataset'")

    // The recording wrapper keeps the value equality of the resources it wraps, also in sub directories
    project.resources.get("data.txt") shouldBe project.resources.get("data.txt")
    project.resources.child("sub").get("data.txt") shouldBe project.resources.child("sub").get("data.txt")
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
    // The restored task keeps its creation metadata and is stamped as modified by the reverting user
    val created = added.change.asInstanceOf[AddTask].task.metaData
    val agent = agentContext(origin = None)
    journal.revert(removed.seq)(agent)
    val restored = project.task[TransformSpec]("other")
    restored.data shouldBe transform(name)
    restored.metaData.created shouldBe created.created
    restored.metaData.createdByUser shouldBe created.createdByUser
    restored.metaData.lastModifiedByUser shouldBe Some(Uri("urn:agent"))
  }

  it should "apply one of two concurrent reverts of the same change" in {
    val project = retrieveOrCreateProject("journalConcurrentRevert")
    val journal = project.changeJournal
    project.addTask[TransformSpec]("transform", transform(name))
    project.updateTask[TransformSpec]("transform", transform(name, age))
    val update = journal.all.last

    val barrier = new CyclicBarrier(2)
    val outcomes = new Array[Try[ChangeEntry]](2)
    val threads = for(i <- 0 until 2) yield new Thread(() => {
      barrier.await()
      outcomes(i) = Try(journal.revert(update.seq))
    })
    threads.foreach(_.start())
    threads.foreach(_.join())

    outcomes.count(_.isSuccess) shouldBe 1
    outcomes.collectFirst { case Failure(ex) => ex }.get shouldBe a[ChangeConflictException]
    journal.all.count(_.reverts.contains(update.seq)) shouldBe 1
    ruleIds(project.task[TransformSpec]("transform")) shouldBe Seq("name")
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
    task.applyChange(UpdateMapping.of(task, "city", city.copy(id = "town")))
    ruleIds(task) shouldBe Seq("name", "age", "town")
    val reverted = project.changeJournal.revert(added.seq)
    reverted.change shouldBe RemoveMapping("transform", "root", age, Some(1))
    ruleIds(task) shouldBe Seq("name", "town")
  }

  it should "not take the operators inside a rule for rules" in {
    val project = retrieveOrCreateProject("journalRuleOperators")
    val complex = ComplexMapping(id = "complex", operator = PathInput(id = "input", path = UntypedPath("name")),
      target = Some(MappingTarget("http://example.org/name")))
    val task = project.addTask[TransformSpec]("transform", transform(complex, age))
    // The rule JSON shows the ids of the input operators, but they do not name a rule
    a[NotFoundException] should be thrownBy RemoveMapping.of(task, "input")
    a[NotFoundException] should be thrownBy UpdateMapping.of(task, "input", age)
    a[NotFoundException] should be thrownBy ReorderMappings.of(task, "input", Seq.empty)
    // A value rule holds no child rules, so there is nothing to reorder under it
    a[BadUserInputException] should be thrownBy ReorderMappings.of(task, "complex", Seq.empty)
  }

  it should "validate a rule addition as requested" in {
    val project = retrieveOrCreateProject("journalAddMapping")
    val address = ObjectMapping(id = "address", rules = MappingRules(propertyRules = Seq(city)))
    val task = project.addTask[TransformSpec]("transform", transform(name, address))
    AddMapping.of(task, "address", age, Some(0)) shouldBe AddMapping("transform", "address", age, Some(0))
    // Removing a container rule mentions the nested rules it takes with it
    RemoveMapping.of(task, "address").describe shouldBe
      "Removed mapping rule 'address' and its nested rule from transform 'transform'"
    // A taken id is rejected as input, naming the parent that holds it; so is one that a nested rule brings along
    (the[BadUserInputException] thrownBy AddMapping.of(task, "root", city)).getMessage should
      include("A rule with id 'city' already exists in this transform (under parent 'address')")
    val nested = ObjectMapping(id = "other", rules = MappingRules(propertyRules = Seq(name)))
    a[BadUserInputException] should be thrownBy AddMapping.of(task, "root", nested)
    // The parent must exist and hold child rules
    a[NotFoundException] should be thrownBy AddMapping.of(task, "missing", age)
    a[BadUserInputException] should be thrownBy AddMapping.of(task, "name", age)
  }

  it should "restore a removed rule at its position" in {
    val project = retrieveOrCreateProject("journalRemove")
    val task = project.addTask[TransformSpec]("transform", transform(name, age, city))
    task.applyChange(RemoveMapping.of(task, "age"))
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
    task.applyChange(UpdateMapping.of(task, "name", renamedTarget))
    val update = project.changeJournal.all.last
    task.applyChange(UpdateMapping.of(task, "name", renamedTarget.copy(id = "fullName")))
    ruleIds(task) shouldBe Seq("fullName")

    a[ChangeConflictException] should be thrownBy project.changeJournal.revert(update.seq)
    ruleIds(task) shouldBe Seq("fullName")
  }

  it should "reorder rules and revert the order" in {
    val project = retrieveOrCreateProject("journalReorder")
    val journal = project.changeJournal
    val rules = MappingRules(typeRules = Seq(TypeMapping(id = "type")), propertyRules = Seq(name, age, city))
    val task = project.addTask[TransformSpec]("transform", TransformSpec(mappingRule = RootMappingRule(rules)))
    val reorder = ReorderMappings.of(task, "root", Seq("city", "name", "age"))
    reorder shouldBe ReorderMappings("transform", "root", Seq("name", "age", "city"), Seq("city", "name", "age"))
    reorder.describe shouldBe "Reordered mapping rules under 'root' in transform 'transform'"
    task.applyChange(reorder)
    // The type rule stays ahead of the property rules
    ruleIds(task) shouldBe Seq("type", "city", "name", "age")

    // The new order must name each rule once
    a[BadUserInputException] should be thrownBy ReorderMappings.of(task, "root", Seq("city", "city", "age"))
    an[IllegalArgumentException] should be thrownBy ReorderMappings("transform", "root", Seq("name"), Seq("age"))

    val reverted = journal.revert(journal.all.last.seq)
    ruleIds(task) shouldBe Seq("type", "name", "age", "city")
    // Redoing the reorder refuses once the rules were reordered again
    task.applyChange(ReorderMappings.of(task, "root", Seq("age", "name", "city")))
    a[ChangeConflictException] should be thrownBy journal.revert(reverted.seq)
    ruleIds(task) shouldBe Seq("type", "age", "name", "city")
  }

  it should "attribute entries to the user and origin of the request" in {
    val project = retrieveOrCreateProject("journalOrigin")
    val agent = agentContext()
    project.addTask[TransformSpec]("transform", transform(name))(implicitly, agent)

    val entry = project.changeJournal.all.last
    entry.user shouldBe Some("urn:agent")
    entry.origin shouldBe Some("mcp:test")
  }

  it should "describe changes by their labels when set" in {
    val project = retrieveOrCreateProject("journalLabels")
    val journal = project.changeJournal
    val task = project.addTask[TransformSpec]("transform", transform(name), MetaData(Some("Persons")))
    journal.all.last.change.describe shouldBe "Added transform 'Persons'"

    val labeledCity = city.copy(metaData = MetaData(Some("City")))
    task.applyChange(AddMapping.of(task, "root", labeledCity))
    val added = journal.all.last
    added.change.describe shouldBe "Added mapping rule 'City' under 'root' in transform 'Persons'"

    // A whole-task update names the task as the update left it; a revert keeps the label captured with the change
    project.updateTask[TransformSpec]("transform", transform(name, labeledCity), Some(MetaData(Some("People"))))
    journal.all.last.change.describe shouldBe "Updated transform 'People', renamed from 'Persons'"
    journal.revert(added.seq).change.describe shouldBe "Removed mapping rule 'City' from transform 'Persons'"
  }

  it should "track open workflow run proposals until they are discarded or consumed" in {
    val project = retrieveOrCreateProject("journalProposals")
    val journal = project.changeJournal
    val agent = agentContext()

    // A proposal queues for review like any other agent change
    val proposal = journal.propose(ProposedWorkflowRun("wf"))(agent)
    proposal.change.describe shouldBe "Proposed to run workflow 'wf'"
    journal.openRunProposal("wf") shouldBe Some(proposal)
    journal.openRunProposal("other") shouldBe None
    journal.unreviewed.map(_.seq) shouldBe Seq(proposal.seq)

    // Discarding a proposal is reverting it; the discard itself cannot be reverted
    val discarded = journal.revert(proposal.seq)
    discarded.change.describe shouldBe "Discarded the proposed run of workflow 'wf'"
    discarded.change.inverse shouldBe None
    journal.openRunProposal("wf") shouldBe None
    journal.unreviewed shouldBe empty

    // A later run of the task consumes the open proposal
    val again = journal.propose(ProposedWorkflowRun("wf"))(agent)
    journal.openRunProposal("wf") shouldBe Some(again)
    journal.record(WorkflowExecuted("wf", None, failed = false))(agent)
    journal.openRunProposal("wf") shouldBe None
  }

  it should "track the reviewed watermark over the agent entries" in {
    val project = retrieveOrCreateProject("journalWatermark")
    val journal = project.changeJournal
    val agent = agentContext()
    project.addTask[TransformSpec]("byUser", transform(name))
    project.addTask[TransformSpec]("byAgent", transform(age))(implicitly, agent)
    project.addTask[TransformSpec]("alsoAgent", transform(city))(implicitly, agent)

    // The user's own writes do not queue for review
    journal.reviewedUpTo shouldBe 0
    journal.unreviewed.map(_.change.describe) shouldBe Seq("Added transform 'byAgent'", "Added transform 'alsoAgent'")

    // Reviews only add up; a review beyond the latest change is refused
    journal.markReviewed(2)
    journal.reviewedUpTo shouldBe 2
    journal.unreviewed.map(_.change.describe) shouldBe Seq("Added transform 'alsoAgent'")
    journal.markReviewed(1)
    journal.reviewedUpTo shouldBe 2
    a[ChangeConflictException] should be thrownBy journal.markReviewed(99)

    // A reverted entry needs no review anymore; reverting does not move the watermark
    journal.revert(3)
    journal.unreviewed shouldBe empty
    journal.reviewedUpTo shouldBe 2
    journal.markReviewed(4)
    journal.reviewedUpTo shouldBe 4
  }

  it should "revert entries newest-first, skipping what cannot be reverted and stopping at a conflict" in {
    val project = retrieveOrCreateProject("journalRevertAll")
    val journal = project.changeJournal
    val task = project.addTask[TransformSpec]("transform", transform(name))
    task.applyChange(AddMapping("transform", "root", age))
    task.applyChange(AddMapping("transform", "root", city))
    journal.revert(3)

    // An unknown entry and a reverted one are skipped, the rest unwinds newest-first back to the empty project
    val outcomes = journal.revertAll(Seq(1, 2, 3, 99))
    outcomes.map(_.seq) shouldBe Seq(99, 3, 2, 1)
    outcomes.take(2).foreach(_ shouldBe a[RevertOutcome.Skipped])
    outcomes.drop(2).foreach(_ shouldBe a[RevertOutcome.Reverted])
    project.anyTaskOption("transform") shouldBe None

    // A conflict stops the batch and leaves the older entries unattempted
    val second = project.addTask[TransformSpec]("second", transform(name, age))
    val added = journal.all.last.seq
    val renamed = name.copy(mappingTarget = MappingTarget("http://example.org/fullName"))
    second.applyChange(UpdateMapping.of(second, "name", renamed))
    val update = journal.all.last.seq
    second.applyChange(UpdateMapping.of(second, "name", renamed.copy(id = "fullName")))

    val stopped = journal.revertAll(Seq(added, update))
    stopped.map(_.seq) shouldBe Seq(update, added)
    stopped.head shouldBe a[RevertOutcome.Conflict]
    stopped.last shouldBe RevertOutcome.NotAttempted(added)
    ruleIds(second) shouldBe Seq("fullName", "age")
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

    project.changeJournal.all.map(_.change.describe) shouldBe Seq("Added variable 'a' = '1'", "Added variable 'b' = '2'",
      "Set variable 'b': '2' → '3'", "Added variable 'c' = '4'", "Removed variable 'a' ('1')",
      "Added variable 'd' = template '{{project.b}}'")

    // The value of a sensitive variable is never printed; a long value is shortened
    val secret = variable("s", "secret").copy(isSensitive = true)
    SetVariable(None, secret).describe shouldBe "Added variable 's'"
    SetVariable(Some(secret), secret.copy(value = "other")).describe shouldBe "Set variable 's'"
    RemoveVariable(secret).describe shouldBe "Removed variable 's'"
    SetVariable(None, variable("l", "x" * 60)).describe shouldBe s"Added variable 'l' = '${"x" * 50}…'"
  }

  it should "apply concurrent variable modifications without losing one" in {
    val project = retrieveOrCreateProject("journalConcurrentVariables")
    val barrier = new CyclicBarrier(2)
    val outcomes = new Array[Try[Unit]](2)
    val threads = for((name, i) <- Seq("a", "b").zipWithIndex) yield new Thread(() => {
      barrier.await()
      outcomes(i) = Try(UpdateVariableModification(project, variable(name, "1")).execute())
    })
    threads.foreach(_.start())
    threads.foreach(_.join())
    outcomes.foreach(_.get)

    // Each modification reads, computes and writes; as one step, neither overwrites the other
    project.templateVariables.all.map.keySet shouldBe Set("a", "b")
    project.changeJournal.all.map(_.change.describe).sorted shouldBe Seq("Added variable 'a' = '1'", "Added variable 'b' = '1'")
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
    journal.all.drop(recorded).map(_.change.describe) shouldBe Seq("Set variable 'fileName': 'a.csv' → 'b.csv'")
    val reverted = journal.revert(journal.all.last.seq)
    file shouldBe "a.csv"
    reverted.reverts shouldBe Some(journal.all(recorded).seq)
    journal.all.drop(recorded + 1).map(_.change.describe) shouldBe Seq("Set variable 'fileName': 'b.csv' → 'a.csv'")

    // A variable that a task uses cannot be removed by reverting its addition
    a[ChangeConflictException] should be thrownBy journal.revert(journal.all.head.seq)
    file shouldBe "a.csv"
    project.templateVariables.all.map("fileName").value shouldBe "a.csv"
  }

  it should "record the file writes and deletions of a request and revert a creation while the file is unchanged" in {
    val project = retrieveOrCreateProject("journalFiles")
    val journal = project.changeJournal
    val file = project.resources.get("data.txt")
    // A write outside of a request, e.g. by a workflow run, is not recorded
    file.writeString("by an activity")
    file.delete()
    journal.all shouldBe empty

    ChangeJournal.onBehalfOf(implicitly[UserContext]) {
      file.writeString("first")
      file.writeString("second!")
      file.delete()
      journal.all.map(_.change.describe) shouldBe
        Seq("Added file 'data.txt' (5 B)", "Overwrote file 'data.txt' (5 B → 7 B)", "Deleted file 'data.txt' (7 B)")
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

      // Deleting a directory records the deletion of each file in it and removes the directory
      project.resources.child("folder").get("nested.txt").writeString("content")
      project.resources.delete("folder")
      journal.all.map(_.change.describe).takeRight(2) shouldBe
        Seq("Added file 'folder/nested.txt' (7 B)", "Deleted file 'folder/nested.txt' (7 B)")
      project.resources.listChildren should not contain "folder"
    }
  }

  it should "start with an empty journal when a project is re-created after deletion" in {
    val project = retrieveOrCreateProject("journalRecreated")
    project.addTask[TransformSpec]("transform", transform(name))
    project.changeJournal.all should not be empty
    project.changeJournal.markReviewed(1)

    WorkspaceFactory().workspace.removeProject("journalRecreated")
    val recreated = retrieveOrCreateProject("journalRecreated").changeJournal
    recreated.all shouldBe empty
    recreated.reviewedUpTo shouldBe 0
  }
}
