package org.silkframework.rule.execution.methods

import org.mockito.Mockito._
import org.mockito.Matchers._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.config.Prefixes
import org.silkframework.dataset.{DataSource, EntitySink}
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.rule.execution.{ExecuteTransform, TransformReport}
import org.silkframework.rule.input.{PathInput, TransformInput, Transformer}
import org.silkframework.rule._
import org.silkframework.runtime.activity.{ActivityContext, StatusHolder, UserContext, ValueHolder}
import org.silkframework.util.{Identifier, Uri}

/**
  * Created on 4/15/16.
  */
class ExecuteTransformTest extends FlatSpec with Matchers with MockitoSugar {
  behavior of "ExecuteTransform"

  implicit val userContext: UserContext = UserContext.Empty
  implicit val prefixes: Prefixes = Prefixes.empty

  it should "output faulty entities to error output" in {
    val prop = "http://prop"
    val prop2 = "http://prop2"
    val outputMock = mock[EntitySink]
    val entities = Seq(entity(IndexedSeq("valid", "valid"), IndexedSeq(prop, prop2)), entity(IndexedSeq("invalid", "valid"), IndexedSeq(prop, prop2)))
    val dataSourceMock = mock[DataSource]
    when(dataSourceMock.retrieve(any(), any())(any())).thenReturn(entities)
    val execute = new ExecuteTransform(
      taskLabel = "transform task",
      input = _ => dataSourceMock,
      transform = TransformSpec(datasetSelection(), RootMappingRule(rules = MappingRules(mapping("propTransform", prop), mapping("prop2Transform", prop2)))),
      output = _ => outputMock
    )
    val contextMock = mock[ActivityContext[TransformReport]]
    val executeTransformResultHolder = new ValueHolder[TransformReport](None)
    when(contextMock.value).thenReturn(executeTransformResultHolder)
    when(contextMock.status).thenReturn(mock[StatusHolder])
    implicit val userContext: UserContext = UserContext.Empty
    execute.run(contextMock)
    // verify(outputMock).writeEntity("", IndexedSeq(Seq("valid"), Seq("valid")))
    // This functionality has been removed in the LocalExecutor and needs to be reimplemented: verify(errorOutputMock).writeEntity("", IndexedSeq(Seq("invalid"), Seq("valid")))
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
    val transformation = TransformInput(inputs = Seq(PathInput(path = UntypedPath(prop))), transformer = transformerWithExceptions())
    ComplexMapping(id = Identifier(id), operator = transformation, target = Some(MappingTarget(Uri(prop + "Target"))))
  }

  private def datasetSelection(): DatasetSelection = {
    DatasetSelection(
      inputId = Identifier("selection"),
      typeUri = Uri("SomeTypeDoesNotMatter")
    )
  }

  private def entity(values: IndexedSeq[String], properties: IndexedSeq[String]): Entity = {
    Entity("", values map (v => Seq(v)), EntitySchema(Uri("entity"), typedPaths = properties.map(UntypedPath.saveApply).map(_.asStringTypedPath)))
  }

  private def entity(value: String, propertyUri: String): Entity = {
    entity(IndexedSeq(value), IndexedSeq(propertyUri))
  }
}

class TestException(msg: String) extends RuntimeException(msg)