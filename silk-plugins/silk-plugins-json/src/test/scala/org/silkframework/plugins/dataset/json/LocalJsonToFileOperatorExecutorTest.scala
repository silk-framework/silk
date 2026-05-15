package org.silkframework.plugins.dataset.json

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.config.{PlainTask, Prefixes}
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.execution.local.{GenericEntityTable, LocalExecution}
import org.silkframework.execution.typed.FileEntitySchema
import org.silkframework.execution.{ExecutorOutput, TaskException}
import org.silkframework.runtime.activity.TestUserContextTrait
import org.silkframework.runtime.iterator.CloseableIterator
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.util.MockitoSugar

class LocalJsonToFileOperatorExecutorTest extends AnyFlatSpec with Matchers with MockitoSugar with TestUserContextTrait {
  behavior of "Local JSON to File Operator Executor"

  private val entitySchema = EntitySchema("type", IndexedSeq(UntypedPath("jsonContent")).map(_.asStringTypedPath))

  private val operator = JsonToFileOperator(inputPath = "jsonContent")
  private val task = PlainTask("JsonToFile", operator)
  private val executor = LocalJsonToFileOperatorExecutor()
  private implicit val prefixes: Prefixes = Prefixes.empty
  private implicit val pluginContext: PluginContext = PluginContext.empty

  private def inputTable(jsonStrings: String*): GenericEntityTable = {
    val entities = jsonStrings.zipWithIndex.map { case (json, idx) =>
      Entity(s"entity$idx", values = IndexedSeq(Seq(json)), schema = entitySchema)
    }
    GenericEntityTable(CloseableIterator(entities.iterator), entitySchema, task)
  }

  private def runAndReadFiles(table: GenericEntityTable): Seq[String] = {
    val result = executor.execute(task, Seq(table), ExecutorOutput.empty, LocalExecution(useLocalInternalDatasets = false))
    result mustBe defined
    val FileEntitySchema(fileEntities) = result.get
    fileEntities.typedEntities.toIndexedSeq.map(_.file.loadAsString())
  }

  it should "write the JSON of a single input entity to a file" in {
    val json = """{"hello":"world"}"""
    val contents = runAndReadFiles(inputTable(json))
    contents mustBe Seq(json)
  }

  it should "default to the first field when no input path is configured" in {
    val defaultTask = PlainTask("JsonToFile", JsonToFileOperator())
    val json = """{"hello":"world"}"""
    val table = GenericEntityTable(
      CloseableIterator(Iterator(Entity("e", IndexedSeq(Seq(json)), entitySchema))),
      entitySchema,
      defaultTask
    )
    val result = executor.execute(defaultTask, Seq(table), ExecutorOutput.empty, LocalExecution(useLocalInternalDatasets = false))
    val FileEntitySchema(fileEntities) = result.get
    fileEntities.typedEntities.toIndexedSeq.map(_.file.loadAsString()) mustBe Seq(json)
  }

  it should "produce one file per input entity" in {
    val json1 = """{"id":"1","name":"Alice"}"""
    val json2 = """{"id":"2","name":"Bob"}"""
    val json3 = """{"id":"3","name":"Carol"}"""
    val contents = runAndReadFiles(inputTable(json1, json2, json3))
    contents mustBe Seq(json1, json2, json3)
  }

  it should "use the literal output file name for a single input entity" in {
    val namedTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputFileName = "out.json"))
    val json = """{"x":1}"""
    val table = GenericEntityTable(
      CloseableIterator(Iterator(Entity("e", IndexedSeq(Seq(json)), entitySchema))),
      entitySchema,
      namedTask
    )
    val result = executor.execute(namedTask, Seq(table), ExecutorOutput.empty, LocalExecution(useLocalInternalDatasets = false))
    val FileEntitySchema(fileEntities) = result.get
    val entities = fileEntities.typedEntities.toIndexedSeq
    entities.size mustBe 1
    entities.head.file.name mustBe "out.json"
  }

  it should "append an index suffix when the output file name is set and the input has multiple entities" in {
    val namedTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputFileName = "out.json"))
    val entities = Seq("""{"a":1}""", """{"a":2}""", """{"a":3}""").zipWithIndex.map { case (json, idx) =>
      Entity(s"e$idx", values = IndexedSeq(Seq(json)), schema = entitySchema)
    }
    val table = GenericEntityTable(CloseableIterator(entities.iterator), entitySchema, namedTask)
    val result = executor.execute(namedTask, Seq(table), ExecutorOutput.empty, LocalExecution(useLocalInternalDatasets = false))
    val FileEntitySchema(fileEntities) = result.get
    val names = fileEntities.typedEntities.toIndexedSeq.map(_.file.name)
    names mustBe Seq("out-0.json", "out-1.json", "out-2.json")
  }

  it should "tag the produced file entities with the application/json MIME type" in {
    val json = """{"hello":"world"}"""
    val result = executor.execute(task, Seq(inputTable(json)), ExecutorOutput.empty, LocalExecution(useLocalInternalDatasets = false))
    val FileEntitySchema(fileEntities) = result.get
    val entities = fileEntities.typedEntities.toIndexedSeq
    entities.size mustBe 1
    entities.head.mimeType mustBe Some("application/json")
  }

  it should "throw a TaskException when the input value is empty" in {
    val emptyEntity = Entity("e", IndexedSeq(Seq("")), entitySchema)
    val table = GenericEntityTable(CloseableIterator(Iterator(emptyEntity)), entitySchema, task)
    val ex = intercept[TaskException] {
      executor.execute(task, Seq(table), ExecutorOutput.empty, LocalExecution(useLocalInternalDatasets = false))
    }
    ex.getMessage must include ("JSON to File")
  }

  it should "throw a TaskException when the input value is not valid JSON" in {
    val invalid = """{"unterminated":"""
    val ex = intercept[TaskException] {
      executor.execute(task, Seq(inputTable(invalid)), ExecutorOutput.empty, LocalExecution(useLocalInternalDatasets = false))
    }
    ex.getMessage must include ("JSON to File")
    ex.getMessage.toLowerCase must include ("not valid json")
  }
}
