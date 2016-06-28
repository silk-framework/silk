package org.silkframework.execution

import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, FlatSpec}
import org.silkframework.config.DatasetSelection
import org.silkframework.dataset.{EntitySink, DataSource}
import org.silkframework.entity.{EntitySchema, Entity, Path}
import org.silkframework.rule.input.{Transformer, PathInput, TransformInput, Input}
import org.silkframework.rule.{ComplexMapping, TransformRule}
import org.silkframework.runtime.activity.{StatusHolder, ValueHolder, ActivityContext}
import org.silkframework.util.{Uri, Identifier}
import org.mockito.Matchers.{eq => mockitoEq, _}
import org.mockito.Mockito._

/**
  * Created on 4/15/16.
  */
class ExecuteTransformTest extends FlatSpec with Matchers with MockitoSugar {
  behavior of "ExecuteTransform"

  it should "output faulty entities to error output" in {
    val prop = "http://prop"
    val prop2 = "http:// prop2"
    val dataSourceMock = mock[DataSource]
    val outputMock = mock[EntitySink]
    val errorOutputMock = mock[EntitySink]
    when(dataSourceMock.retrieve(any(), any())).thenReturn(
      Seq(entity(IndexedSeq("valid", "valid"), IndexedSeq(prop, prop2)), entity(IndexedSeq("invalid", "valid"), IndexedSeq(prop, prop2))))
    val execute = new ExecuteTransform(
      input = dataSourceMock,
      selection = datasetSelection(),
      rules = Seq(mapping("propTransform", prop), mapping("prop2Transform", prop2)),
      outputs = Seq(outputMock),
      errorOutputs = Seq(errorOutputMock)
    )
    val contextMock = mock[ActivityContext[ExecuteTransformResult]]
    val executeTransformResultHolder = new ValueHolder[ExecuteTransformResult](None)
    when(contextMock.value).thenReturn(executeTransformResultHolder)
    when(contextMock.status).thenReturn(mock[StatusHolder])
    execute.run(contextMock)
    verify(outputMock).writeEntity("", IndexedSeq(Seq("valid"), Seq("valid")))
    verify(errorOutputMock).writeEntity("", IndexedSeq(Seq("invalid"), Seq("valid")))
    val resultStats = executeTransformResultHolder()
    resultStats.entityCounter shouldBe 2
    resultStats.entityErrorCounter shouldBe 1
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
    val transformation = TransformInput(inputs = Seq(PathInput(path = Path(prop))), transformer = transformerWithExceptions())
    ComplexMapping(name = Identifier(id), operator = transformation, target = Some(Uri(prop + "Target")))
  }

  private def datasetSelection(): DatasetSelection = {
    DatasetSelection(
      inputId = Identifier("selection"),
      typeUri = Uri("SomeTypeDoesNotMatter")
    )
  }

  private def entity(values: IndexedSeq[String], properties: IndexedSeq[String]): Entity = {
    new Entity("", values map (v => Seq(v)), EntitySchema(Uri("entity"), paths = properties map (Path.apply)))
  }

  private def entity(value: String, propertyUri: String): Entity = {
    entity(IndexedSeq(value), IndexedSeq(propertyUri))
  }
}

class TestException(msg: String) extends RuntimeException(msg)