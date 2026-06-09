package org.silkframework.plugins.dataset.json

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.config.{PlainTask, Prefixes, Task}
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.execution.local.{GenericEntityTable, LocalExecution}
import org.silkframework.execution.typed.{FileEntity, FileEntitySchema}
import org.silkframework.execution.{ExecutorOutput, TaskException}
import org.silkframework.runtime.activity.TestUserContextTrait
import org.silkframework.runtime.iterator.CloseableIterator
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.util.MockitoSugar

import java.util.zip.ZipInputStream

class LocalJsonToFileOperatorExecutorTest extends AnyFlatSpec with Matchers with MockitoSugar with TestUserContextTrait {
  behavior of "Local JSON to File Operator Executor"

  private val entitySchema = EntitySchema("type", IndexedSeq(UntypedPath("jsonContent")).map(_.asStringTypedPath))

  private val operator = JsonToFileOperator(inputPath = "jsonContent")
  private val task = PlainTask("JsonToFile", operator)
  private val executor = LocalJsonToFileOperatorExecutor()
  private implicit val prefixes: Prefixes = Prefixes.empty
  private implicit val pluginContext: PluginContext = PluginContext.empty

  private def inputTable(jsonStrings: String*): GenericEntityTable =
    inputTable(task, jsonStrings: _*)

