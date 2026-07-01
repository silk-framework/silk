package org.silkframework.plugins.dataset.json

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.config.{PlainTask, Task}
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.execution.local.{GenericEntityTable, LocalExecution}
import org.silkframework.execution.typed.{FileEntity, FileEntitySchema}
import org.silkframework.execution.{ExecutionReport, ExecutorOutput}
import org.silkframework.runtime.activity.{ActivityMonitor, TestUserContextTrait}
import org.silkframework.runtime.iterator.CloseableIterator
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.util.MockitoSugar

import java.util.zip.ZipInputStream

class LocalJsonToFileOperatorExecutorTest extends AnyFlatSpec with Matchers with MockitoSugar with TestUserContextTrait {
  import LocalJsonToFileOperatorExecutorTest._

  behavior of "Local JSON to File Operator Executor"

  private implicit val entitySchema: EntitySchema = EntitySchema("type", IndexedSeq(UntypedPath("jsonContent")).map(_.asStringTypedPath))
  private implicit val executor: LocalJsonToFileOperatorExecutor = LocalJsonToFileOperatorExecutor()
  private implicit val pluginContext: PluginContext = PluginContext.empty
  private val task = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent"))
  private val mergedTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputMode = JsonToFileOutputModeEnum.jsonArray))

  it should "write the JSON of a single input entity to a file" in {
    val json = """{"hello":"world"}"""
    runJsonToFile(task, json).map(_.file.loadAsString()) mustBe Seq(json)
  }

  it should "default to the first field when no input path is configured" in {
    val defaultTask = PlainTask("JsonToFile", JsonToFileOperator())
    val json = """{"hello":"world"}"""
    runJsonToFile(defaultTask, json).map(_.file.loadAsString()) mustBe Seq(json)
  }

  it should "produce one file per input entity" in {
    val json1 = """{"id":"1","name":"Alice"}"""
    val json2 = """{"id":"2","name":"Bob"}"""
    val json3 = """{"id":"3","name":"Carol"}"""
    runJsonToFile(task, json1, json2, json3).map(_.file.loadAsString()) mustBe Seq(json1, json2, json3)
  }

  it should "tag the produced file entities with the application/json MIME type" in {
    runJsonToFile(task, """{"hello":"world"}""").map(_.mimeType) mustBe Seq(Some("application/json"))
  }

  it should "skip an entity with an empty value and warn in file mode" in {
    val (files, warnings) = runWithReport(task, "")
    files mustBe empty
    warnings must have size 1
  }

  it should "skip an invalid JSON entity and warn in file mode" in {
    val (files, warnings) = runWithReport(task, """{"unterminated":""")
    files mustBe empty
    warnings must have size 1
    warnings.head.toLowerCase must include ("not valid json")
  }

  it should "write files for the valid entities and skip an invalid one in file mode" in {
    val (files, warnings) = runWithReport(task, """{"id":1}""", """{"unterminated":""", """{"id":2}""")
    files.map(_.file.loadAsString()) mustBe Seq("""{"id":1}""", """{"id":2}""")
    warnings must have size 1
  }

  // ZIP mode tests

  it should "pack a single entity into a ZIP with entry name entry.json" in {
    val json = """{"hello":"world"}"""
    val zipTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputMode = JsonToFileOutputModeEnum.zip))
    val files = runJsonToFile(zipTask, json)
    files.size mustBe 1
    files.head.mimeType mustBe Some("application/zip")
    readZipEntries(files.head) mustBe Seq(("entry.json", json))
  }

  it should "pack multiple entities into a single ZIP with suffixed entry names" in {
    val json0 = """{"id":0}"""
    val json1 = """{"id":1}"""
    val json2 = """{"id":2}"""
    val zipTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputMode = JsonToFileOutputModeEnum.zip))
    val files = runJsonToFile(zipTask, json0, json1, json2)
    files.size mustBe 1
    readZipEntries(files.head) mustBe Seq(("entry-0.json", json0), ("entry-1.json", json1), ("entry-2.json", json2))
  }

  it should "preserve an explicitly configured MIME type in ZIP mode" in {
    val zipTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputMode = JsonToFileOutputModeEnum.zip, mimeType = "application/octet-stream"))
    runJsonToFile(zipTask, """{"x":1}""").head.mimeType mustBe Some("application/octet-stream")
  }

  it should "return an empty FileEntitySchema when there are no input entities in ZIP mode" in {
    val zipTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputMode = JsonToFileOutputModeEnum.zip))
    runJsonToFile(zipTask) mustBe empty
  }

  it should "produce one ZIP entry per entity for a large number of input entities" in {
    val count = 500
    val zipTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputMode = JsonToFileOutputModeEnum.zip))
    val jsonValues = (0 until count).map(i => s"""{"i":$i}""")
    val entries = readZipEntries(runJsonToFile(zipTask, jsonValues: _*).head)
    entries.size mustBe count
    entries.head mustBe (("entry-0.json", """{"i":0}"""))
    entries.last mustBe ((s"entry-${count - 1}.json", s"""{"i":${count - 1}}"""))
  }

  it should "skip an invalid JSON entity and warn in ZIP mode" in {
    val zipTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputMode = JsonToFileOutputModeEnum.zip))
    val (files, warnings) = runWithReport(zipTask, """{"unterminated":""")
    readZipEntries(files.head) mustBe empty
    warnings must have size 1
  }

  it should "zip the valid entities with contiguous entry names and skip an invalid one in ZIP mode" in {
    val zipTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputMode = JsonToFileOutputModeEnum.zip))
    val (files, warnings) = runWithReport(zipTask, """{"id":1}""", """{"unterminated":""", """{"id":2}""")
    readZipEntries(files.head) mustBe Seq(("entry-0.json", """{"id":1}"""), ("entry-1.json", """{"id":2}"""))
    warnings must have size 1
  }

  // outputProperty tests

  it should "wrap the JSON value in an object under the given key" in {
    val wrappedTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputProperty = "payload"))
    runJsonToFile(wrappedTask, """{"name":"Alice"}""").map(_.file.loadAsString()) mustBe Seq("""{"payload":{"name":"Alice"}}""")
  }

  it should "wrap each entity independently when outputProperty is set and there are multiple entities" in {
    val wrappedTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputProperty = "payload"))
    runJsonToFile(wrappedTask, """{"id":1}""", """{"id":2}""").map(_.file.loadAsString()) mustBe Seq("""{"payload":{"id":1}}""", """{"payload":{"id":2}}""")
  }

  it should "wrap each ZIP entry when outputProperty is set in ZIP mode" in {
    val wrappedZipTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputMode = JsonToFileOutputModeEnum.zip, outputProperty = "payload"))
    readZipEntries(runJsonToFile(wrappedZipTask, """{"id":1}""", """{"id":2}""").head) mustBe Seq(("entry-0.json", """{"payload":{"id":1}}"""), ("entry-1.json", """{"payload":{"id":2}}"""))
  }

  it should "wrap a JSON array input when outputProperty is set" in {
    val wrappedTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputProperty = "payload"))
    runJsonToFile(wrappedTask, """[{"id":1},{"id":2}]""").map(_.file.loadAsString()) mustBe Seq("""{"payload":[{"id":1},{"id":2}]}""")
  }

  it should "preserve trailing decimal zeros in file mode with outputProperty set" in {
    val wrappedTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputProperty = "payload"))
    runJsonToFile(wrappedTask, """{"price":1.50}""").map(_.file.loadAsString()) mustBe Seq("""{"payload":{"price":1.50}}""")
  }

  it should "preserve trailing decimal zeros in ZIP mode with outputProperty set" in {
    val wrappedZipTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputMode = JsonToFileOutputModeEnum.zip, outputProperty = "payload"))
    readZipEntries(runJsonToFile(wrappedZipTask, """{"price":1.50}""").head) mustBe Seq(("entry.json", """{"payload":{"price":1.50}}"""))
  }

  it should "preserve whitespace around the value in file mode with outputProperty set" in {
    val wrappedTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputProperty = "payload"))
    runJsonToFile(wrappedTask, """  {"a":1}  """).map(_.file.loadAsString()) mustBe Seq("""{"payload":  {"a":1}  }""")
  }

  it should "preserve whitespace around the value in ZIP mode with outputProperty set" in {
    val wrappedZipTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputMode = JsonToFileOutputModeEnum.zip, outputProperty = "payload"))
    readZipEntries(runJsonToFile(wrappedZipTask, """  {"a":1}  """).head) mustBe Seq(("entry.json", """{"payload":  {"a":1}  }"""))
  }

  it should "skip an invalid JSON entity and warn when outputProperty is set" in {
    val wrappedTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputProperty = "payload"))
    val (files, warnings) = runWithReport(wrappedTask, """{"unterminated":""")
    files mustBe empty
    warnings must have size 1
  }

  // merged JSON mode tests

  it should "merge a single entity into a JSON array" in {
    runJsonToFile(mergedTask, """{"id":1}""").map(_.file.loadAsString()) mustBe Seq("""[{"id":1}]""")
  }

  it should "merge multiple entities into a single JSON array in input order" in {
    runJsonToFile(mergedTask, """{"id":1}""", """{"id":2}""", """{"id":3}""").map(_.file.loadAsString()) mustBe Seq("""[{"id":1},{"id":2},{"id":3}]""")
  }

  it should "return an empty FileEntitySchema when there are no input entities in merged JSON mode" in {
    runJsonToFile(mergedTask) mustBe empty
  }

  it should "wrap each element with outputProperty before merging in merged JSON mode" in {
    val wrappedMergedTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputMode = JsonToFileOutputModeEnum.jsonArray, outputProperty = "payload"))
    runJsonToFile(wrappedMergedTask, """{"id":1}""", """{"id":2}""").map(_.file.loadAsString()) mustBe Seq("""[{"payload":{"id":1}},{"payload":{"id":2}}]""")
  }

  it should "skip an invalid JSON entity and warn in merged JSON mode" in {
    val (files, warnings) = runWithReport(mergedTask, """{"unterminated":""")
    files.map(_.file.loadAsString()) mustBe Seq("[]")
    warnings must have size 1
  }

  it should "merge the valid entities and skip an invalid one in merged JSON mode" in {
    val (files, warnings) = runWithReport(mergedTask, """{"id":1}""", """{"unterminated":""", """{"id":2}""")
    files.map(_.file.loadAsString()) mustBe Seq("""[{"id":1},{"id":2}]""")
    warnings must have size 1
  }

  it should "set no warnings when all entities are valid in merged JSON mode" in {
    val (_, warnings) = runWithReport(mergedTask, """{"id":1}""", """{"id":2}""")
    warnings mustBe empty
  }

  it should "report the valid entity count alongside the skip warnings in merged JSON mode" in {
    val (_, report) = runCapturingReport(mergedTask, """{"id":1}""", """{"unterminated":""", """{"id":2}""")
    report.map(_.entityCount) mustBe Some(2)
    report.map(_.warnings.size) mustBe Some(1)
  }

  it should "report the valid entity count in file mode" in {
    val (_, report) = runCapturingReport(task, """{"id":1}""", """{"id":2}""")
    report.map(_.entityCount) mustBe Some(2)
  }

  it should "report the valid entity count in ZIP mode" in {
    val zipTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputMode = JsonToFileOutputModeEnum.zip))
    val (_, report) = runCapturingReport(zipTask, """{"id":1}""", """{"id":2}""")
    report.map(_.entityCount) mustBe Some(2)
  }

  // merged JSON mode — value preservation, element types, MIME, formatting

  // numeric values pass through verbatim
  it should "preserve a multiple-of-ten integer verbatim in merged JSON mode" in {
    runJsonToFile(mergedTask, """{"id":100}""").map(_.file.loadAsString()) mustBe Seq("""[{"id":100}]""")
  }

  it should "preserve trailing decimal zeros in merged JSON mode" in {
    runJsonToFile(mergedTask, """{"price":1.50}""").map(_.file.loadAsString()) mustBe Seq("""[{"price":1.50}]""")
  }

  it should "preserve a large integer verbatim in merged JSON mode" in {
    runJsonToFile(mergedTask, """{"big":1000000000000000000000}""").map(_.file.loadAsString()) mustBe Seq("""[{"big":1000000000000000000000}]""")
  }

  it should "preserve a plain integer verbatim in merged JSON mode" in {
    runJsonToFile(mergedTask, """{"n":7}""").map(_.file.loadAsString()) mustBe Seq("""[{"n":7}]""")
  }

  it should "preserve numeric scalar elements verbatim in merged JSON mode" in {
    runJsonToFile(mergedTask, "100").map(_.file.loadAsString()) mustBe Seq("[100]")
    runJsonToFile(mergedTask, "2.00").map(_.file.loadAsString()) mustBe Seq("[2.00]")
  }

  // element types: scalars, arrays, null (the other merged tests use objects)
  it should "merge scalar element values in input order in merged JSON mode" in {
    runJsonToFile(mergedTask, "1", "\"text\"", "true", "null").map(_.file.loadAsString()) mustBe Seq("""[1,"text",true,null]""")
  }

  it should "merge array element values in merged JSON mode" in {
    runJsonToFile(mergedTask, "[1,2]", "[3]").map(_.file.loadAsString()) mustBe Seq("[[1,2],[3]]")
  }

  it should "merge a single null element in merged JSON mode" in {
    runJsonToFile(mergedTask, "null").map(_.file.loadAsString()) mustBe Seq("[null]")
  }

  it should "wrap a scalar element with outputProperty before merging in merged JSON mode" in {
    val wrappedMerged = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputMode = JsonToFileOutputModeEnum.jsonArray, outputProperty = "p"))
    runJsonToFile(wrappedMerged, "5").map(_.file.loadAsString()) mustBe Seq("""[{"p":5}]""")
  }

  // MIME type
  it should "tag the merged file with the application/json MIME type by default" in {
    runJsonToFile(mergedTask, """{"id":1}""").head.mimeType mustBe Some("application/json")
  }

  it should "preserve an explicitly configured MIME type in merged JSON mode" in {
    val customMimeTask = PlainTask("JsonToFile", JsonToFileOperator(inputPath = "jsonContent", outputMode = JsonToFileOutputModeEnum.jsonArray, mimeType = "application/octet-stream"))
    runJsonToFile(customMimeTask, """{"id":1}""").head.mimeType mustBe Some("application/octet-stream")
  }

  // duplicate keys and per-element formatting preserved
  it should "preserve duplicate object keys in merged JSON mode" in {
    runJsonToFile(mergedTask, """{"a":1,"b":2,"a":3}""").map(_.file.loadAsString()) mustBe Seq("""[{"a":1,"b":2,"a":3}]""")
  }

  it should "preserve pretty-printed element formatting in merged JSON mode" in {
    runJsonToFile(mergedTask, "{\n  \"a\": 1\n}").map(_.file.loadAsString()) mustBe Seq("[{\n  \"a\": 1\n}]")
  }

  // merged JSON mode — trailing content and whitespace padding

  it should "skip a non-JSON token after a valid value and warn in merged JSON mode" in {
    val (files, warnings) = runWithReport(mergedTask, """{"a":1} x""")
    files.map(_.file.loadAsString()) mustBe Seq("[]")
    warnings must have size 1
  }

  it should "skip a comma-separated trailing value at the root and warn in merged JSON mode" in {
    val (files, warnings) = runWithReport(mergedTask, "1, 2")
    files.map(_.file.loadAsString()) mustBe Seq("[]")
    warnings must have size 1
  }

  it should "preserve leading and trailing whitespace around an element value in merged JSON mode" in {
    runJsonToFile(mergedTask, """  {"a":1}  """).map(_.file.loadAsString()) mustBe Seq("""[  {"a":1}  ]""")
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
  def runJsonToFile(t: Task[JsonToFileOperator], jsonStrings: String*)
                   (implicit executor: LocalJsonToFileOperatorExecutor,
                    entitySchema: EntitySchema,
                    pluginContext: PluginContext): Seq[FileEntity] = {
    val result = executor.execute(t, Seq(inputTable(entitySchema, t, jsonStrings: _*)), ExecutorOutput.empty, LocalExecution(useLocalInternalDatasets = false))
    val FileEntitySchema(fileEntities) = result.getOrElse(throw new AssertionError("'JSON to File' executor returned no output"))
    fileEntities.typedEntities.toIndexedSeq
  }

  /** Runs the executor with an explicit report context, returning the produced file entities and the execution report
    * (present for any non-empty input; an empty input returns early without setting one). */
  def runCapturingReport(t: Task[JsonToFileOperator], jsonStrings: String*)
                        (implicit executor: LocalJsonToFileOperatorExecutor,
                         entitySchema: EntitySchema,
                         pluginContext: PluginContext): (Seq[FileEntity], Option[ExecutionReport]) = {
    val context = new ActivityMonitor[ExecutionReport]("test")
    val result = executor.execute(t, Seq(inputTable(entitySchema, t, jsonStrings: _*)), ExecutorOutput.empty, LocalExecution(useLocalInternalDatasets = false), context)
    val FileEntitySchema(fileEntities) = result.getOrElse(throw new AssertionError("'JSON to File' executor returned no output"))
    (fileEntities.typedEntities.toIndexedSeq, context.value.get)
  }

  /** Runs the executor and returns the produced file entities and any skip warnings. */
  def runWithReport(t: Task[JsonToFileOperator], jsonStrings: String*)
                   (implicit executor: LocalJsonToFileOperatorExecutor,
                    entitySchema: EntitySchema,
                    pluginContext: PluginContext): (Seq[FileEntity], Seq[String]) = {
    val (files, report) = runCapturingReport(t, jsonStrings: _*)
    (files, report.map(_.warnings).getOrElse(Seq.empty))
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
