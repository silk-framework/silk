package controllers.transform

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.rule.ComplexMapping
import org.silkframework.rule.input.{PathInput, TransformInput, Transformer}
import org.silkframework.rule.plugins.transformer.combine.ConcatTransformer
import org.silkframework.rule.plugins.transformer.date.DateToTimestampTransformer
import org.silkframework.rule.plugins.transformer.normalize.LowerCaseTransformer
import org.silkframework.rule.plugins.transformer.tokenization.CamelCaseTokenizer
import org.silkframework.util.Uri

/**
  *
  */
class PeakTransformApiTest extends FlatSpec with MustMatchers {

  behavior of "TransformTask API"

  implicit val schema = EntitySchema(Uri("type"), IndexedSeq(UntypedPath("a").asStringTypedPath, UntypedPath("b").asStringTypedPath))

  it should "collect transformation examples" in {
    val rule = transformRule(CamelCaseTokenizer())
    val entities = Seq(
      entity(Seq("aValue"), Seq("bValue")),
      entity(Seq("aValue1"), Seq()),
      entity(Seq(), Seq()),
      entity(Seq(), Seq("bValue2")),
      entity(Seq(), Seq())
    )
    val (tries, errors, errorMsg, peakResult) =  PeakTransformApi.collectTransformationExamples(rule, entities, limit = 3)
    tries mustBe 4
    errors mustBe 0
    errorMsg mustBe ""
    peakResult mustBe Seq(
      PeakResult(Seq(Seq("aValue"), Seq("bValue")),Seq("a", "Value", "b", "Value")),
      PeakResult(Seq(Seq("aValue1"), Seq()),Seq("a", "Value1")),
      PeakResult(Seq(Seq(), Seq("bValue2")),Seq("b", "Value2"))
    )
  }

  private def transformRule(transformer: Transformer): ComplexMapping = {
    val transformation = TransformInput(transformer = transformer,
      inputs = Seq(PathInput("p", UntypedPath("a")), PathInput("p", UntypedPath("b"))))
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
    val (tries, errors, errorMsg, peakResult) =  PeakTransformApi.collectTransformationExamples(rule, entities, limit = 3)
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
    val (tries, errors, errorMsg, peakResult) =  PeakTransformApi.collectTransformationExamples(rule, entities, limit = 3)
    tries mustBe 2
    errors mustBe 2
    errorMsg mustBe "IllegalArgumentException: no date"
    peakResult mustBe Seq()
  }

  it should "collect the examples lazily" in {
    val rule = transformRule(LowerCaseTransformer())
    var counter = 0
    val entities = for(i <- (1 to 1000).view) yield {
      counter += 1
      entity(Seq("UPPER" + i), Seq("UPPER" + i))
    }
    val (tries, errors, _, peakResult) =  PeakTransformApi.collectTransformationExamples(rule, entities, limit = 3)
    tries mustBe 3
    errors mustBe 0
    counter mustBe 3
  }

  private def entity(values: Seq[String]*)(implicit entitySchema: EntitySchema): Entity = {
    Entity("uri", values.toIndexedSeq, entitySchema)
  }
}