  private def inputTable(t: Task[JsonToFileOperator], jsonStrings: String*): GenericEntityTable = {
    val entities = jsonStrings.zipWithIndex.map { case (json, idx) =>
      Entity(s"entity$idx", values = IndexedSeq(Seq(json)), schema = entitySchema)
    }
    GenericEntityTable(CloseableIterator(entities.iterator), entitySchema, t)
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
    entities.head.mimeType mustBe Some("application/json")
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

  // ZIP mode tests

  it should "pack a single entity into a ZIP with entry name entry.json" in {
    val json = """{"hello":"world"}"""
    val zipTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputMode = JsonToFileOutputModeEnum.zip))
    val result = executor.execute(zipTask, Seq(inputTable(zipTask, json)), ExecutorOutput.empty, LocalExecution(useLocalInternalDatasets = false))
    result mustBe defined
    val FileEntitySchema(fileEntities) = result.get
    val entities = fileEntities.typedEntities.toIndexedSeq
    entities.size mustBe 1
    entities.head.mimeType mustBe Some("application/zip")
    val entries = readZipEntries(entities.head)
    entries mustBe Seq(("entry.json", json))
  }

  it should "pack multiple entities into a single ZIP with suffixed entry names" in {
    val json0 = """{"id":0}"""
    val json1 = """{"id":1}"""
    val json2 = """{"id":2}"""
    val zipTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputMode = JsonToFileOutputModeEnum.zip))
    val result = executor.execute(zipTask, Seq(inputTable(zipTask, json0, json1, json2)), ExecutorOutput.empty, LocalExecution(useLocalInternalDatasets = false))
    result mustBe defined
    val FileEntitySchema(fileEntities) = result.get
    val entities = fileEntities.typedEntities.toIndexedSeq
    entities.size mustBe 1
    val entries = readZipEntries(entities.head)
    entries mustBe Seq(("entry-0.json", json0), ("entry-1.json", json1), ("entry-2.json", json2))
  }

  it should "use the configured output file name as the ZIP container name and suffix entry names" in {
    val json0 = """{"a":1}"""
    val json1 = """{"a":2}"""
    val zipTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputFileName = "out.json", outputMode = JsonToFileOutputModeEnum.zip))
    val result = executor.execute(zipTask, Seq(inputTable(zipTask, json0, json1)), ExecutorOutput.empty, LocalExecution(useLocalInternalDatasets = false))
    result mustBe defined
    val FileEntitySchema(fileEntities) = result.get
    val entities = fileEntities.typedEntities.toIndexedSeq
    entities.size mustBe 1
    entities.head.file.name mustBe "out.json"
    val entries = readZipEntries(entities.head)
    entries mustBe Seq(("out-0.json", json0), ("out-1.json", json1))
  }

  it should "use the literal output file name as the ZIP entry name for a single entity" in {
    val json = """{"x":42}"""
    val zipTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputFileName = "out.json", outputMode = JsonToFileOutputModeEnum.zip))
    val result = executor.execute(zipTask, Seq(inputTable(zipTask, json)), ExecutorOutput.empty, LocalExecution(useLocalInternalDatasets = false))
    result mustBe defined
    val FileEntitySchema(fileEntities) = result.get
    val entries = readZipEntries(fileEntities.typedEntities.toIndexedSeq.head)
    entries.map(_._1) mustBe Seq("out.json")
  }

  it should "preserve an explicitly configured MIME type in ZIP mode" in {
    val json = """{"x":1}"""
    val zipTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputMode = JsonToFileOutputModeEnum.zip, mimeType = "application/octet-stream"))
    val result = executor.execute(zipTask, Seq(inputTable(zipTask, json)), ExecutorOutput.empty, LocalExecution(useLocalInternalDatasets = false))
    result mustBe defined
    val FileEntitySchema(fileEntities) = result.get
    fileEntities.typedEntities.toIndexedSeq.head.mimeType mustBe Some("application/octet-stream")
  }

  it should "return an empty FileEntitySchema when there are no input entities in ZIP mode" in {
    val zipTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputMode = JsonToFileOutputModeEnum.zip))
    val emptyTable = GenericEntityTable(CloseableIterator(Iterator.empty), entitySchema, zipTask)
    val result = executor.execute(zipTask, Seq(emptyTable), ExecutorOutput.empty, LocalExecution(useLocalInternalDatasets = false))
    result mustBe defined
    val FileEntitySchema(fileEntities) = result.get
    fileEntities.typedEntities.toIndexedSeq mustBe empty
  }

  it should "produce one ZIP entry per entity for a large number of input entities" in {
    val count = 500
    val zipTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputMode = JsonToFileOutputModeEnum.zip))
    val jsonValues = (0 until count).map(i => s"""{"i":$i}""")
    val result = executor.execute(zipTask, Seq(inputTable(zipTask, jsonValues: _*)), ExecutorOutput.empty, LocalExecution(useLocalInternalDatasets = false))
    result mustBe defined
    val FileEntitySchema(fileEntities) = result.get
    val entries = readZipEntries(fileEntities.typedEntities.toIndexedSeq.head)
    entries.size mustBe count
    entries.head mustBe (("entry-0.json", """{"i":0}"""))
    entries.last mustBe ((s"entry-${count - 1}.json", s"""{"i":${count - 1}}"""))
  }

  it should "throw a TaskException for invalid JSON in ZIP mode" in {
    val invalid = """{"unterminated":"""
    val zipTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputMode = JsonToFileOutputModeEnum.zip))
    val ex = intercept[TaskException] {
      executor.execute(zipTask, Seq(inputTable(zipTask, invalid)), ExecutorOutput.empty, LocalExecution(useLocalInternalDatasets = false))
    }
    ex.getMessage must include ("JSON to File")
    ex.getMessage.toLowerCase must include ("not valid json")
  }

  // outputProperty tests

  it should "wrap the JSON value in an object under the given key" in {
    val wrappedTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputProperty = "payload"))
    val json = """{"name":"Alice"}"""
    val result = executor.execute(wrappedTask, Seq(inputTable(wrappedTask, json)), ExecutorOutput.empty, LocalExecution(useLocalInternalDatasets = false))
    result mustBe defined
    val FileEntitySchema(fileEntities) = result.get
    fileEntities.typedEntities.toIndexedSeq.map(_.file.loadAsString()) mustBe Seq("""{"payload":{"name":"Alice"}}""")
  }

  it should "wrap each entity independently when outputProperty is set and there are multiple entities" in {
    val wrappedTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputProperty = "payload"))
    val json1 = """{"id":1}"""
    val json2 = """{"id":2}"""
    val result = executor.execute(wrappedTask, Seq(inputTable(wrappedTask, json1, json2)), ExecutorOutput.empty, LocalExecution(useLocalInternalDatasets = false))
    result mustBe defined
    val FileEntitySchema(fileEntities) = result.get
    fileEntities.typedEntities.toIndexedSeq.map(_.file.loadAsString()) mustBe Seq("""{"payload":{"id":1}}""", """{"payload":{"id":2}}""")
  }

  it should "wrap each ZIP entry when outputProperty is set in ZIP mode" in {
    val wrappedZipTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputMode = JsonToFileOutputModeEnum.zip, outputProperty = "payload"))
    val json1 = """{"id":1}"""
    val json2 = """{"id":2}"""
    val result = executor.execute(wrappedZipTask, Seq(inputTable(wrappedZipTask, json1, json2)), ExecutorOutput.empty, LocalExecution(useLocalInternalDatasets = false))
    result mustBe defined
    val FileEntitySchema(fileEntities) = result.get
    val entries = readZipEntries(fileEntities.typedEntities.toIndexedSeq.head)
    entries mustBe Seq(("entry-0.json", """{"payload":{"id":1}}"""), ("entry-1.json", """{"payload":{"id":2}}"""))
  }

  it should "wrap a JSON array input when outputProperty is set" in {
    val wrappedTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputProperty = "payload"))
    val json = """[{"id":1},{"id":2}]"""
    val result = executor.execute(wrappedTask, Seq(inputTable(wrappedTask, json)), ExecutorOutput.empty, LocalExecution(useLocalInternalDatasets = false))
    result mustBe defined
    val FileEntitySchema(fileEntities) = result.get
    fileEntities.typedEntities.toIndexedSeq.map(_.file.loadAsString()) mustBe Seq("""{"payload":[{"id":1},{"id":2}]}""")
  }

  it should "throw a TaskException for invalid JSON when outputProperty is set" in {
    val wrappedTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputProperty = "payload"))
    val invalid = """{"unterminated":"""
    val ex = intercept[TaskException] {
      executor.execute(wrappedTask, Seq(inputTable(wrappedTask, invalid)), ExecutorOutput.empty, LocalExecution(useLocalInternalDatasets = false))
    }
    ex.getMessage must include ("JSON to File")
    ex.getMessage.toLowerCase must include ("not valid json")
  }

  // merged JSON mode tests

  it should "merge a single entity into a JSON array" in {
    val mergedTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputMode = JsonToFileOutputModeEnum.jsonArray))
    val result = executor.execute(mergedTask, Seq(inputTable(mergedTask, """{"id":1}""")), ExecutorOutput.empty, LocalExecution(useLocalInternalDatasets = false))
    result mustBe defined
    val FileEntitySchema(fileEntities) = result.get
    fileEntities.typedEntities.toIndexedSeq.map(_.file.loadAsString()) mustBe Seq("""[{"id":1}]""")
  }

  it should "merge multiple entities into a single JSON array in input order" in {
    val mergedTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputMode = JsonToFileOutputModeEnum.jsonArray))
    val result = executor.execute(mergedTask, Seq(inputTable(mergedTask, """{"id":1}""", """{"id":2}""", """{"id":3}""")), ExecutorOutput.empty, LocalExecution(useLocalInternalDatasets = false))
    result mustBe defined
    val FileEntitySchema(fileEntities) = result.get
    val entities = fileEntities.typedEntities.toIndexedSeq
    entities.size mustBe 1
    entities.map(_.file.loadAsString()) mustBe Seq("""[{"id":1},{"id":2},{"id":3}]""")
  }

  it should "return an empty FileEntitySchema when there are no input entities in merged JSON mode" in {
    val mergedTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputMode = JsonToFileOutputModeEnum.jsonArray))
    val emptyTable = GenericEntityTable(CloseableIterator(Iterator.empty), entitySchema, mergedTask)
    val result = executor.execute(mergedTask, Seq(emptyTable), ExecutorOutput.empty, LocalExecution(useLocalInternalDatasets = false))
    result mustBe defined
    val FileEntitySchema(fileEntities) = result.get
    fileEntities.typedEntities.toIndexedSeq mustBe empty
  }

  it should "use the literal output file name with no index suffix in merged JSON mode" in {
    val mergedTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputFileName = "out.json", outputMode = JsonToFileOutputModeEnum.jsonArray))
    val result = executor.execute(mergedTask, Seq(inputTable(mergedTask, """{"id":1}""", """{"id":2}""")), ExecutorOutput.empty, LocalExecution(useLocalInternalDatasets = false))
    result mustBe defined
    val FileEntitySchema(fileEntities) = result.get
    val entities = fileEntities.typedEntities.toIndexedSeq
    entities.size mustBe 1
    entities.head.file.name mustBe "out.json"
  }

  it should "wrap each element with outputProperty before merging in merged JSON mode" in {
    val mergedTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputMode = JsonToFileOutputModeEnum.jsonArray, outputProperty = "payload"))
    val result = executor.execute(mergedTask, Seq(inputTable(mergedTask, """{"id":1}""", """{"id":2}""")), ExecutorOutput.empty, LocalExecution(useLocalInternalDatasets = false))
    result mustBe defined
    val FileEntitySchema(fileEntities) = result.get
    fileEntities.typedEntities.toIndexedSeq.map(_.file.loadAsString()) mustBe Seq("""[{"payload":{"id":1}},{"payload":{"id":2}}]""")
  }

  it should "throw a TaskException for invalid JSON in merged JSON mode" in {
    val mergedTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputMode = JsonToFileOutputModeEnum.jsonArray))
    val ex = intercept[TaskException] {
      executor.execute(mergedTask, Seq(inputTable(mergedTask, """{"unterminated":""")), ExecutorOutput.empty, LocalExecution(useLocalInternalDatasets = false))
    }
    ex.getMessage must include ("JSON to File")
    ex.getMessage.toLowerCase must include ("not valid json")
  }

  private def readZipEntries(fileEntity: FileEntity): Seq[(String, String)] = {
    val zipInput = new ZipInputStream(fileEntity.file.inputStream)
    try {
      Iterator.continually(zipInput.getNextEntry)
        .takeWhile(_ != null)
        .map { entry =>
          val content = scala.io.Source.fromInputStream(zipInput, "UTF-8").mkString
          (entry.getName, content)
        }.toSeq
    } finally {
      zipInput.close()
    }
  }
}
