package controllers.workflowApi

import controllers.workflowApi.workflow.WorkflowExecutionVariablesJson
import controllers.workspaceApi.VariablesTestTask
import helper.{ApiClient, IntegrationTestTrait, RequestFailedException}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.plugins.operations.SetExecutionVariableOperator
import org.silkframework.rule.input.{PathInput, TransformInput}
import org.silkframework.rule.plugins.transformer.variable.SetExecutionVariableTransformer
import org.silkframework.rule.{ComplexMapping, DatasetSelection, MappingRules, RootMappingRule, TransformSpec}
import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.runtime.templating.{TemplateVariable, TemplateVariables, VariableScope}
import org.silkframework.workspace.activity.workflow.{Workflow, WorkflowOperator, WorkflowOperatorsParameter}
import org.silkframework.workspace.{ProjectConfig, WorkspaceFactory}
import play.api.libs.json.Json
import play.api.routing.Router

class WorkflowExecutionVariablesApiTest extends AnyFlatSpec with IntegrationTestTrait with ApiClient with Matchers with BeforeAndAfterAll {

  behavior of "Workflow execution variables API"

  override def workspaceProviderId: String = "inMemoryWorkspaceProvider"

  override def routes: Option[Class[_ <: Router]] = Some(classOf[testWorkflowApi.Routes])

  private val projectName = "execution-variables-api"

  override def beforeAll(): Unit = {
    super.beforeAll()
    PluginRegistry.registerPlugin(classOf[VariablesTestTask])
  }

  override def afterAll(): Unit = {
    PluginRegistry.unregisterPlugin(classOf[VariablesTestTask])
    super.afterAll()
  }

  it should "report the execution variables that a workflow run needs" in {
    val project = WorkspaceFactory().workspace.createProject(ProjectConfig(projectName))
    // Tasks that reference execution variables at execution time
    project.addTask("needsGreeting", VariablesTestTask("T", 2002, variableReference = "execution.greeting"))
    project.addTask("needsBaseUrl", VariablesTestTask("T", 2002, variableReference = "execution.baseUrl"))
    project.addTask("needsTmp", VariablesTestTask("T", 2002, variableReference = "execution.tmp"))
    project.addTask("needsFromTransformer", VariablesTestTask("T", 2002, variableReference = "execution.fromTransformer"))
    // References to other scopes are not execution variables
    project.addTask("needsProjectVar", VariablesTestTask("T", 2002, variableReference = "project.year"))
    // Setters: a workflow operator and a transformer inside a mapping rule
    project.addTask("setTmp", SetExecutionVariableOperator(variableName = "tmp"))
    project.addTask("setFromTransformer", TransformSpec(mappingRule = RootMappingRule(MappingRules(propertyRules = Seq(
      ComplexMapping("setRule", TransformInput(transformer = SetExecutionVariableTransformer("fromTransformer"),
        inputs = IndexedSeq(PathInput(path = UntypedPath.parse("value"))))))))))
    // Setters that do not precede the referencing node: after it, or in a disconnected branch
    project.addTask("needsLate", VariablesTestTask("T", 2002, variableReference = "execution.late"))
    project.addTask("setLate", SetExecutionVariableOperator(variableName = "late"))
    project.addTask("needsApart", VariablesTestTask("T", 2002, variableReference = "execution.apart"))
    project.addTask("setApart", SetExecutionVariableOperator(variableName = "apart"))
    // A setter inside a sub-workflow counts for the nodes after the sub-workflow
    project.addTask("setInner", SetExecutionVariableOperator(variableName = "inner"))
    project.addTask[Workflow]("setterWf", workflowOf(node("setInner")))
    project.addTask("needsInner", VariablesTestTask("T", 2002, variableReference = "execution.inner"))
    // A sub-workflow defines a default that its own task references; that default does not apply to the run
    project.addTask("needsSubOnly", VariablesTestTask("T", 2002, variableReference = "execution.subOnly"))
    project.addTask[Workflow]("subWf", workflowOf(node("needsSubOnly")),
      executionVariables = TemplateVariables(Seq(executionVariable("subOnly", "sub"))))
    // A transform whose input is a task referencing a variable: the input does not run with the transform
    project.addTask("needsBatch", VariablesTestTask("T", 2002, variableReference = "execution.batch"))
    project.addTask("mapping", TransformSpec(selection = DatasetSelection("needsBatch"), mappingRule = RootMappingRule(MappingRules.empty)))
    // The workflow itself: a referenced default, an unreferenced default and a sensitive default
    project.addTask[Workflow]("wf",
      workflowOf(node("needsGreeting"), node("needsBaseUrl"), node("setTmp"), node("needsTmp", after = "setTmp"),
        node("setFromTransformer"), node("needsFromTransformer", after = "setFromTransformer"), node("needsProjectVar"),
        node("needsLate"), node("setLate", after = "needsLate"), node("needsApart"), node("setApart"),
        node("setterWf"), node("needsInner", after = "setterWf"), node("subWf"), node("mapping")),
      executionVariables = TemplateVariables(Seq(
        executionVariable("baseUrl", "https://example.org"),
        executionVariable("unused", "x"),
        TemplateVariable("secret", "s3cret-value", None, None, isSensitive = true, VariableScope.execution))))

    val response = checkResponse(createRequest(controllers.workflowApi.routes.WorkflowApi.workflowExecutionVariables(projectName, "wf")).get())
    response.body should not include "s3cret-value"
    val result = Json.fromJson[WorkflowExecutionVariablesJson](response.json).get
    result.variables.map(_.name) shouldBe Seq("apart", "baseUrl", "fromTransformer", "greeting", "inner", "late", "secret", "subOnly", "tmp", "unused")
    val byName = result.variables.map(v => v.name -> v).toMap

    val greeting = byName("greeting")
    greeting.required shouldBe true
    greeting.default shouldBe None
    greeting.referencedBy.map(_.id) shouldBe Seq("needsGreeting")
    greeting.referencedBy.map(_.taskType) shouldBe Seq("task")
    greeting.setBy shouldBe empty

    val baseUrl = byName("baseUrl")
    baseUrl.required shouldBe false
    baseUrl.default.flatMap(_.value) shouldBe Some("https://example.org")
    baseUrl.referencedBy.map(_.id) shouldBe Seq("needsBaseUrl")

    val tmp = byName("tmp")
    tmp.required shouldBe false
    tmp.setBy.map(_.id) shouldBe Seq("setTmp")
    tmp.referencedBy.map(_.id) shouldBe Seq("needsTmp")

    val fromTransformer = byName("fromTransformer")
    fromTransformer.required shouldBe false
    fromTransformer.setBy.map(t => (t.id, t.taskType)) shouldBe Seq(("setFromTransformer", "transform"))

    // Set during the run, but not before the referencing node
    val late = byName("late")
    late.required shouldBe true
    late.setBy.map(_.id) shouldBe Seq("setLate")
    val apart = byName("apart")
    apart.required shouldBe true
    apart.setBy.map(_.id) shouldBe Seq("setApart")

    val inner = byName("inner")
    inner.required shouldBe false
    inner.setBy.map(_.id) shouldBe Seq("setInner")

    val unused = byName("unused")
    unused.required shouldBe false
    unused.default.flatMap(_.value) shouldBe Some("x")
    unused.referencedBy shouldBe empty

    val secret = byName("secret")
    secret.default.map(_.isSensitive) shouldBe Some(true)
    secret.default.flatMap(_.value) shouldBe None

    // The default on the sub-workflow does not apply to the run
    val subOnly = byName("subOnly")
    subOnly.required shouldBe true
    subOnly.default shouldBe None
    subOnly.referencedBy.map(_.id) shouldBe Seq("needsSubOnly")
  }

