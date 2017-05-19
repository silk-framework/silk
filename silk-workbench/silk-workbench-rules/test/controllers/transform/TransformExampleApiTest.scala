package controllers.transform

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.entity.{Entity, EntitySchema, Path}
import org.silkframework.rule.ComplexMapping
import org.silkframework.rule.input.{PathInput, TransformInput, Transformer}
import org.silkframework.rule.plugins.transformer.combine.ConcatTransformer
import org.silkframework.rule.plugins.transformer.date.DateToTimestampTransformer
import org.silkframework.rule.plugins.transformer.tokenization.CamelCaseTokenizer
import org.silkframework.util.Uri

/**
  *
  */
class TransformExampleApiTest extends FlatSpec with MustMatchers {
  behavior of "TransformTask API"

  val transformTaskApi = new TransformTaskApi()

  implicit val schema = EntitySchema(Uri("type"), IndexedSeq(Path("a").asStringTypedPath, Path("b").asStringTypedPath))

  it should "collect transformation examples" in {
    val rule = transformRule(CamelCaseTokenizer())
    val entities = Seq(
      entity(Seq("aValue"), Seq("bValue")),
      entity(Seq("aValue1"), Seq()),
      entity(Seq(), Seq()),
      entity(Seq(), Seq("bValue2")),
      entity(Seq(), Seq())
    )
    val (tries, errors, errorMsg, peakResult) =  transformTaskApi.collectTransformationExamples(rule, entities, limit = 3)
    tries mustBe 4
    errors mustBe 0
    errorMsg mustBe ""
    peakResult mustBe Seq(
      PeakResult(Seq(Seq("aValue"), Seq("bValue")),Seq("Value", "a", "Value", "b")),
      PeakResult(Seq(Seq("aValue1"), Seq()),Seq("a", "Value1")),
      PeakResult(Seq(Seq(), Seq("bValue2")),Seq("b", "Value2"))
    )
  }

  private def transformRule(transformer: Transformer): ComplexMapping = {
    val transformation = TransformInput(transformer = transformer,
      inputs = Seq(PathInput("p", Path("a")), PathInput("p", Path("b"))))
    ComplexMapping(operator = transformation)
  }

  it should "collect transformation examples skipping empty transformation results" in {
    val rule = transformRule(ConcatTransformer(" "))
    val entities = Seq(
      entity(Seq("aValue"), Seq("bValue")),
      entity(Seq("aValue1"), Seq()),
      entity(Seq(), Seq()),
      entity(Seq(), Seq("bValue2")),
      entity(Seq("aValue3"), Seq("bValue3")),
      entity(Seq(), Seq())
    )
    val (tries, errors, errorMsg, peakResult) =  transformTaskApi.collectTransformationExamples(rule, entities, limit = 3)
    tries mustBe 6
    errors mustBe 0
    errorMsg mustBe ""
    peakResult mustBe Seq(
      PeakResult(Seq(Seq("aValue"), Seq("bValue")), Seq("aValue bValue")),
      PeakResult(Seq(Seq("aValue3"), Seq("bValue3")), Seq("aValue3 bValue3"))
    )
  }

  it should "return exception count and message when collecting transformation examples" in {
    val rule = transformRule(DateToTimestampTransformer())
    val entities = Seq(
      entity(Seq("2015"), Seq("no date")),
      entity(Seq("123"), Seq("also no date"))
    )
    val (tries, errors, errorMsg, peakResult) =  transformTaskApi.collectTransformationExamples(rule, entities, limit = 3)
    tries mustBe 2
    errors mustBe 2
    errorMsg mustBe "IllegalArgumentException: no date"
    peakResult mustBe Seq()
  }

  private def entity(values: Seq[String]*)
                    (implicit entitySchema: EntitySchema): Entity = {
    new Entity("uri", values.toIndexedSeq, entitySchema)
  }
}
