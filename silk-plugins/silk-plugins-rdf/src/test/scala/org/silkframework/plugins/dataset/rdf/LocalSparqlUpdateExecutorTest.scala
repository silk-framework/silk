package org.silkframework.plugins.dataset.rdf

import org.scalatest.{FlatSpec, MustMatchers}
import org.scalatestplus.mockito.MockitoSugar
import org.silkframework.config.{PlainTask, Prefixes}
import org.silkframework.entity._
import org.silkframework.entity.paths.{TypedPath, UntypedPath}
import org.silkframework.execution.ExecutorOutput
import org.silkframework.execution.local.{GenericEntityTable, LocalExecution, SparqlUpdateEntitySchema}
import org.silkframework.plugins.dataset.rdf.executors.LocalSparqlUpdateExecutor
import org.silkframework.plugins.dataset.rdf.tasks.SparqlUpdateCustomTask
import org.silkframework.rule.TransformSpec
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.TestMocks

class LocalSparqlUpdateExecutorTest extends FlatSpec with MustMatchers with MockitoSugar {
  behavior of "Local SPARQL Update Executor"

  implicit val uc: UserContext = UserContext.Empty

  // execution data
  private val executor = LocalSparqlUpdateExecutor()
  private val batchSize = 5
  private val task = PlainTask("task", SparqlUpdateCustomTask(s"""INSERT DATA { $${<s>} <urn:prop> $${"v"} } ;""", batchSize = batchSize))
  private val schema = EntitySchema("", typedPaths = IndexedSeq(TypedPath("s", UriValueType), TypedPath("v", StringValueType)))
  private val notIncluded = "NOT_INCLUDED"
  private val inputEntities: Seq[Entity] = Seq(
    Entity("http://example.org/entity/1", IndexedSeq(Seq("http://s1"), Seq("s1a", "s1b")), schema),
    Entity("http://example.org/entity/2", IndexedSeq(Seq(s"http://$notIncluded"), Seq()), schema),
    Entity("http://example.org/entity/3", IndexedSeq(Seq(), Seq(notIncluded)), schema),
    Entity("http://example.org/entity/4", IndexedSeq(Seq("http://s2a", "http://s2b"), Seq("s2a", "s2b", "s2c")), schema)
  )
  private val inputTask = mock[PlainTask[TransformSpec]]
  private val context = TestMocks.activityContextMock()
  private val input = Seq(GenericEntityTable(inputEntities, schema, inputTask))

  it should "generate the correct batches" in {
    val result = executor.execute(task, input, ExecutorOutput.empty, LocalExecution(true), context)
    result mustBe defined
    result.get.entitySchema mustBe SparqlUpdateEntitySchema.schema
    val entities = result.get.entities.toSeq
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
    val input = Seq(GenericEntityTable(inputEntities, invalidSchema, inputTask))
    intercept[ValidationException] {
      executor.execute(task, input, ExecutorOutput.empty, LocalExecution(true), context).get.entities.head
    }
  }

  it should "output only one UPDATE query when the template contains no placeholders" in {
    val staticTemplate = """INSERT DATA { <urn:s> <urn:prop> "" } ;"""
    val staticTemplateTask = PlainTask("task", SparqlUpdateCustomTask(staticTemplate, batchSize = batchSize))
    val entities = executor.execute(staticTemplateTask, input, ExecutorOutput.empty, LocalExecution(true), context).get.entities
    entities.size mustBe 1
    entities.head.values mustBe IndexedSeq(Seq(staticTemplate))
  }
}
