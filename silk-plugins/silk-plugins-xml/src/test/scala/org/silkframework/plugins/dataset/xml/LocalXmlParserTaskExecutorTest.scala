package org.silkframework.plugins.dataset.xml

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.config.PlainTask
import org.silkframework.entity.{Entity, EntitySchema, Path}
import org.silkframework.execution.ExecutorRegistry
import org.silkframework.execution.local.{FlatEntityTable, GenericEntityTable, LocalExecution}
import org.silkframework.util.{Identifier, Uri}

/**
  * Created on 8/22/16.
  */
class LocalXmlParserTaskExecutorTest extends FlatSpec with MustMatchers with ExecutorRegistry {
  behavior of "Local XML Parser Task Executor"

  val localExecutionContext = LocalExecution(false)
  val exec = LocalXmlParserTaskExecutor()
  val task = XmlParserTask(
    inputPath = "",
    basePath = "",
    uriSuffixPattern = "/someSuffix"
  )
  val inputEntitySchema = EntitySchema(Uri("http://type"), IndexedSeq(Path("http://prop1").asStringTypedPath, Path("http://prop2").asStringTypedPath))
  val inputs = Seq(GenericEntityTable(
    entities = Seq(new Entity(
      "http://entity1",
      IndexedSeq(
        Seq("<root><a>some value</a><b>other value</b></root>"),
        Seq("<root><a>some value2</a><b>other value2</b></root>")),
      inputEntitySchema)),
    entitySchema = inputEntitySchema,
    task = PlainTask(Identifier("id"), task)))

  it should "return no result if no outputSchema was defined" in {
    val result = exec.execute(PlainTask(Identifier("id"), task), inputs = inputs, outputSchemaOpt = None, execution = localExecutionContext)
    result mustBe None
  }

  it should "return the specified result if an outputSchema was defined" in {
    val outputSchema = Some(EntitySchema(Uri(""), IndexedSeq(Path("a").asStringTypedPath)))
    val result = exec.execute(PlainTask(Identifier("id"), task), inputs = inputs, outputSchemaOpt = outputSchema, execution = localExecutionContext)
    result mustBe defined
    val entities = result.get.entities
    entities.size mustBe 1
    val entity = entities.head.asInstanceOf[Entity]
    entity.values mustBe IndexedSeq(Seq("some value"))
    entity.uri.toString mustBe "http://entity1/someSuffix"
  }

  it should "use the inputPath if defined" in {
    val adaptedTask = task.copy(inputPath = "<http://prop2>")
    val outputSchema = Some(EntitySchema(Uri(""), IndexedSeq(Path("a").asStringTypedPath)))
    val result = exec.execute(PlainTask(Identifier("id"), adaptedTask), inputs = inputs, outputSchemaOpt = outputSchema, execution = localExecutionContext)
    result mustBe defined
    val entities = result.get.entities
    entities.size mustBe 1
    entities.head.asInstanceOf[Entity].values mustBe IndexedSeq(Seq("some value2"))
  }

  it should "load from the registry" in {
    executor(XmlParserTask(), LocalExecution(false)).getClass mustBe classOf[LocalXmlParserTaskExecutor]
  }
}
