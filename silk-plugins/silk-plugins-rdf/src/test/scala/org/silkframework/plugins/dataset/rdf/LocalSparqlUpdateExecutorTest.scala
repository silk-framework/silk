package org.silkframework.plugins.dataset.rdf


import org.silkframework.config.{CustomTask, FixedNumberOfInputs, InputPorts, PlainTask, Port, Prefixes, Task}
import org.silkframework.entity._
import org.silkframework.entity.paths.{TypedPath, UntypedPath}
import org.silkframework.execution.{ExecutionReport, ExecutorOutput}
import org.silkframework.execution.local.{GenericEntityTable, LocalEntities, LocalExecution, SparqlUpdateEntitySchema}
import org.silkframework.plugins.dataset.rdf.executors.LocalSparqlUpdateExecutor
import org.silkframework.plugins.dataset.rdf.tasks.SparqlUpdateCustomTask
import org.silkframework.plugins.dataset.rdf.tasks.templating.SparqlUpdateTemplatingMode
import org.silkframework.runtime.activity.{ActivityContext, UserContext}
import org.silkframework.runtime.plugin.{ParameterValues, PluginContext, TestPluginContext}
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.{Identifier, TestMocks}
import org.silkframework.workspace.TestWorkspaceProviderTestTrait
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.execution.report.EntitySample

class LocalSparqlUpdateExecutorTest extends AnyFlatSpec with Matchers with TestWorkspaceProviderTestTrait {
  behavior of "Local SPARQL Update Executor"

  implicit val uc: UserContext = UserContext.Empty
  implicit val prefixes: Prefixes = Prefixes.empty
  implicit val pluginContext: PluginContext = TestPluginContext()

  // execution data
  private val executor = LocalSparqlUpdateExecutor()
  private val batchSize = 5
  private val sparqlUpdateTemplate = s"""INSERT DATA { $${<s>} <urn:prop> $${"v"} } ;"""
  private val schema = EntitySchema("", typedPaths = IndexedSeq(TypedPath("s", ValueType.URI), TypedPath("v", ValueType.STRING)))
  private val notIncluded = "NOT_INCLUDED"
  private val inputEntities: Seq[Entity] = Seq(
    Entity("http://example.org/entity/1", IndexedSeq(Seq("http://s1"), Seq("s1a", "s1b")), schema),
    Entity("http://example.org/entity/2", IndexedSeq(Seq(s"http://$notIncluded"), Seq()), schema),
    Entity("http://example.org/entity/3", IndexedSeq(Seq(), Seq(notIncluded)), schema),
    Entity("http://example.org/entity/4", IndexedSeq(Seq("http://s2a", "http://s2b"), Seq("s2a", "s2b", "s2c")), schema)
  )
  private def mockInputTable(properties: Seq[(String, String)] = Seq.empty,
                             schema: EntitySchema = schema): GenericEntityTable = {
    val inputTaskMock = PlainTask("mockTask", new DummyTaskSpec(properties.toMap))
    GenericEntityTable(inputEntities, schema, inputTaskMock)
  }

  private val context = TestMocks.activityContextMock()

  override def workspaceProviderId: String = "inMemory"

  private val projectName: Identifier = "LocalSparqlUpdateExecutorTest"
  private val project = retrieveOrCreateProject(projectName)

