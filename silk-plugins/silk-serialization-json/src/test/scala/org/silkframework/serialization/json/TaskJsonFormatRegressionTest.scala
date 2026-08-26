package org.silkframework.serialization.json

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.config._
import org.silkframework.dataset._
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.rule._
import org.silkframework.rule.input.{PathInput, TransformInput}
import org.silkframework.rule.plugins.distance.equality.EqualityMetric
import org.silkframework.rule.plugins.transformer.value.ConstantTransformer
import org.silkframework.rule.similarity.Comparison
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.runtime.plugin.{ClassPluginDescription, PluginRegistry}
import org.silkframework.runtime.serialization.{ReadContext, TestReadContext, TestWriteContext, WriteContext}
import org.silkframework.runtime.templating.{SimpleSubstitutionTemplateEngine, TemplateVariable, TemplateVariables, VariableScope}
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.serialization.json.JsonSerializers._
import org.silkframework.util.{ConfigTestTrait, DPair, Identifier, Uri}
import org.silkframework.workspace.activity.workflow.WorkflowTest
import org.silkframework.workspace.annotation.{StickyNote, UiAnnotations}
import play.api.libs.json.{JsBoolean, JsObject, JsString, JsValue, Json}

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.time.Instant

/**
  * Locks the canonical task JSON format produced by [[TaskJsonFormat]] for all task types.
  *
  * The emitted task JSON is a public contract: the task CRUD API, persisted execution reports,
  * search results and the generated client SDKs all depend on it. Round trip tests cannot protect
  * it, since a change that is applied to reading and writing alike passes them. Each test here
  * serializes a deterministic task, compares it structurally against a committed fixture, and
  * additionally checks that reading the canonical JSON back and re-writing it is stable.
  * The remaining tests cover the payloads the format rejects and the value shapes it accepts.
  *
  * To (re-)generate the fixtures, set the environment variable TASK_JSON_REGRESSION_WRITE_DIR to
  * this module's src/test/resources/org/silkframework/serialization/json/taskJsonRegression
  * directory and run the suite once; it will
  * write the fixtures and fail, so that generation cannot silently pass in CI. Review the diff of
  * the generated files before committing — a changed fixture means a changed wire format.
  */
class TaskJsonFormatRegressionTest extends AnyFlatSpec with Matchers with ConfigTestTrait {

  behavior of "Canonical task JSON"

  // Use the dependency-free substitution engine, so that parameter templates can be evaluated in this module's tests.
  override def propertyMap: Map[String, Option[String]] = Map(
    "config.variables.engine" -> Some(SimpleSubstitutionTemplateEngine.id)
  )

  // Suite-local plugins: JsonSerializersTest unregisters its SomeDatasetPlugin mid-run and suites
  // may execute in parallel in the same JVM, so this suite must not share plugin registrations.
  // They are removed again afterwards, since the registry is global and registering twice fails.
  override protected def beforeAll(): Unit = {
    super.beforeAll()
    PluginRegistry.registerPlugin(classOf[TaskJsonRegressionDataset])
    PluginRegistry.registerPlugin(classOf[TaskJsonRegressionCustomTask])
  }

  override protected def afterAll(): Unit = {
    PluginRegistry.unregisterPlugin(classOf[TaskJsonRegressionDataset])
    PluginRegistry.unregisterPlugin(classOf[TaskJsonRegressionCustomTask])
    super.afterAll()
  }

  private implicit val readContext: ReadContext = TestReadContext()

  private final val regenerateDirEnvVar = "TASK_JSON_REGRESSION_WRITE_DIR"
  private final val fixtureResourceFolder = "taskJsonRegression"

  private val metaData = MetaData(
    label = Some("Task label"),
    description = Some("Task description"),
    modified = Some(Instant.parse("2026-07-14T10:30:00Z")),
    created = Some(Instant.parse("2026-07-01T08:00:00Z"))
  )

