package org.silkframework.serialization.json.changes

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.config.{CustomTask, MetaData, Prefixes}
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.dataset.{Dataset, DatasetSpec}
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.entity.{Restriction, ValueType}
import org.silkframework.plugins.dataset.csv.CsvDataset
import org.silkframework.plugins.operations.SetExecutionVariableOperator
import org.silkframework.rule._
import org.silkframework.rule.input.{PathInput, TransformInput}
import org.silkframework.rule.plugins.distance.characterbased.QGramsMetric
import org.silkframework.rule.plugins.transformer.normalize.LowerCaseTransformer
import org.silkframework.rule.similarity.Comparison
import org.silkframework.runtime.activity.{SimpleUserContext, TestUserContextTrait, UserContext, UserExecutionContext}
import org.silkframework.runtime.plugin.types.IdentifierOptionParameter
import org.silkframework.runtime.plugin.{ParameterStringValue, ParameterTemplateValue, ParameterValues, PluginContext, PluginRegistry}
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.runtime.templating.{SimpleSubstitutionTemplateEngine, TemplateVariable, TemplateVariables, VariableScope}
import org.silkframework.runtime.users.DefaultUserManager
import org.silkframework.serialization.json.JsonSerializers.{GenericTaskJsonFormat, TransformRuleJsonFormat}
import org.silkframework.serialization.json.TemplateVariableJson
import org.silkframework.serialization.json.WorkflowSerializers.{WorkflowDatasetJsonFormat, WorkflowOperatorJsonFormat}
import org.silkframework.serialization.json.changes.ChangeJsonFormats.ChangeEntryJsonFormat
import org.silkframework.util.{ConfigTestTrait, DPair, Identifier, Uri}
import org.silkframework.workspace.activity.workflow.{TaskIdentifierParameter, Workflow, WorkflowDataset, WorkflowOperator}
import org.silkframework.workspace.annotation.{StickyNote, UiAnnotations}
import org.silkframework.workspace.changes._
import org.silkframework.workspace.variables.{DeleteVariableModification, UpdateVariableModification}
import org.silkframework.workspace.{Project, TestWorkspaceProviderTestTrait, WorkspaceFactory}
import play.api.libs.json.{JsValue, Json}

import java.time.Instant

/**
  * Round-trip spike for the change journal store: the journal will store its payloads as
  * JSON and its conflict checks compare them with the live objects, so a payload must read back equal to the task,
  * rule, workflow node or variable it was written from — also after the project was written by the workspace
  * provider and read back on a reload, which is where a normalization difference between the provider's format
  * and JSON would show. Subclasses pick the provider.
  */
