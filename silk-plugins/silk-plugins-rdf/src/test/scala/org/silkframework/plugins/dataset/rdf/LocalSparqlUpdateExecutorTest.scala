package org.silkframework.plugins.dataset.rdf

import org.mockito.Mockito._
import org.scalatest.{FlatSpec, MustMatchers}
import org.scalatestplus.mockito.MockitoSugar
import org.silkframework.config.{PlainTask, Prefixes, Task}
import org.silkframework.entity._
import org.silkframework.entity.paths.{TypedPath, UntypedPath}
import org.silkframework.execution.ExecutorOutput
import org.silkframework.execution.local.{GenericEntityTable, LocalEntities, LocalExecution, SparqlUpdateEntitySchema}
import org.silkframework.plugins.dataset.rdf.executors.LocalSparqlUpdateExecutor
import org.silkframework.plugins.dataset.rdf.tasks.SparqlUpdateCustomTask
import org.silkframework.plugins.dataset.rdf.tasks.templating.SparqlUpdateTemplatingMode
import org.silkframework.rule.TransformSpec
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.{EmptyResourceManager, ResourceLoader}
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.TestMocks

class LocalSparqlUpdateExecutorTest extends FlatSpec with MustMatchers with MockitoSugar {
  behavior of "Local SPARQL Update Executor"

  implicit val uc: UserContext = UserContext.Empty
  implicit val prefixes: Prefixes = Prefixes.empty
  implicit val resourceLoader: ResourceLoader = EmptyResourceManager()

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
    val inputTaskMock = mock[PlainTask[TransformSpec]]
    val specMock = mock[TransformSpec]
    when(inputTaskMock.data).thenReturn(specMock)
    when(specMock.properties).thenReturn(properties)
    GenericEntityTable(inputEntities, schema, inputTaskMock)
  }

  private val context = TestMocks.activityContextMock()

  it should "generate the correct batches" in {
    val result = executeTask(sparqlUpdateTemplate, Seq(mockInputTable()))
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
    val entities = executeTask(staticTemplate, Seq(mockInputTable())).entities
    entities.size mustBe 1
    entities.head.values mustBe IndexedSeq(Seq(staticTemplate))
  }

  it should "output one UPDATE query per input task when the template contains input property placeholders" in {
    val templateWithInputPropertyPlaceholders = """INSERT DATA { $inputProperties.uri("graph") <urn:prop:label> $inputProperties.plainLiteral("graph") };"""
    val result = executeTask(templateWithInputPropertyPlaceholders, Seq(mockInputTable(Seq("graph" -> "g1")),
      mockInputTable(Seq("graph" -> "g2"))), SparqlUpdateTemplatingMode.velocity)
    result.entities.map(_.values.flatten.head).toList mustBe List("""INSERT DATA { <g1> <urn:prop:label> "g1" };
                                                                    |INSERT DATA { <g2> <urn:prop:label> "g2" };""".stripMargin)
  }

  it should "output one UPDATE query overall even for multiple inputs when no placeholder is used at all" in {
    val staticTemplate = """INSERT DATA { <urn:subject:1> <urn:prop:label> "1" };"""
    val result = executeTask(staticTemplate, Seq(mockInputTable(Seq("graph" -> "g1")),
      mockInputTable(Seq("graph" -> "g2"))), SparqlUpdateTemplatingMode.velocity)
    result.entities.map(_.values.flatten.head).toList mustBe List(staticTemplate)
  }

  private def sparqlUpdateTask(template: String,
                               mode: SparqlUpdateTemplatingMode): Task[SparqlUpdateCustomTask] = {
    PlainTask("task", SparqlUpdateCustomTask(template, batchSize = batchSize, templatingMode = mode))
  }

  private def executeTask(template: String,
                      input: Seq[GenericEntityTable],
                      mode: SparqlUpdateTemplatingMode = SparqlUpdateTemplatingMode.simple): LocalEntities = {
    val result = executor.execute(sparqlUpdateTask(template, mode), input, ExecutorOutput.empty, LocalExecution(true), context)
    result mustBe defined
    result.get
  }
}