  private def datasetTask = PlainTask[TaskSpec]("datasetTask", new DatasetSpec(TaskJsonRegressionDataset("stringValue", 6.0)), metaData)

  it should "stay stable for dataset tasks" in {
    checkFixture("dataset", datasetTask)
  }

  it should "write empty meta data as an empty object" in {
    val task = PlainTask[TaskSpec]("minimalDatasetTask", new DatasetSpec(TaskJsonRegressionDataset("stringValue", 6.0)))
    val json = GenericTaskJsonFormat.write(task)(TestWriteContext[JsValue]())
    (json \ METADATA).as[JsObject] shouldBe Json.obj()
  }

  it should "stay stable for dataset tasks with uriProperty and readOnly" in {
    val spec = new DatasetSpec(TaskJsonRegressionDataset("stringValue", 6.0), uriAttribute = Some(Uri("urn:prop:instanceUri")), readOnly = true)
    checkFixture("dataset-uri-property", PlainTask[TaskSpec]("readOnlyDatasetTask", spec, metaData))
  }

  it should "write the project only in a project context" in {
    val projectContext = TestWriteContext[JsValue]().copy(projectId = Some(Identifier("someProject")))
    (GenericTaskJsonFormat.write(datasetTask)(projectContext) \ "project").asOpt[String] shouldBe Some("someProject")
    (GenericTaskJsonFormat.write(datasetTask)(TestWriteContext[JsValue]()) \ "project").asOpt[String] shouldBe None
  }

  it should "stay stable for tasks with parameter templates and execution variables" in {
    val pluginId = ClassPluginDescription(classOf[TaskJsonRegressionDataset]).id.toString
    val executionVariables = TemplateVariables(Seq(
      TemplateVariable("param1Value", "valueFromVariable", None, None, isSensitive = false, VariableScope.execution)))
    // Built by reading canonical JSON, since parameter templates cannot be set via plugin constructors.
    val seedJson = Json.obj(
      ID -> "templatedDatasetTask",
      METADATA -> Json.obj("label" -> "Task label"),
      TaskJsonFormat.EXECUTION_VARIABLES -> JsonSerialization.toJson(executionVariables),
      DATA -> Json.obj(
        TASKTYPE -> TASK_TYPE_DATASET,
        TYPE -> pluginId,
        PARAMETERS -> Json.obj("param2" -> "6.0"),
        TEMPLATES -> Json.obj("param1" -> "{{execution.param1Value}}")
      )
    )
    checkFixture("dataset-templates-execution-variables", JsonSerialization.fromJson[Task[TaskSpec]](seedJson))
  }

  private def transformTask = PlainTask[TaskSpec]("transformTask", TransformSpec(
    selection = DatasetSelection("inputDataset", Uri("urn:type:Person")),
    mappingRule = RootMappingRule(
      rules = MappingRules(propertyRules = Seq(
        DirectMapping(
          id = "labelMapping",
          sourcePath = UntypedPath("label"),
          mappingTarget = MappingTarget("http://www.w3.org/2000/01/rdf-schema#label"),
          metaData = MetaData(Some("Label mapping"))
        )
      )),
      metaData = MetaData(Some("Root mapping"))
    )
  ), metaData)

  it should "stay stable for transform tasks" in {
    checkFixture("transform", transformTask)
  }

  it should "stay stable for tasks serialized with all format options enabled" in {
    val format = new TaskJsonFormat[TaskSpec](
      TaskFormatOptions(
        includeMetaData = Some(true),
        includeTaskData = Some(true),
        includeTaskProperties = Some(true),
        includeRelations = Some(true),
        includeSchemata = Some(true)
      ),
      userContext = Some(UserContext.Empty)
    )
    implicit val context: WriteContext[JsValue] = TestWriteContext[JsValue]()
    val actual = format.write(transformTask)
    compareWithFixture("transform-all-options", actual)
    // Canonical JSON must survive a read -> write round trip unchanged.
    format.write(format.read(actual)) shouldBe actual
  }