abstract class ChangePayloadRoundTripTrait extends AnyFlatSpec with Matchers with ConfigTestTrait
    with TestWorkspaceProviderTestTrait with TestUserContextTrait {

  override def propertyMap: Map[String, Option[String]] = Map(
    // The Jinja engine is not on every test classpath
    "config.variables.engine" -> Some(SimpleSubstitutionTemplateEngine.id),
    // No store is configured by default, which records nothing
    "workspace.changes.plugin" -> Some("inMemoryChangeJournal"))

  private val projectId = Identifier("roundTrip")

  /** The project after it was written by the provider and read back by a reload. */
  private lazy val project: Project = {
    createProject()
    WorkspaceFactory().workspace.reload()
    WorkspaceFactory().workspace.project(projectId)
  }

  behavior of "The JSON payload formats after a reload"

  it should "read every task back equal to the live task" in {
    implicit val readContext: ReadContext = ReadContext.fromProject(project)
    implicit val writeContext: WriteContext[JsValue] = WriteContext.fromProject[JsValue](project)
    val tasks = project.allTasks
    tasks.map(_.id.toString).toSet shouldBe Set("persons", "output", "transform", "linking", "setVariable", "workflow")
    for(task <- tasks) {
      val json = GenericTaskJsonFormat.write(task)
      val read = GenericTaskJsonFormat.read(json)
      withClue(s"Task '${task.id}' written as\n${Json.prettyPrint(json)}\n") {
        read.data shouldBe task.data
        read.metaData.withoutUserData shouldBe task.metaData.withoutUserData
        read.executionVariables shouldBe task.executionVariables
        TaskChanges.same(task, read) shouldBe true
      }
    }
  }

  it should "read every mapping rule back equal to the live rule" in {
    implicit val readContext: ReadContext = ReadContext.fromProject(project)
    implicit val writeContext: WriteContext[JsValue] = WriteContext.fromProject[JsValue](project)
    for(rule <- project.task[TransformSpec]("transform").data.allRulesRecursive) {
      val json = TransformRuleJsonFormat.write(rule)
      withClue(s"Rule '${rule.id}' written as\n${Json.prettyPrint(json)}\n") {
        TransformRuleJsonFormat.read(json) shouldBe rule
      }
    }
  }

  it should "read every workflow node back equal to the live node" in {
    implicit val readContext: ReadContext = ReadContext.fromProject(project)
    implicit val writeContext: WriteContext[JsValue] = WriteContext.fromProject[JsValue](project)
    for(node <- project.task[Workflow]("workflow").data.nodes) {
      withClue(s"Node '${node.nodeId}'") {
        node match {
          case operator: WorkflowOperator =>
            WorkflowOperatorJsonFormat.read(WorkflowOperatorJsonFormat.write(operator)) shouldBe operator
          case dataset: WorkflowDataset =>
            WorkflowDatasetJsonFormat.read(WorkflowDatasetJsonFormat.write(dataset)) shouldBe dataset
        }
      }
    }
  }

  // TemplateVariable equality covers the name, the scope and the value only, so the other fields are compared one by one
  it should "read every variable back equal to the live variable" in {
    for(variable <- project.templateVariables.all.variables) {
      val read = TemplateVariableJson(variable).convert
      withClue(s"Variable '${variable.name}'") {
        read shouldBe variable
        read.template shouldBe variable.template
        read.description shouldBe variable.description
        read.isSensitive shouldBe variable.isSensitive
      }
    }
  }

  // A journal entry is read with the variables of the time of reading, not of writing, as the live task is.
  it should "read a task with a templated parameter back against the current variables" in {
    val written = GenericTaskJsonFormat.write(project.task[GenericDatasetSpec]("persons"))(WriteContext.fromProject[JsValue](project))
    val separator = project.templateVariables.all.map("separator")
    // The modification is what the API, the MCP tools and a revert run: it updates the tasks that use the variable
    UpdateVariableModification(project, separator.copy(value = ",")).execute()
    try {
      val live = project.task[GenericDatasetSpec]("persons")
      val read = GenericTaskJsonFormat.read(written)(ReadContext.fromProject(project))
      withClue(s"Task 'persons' written as\n${Json.prettyPrint(written)}\n") {
        read.data shouldBe live.data
        TaskChanges.same(live, read) shouldBe true
      }
    } finally {
      UpdateVariableModification(project, separator).execute()
    }
  }

  it should "write every journal entry as one line and read it back equal, with the same revert conflicts" in {
    recordEveryChangeType()
    val journal = project.changeJournal
    implicit val readContext: ReadContext = ReadContext.fromProject(project)
    implicit val writeContext: WriteContext[JsValue] = WriteContext.fromProject[JsValue](project)
    val entries = journal.all
    entries.map(_.change.changeType).toSet should contain allElementsOf Seq("AddTask", "ReplaceTask", "RemoveTask",
      "AddMapping", "UpdateMapping", "ReorderMappings", "RemoveMapping", "AddWorkflowNode", "ConnectWorkflowNodes",
      "DisconnectWorkflowNodes", "RemoveWorkflowNode", "SetVariable", "RemoveVariable", "ResourceCreated",
      "ResourceOverwritten", "ResourceDeleted", "ProposedWorkflowRun", "DiscardedWorkflowRun")
    val read = for(entry <- entries) yield {
      val line = Json.stringify(ChangeEntryJsonFormat.write(entry))
      line should not include "\n"
      val readEntry = ChangeEntryJsonFormat.read(Json.parse(line))
      withClue(s"Entry ${entry.seq} written as\n$line\n") { readEntry shouldBe entry }
      readEntry
    }
    journal.revertConflicts(read) shouldBe journal.revertConflicts(entries)
  }

  /** Records one entry of every change type that the project's writes produce; the recorded changes without a payload
    * of their own (e.g. a workflow run) are covered by [[ChangeJsonFormatsTest]]. A task uses none of the variables
    * changed here, so that no task is re-resolved and every stored task payload reads back as it was written. */
  private def recordEveryChangeType(): Unit = {
    val transform = project.task[TransformSpec]("transform")
    val label = Some("Transform")
    val age = DirectMapping("age", UntypedPath("age"), MappingTarget(Uri("http://example.org/age")))
    transform.applyChange(AddMapping("transform", "root", age, Some(1), label))
    transform.applyChange(UpdateMapping("transform", age, age.copy(sourcePath = UntypedPath("years")), label))
    val order = transform.data.rules.propertyRules.map(_.id)
    transform.applyChange(ReorderMappings("transform", "root", order, order.reverse, label))
    transform.applyChange(RemoveMapping.of(transform, "age"))

    val workflow = project.task[Workflow]("workflow")
    val extra = WorkflowDataset(inputs = Seq.empty, task = "output", outputs = Seq.empty, position = (90, 90), nodeId = "extraNode",
      configInputs = Seq.empty, dependencyInputs = Seq.empty)
    workflow.applyChange(AddWorkflowNode("workflow", extra, taskLabel = Some("Workflow")))
    val edges = Seq("linkingNode" -> WorkflowEdge.Data(Seq(0)), "transformNode" -> WorkflowEdge.Dependency,
      "setVariableNode" -> WorkflowEdge.Config, "linkingNode" -> WorkflowEdge.Error)
    for((source, edge) <- edges) workflow.applyChange(ConnectWorkflowNodes("workflow", source, "extraNode", edge, Some("Workflow")))
    for((source, edge) <- edges.reverse) workflow.applyChange(DisconnectWorkflowNodes("workflow", source, "extraNode", edge, Some("Workflow")))
    workflow.applyChange(RemoveWorkflowNode("workflow", workflow.data.nodes.find(_.nodeId == "extraNode").get, taskLabel = Some("Workflow")))

    project.addTask[CustomTask]("tmp", SetExecutionVariableOperator("tmpVar"), MetaData(Some("Temporary")))
    project.removeAnyTask("tmp", removeDependentTasks = false)
    val linking = project.task[LinkSpec]("linking")
    linking.update(linking.data.copy(linkLimit = 7), Some(MetaData(Some("Linking"), Some("Now with a limit\nand a second line"))))

    UpdateVariableModification(project, project.templateVariables.all.map("secret").copy(value = "hunter3")).execute()
    DeleteVariableModification(project, "secret").execute()

    // File writes are recorded on behalf of a request only
    ChangeJournal.onBehalfOf(implicitly[UserContext]) {
      val notes = project.resources.get("notes.txt")
      notes.writeString("a")
      notes.writeString("ab")
      notes.delete()
    }

    val agent = SimpleUserContext(Some(DefaultUserManager.get("urn:agent")), UserExecutionContext(origin = Some("mcp:test")))
    val proposal = project.changeJournal.proposeRunIfAbsent("workflow", ProposedWorkflowRun("workflow", Some("Workflow")))(agent)
    project.changeJournal.revert(proposal.seq)
  }

  /** A project with one task of every kind, nested rules, every workflow edge kind and every variable kind. */
  private def createProject(): Unit = {
    val prefixes = Prefixes.default ++ Map("ex" -> "http://example.org/")
    val project = retrieveOrCreateProject(projectId, prefixes)
    project.templateVariables.put(TemplateVariables(Seq(
      TemplateVariable("separator", ";", description = Some("The CSV separator"), scope = VariableScope.project),
      TemplateVariable("secret", "hunter2", isSensitive = true, scope = VariableScope.project),
      TemplateVariable("suffix", "", template = Some("{{project.separator}}x"), scope = VariableScope.project)
    )))

    implicit val pluginContext: PluginContext = PluginContext.fromProject(project)
    project.resources.get("persons.csv").writeString("id;name;street\n1;Alice;Main St\n")
    val persons = PluginRegistry.create[Dataset]("csv", ParameterValues(Map(
      "file" -> ParameterStringValue("persons.csv"),
      "separator" -> ParameterTemplateValue("{{project.separator}}"))))
    project.addTask[GenericDatasetSpec]("persons", DatasetSpec(persons), MetaData(Some("Persons"), Some("The persons")))
    project.addTask[GenericDatasetSpec]("output", DatasetSpec(CsvDataset(file = project.resources.get("output.csv"))), MetaData(Some("Output")))

    val now = Instant.now()
    val user = Some(Uri("urn:elds-backend-users:alice"))
    def ruleMetaData(label: String): MetaData = {
      MetaData(Some(label), Some(s"$label description"), modified = Some(now), created = Some(now), createdByUser = user, lastModifiedByUser = user)
    }
    val transform = TransformSpec(
      selection = DatasetSelection("persons", Uri("http://example.org/Person"), Restriction.custom("?a <urn:p> 1 .")(prefixes)),
      mappingRule = RootMappingRule(
        id = "root",
        rules = MappingRules(
          uriRule = Some(PatternUriMapping("uri", "http://example.org/person/{id}", ruleMetaData("URI"), prefixes)),
          typeRules = Seq(TypeMapping("type", Uri("http://example.org/Person"), ruleMetaData("Type"))),
          propertyRules = Seq(
            DirectMapping("name", UntypedPath("name"), MappingTarget(Uri("http://example.org/name")), ruleMetaData("Name")),
            ComplexMapping("lowerName",
              operator = TransformInput("lower", LowerCaseTransformer(), IndexedSeq(PathInput("namePath", UntypedPath.parse("name")))),
              target = Some(MappingTarget(Uri("http://example.org/lowerName"))),
              metaData = ruleMetaData("Lower name"),
              layout = RuleLayout(Map("lower" -> NodePosition(0, 1), "namePath" -> NodePosition(3, 4, 250, 300))),
              uiAnnotations = UiAnnotations(Seq(StickyNote("note", "text", "#000", NodePosition(1, 2, 3, 4))))),
            ObjectMapping("address", UntypedPath("address"), Some(MappingTarget(Uri("http://example.org/address"), ValueType.URI)),
              rules = MappingRules(
                uriRule = Some(PatternUriMapping("addressUri", "http://example.org/address/{street}", ruleMetaData("Address URI"), prefixes)),
                propertyRules = Seq(DirectMapping("street", UntypedPath("street"), MappingTarget(Uri("http://example.org/street")), ruleMetaData("Street")))),
              metaData = ruleMetaData("Address"),
              prefixes = prefixes))),
        metaData = ruleMetaData("Root")),
      output = IdentifierOptionParameter(Some("output")),
      abortIfErrorsOccur = true)
    project.addTask[TransformSpec]("transform", transform, MetaData(Some("Transform"), Some("Persons to RDF")),
      executionVariables = TemplateVariables(Seq(TemplateVariable("batch", "10", scope = VariableScope.execution))))

    val linking = LinkSpec(
      source = DatasetSelection("persons", Uri("http://example.org/Person")),
      target = DatasetSelection("persons", Uri("http://example.org/Person")),
      rule = LinkageRule(
        operator = Some(Comparison(id = "compareNames", threshold = 0.5, metric = QGramsMetric(),
          inputs = DPair(PathInput("sourceName", UntypedPath.parse("name")), PathInput("targetName", UntypedPath.parse("name"))))),
        filter = LinkFilter(limit = Some(5)),
        linkType = Uri("http://www.w3.org/2004/02/skos/core#exactMatch"),
        layout = RuleLayout(Map("compareNames" -> NodePosition(1, 2))),
        uiAnnotations = UiAnnotations(Seq(StickyNote("compareNames", "content", "#fff", NodePosition(0, 0, 1, 1))))),
      output = IdentifierOptionParameter(Some("output")),
      linkLimit = LinkSpec.DEFAULT_LINK_LIMIT + 1)
    project.addTask[LinkSpec]("linking", linking, MetaData(Some("Linking")))

    project.addTask[CustomTask]("setVariable", SetExecutionVariableOperator("myVar", "name"), MetaData(Some("Set variable")))

    val workflow = Workflow(
      operators = Seq(
        WorkflowOperator(inputs = Seq(Some("personsNode")), task = "transform", outputs = Seq("outputNode"), errorOutputs = Seq("errorNode"),
          position = (10, 20), nodeId = "transformNode", configInputs = Seq("setVariableNode"), dependencyInputs = Seq.empty),
        WorkflowOperator(inputs = Seq.empty, task = "setVariable", outputs = Seq.empty, errorOutputs = Seq.empty,
          position = (0, 0), nodeId = "setVariableNode", configInputs = Seq.empty, dependencyInputs = Seq.empty),
        WorkflowOperator(inputs = Seq(Some("personsNode"), Some("personsNode")), task = "linking", outputs = Seq.empty, errorOutputs = Seq.empty,
          position = (30, 40), nodeId = "linkingNode", configInputs = Seq.empty, dependencyInputs = Seq("transformNode"))),
      datasets = Seq(
        WorkflowDataset(inputs = Seq.empty, task = "persons", outputs = Seq("transformNode", "linkingNode"),
          position = (0, 0), nodeId = "personsNode", configInputs = Seq.empty, dependencyInputs = Seq.empty),
        WorkflowDataset(inputs = Seq(Some("transformNode")), task = "output", outputs = Seq.empty,
          position = (50, 60), nodeId = "outputNode", configInputs = Seq.empty, dependencyInputs = Seq.empty),
        WorkflowDataset(inputs = Seq.empty, task = "output", outputs = Seq.empty,
          position = (70, 80), nodeId = "errorNode", configInputs = Seq.empty, dependencyInputs = Seq.empty)),
      uiAnnotations = UiAnnotations(Seq(StickyNote("wfNote", "text", "#fff", NodePosition(1, 1, 2, 2)))),
      replaceableInputs = TaskIdentifierParameter(Seq("persons")),
      replaceableOutputs = TaskIdentifierParameter(Seq("output")))
    project.addTask[Workflow]("workflow", workflow, MetaData(Some("Workflow")))
  }
}
