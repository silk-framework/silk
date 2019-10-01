package org.silkframework.plugins.dataset.xml

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.config.{PlainTask, Prefixes}
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.entity.{Entity, EntitySchema, MultiEntitySchema}
import org.silkframework.execution.{ExecutorOutput, ExecutorRegistry}
import org.silkframework.execution.local.{GenericEntityTable, LocalExecution, MultiEntityTable}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.{Identifier, Uri}

class LocalXmlParserTaskExecutorTest extends FlatSpec with MustMatchers with ExecutorRegistry {

  implicit val userContext: UserContext = UserContext.Empty
  implicit val prefixes: Prefixes = Prefixes.empty

  behavior of "Local XML Parser Task Executor"

  val localExecutionContext = LocalExecution(false)
  val exec = LocalXmlParserTaskExecutor()
  val task = XmlParserTask(
    uriSuffixPattern = "/someSuffix"
  )
  val inputEntitySchema = EntitySchema(Uri("http://type"), IndexedSeq(UntypedPath("http://prop1").asStringTypedPath, UntypedPath("http://prop2").asStringTypedPath))
  val inputs = Seq(GenericEntityTable(
    entities = Seq(Entity(
      "http://entity1",
      IndexedSeq(
        Seq("<root><a>some value</a><b>other value</b></root>"),
        Seq("<root><a>some value2</a><b>other value2</b></root>")),
      inputEntitySchema)),
    entitySchema = inputEntitySchema,
    task = PlainTask(Identifier("id"), task))
  )

  it should "return no result if no outputSchema was defined" in {
    val result = exec.execute(PlainTask(Identifier("id"), task), inputs = inputs, ExecutorOutput.empty, execution = localExecutionContext)
    result mustBe None
  }

  it should "return the specified result if an outputSchema was defined" in {
    val outputSchema = Some(EntitySchema(Uri(""), IndexedSeq(UntypedPath("a").asStringTypedPath)))
    val result = exec.execute(PlainTask(Identifier("id"), task), inputs = inputs, ExecutorOutput(None, outputSchema), execution = localExecutionContext)
    result mustBe defined
    val entities = result.get.entities
    entities.size mustBe 1
    val entity = entities.head
    entity.values mustBe IndexedSeq(Seq("some value"))
    entity.uri.toString mustBe "http://entity1/someSuffix"
  }

  it should "use the inputPath if defined" in {
    val adaptedTask = task.copy(inputPath = "<http://prop2>")
    val outputSchema = Some(EntitySchema(Uri(""), IndexedSeq(UntypedPath("a").asStringTypedPath)))
    val result = exec.execute(PlainTask(Identifier("id"), adaptedTask), inputs = inputs, ExecutorOutput(None, outputSchema), execution = localExecutionContext)
    result mustBe defined
    val entities = result.get.entities
    entities.size mustBe 1
    entities.head.values mustBe IndexedSeq(Seq("some value2"))
  }

  it should "support full dataset execution on the parsed XML" in {
    val outputSchema = EntitySchema(Uri(""), IndexedSeq(UntypedPath("a").asStringTypedPath))
    val result = exec.execute(PlainTask(Identifier("id"), task), inputs,
      output = ExecutorOutput(None, Some(new MultiEntitySchema(outputSchema, IndexedSeq(outputSchema)))),
      execution = LocalExecution(false)
    )
    result mustBe defined
    result.get mustBe a[MultiEntityTable]
    val multiEntityTable = result.get.asInstanceOf[MultiEntityTable]
    multiEntityTable.entities.map(_.values.flatten.head) mustBe Seq("some value")
    multiEntityTable.subTables.size mustBe 1
    multiEntityTable.subTables.head.entities.map(_.values.flatten.head) mustBe Seq("some value")
  }

  it should "load from the registry" in {
    executor(XmlParserTask(), LocalExecution(false)).getClass mustBe classOf[LocalXmlParserTaskExecutor]
  }
}