  it should "stay stable for linking tasks" in {
    val spec = LinkSpec(
      source = DatasetSelection("sourceDataset", Uri("urn:type:Person")),
      target = DatasetSelection("targetDataset", Uri("urn:type:Person")),
      rule = LinkageRule(operator = Some(Comparison(
        id = "labelComparison",
        threshold = 0.1,
        metric = EqualityMetric(),
        inputs = DPair[org.silkframework.rule.input.Input](
          PathInput("sourceLabel", UntypedPath("label")),
          PathInput("targetLabel", UntypedPath("label"))
        )
      )))
    )
    checkFixture("linking", PlainTask[TaskSpec]("linkingTask", spec, metaData))
  }

  it should "stay stable for workflow tasks" in {
    val workflow = WorkflowTest.testWorkflow.copy(
      replaceableInputs = Seq(WorkflowTest.DS_A1),
      replaceableOutputs = Seq(WorkflowTest.OUTPUT)
    )
    checkFixture("workflow", PlainTask[TaskSpec]("workflowTask", workflow, metaData))
  }

  it should "stay stable for custom tasks" in {
    checkFixture("custom-task", PlainTask[TaskSpec]("customTask", TaskJsonRegressionCustomTask("stringValue", 42), metaData))
  }

  it should "stay stable for rule block tasks" in {
    val stickyNote = StickyNote(
      "sticky ID",
      "content with\nnew\n\nlines",
      "#fff",
      NodePosition(3, 6, 20, 24)
    )
    val spec = RuleBlockSpec(
      RuleBlockModel(
        ports = IndexedSeq(
          RuleBlockPort(
            id = "firstInput",
            label = "First input",
            description = "Used in the main branch.",
            displayOrder = 0
          ),
          RuleBlockPort(
            id = "secondInput",
            label = "Second input",
            description = "Deprecated fallback.",
            displayOrder = 1,
            deprecated = true
          )
        ),
        inputExamples = IndexedSeq(
          RuleBlockInputExample(
            id = "example-1",
            label = Some("Primary example"),
            inputs = Map(
              Identifier("firstInput") -> Seq("value 1", "value 2"),
              Identifier("secondInput") -> Seq("fallback")
            )
          )
        ),
        operator = Some(TransformInput(id = "rootTransform", transformer = ConstantTransformer("constant result"))),
        layout = RuleLayout(Map("rootTransform" -> NodePosition(10, 20, Some(120), Some(60)))),
        uiAnnotations = UiAnnotations(Seq(stickyNote))
      )
    )
    checkFixture("rule-block", PlainTask[TaskSpec]("ruleBlockTask", spec, metaData))
  }

  it should "reject task JSON without a data object" in {
    val json = Json.obj(ID -> "someTask", TASKTYPE -> TASK_TYPE_DATASET)
    val ex = the[BadUserInputException] thrownBy {
      JsonSerialization.fromJson[Task[TaskSpec]](json)
    }
    ex.getMessage should include("data")
  }

  it should "reject the legacy layout with the task data attached at the top level" in {
    val pluginId = ClassPluginDescription(classOf[TaskJsonRegressionDataset]).id.toString
    val json = Json.obj(
      ID -> "legacyDataset",
      TASKTYPE -> TASK_TYPE_DATASET,
      TYPE -> pluginId,
      PARAMETERS -> Json.obj("param1" -> "stringValue", "param2" -> "6.0")
    )
    val ex = the[BadUserInputException] thrownBy {
      JsonSerialization.fromJson[Task[TaskSpec]](json)
    }
    ex.getMessage should include("data")
  }