  it should "report tasks that are not workflows, missing tasks and missing projects as not found" in {
    val errorProjectName = "execution-variables-api-errors"
    val project = WorkspaceFactory().workspace.createProject(ProjectConfig(errorProjectName))
    project.addTask("notAWorkflow", VariablesTestTask("T", 2002))

    // A task of another type is not found, consistent with the workflow info route
    val notAWorkflow = the[RequestFailedException] thrownBy {
      checkResponse(createRequest(controllers.workflowApi.routes.WorkflowApi.workflowExecutionVariables(errorProjectName, "notAWorkflow")).get())
    }
    notAWorkflow.response.status shouldBe 404

    val missingTask = the[RequestFailedException] thrownBy {
      checkResponse(createRequest(controllers.workflowApi.routes.WorkflowApi.workflowExecutionVariables(errorProjectName, "doesNotExist")).get())
    }
    missingTask.response.status shouldBe 404

    val missingProject = the[RequestFailedException] thrownBy {
      checkResponse(createRequest(controllers.workflowApi.routes.WorkflowApi.workflowExecutionVariables("doesNotExist", "anyWorkflow")).get())
    }
    missingProject.response.status shouldBe 404
  }

  private def executionVariable(name: String, value: String): TemplateVariable = {
    TemplateVariable(name, value, None, None, isSensitive = false, VariableScope.execution)
  }

  private def workflowOf(operators: WorkflowOperator*): Workflow = {
    Workflow(operators = WorkflowOperatorsParameter(operators))
  }

  /** A node for the given task, identified by the task id, optionally running after another node via a dependency edge. */
  private def node(taskId: String, after: String = ""): WorkflowOperator = {
    WorkflowOperator(inputs = Seq.empty, task = taskId, outputs = Seq.empty, errorOutputs = Seq.empty,
      position = (0, 0), nodeId = taskId, configInputs = Seq.empty, dependencyInputs = Option(after).filter(_.nonEmpty).toSeq)
  }
}
