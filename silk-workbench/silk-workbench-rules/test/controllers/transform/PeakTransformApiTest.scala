package controllers.transform

import helper.IntegrationTestTrait

import org.silkframework.entity.paths.UntypedPath
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.rule.input.{PathInput, TransformInput, Transformer}
import org.silkframework.rule.plugins.transformer.combine.ConcatTransformer
import org.silkframework.rule.plugins.transformer.date.DateToTimestampTransformer
import org.silkframework.rule.plugins.transformer.normalize.LowerCaseTransformer
import org.silkframework.rule.plugins.transformer.tokenization.CamelCaseTokenizer
import org.silkframework.rule.{ComplexMapping, PatternUriMapping, TransformRule}
import org.silkframework.serialization.json.JsonSerializers.TransformRuleJsonFormat
import org.silkframework.serialization.json.{JsonHelpers, JsonSerialization}
import org.silkframework.util.Uri
import org.silkframework.workspace.SingleProjectWorkspaceProviderTestTrait
import test.Routes
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

/**
  *
  */
class PeakTransformApiTest extends AnyFlatSpec with SingleProjectWorkspaceProviderTestTrait with Matchers with IntegrationTestTrait {

  behavior of "TransformTask API"

  implicit val schema: EntitySchema = EntitySchema(Uri("type"), IndexedSeq(UntypedPath("a").asStringTypedPath, UntypedPath("b").asStringTypedPath))

  override def workspaceProviderId: String = "inMemoryWorkspaceProvider"

  protected override def routes: Option[Class[Routes]] = Some(classOf[test.Routes])

  it should "collect transformation examples" in {
    val rule = transformRule(CamelCaseTokenizer())
    val entities = Seq(
      entity(Seq("aValue"), Seq("bValue")),
      entity(Seq("aValue1"), Seq()),
      entity(Seq(), Seq()),
      entity(Seq(), Seq("bValue2")),
      entity(Seq(), Seq())
    )
    val (tries, errors, errorMsg, peakResult) =  PeakTransformApi.collectTransformationExamples(rule, entities.iterator, limit = 3)
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
      inputs = IndexedSeq(PathInput("p", UntypedPath("a")), PathInput("p", UntypedPath("b"))))
    ComplexMapping(operator = transformation)
  }

  it should "collect transformation examples skipping empty transformation results" in {
    val rule = transformRule(ConcatTransformer(" "))
    val entities = Iterator(
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
    val entities = Iterator(
      entity(Seq("2015"), Seq("no date")),
      entity(Seq("123"), Seq("also no date"))
    )
    val (tries, errors, errorMsg, peakResult) =  PeakTransformApi.collectTransformationExamples(rule, entities, limit = 3)
    tries mustBe 2
    errors mustBe 2
    errorMsg must include ("Invalid date format")
    peakResult mustBe Seq()
  }

  it should "collect the examples lazily" in {
    val rule = transformRule(LowerCaseTransformer())
    var counter = 0
    val entities = for(i <- (1 to 1000).view) yield {
      counter += 1
      entity(Seq("UPPER" + i), Seq("UPPER" + i))
    }
    val (tries, errors, _, peakResult) =  PeakTransformApi.collectTransformationExamples(rule, entities.iterator, limit = 3)
    tries mustBe 3
    errors mustBe 0
    counter mustBe 3
  }

  it should "return results from the API" in {
    val peakResult = peakChildRuleRequest(PatternUriMapping(pattern = "urn:{Name}/{Events/Birth}"))
    peakResult.status.id mustBe "success"
    peakResult.sourcePaths mustBe Some(
      Seq(
        Seq("/Name"),
        Seq("/Events", "/Birth")
      )
    )
    peakResult.results mustBe Some(Seq(
      PeakResult(Seq(Seq("Max Doe"), Seq("May 1900")), Seq("urn:Max+Doe/May+1900"))
    ))
  }

  it should "return results from the API with an object path context" in {
    val peakResult = peakChildRuleRequest(PatternUriMapping(pattern = "urn:{Birth}"), objectPath = Some("Events"))
    peakResult.sourcePaths mustBe Some(
      Seq(
        Seq("/Birth")
      )
    )
    peakResult.results mustBe Some(Seq(
      PeakResult(Seq(Seq("May 1900")), Seq("urn:May+1900"))
    ))
  }

  private def peakChildRuleRequest(transformRule: TransformRule, objectPath: Option[String] = None): PeakResults = {
    val uriPatternUrl = controllers.transform.routes.PeakTransformApi.peakChildRule(projectId, transformXmlTask, rootRuleId).url
    var request = client.url(s"$baseUrl$uriPatternUrl")
    if (objectPath.isDefined) {
      request = request.withQueryStringParameters("objectPath" -> objectPath.get)
    }
    val jsonResponse = checkResponse(request.post(JsonSerialization.toJson(transformRule))).json
    JsonHelpers.fromJsonValidated[PeakResults](jsonResponse)
  }

  private val transformXmlTask = "Transform"
  private val rootRuleId = "root"

  private def entity(values: Seq[String]*)(implicit entitySchema: EntitySchema): Entity = {
    Entity("uri", values.toIndexedSeq, entitySchema)
  }

  /**
    * Returns the path of the XML zip project that should be loaded before the test suite starts.
    */
  override def projectPathInClasspath: String = "controllers/transform/hierarchicalPerson.zip"
}
