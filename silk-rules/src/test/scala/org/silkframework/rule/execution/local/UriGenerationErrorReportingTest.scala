package org.silkframework.rule.execution.local

import org.mockito.Mockito.{mock, when}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.config.{PlainTask, Prefixes}
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.rule.execution.{TransformReport, TransformReportBuilder}
import org.silkframework.rule.input.{InlineTransformer, PathInput, TransformInput}
import org.silkframework.rule.plugins.transformer.selection.CoalesceTransformer
import org.silkframework.rule._
import org.silkframework.runtime.activity.{ActivityContext, StatusHolder, ValueHolder}
import org.silkframework.runtime.iterator.CloseableIterator
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.util.Uri

class UriGenerationErrorReportingTest extends AnyFlatSpec with Matchers {

  behavior of "TransformedEntities URI error reporting"

  it should "report the empty input path for pattern URI rules" in {
    val report = executeReport(
      uriRule = PatternUriMapping(pattern = "http://example.org/{ID}"),
      entity = entity(
        uri = "source",
        pathsAndValues = Seq(
          UntypedPath("ID") -> Seq.empty
        )
      )
    )

    val sampleErrors = report.ruleResults("URI").sampleErrors
    sampleErrors should have size 1
    sampleErrors.head.message should include("ID")
    sampleErrors.head.message should include("empty")
  }

  it should "report the input values for complex URI rules without operator errors" in {
    val report = executeReport(
      uriRule = ComplexUriMapping(
        id = "complexURI",
        operator = TransformInput(
          id = "uri",
          transformer = CoalesceTransformer(),
          inputs = IndexedSeq(
            PathInput("id", UntypedPath("id")),
            PathInput("name", UntypedPath("name"))
          )
        )
      ),
      entity = entity(
        uri = "source",
        pathsAndValues = Seq(
          UntypedPath("id") -> Seq.empty,
          UntypedPath("name") -> Seq.empty
        )
      )
    )

    val sampleErrors = report.ruleResults("complexURI").sampleErrors
    sampleErrors should have size 1
    sampleErrors.head.message should include("Input values")
    sampleErrors.head.message should include("id")
    sampleErrors.head.message should include("name")
  }

  it should "not add a generic no-URI error if a complex URI rule already reported an operator error" in {
    val report = executeReport(
      uriRule = ComplexUriMapping(
        id = "complexURI",
        operator = TransformInput(
          id = "uri",
          transformer = ThrowingTransformer(),
          inputs = IndexedSeq(
            PathInput("id", UntypedPath("id"))
          )
        )
      ),
      entity = entity(
        uri = "source",
        pathsAndValues = Seq(
          UntypedPath("id") -> Seq("boom")
        )
      )
    )

    val sampleErrors = report.ruleResults("complexURI").sampleErrors
    sampleErrors should have size 1
    sampleErrors.head.message shouldBe "Kaboom: boom"
  }

  private def executeReport(uriRule: UriMapping, entity: Entity): TransformReport = {
    val rootRule = RootMappingRule(MappingRules(uriRule = Some(uriRule)))
    val task = PlainTask(
      "transformTask",
      TransformSpec(
        selection = DatasetSelection(inputId = "input"),
        mappingRule = rootRule
      )
    )
    val context = mock(classOf[ActivityContext[TransformReport]])
    val reportHolder = new ValueHolder[TransformReport](None)
    when(context.value).thenReturn(reportHolder)
    when(context.status).thenReturn(mock(classOf[StatusHolder]))
    val reportBuilder = new TransformReportBuilder(task, context)

    implicit val prefixes: Prefixes = Prefixes.empty
    implicit val taskContext: TaskContext = TaskContext(Seq.empty, PluginContext.empty)

    val transformedEntities = new TransformedEntities(
      task = task,
      entities = CloseableIterator.single(entity),
      ruleLabel = rootRule.label(),
      ruleExecution = rootRule.execution(taskContext),
      outputSchema = EntitySchema(Uri("urn:test"), typedPaths = IndexedSeq.empty),
      isRequestedSchema = false,
      abortIfErrorsOccur = false,
      report = reportBuilder
    ).iterator

    transformedEntities.foreach(_ => ())
    reportHolder()
  }

  private def entity(uri: String, pathsAndValues: Seq[(UntypedPath, Seq[String])]): Entity = {
    val typedPaths = pathsAndValues.map(_._1.asStringTypedPath).toIndexedSeq
    val values = pathsAndValues.map(_._2).toIndexedSeq
    Entity(uri = uri, values = values, schema = EntitySchema(Uri("urn:source"), typedPaths = typedPaths))
  }
}

case class ThrowingTransformer() extends InlineTransformer {
  override def apply(values: Seq[Seq[String]]): Seq[String] = {
    val value = values.flatten.headOption.getOrElse("")
    throw new RuntimeException(s"Kaboom: $value")
  }
}