  it should "reject the legacy transform layout without a parameters object" in {
    val json = Json.obj(
      ID -> "legacyTransform",
      DATA -> Json.obj(
        TASKTYPE -> TASK_TYPE_TRANSFORM,
        "selection" -> Json.obj("inputId" -> "inputDataset", "typeUri" -> "", "restriction" -> ""),
        "root" -> Json.obj(),
        "outputs" -> Json.arr(),
        "targetVocabularies" -> Json.arr()
      )
    )
    val ex = the[BadUserInputException] thrownBy {
      JsonSerialization.fromJson[Task[TaskSpec]](json)
    }
    // The reported path is the one the client sent, without Play's internal 'obj' root.
    ex.getMessage shouldBe "The task JSON is invalid. At 'data': unknown attribute(s): outputs, root, selection, " +
      "targetVocabularies. Valid attributes are: parameters, readOnly, taskType, templates, type, uriProperty. " +
      "Plugin parameters must be provided in the 'parameters' object."
  }

  it should "report a missing parameters object" in {
    val json = Json.obj(ID -> "noParameters", DATA -> Json.obj(TASKTYPE -> TASK_TYPE_TRANSFORM))
    val ex = the[BadUserInputException] thrownBy {
      JsonSerialization.fromJson[Task[TaskSpec]](json)
    }
    ex.getMessage shouldBe "The task JSON is invalid. At 'data.parameters': attribute is missing"
  }

  it should "reject the legacy workflow layout without a parameters object" in {
    val json = Json.obj(
      ID -> "legacyWorkflow",
      DATA -> Json.obj(
        TASKTYPE -> TASK_TYPE_WORKFLOW,
        TYPE -> "workflow",
        "operators" -> Json.arr(),
        "datasets" -> Json.arr()
      )
    )
    val ex = the[BadUserInputException] thrownBy {
      JsonSerialization.fromJson[Task[TaskSpec]](json)
    }
    ex.getMessage should include("parameters")
  }

  it should "reject an unknown attribute in the task data" in {
    // A plugin parameter next to 'parameters' instead of inside it used to be dropped silently.
    val ex = the[BadUserInputException] thrownBy {
      JsonSerialization.fromJson[Task[TaskSpec]](datasetJson("param1" -> JsString("stringValue")))
    }
    ex.getMessage shouldBe "The task JSON is invalid. At 'data': unknown attribute(s): param1. " +
      "Valid attributes are: parameters, readOnly, taskType, templates, type, uriProperty. " +
      "Plugin parameters must be provided in the 'parameters' object."
  }

  it should "list all unknown attributes of the task data" in {
    val ex = the[BadUserInputException] thrownBy {
      JsonSerialization.fromJson[Task[TaskSpec]](datasetJson("selection" -> Json.obj(), "outputs" -> Json.arr()))
    }
    ex.getMessage should include("unknown attribute(s): outputs, selection.")
  }

  it should "reject a parameter that the plugin does not declare" in {
    val json = datasetJson()
    val withUnknownParameter = json ++ Json.obj(
      DATA -> ((json \ DATA).as[JsObject] ++ Json.obj(
        PARAMETERS -> Json.obj("param1" -> "stringValue", "param2" -> "6.0", "param3" -> "unknown"))))
    // Reported as bad user input, so that the task endpoints answer with 400 rather than 500.
    val ex = the[BadUserInputException] thrownBy {
      JsonSerialization.fromJson[Task[TaskSpec]](withUnknownParameter)
    }
    ex.getMessage should include("param3")
    ex.getMessage should include("Valid parameters are: param1, param2")
  }

  it should "accept boolean 'readOnly' values" in {
    val task = JsonSerialization.fromJson[Task[TaskSpec]](datasetJson(TaskDataDto.READ_ONLY -> JsBoolean(true)))
    task.data.asInstanceOf[DatasetSpec[_]].readOnly shouldBe true
  }

  it should "accept 'readOnly' transported as a string, as the workbench does" in {
    val task = JsonSerialization.fromJson[Task[TaskSpec]](datasetJson(TaskDataDto.READ_ONLY -> JsString("true")))
    task.data.asInstanceOf[DatasetSpec[_]].readOnly shouldBe true
  }

