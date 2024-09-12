package org.silkframework.rule.execution

import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.config.{PlainTask, Prefixes}
import org.silkframework.dataset.{DataSource, DatasetSpec, EntitySink}
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.execution.local.GenericEntityTable
import org.silkframework.plugins.dataset.InternalDataset
import org.silkframework.rule._
import org.silkframework.rule.input.{PathInput, TransformInput, Transformer}
import org.silkframework.runtime.activity.{ActivityContext, StatusHolder, UserContext, ValueHolder}
import org.silkframework.runtime.iterator.CloseableIterator
import org.silkframework.util.{Identifier, MockitoSugar, Uri}

class ExecuteTransformTest extends AnyFlatSpec with Matchers with MockitoSugar {
  behavior of "ExecuteTransform"

  implicit val userContext: UserContext = UserContext.Empty
  implicit val prefixes: Prefixes = Prefixes.empty

  it should "output faulty entities to error output" in {
    val prop = "http://prop"
    val prop2 = "http://prop2"
    val outputMock = mock[EntitySink]
    val entities = Seq(entity(IndexedSeq("valid", "valid"), IndexedSeq(prop, prop2)), entity(IndexedSeq("invalid", "valid"), IndexedSeq(prop, prop2)))
    val dataSourceMock = mock[DataSource]
    when(dataSourceMock.retrieve(any(), any())(any(), any())).thenReturn(GenericEntityTable(CloseableIterator(entities.iterator), entities.head.schema, null))
    when(dataSourceMock.underlyingTask).thenReturn(PlainTask("inputTaskDummy", DatasetSpec(InternalDataset())))
    val transform = TransformSpec(datasetSelection(), RootMappingRule(rules = MappingRules(mapping("propTransform", prop), mapping("prop2Transform", prop2))))
    val execute = new ExecuteTransform(
      PlainTask("transformTask", transform),
      input = _ => dataSourceMock,
      output = _ => outputMock
    )
    val contextMock = mock[ActivityContext[TransformReport]]
    val executeTransformResultHolder = new ValueHolder[TransformReport](None)
    when(contextMock.value).thenReturn(executeTransformResultHolder)
    when(contextMock.status).thenReturn(mock[StatusHolder])
    implicit val userContext: UserContext = UserContext.Empty
    execute.run(contextMock)
    verify(outputMock).writeEntity("uri", IndexedSeq(Seq("valid"), Seq("valid")))
    // This functionality has been removed in the LocalExecutor and needs to be reimplemented: verify(errorOutputMock).writeEntity("", IndexedSeq(Seq("invalid"), Seq("valid")))
    val resultStats = executeTransformResultHolder()
    resultStats.entityCount shouldBe 2
    resultStats.entityErrorCount shouldBe 1
    resultStats.ruleResults.size shouldBe 2
    resultStats.ruleResults("propTransform").errorCount shouldBe 1
    resultStats.ruleResults("prop2Transform").errorCount shouldBe 0
  }

  private def transformerWithExceptions(): Transformer = {
    new Transformer {
      override def apply(values: Seq[Seq[String]]): Seq[String] = {
        values.flatten map { v =>
          if(v == "invalid") {
            throw new TestException("Invalid value: " + v)
          } else {
            v
          }
        }
      }
    }
  }

  private def mapping(id: String, prop: String) = {
    val transformation = TransformInput(inputs = IndexedSeq(PathInput(path = UntypedPath(prop))), transformer = transformerWithExceptions())
    ComplexMapping(id = Identifier(id), operator = transformation, target = Some(MappingTarget(Uri(prop + "Target"))))
  }

  private def datasetSelection(): DatasetSelection = {
    DatasetSelection(
      inputId = Identifier("selection"),
      typeUri = Uri("SomeTypeDoesNotMatter")
    )
  }

  private def entity(values: IndexedSeq[String], properties: IndexedSeq[String]): Entity = {
    Entity("uri", values map (v => Seq(v)), EntitySchema(Uri("entity"), typedPaths = properties.map(UntypedPath.saveApply).map(_.asStringTypedPath)))
  }

  private def entity(value: String, propertyUri: String): Entity = {
    entity(IndexedSeq(value), IndexedSeq(propertyUri))
  }
}

class TestException(msg: String) extends RuntimeException(msg)