package controllers.workspaceApi

import controllers.autoCompletion.AutoSuggestAutoCompletionResponse
import controllers.workspaceApi.coreApi.VariableTemplateApi.VariableDependencies
import org.silkframework.serialization.json.{TemplateVariableJson, TemplateVariablesJson}
import helper.{ApiClient, IntegrationTestTrait, RequestFailedException}
import org.silkframework.runtime.templating.{SimpleSubstitutionTemplateEngine, TemplateVariable, TemplateVariables, VariableScope}
import org.silkframework.workspace.activity.workflow.{Workflow, WorkflowOperator, WorkflowOperatorsParameter}
import org.silkframework.workspace.{Project, ProjectConfig, WorkspaceFactory}
import play.api.libs.json.{JsObject, JsValue, Json}
import controllers.workspaceApi.coreApi.routes.{VariableTemplateApi => TemplateApi}
import controllers.workspaceApi.coreApi.variableTemplate.{AutoCompleteVariableTemplateRequest, ValidateVariableTemplateRequest, VariableTemplateValidationResponse}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.config.{CustomTask, InputPorts, Port}
import org.silkframework.runtime.plugin.{ClassPluginDescription, ParameterTemplateValue, ParameterValues, PluginContext, PluginRegistry}
import org.silkframework.runtime.templating.exceptions.{CannotDeleteUsedVariableException, InvalidScopeException}
import org.silkframework.util.{ConfigTestTrait, Identifier}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class VariableTemplateApiTest extends AnyFlatSpec with IntegrationTestTrait with ApiClient with Matchers with ConfigTestTrait with BeforeAndAfterAll {

  behavior of "VariableTemplate API"

  override def workspaceProviderId: String = "inMemoryWorkspaceProvider"

  // Includes the task API routes in addition to the variables API.
  protected override def routes = Some(classOf[testWorkspace.Routes])

  // All templates in this suite are plain substitutions, so the simple engine suffices.
  override def propertyMap: Map[String, Option[String]] = Map(
    "config.variables.engine" -> Some(SimpleSubstitutionTemplateEngine.id)
  )

  override def beforeAll(): Unit = {
    super.beforeAll()
    PluginRegistry.registerPlugin(classOf[VariablesTestTask])
  }

  override def afterAll(): Unit = {
    PluginRegistry.unregisterPlugin(classOf[VariablesTestTask])
    super.afterAll()
  }

  it should "allow managing variables" in {
    val projectName = "variables-test-1"
    WorkspaceFactory().workspace.createProject(ProjectConfig(projectName))
    getVariables(projectName).variables shouldBe empty

    val variables = TemplateVariables(Seq(TemplateVariable("myVar", "myValue", None, Some("test description"), isSensitive = false, VariableScope.project)))
    putVariables(projectName, variables)
    getVariables(projectName) shouldBe variables
  }

  it should "allow defined variables with templates" in {
    val projectName = "variables-test-2"
    WorkspaceFactory().workspace.createProject(ProjectConfig(projectName))

    val variables = TemplateVariables(Seq(TemplateVariable("year", "2002", None, None, isSensitive = false, VariableScope.project),
                                          TemplateVariable("movie", "", Some("Let Me In ({{project.year}})"), None, isSensitive = false, VariableScope.project)))
    putVariables(projectName, variables)
    getVariables(projectName).variables(1).value shouldBe "Let Me In (2002)"

    val validationResponse = validateTemplate(ValidateVariableTemplateRequest("Beauty and the Beast ({{project.year}})", Some(projectName)))
    validationResponse.valid shouldBe true
    validationResponse.evaluatedTemplate shouldBe Some("Beauty and the Beast (2002)")
  }

  it should "not report unbound variables if they are ignored" in {
    val projectName = "variables-test-lenient"
    WorkspaceFactory().workspace.createProject(ProjectConfig(projectName))

    val strictResponse = validateTemplate(ValidateVariableTemplateRequest("{{custom.unknown}}", Some(projectName)))
    strictResponse.valid shouldBe false

    // Variables from unknown scopes cannot be validated: they evaluate to their names
    val lenientResponse = validateTemplate(ValidateVariableTemplateRequest("{{custom.unknown}}", Some(projectName), ignoreUnboundVariables = Some(true)))
    lenientResponse.valid shouldBe true
    lenientResponse.evaluatedTemplate shouldBe Some("custom.unknown")
  }

  it should "report missing variables of known scopes even if unbound variables are ignored" in {
    val projectName = "variables-test-lenient-known-scopes"
    WorkspaceFactory().workspace.createProject(ProjectConfig(projectName))
    putVariables(projectName, TemplateVariables(Seq(projectVariable("year", "2002"))))

    // An existing project variable resolves
    val validResponse = validateTemplate(ValidateVariableTemplateRequest("{{project.year}}", Some(projectName), ignoreUnboundVariables = Some(true)))
    validResponse.valid shouldBe true
    validResponse.evaluatedTemplate shouldBe Some("2002")

    // A missing project variable is reported
    val invalidResponse = validateTemplate(ValidateVariableTemplateRequest("{{project.typo}}", Some(projectName), ignoreUnboundVariables = Some(true)))
    invalidResponse.valid shouldBe false
    invalidResponse.parseError.map(_.message).getOrElse("") should include ("project.typo")

    // Property access on an existing variable stays valid
    validateTemplate(ValidateVariableTemplateRequest("{{project.year.length}}", Some(projectName), ignoreUnboundVariables = Some(true))).valid shouldBe true

    // Execution references are only checked in a task context
    validateTemplate(ValidateVariableTemplateRequest("{{execution.unknown}}", Some(projectName), ignoreUnboundVariables = Some(true))).valid shouldBe true
  }

  it should "report missing execution variables in a task context even if unbound variables are ignored" in {
    val projectName = "variables-test-lenient-execution"
    val taskName = "lenientExecutionTask"
    createProjectWithVariablesTask(projectName, taskName,
      taskExecutionVariables = TemplateVariables(Seq(executionVariable("greeting", "Hello"))))

    validateTemplate(ValidateVariableTemplateRequest("{{execution.greeting}}", Some(projectName), task = Some(taskName), ignoreUnboundVariables = Some(true))).valid shouldBe true
    validateTemplate(ValidateVariableTemplateRequest("{{execution.typo}}", Some(projectName), task = Some(taskName), ignoreUnboundVariables = Some(true))).valid shouldBe false
  }

  it should "validate execution scope references against the task's execution variables only" in {
    val projectName = "variables-test-execution-validation"
    val taskName = "validationTask"
    createProjectWithVariablesTask(projectName, taskName)

    // Add an execution variable to the task
    putVariables(projectName, TemplateVariables(Seq(
      TemplateVariable("greeting", "Hello", None, None, isSensitive = false, VariableScope.execution))), Some(taskName))

    // In a task context, a stored execution variable resolves.
    val okResponse = validateTemplate(ValidateVariableTemplateRequest("{{execution.greeting}} World", Some(projectName), task = Some(taskName)))
    okResponse.valid shouldBe true
    okResponse.evaluatedTemplate shouldBe Some("Hello World")

    // There is no fallback: a project variable is not addressable through the execution scope.
    val noFallbackResponse = validateTemplate(ValidateVariableTemplateRequest("Movie ({{execution.year}})", Some(projectName), task = Some(taskName)))
    noFallbackResponse.valid shouldBe false

    // Without a task context, no execution variables are available at all.
    val projectContextResponse = validateTemplate(ValidateVariableTemplateRequest("{{execution.greeting}}", Some(projectName)))
    projectContextResponse.valid shouldBe false
  }

  it should "validate templates of an edited execution variable against the variables defined before it" in {
    val projectName = "variables-test-execution-validation-edit"
    val taskName = "editValidationTask"
    createProjectWithVariablesTask(projectName, taskName,
      taskExecutionVariables = TemplateVariables(Seq(
        executionVariable("first", "hello"),
        executionVariable("second", "world"))))

    // Editing 'second': the earlier variable 'first' can be referenced.
    val editResponse = validateTemplate(ValidateVariableTemplateRequest("{{execution.first}}-updated", Some(projectName),
      variableName = Some("second"), task = Some(taskName)))
    editResponse.valid shouldBe true
    editResponse.evaluatedTemplate shouldBe Some("hello-updated")

    // Editing 'first': referencing the later variable 'second' must be rejected with the ordering error.
    val forwardResponse = validateTemplate(ValidateVariableTemplateRequest("{{execution.second}}", Some(projectName),
      variableName = Some("first"), task = Some(taskName)))
    forwardResponse.valid shouldBe false
    forwardResponse.parseError.map(_.message) shouldBe Some("'execution.second' cannot be used because it's defined after 'first'.")
  }

  it should "auto-complete execution variables in the task context" in {
    val projectName = "variables-test-execution-completion"
    val taskName = "completionTask"
    createProjectWithVariablesTask(projectName, taskName,
      taskExecutionVariables = TemplateVariables(Seq(
        executionVariable("first", "hello"),
        executionVariable("second", "world"))))

    // For a new variable, all execution variables of the task are suggested.
    autoCompleteTemplate(projectName, task = Some(taskName)) should contain allOf("execution.first", "execution.second", "project.year")

    // For an edited variable, only the execution variables defined before it are suggested.
    val editSuggestions = autoCompleteTemplate(projectName, task = Some(taskName), variableName = Some("second"))
    editSuggestions should contain("execution.first")
    editSuggestions should not contain "execution.second"

    // Without a task context, no execution variables are suggested.
    autoCompleteTemplate(projectName).filter(_.startsWith("execution.")) shouldBe empty
  }

  it should "allow to retrieve, update and remove single variables" in {
    val projectName = "variables-test-3"
    WorkspaceFactory().workspace.createProject(ProjectConfig(projectName))

    val variables = TemplateVariables(Seq(TemplateVariable("year", "2002", None, None, isSensitive = false, VariableScope.project),
                                          TemplateVariable("month", "June", None, None, isSensitive = false, VariableScope.project),
                                          TemplateVariable("movie1", "", Some("Terminator ({{project.year}})"), None, isSensitive = false, VariableScope.project)))
    putVariables(projectName, variables)

    // Add new variable and retrieve again
    val newVariable = TemplateVariable("movie2", "", Some("Let Me In ({{project.year}})"), None, isSensitive = false, VariableScope.project)
    putVariable(projectName, newVariable)
    getVariable(projectName, "movie2") shouldBe newVariable.copy(value = "Let Me In (2002)")

    // New variables are added last
    getVariables(projectName).variables.map(_.name) shouldBe Seq("year", "month", "movie1", "movie2")

    // Update existing variable
    val updatedVariable = TemplateVariable("month", "June", None, None, isSensitive = false, VariableScope.project)
    putVariable(projectName, updatedVariable)
    getVariable(projectName, "month") shouldBe updatedVariable

    // Updating variables should retain order
    getVariables(projectName).variables.map(_.name) shouldBe Seq("year", "month", "movie1", "movie2")

    // Remove variable without error
    removeVariableError(projectName, "month") shouldBe None
    getVariables(projectName).variables.map(_.name) shouldBe Seq("year", "movie1", "movie2")

    // Remove variable with error
    val error = removeVariableError(projectName, "year")
    error should not be empty
    error.get.variable shouldBe "year"
    error.get.dependentVariables shouldBe Seq("movie1", "movie2")
  }

  it should "allow to reorder variables (simple)" in {
    val projectName = "variables-test-4"
    WorkspaceFactory().workspace.createProject(ProjectConfig(projectName))

    val variables = TemplateVariables(Seq(TemplateVariable("year", "2002", None, None, isSensitive = false, VariableScope.project),
                                          TemplateVariable("month", "June", None, None, isSensitive = false, VariableScope.project),
                                          TemplateVariable("movie1", "", Some("Terminator ({{project.year}})"), None, isSensitive = false, VariableScope.project)))
    putVariables(projectName, variables)

    reorderVariablesError(projectName, Seq("year", "month")).get.toString should include ("Provided variable names don't match the existing variables")
    reorderVariablesError(projectName, Seq("year", "month", "movie1", "movie2")).get.toString should include ("Provided variable names don't match the existing variables")

    val json = reorderVariablesError(projectName, Seq("month", "movie1", "year")).get
    (json \ "dependencies").as[JsObject] shouldBe Json.obj("movie1" -> Seq("year"))

    reorderVariablesError(projectName, Seq("year", "month", "movie1")) shouldBe None
    getVariables(projectName).variables.map(_.name) shouldBe Seq("year", "month", "movie1")
  }

  it should "allow to reorder variables (complex)" in {
    val projectName = "variables-test-5"
    WorkspaceFactory().workspace.createProject(ProjectConfig(projectName))

    val variables = TemplateVariables(
      Seq(TemplateVariable("name", "John", None, None, isSensitive = false, VariableScope.project),
          TemplateVariable("day", "22th", None, None, isSensitive = false, VariableScope.project),
          TemplateVariable("month", "June", None, None, isSensitive = false, VariableScope.project),
          TemplateVariable("monthDay", "", Some("{{project.day}} {{project.month}}"), None, isSensitive = false, VariableScope.project),
          TemplateVariable("year", "2002", None, None, isSensitive = false, VariableScope.project),
          TemplateVariable("message", "", Some("({{project.name}} {{project.monthDay}} {{project.year}})"), None, isSensitive = false, VariableScope.project)))
    putVariables(projectName, variables)

    val json1 = reorderVariablesError(projectName, Seq("name", "day", "message", "month", "monthDay", "year")).get
    (json1 \ "dependencies").as[JsObject] shouldBe Json.obj("message" -> Seq("monthDay", "year"))

    val json2 = reorderVariablesError(projectName, Seq("name", "day", "message", "monthDay", "month", "year")).get
    (json2 \ "dependencies").as[JsObject] shouldBe Json.obj("message" -> Seq("monthDay", "year"), "monthDay" -> Seq("month"))
  }

  it should "not allow modifications that would break existing tasks" in {
    val projectName = "variables-test-6"
    val titleVar = projectVariable("title", "Terminator")
    val yearVar = projectVariable("year", "2002")
    val taskName = "movie"
    createProjectWithVariablesTask(projectName, taskName,
      projectVariables = Seq(titleVar, yearVar),
      taskParameters = Map("title" -> "{{project.title}}", "year" -> "{{project.year}}"))

    // Try to delete a used variable
    val ex1 = the[RequestFailedException] thrownBy {
      removeVariable(projectName, "title")
    }
    val json1 = ex1.response.json
    (json1 \ "title").as[String] shouldBe "Cannot delete variable"
    (json1 \ "taskId").as[String] shouldBe taskName

    // Try to update a variable to an invalid type (no integer)
    val ex2 = the[RequestFailedException] thrownBy {
      putVariable(projectName, yearVar.copy(value = "2002x"))
    }
    val json2 = ex2.response.json
    (json2 \ "title").as[String] shouldBe "Cannot update variable"
    (json2 \ "taskId").as[String] shouldBe taskName

    // Try to update a variable to an invalid value
    val ex3 = the[RequestFailedException] thrownBy {
      putVariable(projectName, yearVar.copy(value =  "-1"))
    }
    val json3 = ex3.response.json
    (json3 \ "title").as[String] shouldBe "Cannot update variable"
    (json3 \ "taskId").as[String] shouldBe taskName

    // Try to update all variables
    val ex4 = the[RequestFailedException] thrownBy {
      putVariables(projectName, TemplateVariables(Seq(titleVar, yearVar.copy(value = "2002x"))))
    }
    val json4 = ex4.response.json
    (json4 \ "title").as[String] shouldBe "Cannot update variables"
    (json4 \ "taskId").as[String] shouldBe taskName
  }

  it should "resolve execution variable templates referencing project variables and other execution variables" in {
    val projectName = "variables-test-task-2"
    val taskName = "testTask2"
    createProjectWithVariablesTask(projectName, taskName)

    // Add execution variables: a plain one and one referencing both the project scope and the plain execution variable
    val executionVariables = TemplateVariables(Seq(
      TemplateVariable("title", "Terminator", None, None, isSensitive = false, VariableScope.execution),
      TemplateVariable("movie", "", Some("{{execution.title}} ({{project.year}})"), None, isSensitive = false, VariableScope.execution)
    ))
    putVariables(projectName, executionVariables, Some(taskName))

    val result = getVariables(projectName, Some(taskName))
    result.variables(1).value shouldBe "Terminator (2002)"
  }

  it should "allow adding and updating single execution variables" in {
    val projectName = "variables-test-task-3"
    val taskName = "testTask3"
    createProjectWithVariablesTask(projectName, taskName)

    // Add an execution variable referencing a project variable
    val newVariable = TemplateVariable("movie", "", Some("Let Me In ({{project.year}})"), None, isSensitive = false, VariableScope.execution)
    putVariable(projectName, newVariable, Some(taskName))

    // Verify single variable GET returns resolved value
    getVariable(projectName, "movie", Some(taskName)).value shouldBe "Let Me In (2002)"
  }

  it should "update project variables while tasks reference execution variables" in {
    val projectName = "variables-test-execution-task-update"
    // A task with one parameter template referencing a project variable and one referencing an execution variable
    val taskName = "executionUpdateTask"
    val executionVars = TemplateVariables(Seq(
      TemplateVariable("titleVar", "Terminator", None, None, isSensitive = false, VariableScope.execution)))
    val project = createProjectWithVariablesTask(projectName, taskName,
      projectVariables = Seq(projectVariable("year", "2002"), projectVariable("month", "June")),
      taskParameters = Map("title" -> "{{execution.titleVar}}", "year" -> "{{project.year}}"),
      taskExecutionVariables = executionVars)

    // Updating a referenced project variable must succeed and update the task
    putVariable(projectName, TemplateVariable("year", "2003", None, None, isSensitive = false, VariableScope.project))
    val updatedTask = project.anyTask(taskName).data.asInstanceOf[VariablesTestTask]
    updatedTask.year shouldBe 2003
    updatedTask.title shouldBe "Terminator"

    // Updating and deleting an unreferenced project variable must work and not consider the task invalid
    val monthDependencies = variablesDependencies(projectName, "month")
    monthDependencies.dependentTasks shouldBe empty
    removeVariableError(projectName, "month") shouldBe None

    // The dependency check must list the task for the referenced variable without errors
    variablesDependencies(projectName, "year").dependentTasks.map(_.id) shouldBe Seq(taskName)
  }

  it should "not disclose sensitive parent variables when retrieving variables" in {
    val projectName = "variables-test-sensitive"
    val secretValue = "very-secret-password"
    val taskName = "sensitiveTask"
    val project = createProjectWithVariablesTask(projectName, taskName,
      projectVariables = Seq(projectVariable("password", secretValue, isSensitive = true)),
      taskParameters = Map("title" -> "T", "year" -> "2002"))

    // The write path must reject execution variables that reference a sensitive parent variable
    val referencingVariables = TemplateVariables(Seq(
      TemplateVariable("leak", "", Some("{{project.password}}"), None, isSensitive = false, VariableScope.execution)))
    an[RequestFailedException] should be thrownBy {
      putVariables(projectName, referencingVariables, Some(taskName))
    }

    // Even if such a variable exists (e.g. imported before the parent was marked sensitive),
    // retrieving the variables must not materialize the sensitive value.
    project.anyTask(taskName).updateExecutionVariables(referencingVariables)
    val response = checkResponse(createRequest(TemplateApi.getVariables(projectName, Some(taskName))).get())
    response.body should not include secretValue
  }

  it should "reject invalid-scope execution variables without persisting them" in {
    val projectName = "variables-test-invalid-scope"
    val taskName = "invalidScopeTask"
    val project = createProjectWithVariablesTask(projectName, taskName,
      taskExecutionVariables = TemplateVariables(Seq(
        TemplateVariable("greeting", "Hello", None, None, isSensitive = false, VariableScope.execution))))

    def taskVariableNames: Seq[String] =
      WorkspaceFactory().workspace.project(projectName).anyTask(taskName).executionVariables.variables.map(_.name)

    // The variables endpoint must reject variables that are not in the execution scope
    an[RequestFailedException] should be thrownBy {
      putVariables(projectName, TemplateVariables(Seq(projectVariable("invalid", "value"))), Some(taskName))
    }
    taskVariableNames shouldBe Seq("greeting")

    // The invalid variables must not have been persisted either: reload the project from the workspace provider
    WorkspaceFactory().workspace.reloadProject(projectName)
    taskVariableNames shouldBe Seq("greeting")

    // Creating a task with invalid-scope variables must fail without creating the task
    implicit val pluginContext: PluginContext = PluginContext.fromProject(project)
    val plugin = ClassPluginDescription(classOf[VariablesTestTask])(
      ParameterValues(Map("title" -> ParameterTemplateValue("T"), "year" -> ParameterTemplateValue("2002"))))
    an[InvalidScopeException] should be thrownBy {
      WorkspaceFactory().workspace.project(projectName)
        .addTask("invalidScopeTask2", plugin, executionVariables = TemplateVariables(Seq(projectVariable("invalid", "value"))))
    }
    WorkspaceFactory().workspace.project(projectName).anyTaskOption("invalidScopeTask2") shouldBe empty
  }

  it should "delete unrelated variables even when a variable references a sensitive parent" in {
    val projectName = "variables-test-sensitive-delete"
    val taskName = "sensitiveDeleteTask"
    val project = createProjectWithVariablesTask(projectName, taskName,
      projectVariables = Seq(projectVariable("password", "very-secret-password", isSensitive = true)),
      taskParameters = Map("title" -> "T", "year" -> "2002"))

    // Simulate a pre-existing (e.g. imported) variable referencing a sensitive parent, plus an unrelated variable.
    val leakVariable = TemplateVariable("leak", "previouslyResolved", Some("{{project.password}}"), None, isSensitive = false, VariableScope.execution)
    val otherVariable = TemplateVariable("other", "someValue", None, None, isSensitive = false, VariableScope.execution)
    project.anyTask(taskName).updateExecutionVariables(TemplateVariables(Seq(leakVariable, otherVariable)))

    // Deleting the unrelated variable must succeed...
    removeVariableError(projectName, "other", Some(taskName)) shouldBe None
    // ...and the unresolvable variable must keep its stored value (the sensitive value must not be materialized).
    val remaining = project.anyTask(taskName).executionVariables
    remaining.variables.map(_.name) shouldBe Seq("leak")
    remaining.variables.head.value shouldBe "previouslyResolved"

    // The offending variable itself can be deleted as well.
    removeVariableError(projectName, "leak", Some(taskName)) shouldBe None
    project.anyTask(taskName).executionVariables.variables shouldBe empty
  }

  it should "reorder variables even when a variable references a sensitive parent" in {
    val projectName = "variables-test-sensitive-reorder"
    val taskName = "sensitiveReorderTask"
    val project = createProjectWithVariablesTask(projectName, taskName,
      projectVariables = Seq(projectVariable("password", "very-secret-password", isSensitive = true)),
      taskParameters = Map("title" -> "T", "year" -> "2002"))

    // Simulate a pre-existing (e.g. imported) variable referencing a sensitive parent, plus two unrelated variables.
    val leakVariable = TemplateVariable("leak", "previouslyResolved", Some("{{project.password}}"), None, isSensitive = false, VariableScope.execution)
    val firstVariable = TemplateVariable("first", "1", None, None, isSensitive = false, VariableScope.execution)
    val secondVariable = TemplateVariable("second", "", Some("{{execution.first}}"), None, isSensitive = false, VariableScope.execution)
    project.anyTask(taskName).updateExecutionVariables(TemplateVariables(Seq(leakVariable, firstVariable, secondVariable)))

    // Reordering must succeed and keep the unresolvable variable's stored value
    reorderVariablesError(projectName, Seq("first", "second", "leak"), Some(taskName)) shouldBe None
    val reordered = project.anyTask(taskName).executionVariables
    reordered.variables.map(_.name) shouldBe Seq("first", "second", "leak")
    reordered.variables.find(_.name == "leak").get.value shouldBe "previouslyResolved"

    // Genuine ordering violations are still rejected with the dependency information
    val json = reorderVariablesError(projectName, Seq("second", "first", "leak"), Some(taskName)).get
    (json \ "dependencies").as[JsObject] shouldBe Json.obj("second" -> Seq("first"))
  }

  it should "provide a list of dependent variables and tasks" in {
    val projectName = "variables-test-7"
    val personVar = projectVariable("person", "Arnold")
    val titleVar = projectVariable("title", "Terminator")
    val yearVar = projectVariable("year", "2002")
    val movie1Var = TemplateVariable("movie1", "", Some("Terminator ({{project.year}})"), None, isSensitive = false, VariableScope.project)
    val taskName = "movie"
    createProjectWithVariablesTask(projectName, taskName,
      projectVariables = Seq(personVar, titleVar, yearVar, movie1Var),
      taskParameters = Map("title" -> "{{project.title}}", "year" -> "{{project.year}}"))

    // Variable with no dependencies
    variablesDependencies(projectName, personVar.name) shouldBe VariableDependencies(Seq.empty, Seq.empty)

    // Variable that depends on a task
    val dependencies1 = variablesDependencies(projectName, titleVar.name)
    dependencies1.dependentVariables shouldBe empty
    dependencies1.dependentTasks.map(_.id) shouldBe Seq(taskName)

    // Variable that depends on other variables
    val dependencies2 = variablesDependencies(projectName, yearVar.name)
    dependencies2.dependentVariables shouldBe Seq(movie1Var.name)
    dependencies2.dependentTasks.map(_.id) shouldBe Seq(taskName)
  }

  it should "provide dependent execution variables for task-scoped dependency requests" in {
    val projectName = "variables-test-task-dependencies"
    val taskName = "dependencyTask"
    createProjectWithVariablesTask(projectName, taskName,
      taskExecutionVariables = TemplateVariables(Seq(
        TemplateVariable("a", "1", None, None, isSensitive = false, VariableScope.execution),
        TemplateVariable("b", "", Some("{{execution.a}}"), None, isSensitive = false, VariableScope.execution))))

    // The dependency endpoint must list the dependent execution variable
    val dependencies = variablesDependencies(projectName, "a", Some(taskName))
    dependencies.dependentVariables shouldBe Seq("b")
    dependencies.dependentTasks shouldBe empty

    // Deleting is refused as long as the dependent variable exists
    val error = removeVariableError(projectName, "a", Some(taskName))
    error should not be empty
    error.get.dependentVariables shouldBe Seq("b")

    // After deleting the dependent variable, the delete succeeds and no dependencies are reported
    variablesDependencies(projectName, "b", Some(taskName)) shouldBe VariableDependencies(Seq.empty, Seq.empty)
    removeVariableError(projectName, "b", Some(taskName)) shouldBe None
    removeVariableError(projectName, "a", Some(taskName)) shouldBe None
  }

  it should "refuse deleting an execution variable that a parameter template of the task references" in {
    val projectName = "variables-test-task-dependencies-2"
    val taskName = "dependencyTask2"
    createProjectWithVariablesTask(projectName, taskName,
      taskParameters = Map("title" -> "{{execution.titleVar}}", "year" -> "2002"),
      taskExecutionVariables = TemplateVariables(Seq(
        TemplateVariable("titleVar", "Terminator", None, None, isSensitive = false, VariableScope.execution),
        TemplateVariable("other", "x", None, None, isSensitive = false, VariableScope.execution))))

    // The dependency endpoint must list the task itself
    val dependencies = variablesDependencies(projectName, "titleVar", Some(taskName))
    dependencies.dependentVariables shouldBe empty
    dependencies.dependentTasks.map(_.id) shouldBe Seq(taskName)

    // The delete must be refused with the used-by-task error
    val ex = the[RequestFailedException] thrownBy {
      removeVariable(projectName, "titleVar", Some(taskName))
    }
    (ex.response.json \ "title").as[String] shouldBe "Cannot delete variable"
    (ex.response.json \ "taskId").as[String] shouldBe taskName

    // Unreferenced variables can still be deleted
    variablesDependencies(projectName, "other", Some(taskName)) shouldBe VariableDependencies(Seq.empty, Seq.empty)
    removeVariableError(projectName, "other", Some(taskName)) shouldBe None
  }

  it should "refuse deleting a project variable that an execution-variable template of a task references" in {
    val projectName = "variables-test-execution-variable-template-dependency"
    val taskName = "executionVariableTemplateTask"
    createProjectWithVariablesTask(projectName, taskName,
      projectVariables = Seq(projectVariable("base", "2002"), projectVariable("unreferenced", "x")),
      taskParameters = Map("title" -> "T", "year" -> "2002"),
      taskExecutionVariables = TemplateVariables(Seq(
        TemplateVariable("derived", "2002", Some("{{project.base}}"), None, isSensitive = false, VariableScope.execution))))

    // The dependency endpoint must list the task whose execution variable references the project variable
    val dependencies = variablesDependencies(projectName, "base")
    dependencies.dependentVariables shouldBe empty
    dependencies.dependentTasks.map(_.id) shouldBe Seq(taskName)

    // The delete must be refused with the used-by-task error and must not remove the variable
    val ex = the[RequestFailedException] thrownBy {
      removeVariable(projectName, "base")
    }
    (ex.response.json \ "title").as[String] shouldBe "Cannot delete variable"
    (ex.response.json \ "taskId").as[String] shouldBe taskName
    getVariables(projectName).map.keySet should contain("base")

    // Unreferenced project variables can still be deleted
    variablesDependencies(projectName, "unreferenced") shouldBe VariableDependencies(Seq.empty, Seq.empty)
    removeVariableError(projectName, "unreferenced") shouldBe None

    // After overwriting the execution variable with a plain value, the delete succeeds
    putVariable(projectName, TemplateVariable("derived", "2002", None, None, isSensitive = false, VariableScope.execution), Some(taskName))
    variablesDependencies(projectName, "base") shouldBe VariableDependencies(Seq.empty, Seq.empty)
    removeVariableError(projectName, "base") shouldBe None
  }

  it should "refuse replacing the project variables when a removed variable is referenced by an execution-variable template" in {
    val projectName = "variables-test-execution-variable-template-put"
    val taskName = "executionVariableTemplatePutTask"
    val baseVar = projectVariable("base", "2002")
    val otherVar = projectVariable("other", "x")
    createProjectWithVariablesTask(projectName, taskName,
      projectVariables = Seq(baseVar, otherVar),
      taskParameters = Map("title" -> "T", "year" -> "2002"),
      taskExecutionVariables = TemplateVariables(Seq(
        TemplateVariable("derived", "2002", Some("{{project.base}}"), None, isSensitive = false, VariableScope.execution))))

    // Replacing the variable set without the referenced variable must be refused and must not change anything
    val ex = the[RequestFailedException] thrownBy {
      putVariables(projectName, TemplateVariables(Seq(otherVar)))
    }
    (ex.response.json \ "title").as[String] shouldBe "Cannot update variables"
    (ex.response.json \ "taskId").as[String] shouldBe taskName
    getVariables(projectName).map.keySet shouldBe Set("base", "other")

    // Replacing the variable set while keeping the referenced variable succeeds
    putVariables(projectName, TemplateVariables(Seq(baseVar)))
    getVariables(projectName).map.keySet shouldBe Set("base")
  }

  it should "update the task when a changed execution variable is referenced by a parameter template" in {
    val projectName = "variables-test-task-variable-update"
    val taskName = "executionVariableUpdateTask"
    val project = createProjectWithVariablesTask(projectName, taskName,
      taskParameters = Map("title" -> "T", "year" -> "{{execution.yearVar}}"),
      taskExecutionVariables = TemplateVariables(Seq(
        TemplateVariable("yearVar", "2002", None, None, isSensitive = false, VariableScope.execution))))
    project.anyTask(taskName).data.asInstanceOf[VariablesTestTask].year shouldBe 2002

    // Updating the execution variable must re-instantiate the task with the new default
    putVariable(projectName, TemplateVariable("yearVar", "2003", None, None, isSensitive = false, VariableScope.execution), Some(taskName))
    project.anyTask(taskName).data.asInstanceOf[VariablesTestTask].year shouldBe 2003

    // Updating to a value the task cannot be instantiated with must be refused without persisting
    val ex = the[RequestFailedException] thrownBy {
      putVariable(projectName, TemplateVariable("yearVar", "2003x", None, None, isSensitive = false, VariableScope.execution), Some(taskName))
    }
    (ex.response.json \ "title").as[String] shouldBe "Cannot update variable"
    (ex.response.json \ "taskId").as[String] shouldBe taskName
    getVariable(projectName, "yearVar", Some(taskName)).value shouldBe "2003"
    project.anyTask(taskName).data.asInstanceOf[VariablesTestTask].year shouldBe 2003
  }

  it should "re-resolve execution variable templates when a referenced project variable is updated" in {
    val projectName = "variables-test-execution-variable-refresh"
    val taskName = "executionVariableRefreshTask"
    val project = createProjectWithVariablesTask(projectName, taskName,
      projectVariables = Seq(projectVariable("base", "2002")),
      taskParameters = Map("title" -> "T", "year" -> "{{execution.derived}}"),
      taskExecutionVariables = TemplateVariables(Seq(
        TemplateVariable("derived", "2002", Some("{{project.base}}"), None, isSensitive = false, VariableScope.execution))))

    putVariable(projectName, projectVariable("base", "2003"))

    // The stored value (which seeds each run) must be re-resolved, not only the live GET view
    project.anyTask(taskName).executionVariables.variables.head.value shouldBe "2003"
    // A parameter template referencing the execution variable is re-instantiated as well
    project.anyTask(taskName).data.asInstanceOf[VariablesTestTask].year shouldBe 2003
  }

  it should "resolve execution variable templates when tasks are saved through the task API" in {
    val projectName = "variables-test-task-api"
    val taskName = "taskApiDataset"
    WorkspaceFactory().workspace.createProject(ProjectConfig(projectName))
    putVariables(projectName, TemplateVariables(Seq(projectVariable("graph", "urn:graph:initial"))))

    def taskJson(withVariables: Boolean): JsObject = {
      val base = Json.obj(
        "id" -> taskName,
        "data" -> Json.obj(
          "taskType" -> "Dataset",
          "type" -> "internal",
          "parameters" -> Json.obj("graphUri" -> "urn:graph:unused"),
          "templates" -> Json.obj("graphUri" -> "{{execution.graphVar}}")
        ))
      if (withVariables) {
        base + ("executionVariables" -> Json.arr(Json.obj(
          "name" -> "graphVar", "template" -> "{{project.graph}}", "isSensitive" -> false, "scope" -> "execution")))
      } else {
        base
      }
    }
    def storedTask = WorkspaceFactory().workspace.project(projectName).anyTask(taskName)

    // Creating the task resolves the variable template at save time (no value is provided in the payload)
    checkResponse(createRequest(controllers.workspace.routes.TaskApi.postTask(projectName)).post(taskJson(withVariables = true)))
    storedTask.executionVariables.variables.map(_.value) shouldBe Seq("urn:graph:initial")

    // Updating without the variables resolves the data templates against the stored variables and keeps them
    checkResponse(createRequest(controllers.workspace.routes.TaskApi.putTask(projectName, taskName)).put(taskJson(withVariables = false)))
    storedTask.executionVariables.variables.map(_.value) shouldBe Seq("urn:graph:initial")

    // Retrieving the task evaluates the parameter templates with the execution scope seeded
    checkResponse(createRequest(controllers.workspace.routes.TaskApi.getTask(projectName, taskName)).get())
  }

  it should "return the execution variables of all referenced tasks when retrieving variables transitively" in {
    val projectName = "variables-test-transitive"
    val project = WorkspaceFactory().workspace.createProject(ProjectConfig(projectName))
    // A non-workflow task referenced by the nested sub-workflow
    project.addTask("nestedTask", VariablesTestTask("T", 2002),
      executionVariables = TemplateVariables(Seq(executionVariable("d", "4"), executionVariable("shared", "nestedTaskValue"))))
    project.addTask[Workflow]("sub1", workflowReferencing("nestedTask"),
      executionVariables = TemplateVariables(Seq(executionVariable("b", "2"), executionVariable("shared", "subValue"))))
    project.addTask[Workflow]("sub2", Workflow(),
      executionVariables = TemplateVariables(Seq(executionVariable("c", "3"))))
    // A task that is not part of any workflow, but referenced from the data of a workflow task
    // (like a transform task references rule blocks).
    project.addTask("indirectTask", VariablesTestTask("T", 2002),
      executionVariables = TemplateVariables(Seq(executionVariable("f", "6"))))
    // A non-workflow task referenced by the parent workflow directly
    project.addTask("plainTask", VariablesTestTask("T", 2002, referenced = "indirectTask"),
      executionVariables = TemplateVariables(Seq(executionVariable("e", "5"))))
    // The parent workflow references sub1 twice; its variables must still be returned only once.
    project.addTask[Workflow]("parentWf", workflowReferencing("sub1", "sub2", "sub1", "plainTask"),
      executionVariables = TemplateVariables(Seq(executionVariable("a", "1"), executionVariable("shared", "parentValue"))))

    // Without the flag, only the workflow's own variables are returned.
    getVariables(projectName, Some("parentWf")).variables.map(_.name) shouldBe Seq("a", "shared")

    // With the flag, the variables of all tasks that may take part in the execution follow the own ones
    // in breadth-first order: workflow nodes, tasks referenced by their data and sub-workflows alike.
    // The enclosing workflow's variable wins over sub-task variables of the same name, matching the
    // values that apply when it is executed.
    val transitive = getVariables(projectName, Some("parentWf"), transitive = true)
    transitive.variables.map(v => (v.name, v.value)) shouldBe Seq(
      ("a", "1"), ("shared", "parentValue"), ("b", "2"), ("c", "3"), ("e", "5"), ("d", "4"), ("f", "6"))
  }

  it should "reject transitive variable retrieval without a task and tolerate non-workflow tasks" in {
    val projectName = "variables-test-transitive-edge"
    val taskName = "transitivePlainTask"
    createProjectWithVariablesTask(projectName, taskName,
      taskExecutionVariables = TemplateVariables(Seq(executionVariable("greeting", "Hello"))))

    // The flag requires a task.
    val ex = the[RequestFailedException] thrownBy {
      getVariables(projectName, task = None, transitive = true)
    }
    ex.response.status shouldBe 400

    // On a non-workflow task, only the task's own variables are returned.
    getVariables(projectName, Some(taskName), transitive = true).variables.map(_.name) shouldBe Seq("greeting")
  }

  private def projectVariable(name: String, value: String, isSensitive: Boolean = false): TemplateVariable =
    TemplateVariable(name, value, None, None, isSensitive, VariableScope.project)

  private def executionVariable(name: String, value: String): TemplateVariable =
    TemplateVariable(name, value, None, None, isSensitive = false, VariableScope.execution)

  /** A workflow whose operators reference the given tasks. */
  private def workflowReferencing(taskIds: String*): Workflow = {
    val operators = taskIds.zipWithIndex.map { case (taskId, index) =>
      WorkflowOperator(inputs = Seq.empty, task = taskId, outputs = Seq.empty, errorOutputs = Seq.empty,
        position = (0, 0), nodeId = s"${taskId}_$index", configInputs = Seq.empty, dependencyInputs = Seq.empty)
    }
    Workflow(operators = WorkflowOperatorsParameter(operators))
  }

  /**
    * Creates a project with the given project variables and a [[VariablesTestTask]] whose parameters
    * are set as templates. Optionally defines execution variables on the created task.
    */
  private def createProjectWithVariablesTask(projectName: String,
                                             taskName: String,
                                             projectVariables: Seq[TemplateVariable] = Seq(projectVariable("year", "2002")),
                                             taskParameters: Map[String, String] = Map("title" -> "T", "year" -> "{{project.year}}"),
                                             taskExecutionVariables: TemplateVariables = TemplateVariables.empty): Project = {
    val project = WorkspaceFactory().workspace.createProject(ProjectConfig(projectName))
    if (projectVariables.nonEmpty) {
      putVariables(projectName, TemplateVariables(projectVariables))
    }
    val baseContext = PluginContext.fromProject(project)
    implicit val pluginContext: PluginContext =
      if (taskExecutionVariables.variables.isEmpty) baseContext
      else baseContext.copy(templateVariables = baseContext.templateVariables.withExecutionDefaults(taskExecutionVariables))
    val plugin = ClassPluginDescription(classOf[VariablesTestTask])(
      ParameterValues(taskParameters.view.mapValues(ParameterTemplateValue(_)).toMap))
    project.addTask(taskName, plugin, executionVariables = taskExecutionVariables)
    project
  }

  def getVariables(projectId: String, task: Option[String] = None, transitive: Boolean = false): TemplateVariables = {
    val request = createRequest(TemplateApi.getVariables(projectId, task, transitive))
    val json = checkResponse(request.get()).json
    Json.fromJson[TemplateVariablesJson](json).get.convert
  }

  def putVariables(projectId: String, variables: TemplateVariables, task: Option[String] = None): Unit = {
    val request = createRequest(TemplateApi.putVariables(projectId, task))
    checkResponse(request.post(Json.toJson(TemplateVariablesJson(variables))))
  }

  def getVariable(projectId: String, variableName: String, task: Option[String] = None): TemplateVariable = {
    val request = createRequest(TemplateApi.getVariable(projectId, variableName, task))
    val json = checkResponse(request.get()).json
    Json.fromJson[TemplateVariableJson](json).get.convert
  }

  def putVariable(projectId: String, variable: TemplateVariable, task: Option[String] = None): Unit = {
    val request = createRequest(TemplateApi.putVariable(projectId, variable.name, task))
    checkResponse(request.put(Json.toJson(TemplateVariableJson(variable))))
  }

  def removeVariableError(projectId: String, variableName: String, task: Option[String] = None): Option[CannotDeleteUsedVariableException] = {
    val request = createRequest(TemplateApi.deleteVariable(projectId, variableName, task))
    val response = Await.result(request.delete(), 200.seconds)
    if(response.status == 200) {
      None
    } else if(response.status == 400) {
      val json = response.json
      Some(CannotDeleteUsedVariableException((json \ "variable").as[String], (json \ "dependentVariables").as[Seq[String]]))
    } else {
      fail(s"Unexpected error code $response.status for endpoint /deleteVariable:" + response.body)
    }
  }

  def removeVariable(projectId: String, variableName: String, task: Option[String] = None): Unit = {
    val request = createRequest(TemplateApi.deleteVariable(projectId, variableName, task))
    checkResponse(request.delete())
  }

  def variablesDependencies(projectId: String, variableName: String, task: Option[String] = None): VariableDependencies = {
    val request = createRequest(TemplateApi.variableDependencies(projectId, variableName, task))
    val json = checkResponse(request.get()).json
    Json.fromJson[VariableDependencies](json).get
  }

  def reorderVariablesError(projectId: String, variableNames: Seq[String], task: Option[String] = None): Option[JsValue] = {
    val request = createRequest(TemplateApi.reorderVariables(projectId, task))
    val response = Await.result(request.post(Json.toJson(variableNames)), 200.seconds)
    if (response.status == 200) {
      None
    } else if (response.status == 400) {
      Some(response.json)
    } else {
      fail(s"Unexpected error code $response.status for endpoint /reorderVariables: " + response.body)
    }
  }

  def validateTemplate(validationRequest: ValidateVariableTemplateRequest): VariableTemplateValidationResponse = {
    val request = createRequest(TemplateApi.validateTemplate())
    val json = checkResponse(request.post(Json.toJson(validationRequest))).json
    Json.fromJson[VariableTemplateValidationResponse](json).get
  }

  def autoCompleteTemplate(projectId: String, task: Option[String] = None, variableName: Option[String] = None): Seq[String] = {
    val request = createRequest(TemplateApi.autoCompleteTemplate())
    val json = checkResponse(request.post(Json.toJson(AutoCompleteVariableTemplateRequest(
      inputString = "{{", cursorPosition = 2, maxSuggestions = None,
      project = Some(projectId), variableName = variableName, task = task)))).json
    Json.fromJson[AutoSuggestAutoCompletionResponse](json).get.replacementResults.flatMap(_.replacements.map(_.value))
  }
}

case class VariablesTestTask(title: String, year: Int, referenced: String = "") extends CustomTask {
  require(year >= 0, "year cannot be negative")

  override def inputPorts: InputPorts = InputPorts.NoInputPorts
  override def outputPort: Option[Port] = None
  // Simulates a task that references another task from its data, like a transform task references rule blocks.
  override def referencedTasks: Set[Identifier] = if (referenced.isEmpty) Set.empty else Set(Identifier(referenced))
}
