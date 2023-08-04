package org.silkframework.plugins.dataset.json


import org.silkframework.config.{PlainTask, Prefixes}
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.entity.{Entity, EntitySchema, MultiEntitySchema}
import org.silkframework.execution.{ExecutorOutput, ExecutorRegistry}
import org.silkframework.execution.local.{GenericEntityTable, GenericLocalDatasetExecutor, LocalExecution, MultiEntityTable}
import org.silkframework.runtime.activity.TestUserContextTrait
import org.silkframework.runtime.plugin.{PluginContext, PluginRegistry}
import org.silkframework.util.MockitoSugar
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.runtime.iterator.CloseableIterator

class LocalJsonParserTaskExecutorTest extends AnyFlatSpec with Matchers with MockitoSugar with TestUserContextTrait with ExecutorRegistry {
  behavior of "Local JSON Parser Task Executor"

  private val entitySchema = EntitySchema("type", IndexedSeq(UntypedPath("id"), UntypedPath("jsonContent")).map(_.asStringTypedPath))

  private val jsonContent =
    """{
      |  "rootId": 1,
      |  "persons": [
      |    {
      |      "id": "0",
      |      "name": "John"
      |    },
      |    {
      |      "id": "1",
      |      "name": "Max"
      |    }
      |  ]
      |}
    """.stripMargin
  private val entities = Seq(
    Entity("entity1", values = IndexedSeq(Seq("1"), Seq(jsonContent)), schema = entitySchema)
  )
  private val executor = LocalJsonParserTaskExecutor()
  private val jsonParserTask = JsonParserTask("jsonContent", "persons")
  private val task = PlainTask("JsonParser", jsonParserTask)
  private def inputEntities = GenericEntityTable(CloseableIterator(entities.iterator), entitySchema, task)
  private implicit val prefixes = Prefixes.empty
  private implicit val pluginContext: PluginContext = PluginContext.empty

  PluginRegistry.registerPlugin(classOf[GenericLocalDatasetExecutor])

  it should "parse the JSON and allow entity schema requests against it" in {
    val result = executor.execute(task, Seq(inputEntities),
      output = ExecutorOutput(None, Some(EntitySchema("", IndexedSeq(UntypedPath("name")).map(_.asStringTypedPath)))),
      execution = LocalExecution(false)
    )
    result mustBe defined
    result.get.entities.map(_.values.flatten.head).toSeq mustBe Seq("John", "Max")
  }

  it should "produce multi schema entities" in {
    val outputSchema = EntitySchema("", IndexedSeq(UntypedPath("name")).map(_.asStringTypedPath))
    val result = executor.execute(task, Seq(inputEntities),
      output = ExecutorOutput(None, Some(new MultiEntitySchema(outputSchema, IndexedSeq(outputSchema)))),
      execution = LocalExecution(false)
    )
    result mustBe defined
    result.get mustBe a[MultiEntityTable]
    val multiEntityTable = result.get.asInstanceOf[MultiEntityTable]
    multiEntityTable.entities.map(_.values.flatten.head).toSeq mustBe Seq("John", "Max")
    multiEntityTable.subTables.size mustBe 1
    multiEntityTable.subTables.head.entities.map(_.values.flatten.head).toSeq mustBe Seq("John", "Max")
  }

  it should "support the in-memory version of the JSON dataset, e.g. supporting backward paths" in {
    val outputSchema = EntitySchema("", IndexedSeq(UntypedPath.parse("\\../rootId")).map(_.asStringTypedPath))
    val result = executor.execute(task, Seq(inputEntities),
      output = ExecutorOutput(None, Some(new MultiEntitySchema(outputSchema, IndexedSeq(outputSchema)))),
      execution = LocalExecution(false)
    )
    result mustBe defined
    val values = result.get.entities.flatMap(_.values).flatten.toSeq
    values mustBe Seq("1", "1")
  }

  it should "parse the JSON of multiple entities" in {
    def jsonContent(id: Int): String =
      s"""{
         |  "rootId": 1,
         |  "persons": [
         |    {
         |      "id": "${id}a",
         |      "name": "John"
         |    },
         |    {
         |      "id": "${id}b",
         |      "name": "Max"
         |    }
         |  ]
         |}
      """.stripMargin
    val entityCount = 3
    val entities = Seq.tabulate(entityCount)( i =>
      Entity("entity" + i, values = IndexedSeq(Seq(i.toString), Seq(jsonContent(i))), schema = entitySchema)
    )
    def inputEntities = GenericEntityTable(CloseableIterator(entities.iterator), entitySchema, task)

    val result = executor.execute(task, Seq(inputEntities),
      output = ExecutorOutput(None, Some(EntitySchema("", IndexedSeq(UntypedPath("id")).map(_.asStringTypedPath)))),
      execution = LocalExecution(false)
    )
    result mustBe defined
    result.get.entities.map(_.values.flatten.head).toSeq mustBe Seq("0a", "0b", "1a", "1b", "2a", "2b")
  }
}