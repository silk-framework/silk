package org.silkframework.plugins.dataset.json

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.config.{PlainTask, Task}
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
  import LocalJsonToFileOperatorExecutorTest._

  behavior of "Local JSON to File Operator Executor"

  private val entitySchema = EntitySchema("type", IndexedSeq(UntypedPath("jsonContent")).map(_.asStringTypedPath))
  private val operator = JsonToFileOperator(inputPath = "jsonContent")
  private val task = PlainTask("JsonToFile", operator)
  private val executor = LocalJsonToFileOperatorExecutor()
  private val mergedTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputMode = JsonToFileOutputModeEnum.jsonArray))
  private implicit val pluginContext: PluginContext = PluginContext.empty

  it should "write the JSON of a single input entity to a file" in {
    val json = """{"hello":"world"}"""
    runJsonToFile(executor, entitySchema, task, json).map(_.file.loadAsString()) mustBe Seq(json)
  }

  it should "default to the first field when no input path is configured" in {
    val defaultTask = PlainTask("JsonToFile", JsonToFileOperator())
    val json = """{"hello":"world"}"""
    runJsonToFile(executor, entitySchema, defaultTask, json).map(_.file.loadAsString()) mustBe Seq(json)
  }

  it should "produce one file per input entity" in {
    val json1 = """{"id":"1","name":"Alice"}"""
    val json2 = """{"id":"2","name":"Bob"}"""
    val json3 = """{"id":"3","name":"Carol"}"""
    runJsonToFile(executor, entitySchema, task, json1, json2, json3).map(_.file.loadAsString()) mustBe Seq(json1, json2, json3)
  }

  it should "use the literal output file name for a single input entity" in {
    val namedTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputFileName = "out.json"))
    val files = runJsonToFile(executor, entitySchema, namedTask, """{"x":1}""")
    files.map(_.file.name) mustBe Seq("out.json")
    files.head.mimeType mustBe Some("application/json")
  }

  it should "append an index suffix when the output file name is set and the input has multiple entities" in {
    val namedTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputFileName = "out.json"))
    runJsonToFile(executor, entitySchema, namedTask, """{"a":1}""", """{"a":2}""", """{"a":3}""").map(_.file.name) mustBe Seq("out-0.json", "out-1.json", "out-2.json")
  }

  it should "tag the produced file entities with the application/json MIME type" in {
    runJsonToFile(executor, entitySchema, task, """{"hello":"world"}""").map(_.mimeType) mustBe Seq(Some("application/json"))
  }

  it should "throw a TaskException when the input value is empty" in {
    val ex = intercept[TaskException] {
      runJsonToFile(executor, entitySchema, task, "")
    }
    ex.getMessage must include ("JSON to File")
  }

  it should "throw a TaskException when the input value is not valid JSON" in {
    val ex = intercept[TaskException] {
      runJsonToFile(executor, entitySchema, task, """{"unterminated":""")
    }
    ex.getMessage must include ("JSON to File")
    ex.getMessage.toLowerCase must include ("not valid json")
  }

  // ZIP mode tests

  it should "pack a single entity into a ZIP with entry name entry.json" in {
    val json = """{"hello":"world"}"""
    val zipTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputMode = JsonToFileOutputModeEnum.zip))
    val files = runJsonToFile(executor, entitySchema, zipTask, json)
    files.size mustBe 1
    files.head.mimeType mustBe Some("application/zip")
    readZipEntries(files.head) mustBe Seq(("entry.json", json))
  }

  it should "pack multiple entities into a single ZIP with suffixed entry names" in {
    val json0 = """{"id":0}"""
    val json1 = """{"id":1}"""
    val json2 = """{"id":2}"""
    val zipTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputMode = JsonToFileOutputModeEnum.zip))
    val files = runJsonToFile(executor, entitySchema, zipTask, json0, json1, json2)
    files.size mustBe 1
    readZipEntries(files.head) mustBe Seq(("entry-0.json", json0), ("entry-1.json", json1), ("entry-2.json", json2))
  }

  it should "use the configured output file name as the ZIP container name and suffix entry names" in {
    val json0 = """{"a":1}"""
    val json1 = """{"a":2}"""
    val zipTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputFileName = "out.json", outputMode = JsonToFileOutputModeEnum.zip))
    val files = runJsonToFile(executor, entitySchema, zipTask, json0, json1)
    files.size mustBe 1
    files.head.file.name mustBe "out.json"
    readZipEntries(files.head) mustBe Seq(("out-0.json", json0), ("out-1.json", json1))
  }

  it should "use the literal output file name as the ZIP entry name for a single entity" in {
    val json = """{"x":42}"""
    val zipTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputFileName = "out.json", outputMode = JsonToFileOutputModeEnum.zip))
    readZipEntries(runJsonToFile(executor, entitySchema, zipTask, json).head).map(_._1) mustBe Seq("out.json")
  }

  it should "preserve an explicitly configured MIME type in ZIP mode" in {
    val zipTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputMode = JsonToFileOutputModeEnum.zip, mimeType = "application/octet-stream"))
    runJsonToFile(executor, entitySchema, zipTask, """{"x":1}""").head.mimeType mustBe Some("application/octet-stream")
  }

  it should "return an empty FileEntitySchema when there are no input entities in ZIP mode" in {
    val zipTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputMode = JsonToFileOutputModeEnum.zip))
    runJsonToFile(executor, entitySchema, zipTask) mustBe empty
  }

  it should "produce one ZIP entry per entity for a large number of input entities" in {
    val count = 500
    val zipTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputMode = JsonToFileOutputModeEnum.zip))
    val jsonValues = (0 until count).map(i => s"""{"i":$i}""")
    val entries = readZipEntries(runJsonToFile(executor, entitySchema, zipTask, jsonValues: _*).head)
    entries.size mustBe count
    entries.head mustBe (("entry-0.json", """{"i":0}"""))
    entries.last mustBe ((s"entry-${count - 1}.json", s"""{"i":${count - 1}}"""))
  }

  it should "throw a TaskException for invalid JSON in ZIP mode" in {
    val zipTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputMode = JsonToFileOutputModeEnum.zip))
    val ex = intercept[TaskException] {
      runJsonToFile(executor, entitySchema, zipTask, """{"unterminated":""")
    }
    ex.getMessage must include ("JSON to File")
    ex.getMessage.toLowerCase must include ("not valid json")
  }

  // outputProperty tests

  it should "wrap the JSON value in an object under the given key" in {
    val wrappedTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputProperty = "payload"))
    runJsonToFile(executor, entitySchema, wrappedTask, """{"name":"Alice"}""").map(_.file.loadAsString()) mustBe Seq("""{"payload":{"name":"Alice"}}""")
  }

  it should "wrap each entity independently when outputProperty is set and there are multiple entities" in {
    val wrappedTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputProperty = "payload"))
    runJsonToFile(executor, entitySchema, wrappedTask, """{"id":1}""", """{"id":2}""").map(_.file.loadAsString()) mustBe Seq("""{"payload":{"id":1}}""", """{"payload":{"id":2}}""")
  }

  it should "wrap each ZIP entry when outputProperty is set in ZIP mode" in {
    val wrappedZipTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputMode = JsonToFileOutputModeEnum.zip, outputProperty = "payload"))
    readZipEntries(runJsonToFile(executor, entitySchema, wrappedZipTask, """{"id":1}""", """{"id":2}""").head) mustBe Seq(("entry-0.json", """{"payload":{"id":1}}"""), ("entry-1.json", """{"payload":{"id":2}}"""))
  }

  it should "wrap a JSON array input when outputProperty is set" in {
    val wrappedTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputProperty = "payload"))
    runJsonToFile(executor, entitySchema, wrappedTask, """[{"id":1},{"id":2}]""").map(_.file.loadAsString()) mustBe Seq("""{"payload":[{"id":1},{"id":2}]}""")
  }

  it should "throw a TaskException for invalid JSON when outputProperty is set" in {
    val wrappedTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputProperty = "payload"))
    val ex = intercept[TaskException] {
      runJsonToFile(executor, entitySchema, wrappedTask, """{"unterminated":""")
    }
    ex.getMessage must include ("JSON to File")
    ex.getMessage.toLowerCase must include ("not valid json")
  }

  // merged JSON mode tests

  it should "merge a single entity into a JSON array" in {
    runJsonToFile(executor, entitySchema, mergedTask, """{"id":1}""").map(_.file.loadAsString()) mustBe Seq("""[{"id":1}]""")
  }

  it should "merge multiple entities into a single JSON array in input order" in {
    runJsonToFile(executor, entitySchema, mergedTask, """{"id":1}""", """{"id":2}""", """{"id":3}""").map(_.file.loadAsString()) mustBe Seq("""[{"id":1},{"id":2},{"id":3}]""")
  }

  it should "return an empty FileEntitySchema when there are no input entities in merged JSON mode" in {
    runJsonToFile(executor, entitySchema, mergedTask) mustBe empty
  }

  it should "use the literal output file name with no index suffix in merged JSON mode" in {
    val namedMergedTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputFileName = "out.json", outputMode = JsonToFileOutputModeEnum.jsonArray))
    runJsonToFile(executor, entitySchema, namedMergedTask, """{"id":1}""", """{"id":2}""").map(_.file.name) mustBe Seq("out.json")
  }

  it should "wrap each element with outputProperty before merging in merged JSON mode" in {
    val wrappedMergedTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputMode = JsonToFileOutputModeEnum.jsonArray, outputProperty = "payload"))
    runJsonToFile(executor, entitySchema, wrappedMergedTask, """{"id":1}""", """{"id":2}""").map(_.file.loadAsString()) mustBe Seq("""[{"payload":{"id":1}},{"payload":{"id":2}}]""")
  }

  it should "throw a TaskException for invalid JSON in merged JSON mode" in {
    val ex = intercept[TaskException] {
      runJsonToFile(executor, entitySchema, mergedTask, """{"unterminated":""")
    }
    ex.getMessage must include ("JSON to File")
    ex.getMessage.toLowerCase must include ("not valid json")
  }

  // merged JSON mode — value preservation, element types, MIME, formatting

  // numeric values pass through verbatim
  it should "preserve a multiple-of-ten integer verbatim in merged JSON mode" in {
    runJsonToFile(executor, entitySchema, mergedTask, """{"id":100}""").map(_.file.loadAsString()) mustBe Seq("""[{"id":100}]""")
  }

  it should "preserve trailing decimal zeros in merged JSON mode" in {
    runJsonToFile(executor, entitySchema, mergedTask, """{"price":1.50}""").map(_.file.loadAsString()) mustBe Seq("""[{"price":1.50}]""")
  }

  it should "preserve a large integer verbatim in merged JSON mode" in {
    runJsonToFile(executor, entitySchema, mergedTask, """{"big":1000000000000000000000}""").map(_.file.loadAsString()) mustBe Seq("""[{"big":1000000000000000000000}]""")
  }

  it should "preserve a plain integer verbatim in merged JSON mode" in {
    runJsonToFile(executor, entitySchema, mergedTask, """{"n":7}""").map(_.file.loadAsString()) mustBe Seq("""[{"n":7}]""")
  }

  it should "preserve numeric scalar elements verbatim in merged JSON mode" in {
    runJsonToFile(executor, entitySchema, mergedTask, "100").map(_.file.loadAsString()) mustBe Seq("[100]")
    runJsonToFile(executor, entitySchema, mergedTask, "2.00").map(_.file.loadAsString()) mustBe Seq("[2.00]")
  }

  // element types: scalars, arrays, null (the other merged tests use objects)
  it should "merge scalar element values in input order in merged JSON mode" in {
    runJsonToFile(executor, entitySchema, mergedTask, "1", "\"text\"", "true", "null").map(_.file.loadAsString()) mustBe Seq("""[1,"text",true,null]""")
  }

  it should "merge array element values in merged JSON mode" in {
    runJsonToFile(executor, entitySchema, mergedTask, "[1,2]", "[3]").map(_.file.loadAsString()) mustBe Seq("[[1,2],[3]]")
  }

  it should "merge a single null element in merged JSON mode" in {
    runJsonToFile(executor, entitySchema, mergedTask, "null").map(_.file.loadAsString()) mustBe Seq("[null]")
  }

  it should "wrap a scalar element with outputProperty before merging in merged JSON mode" in {
    val wrappedMerged = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputMode = JsonToFileOutputModeEnum.jsonArray, outputProperty = "p"))
    runJsonToFile(executor, entitySchema, wrappedMerged, "5").map(_.file.loadAsString()) mustBe Seq("""[{"p":5}]""")
  }

  // MIME type
  it should "tag the merged file with the application/json MIME type by default" in {
    runJsonToFile(executor, entitySchema, mergedTask, """{"id":1}""").head.mimeType mustBe Some("application/json")
  }

  it should "preserve an explicitly configured MIME type in merged JSON mode" in {
    val customMimeTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputMode = JsonToFileOutputModeEnum.jsonArray, mimeType = "application/octet-stream"))
    runJsonToFile(executor, entitySchema, customMimeTask, """{"id":1}""").head.mimeType mustBe Some("application/octet-stream")
  }

  // duplicate keys and per-element formatting preserved
  it should "preserve duplicate object keys in merged JSON mode" in {
    runJsonToFile(executor, entitySchema, mergedTask, """{"a":1,"b":2,"a":3}""").map(_.file.loadAsString()) mustBe Seq("""[{"a":1,"b":2,"a":3}]""")
  }

  it should "preserve pretty-printed element formatting in merged JSON mode" in {
    runJsonToFile(executor, entitySchema, mergedTask, "{\n  \"a\": 1\n}").map(_.file.loadAsString()) mustBe Seq("[{\n  \"a\": 1\n}]")
  }

  // merged JSON mode — trailing content and whitespace padding

  it should "throw a TaskException for a non-JSON token after a valid value in merged JSON mode" in {
    val ex = intercept[TaskException] {
      runJsonToFile(executor, entitySchema, mergedTask, """{"a":1} x""")
    }
    ex.getMessage must include ("JSON to File")
    ex.getMessage.toLowerCase must include ("not valid json")
  }

  it should "throw a TaskException for a comma-separated trailing value at the root in merged JSON mode" in {
    val ex = intercept[TaskException] {
      runJsonToFile(executor, entitySchema, mergedTask, "1, 2")
    }
    ex.getMessage must include ("JSON to File")
    ex.getMessage.toLowerCase must include ("not valid json")
  }

  it should "preserve leading and trailing whitespace around an element value in merged JSON mode" in {
    runJsonToFile(executor, entitySchema, mergedTask, """  {"a":1}  """).map(_.file.loadAsString()) mustBe Seq("""[  {"a":1}  ]""")
  }
}