  it should "reject 'readOnly' values that are neither a boolean nor its string representation" in {
    val ex = the[BadUserInputException] thrownBy {
      JsonSerialization.fromJson[Task[TaskSpec]](datasetJson(TaskDataDto.READ_ONLY -> JsString("yes")))
    }
    ex.getMessage shouldBe "The task JSON is invalid. At 'data.readOnly': attribute must be a boolean"
  }

  it should "report an invalid task id as a loading error instead of throwing" in {
    val loadedTask = new TaskJsonFormat[TaskSpec]().read(datasetJson(ID -> JsString("invalid task id")))
    // The invalid id itself cannot be reported, but reading it must not fail the load of the whole project.
    loadedTask.error.map(_.taskId) shouldBe Some(Identifier("__unknown__"))
    loadedTask.error.get.throwable.getMessage should include("invalid task id")
  }

  /** Canonical JSON of a dataset task, with the given attributes added to the data object or the envelope. */
  private def datasetJson(additionalFields: (String, JsValue)*): JsObject = {
    val pluginId = ClassPluginDescription(classOf[TaskJsonRegressionDataset]).id.toString
    val (envelopeFields, dataFields) = additionalFields.partition(_._1 == ID)
    Json.obj(
      ID -> "someDataset",
      DATA -> (Json.obj(
        TASKTYPE -> TASK_TYPE_DATASET,
        TYPE -> pluginId,
        PARAMETERS -> Json.obj("param1" -> "stringValue", "param2" -> "6.0")
      ) ++ JsObject(dataFields))
    ) ++ JsObject(envelopeFields)
  }

  /**
    * Serializes the task, compares it against the committed fixture and checks read/write round-trip stability.
    */
  private def checkFixture(name: String,
                           task: Task[TaskSpec],
                           context: WriteContext[JsValue] = TestWriteContext[JsValue]()): Unit = {
    val actual = GenericTaskJsonFormat.write(task)(context)
    compareWithFixture(name, actual)
    // Canonical JSON must survive a read -> write round trip unchanged.
    val roundTrip = GenericTaskJsonFormat.write(GenericTaskJsonFormat.read(actual))(context)
    roundTrip shouldBe actual
  }

  private def compareWithFixture(name: String, actual: JsValue): Unit = {
    sys.env.get(regenerateDirEnvVar) match {
      case Some(dir) =>
        val file = Paths.get(dir).resolve(s"$name.json")
        Files.createDirectories(file.getParent)
        Files.write(file, Json.prettyPrint(actual).getBytes(StandardCharsets.UTF_8))
        fail(s"Fixture '$name' has been (re-)written to $file. Review the diff and re-run without $regenerateDirEnvVar set.")
      case None =>
        // Resolved relative to this class, i.e. src/test/resources/org/silkframework/serialization/json/
        val resourcePath = s"$fixtureResourceFolder/$name.json"
        Option(getClass.getResourceAsStream(resourcePath)) match {
          case Some(stream) =>
            val expected = try { Json.parse(stream) } finally { stream.close() }
            actual shouldBe expected
          case None =>
            fail(s"Missing fixture '$resourcePath'. Set $regenerateDirEnvVar to the fixture resource " +
              s"directory of this module (see class doc) and re-run to generate it. Actual JSON:\n${Json.prettyPrint(actual)}")
        }
    }
  }
}

@Plugin(id = "taskJsonRegressionCustomTask", label = "Task JSON regression custom task")
case class TaskJsonRegressionCustomTask(stringParam: String, numberParam: Int) extends CustomTask {
  override def inputPorts: InputPorts = InputPorts.NoInputPorts
  override def outputPort: Option[Port] = None
}

@Plugin(id = "taskJsonRegressionDataset", label = "Task JSON regression dataset")
case class TaskJsonRegressionDataset(param1: String, param2: Double) extends Dataset {
  override def characteristics: DatasetCharacteristics = DatasetCharacteristics.attributesOnly()
}
