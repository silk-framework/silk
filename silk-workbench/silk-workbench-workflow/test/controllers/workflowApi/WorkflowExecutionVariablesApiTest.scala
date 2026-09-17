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
import org.silkframework.rule.{ComplexMapping, MappingRules, RootMappingRule, TransformSpec}
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
    // A sub-workflow defines a default that its own task references; that default does not apply to the run
    project.addTask("needsSubOnly", VariablesTestTask("T", 2002, variableReference = "execution.subOnly"))
    project.addTask[Workflow]("subWf", workflowReferencing("needsSubOnly"),
      executionVariables = TemplateVariables(Seq(executionVariable("subOnly", "sub"))))
    // The workflow itself: a referenced default, an unreferenced default and a sensitive default
    project.addTask[Workflow]("wf",
      workflowReferencing("needsGreeting", "needsBaseUrl", "needsTmp", "setTmp", "needsFromTransformer", "setFromTransformer", "needsProjectVar", "subWf"),
      executionVariables = TemplateVariables(Seq(
        executionVariable("baseUrl", "https://example.org"),
        executionVariable("unused", "x"),
        TemplateVariable("secret", "s3cret-value", None, None, isSensitive = true, VariableScope.execution))))

    val response = checkResponse(createRequest(controllers.workflowApi.routes.WorkflowApi.workflowExecutionVariables(projectName, "wf")).get())
    response.body should not include "s3cret-value"
    val result = Json.fromJson[WorkflowExecutionVariablesJson](response.json).get
    result.variables.map(_.name) shouldBe Seq("baseUrl", "fromTransformer", "greeting", "secret", "subOnly", "tmp", "unused")
    val byName = result.variables.map(v => v.name -> v).toMap

    val greeting = byName("greeting")
    greeting.required shouldBe true
    greeting.default shouldBe None
    greeting.referencedBy.map(_.id) shouldBe Seq("needsGreeting")
    greeting.referencedBy.map(_.taskType) shouldBe Seq("task")
    greeting.definedOn shouldBe empty
    greeting.setDuringExecution shouldBe false

    val baseUrl = byName("baseUrl")
    baseUrl.required shouldBe false
    baseUrl.default.flatMap(_.value) shouldBe Some("https://example.org")
    baseUrl.referencedBy.map(_.id) shouldBe Seq("needsBaseUrl")

    val tmp = byName("tmp")
    tmp.required shouldBe false
    tmp.setDuringExecution shouldBe true
    tmp.setBy.map(_.id) shouldBe Seq("setTmp")
    tmp.referencedBy.map(_.id) shouldBe Seq("needsTmp")

    val fromTransformer = byName("fromTransformer")
    fromTransformer.required shouldBe false
    fromTransformer.setBy.map(t => (t.id, t.taskType)) shouldBe Seq(("setFromTransformer", "transform"))

    val unused = byName("unused")
    unused.required shouldBe false
    unused.default.flatMap(_.value) shouldBe Some("x")
    unused.referencedBy shouldBe empty

    val secret = byName("secret")
    secret.default.map(_.isSensitive) shouldBe Some(true)
    secret.default.flatMap(_.value) shouldBe None

    val subOnly = byName("subOnly")
    subOnly.required shouldBe true
    subOnly.definedOn.map(t => (t.id, t.taskType)) shouldBe Seq(("subWf", "workflow"))
    subOnly.referencedBy.map(_.id) shouldBe Seq("needsSubOnly")
  }

  it should "reject tasks that are not workflows and report missing tasks and projects" in {
    val notAWorkflow = the[RequestFailedException] thrownBy {
      checkResponse(createRequest(controllers.workflowApi.routes.WorkflowApi.workflowExecutionVariables(projectName, "needsGreeting")).get())
    }
    notAWorkflow.response.status shouldBe 400

    val missingTask = the[RequestFailedException] thrownBy {
      checkResponse(createRequest(controllers.workflowApi.routes.WorkflowApi.workflowExecutionVariables(projectName, "doesNotExist")).get())
    }
    missingTask.response.status shouldBe 404

    val missingProject = the[RequestFailedException] thrownBy {
      checkResponse(createRequest(controllers.workflowApi.routes.WorkflowApi.workflowExecutionVariables("doesNotExist", "wf")).get())
    }
    missingProject.response.status shouldBe 404
  }

  private def executionVariable(name: String, value: String): TemplateVariable = {
    TemplateVariable(name, value, None, None, isSensitive = false, VariableScope.execution)
  }

  /** A workflow whose operators reference the given tasks. */
  private def workflowReferencing(taskIds: String*): Workflow = {
    val operators = taskIds.zipWithIndex.map { case (taskId, index) =>
      WorkflowOperator(inputs = Seq.empty, task = taskId, outputs = Seq.empty, errorOutputs = Seq.empty,
        position = (0, 0), nodeId = s"${taskId}_$index", configInputs = Seq.empty, dependencyInputs = Seq.empty)
    }
    Workflow(operators = WorkflowOperatorsParameter(operators))
  }
}