  it should "generate the correct batches" in {
    val activityContext = TestMocks.activityContextMock()
    val result = executeTask(sparqlUpdateTemplate, Seq(mockInputTable()), activityContext = activityContext)
    result.entitySchema mustBe SparqlUpdateEntitySchema.schema
    val entities = result.entities.toSeq
    entities.size mustBe 2
    val list = entities.map(_.values.flatten.head).toList
    val comp = Seq(
      """INSERT DATA { <http://s1> <urn:prop> "s1a" } ;
        |INSERT DATA { <http://s1> <urn:prop> "s1b" } ;
        |INSERT DATA { <http://s2a> <urn:prop> "s2a" } ;
        |INSERT DATA { <http://s2a> <urn:prop> "s2b" } ;
        |INSERT DATA { <http://s2a> <urn:prop> "s2c" } ;""".stripMargin,
      """INSERT DATA { <http://s2b> <urn:prop> "s2a" } ;
        |INSERT DATA { <http://s2b> <urn:prop> "s2b" } ;
        |INSERT DATA { <http://s2b> <urn:prop> "s2c" } ;""".stripMargin)
    list mustBe comp.map(_.replace("\r\n", "\n"))
    // Check report for sample queries
    val report = activityContext.value.get
    report mustBe defined
    report.get.sampleOutputEntities must not be empty
    val samplesEntities = report.get.sampleOutputEntities.head
    samplesEntities.schema.properties must not be empty
    samplesEntities.entities must have size ExecutionReport.SAMPLE_ENTITY_LIMIT
    // Each entity currently represents one query, NOT one batch
    samplesEntities.entities.head.values mustBe IndexedSeq(Seq("""INSERT DATA { <http://s1> <urn:prop> "s1a" } ;"""))
  }

  it should "throw validation exception if an invalid input schema is found" in {
    val invalidSchema = EntitySchema("", typedPaths = IndexedSeq("s", "wrong").map(UntypedPath(_).asUntypedValueType))
    val input = Seq(mockInputTable(schema = invalidSchema))
    intercept[ValidationException] {
      executeTask(sparqlUpdateTemplate, input).entities.head
    }
  }

  it should "output only one UPDATE query when the template contains no placeholders" in {
    val staticTemplate = """INSERT DATA { <urn:s> <urn:prop> "" } ;"""
    val entities = executeTask(staticTemplate, Seq(mockInputTable())).entities.toSeq
    entities.size mustBe 1
    entities.head.values mustBe IndexedSeq(Seq(staticTemplate))
  }

  it should "output one UPDATE query per input task when the template contains input property placeholders" in {
    val templateWithInputPropertyPlaceholders = """INSERT DATA { $inputProperties.uri("graph") <urn:prop:label> $inputProperties.plainLiteral("graph") };"""
    val result = executeTask(templateWithInputPropertyPlaceholders, Seq(mockInputTable(Seq("graph" -> "g1")),
      mockInputTable(Seq("graph" -> "g2"))), SparqlUpdateTemplatingMode.velocity)
    result.entities.map(_.values.flatten.head).toList mustBe List("INSERT DATA { <g1> <urn:prop:label> \"g1\" };\n" +
                                                                  "INSERT DATA { <g2> <urn:prop:label> \"g2\" };")
  }

  it should "output one UPDATE query overall even for multiple inputs when no placeholder is used at all" in {
    val staticTemplate = """INSERT DATA { <urn:subject:1> <urn:prop:label> "1" };"""
    val result = executeTask(staticTemplate, Seq(mockInputTable(Seq("graph" -> "g1")),
      mockInputTable(Seq("graph" -> "g2"))), SparqlUpdateTemplatingMode.velocity)
    result.entities.map(_.values.flatten.head).toList mustBe List(staticTemplate)
  }

  private def sparqlUpdateTask(template: String,
                               mode: SparqlUpdateTemplatingMode): Task[SparqlUpdateCustomTask] = {
    project.updateTask("task", SparqlUpdateCustomTask(template, batchSize = batchSize, templatingMode = mode))
  }

  private def executeTask(template: String,
                          input: Seq[GenericEntityTable],
                          mode: SparqlUpdateTemplatingMode = SparqlUpdateTemplatingMode.simple,
                          activityContext: ActivityContext[ExecutionReport] = context): LocalEntities = {
    val result = executor.execute(sparqlUpdateTask(template, mode), input, ExecutorOutput.empty, LocalExecution(true), activityContext)
    result mustBe defined
    result.get
  }
}

class DummyTaskSpec(params: Map[String, String]) extends CustomTask {

  override def inputPorts: InputPorts = InputPorts.NoInputPorts

  override def outputPort: Option[Port] = None

  override def parameters(implicit pluginContext: PluginContext): ParameterValues = ParameterValues.fromStringMap(params)

}