object LocalJsonToFileOperatorExecutorTest {

  /** Builds an entity table holding one JSON string per entity under the given schema. */
  def inputTable(entitySchema: EntitySchema, t: Task[JsonToFileOperator], jsonStrings: String*): GenericEntityTable = {
    val entities = jsonStrings.zipWithIndex.map { case (json, idx) =>
      Entity(s"entity$idx", values = IndexedSeq(Seq(json)), schema = entitySchema)
    }
    GenericEntityTable(CloseableIterator(entities.iterator), entitySchema, t)
  }

  /** Runs the executor on a task with the given JSON inputs and returns the produced file entities. */
  def runJsonToFile(executor: LocalJsonToFileOperatorExecutor,
          entitySchema: EntitySchema,
          t: Task[JsonToFileOperator],
          jsonStrings: String*)
         (implicit pluginContext: PluginContext): Seq[FileEntity] = {
    val result = executor.execute(t, Seq(inputTable(entitySchema, t, jsonStrings: _*)), ExecutorOutput.empty, LocalExecution(useLocalInternalDatasets = false))
    val FileEntitySchema(fileEntities) = result.getOrElse(throw new AssertionError("'JSON to File' executor returned no output"))
    fileEntities.typedEntities.toIndexedSeq
  }

  /** Reads a ZIP file entity into its (entry name, content) pairs. */
  def readZipEntries(fileEntity: FileEntity): Seq[(String, String)] = {
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
