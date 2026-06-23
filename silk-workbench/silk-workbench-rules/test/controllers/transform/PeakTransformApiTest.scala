package controllers.transform

import helper.IntegrationTestTrait
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.rule.input.{PathInput, TransformInput, Transformer}
import org.silkframework.rule.plugins.transformer.combine.ConcatTransformer
import org.silkframework.rule.plugins.transformer.date.DateToTimestampTransformer
import org.silkframework.rule.plugins.transformer.normalize.LowerCaseTransformer
import org.silkframework.rule.plugins.transformer.tokenization.CamelCaseTokenizer
import org.silkframework.rule.{ComplexMapping, PatternUriMapping, TaskContext, TransformRule, TransformRuleExecution}
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
    val (tries, errors, errorMsg, peakResult, hasMore, total) =
      PeakTransformApi.collectTransformationExamples(rule, entities.iterator, limit = 10, computeTotal = true)
    tries mustBe 5
    errors mustBe 0
    errorMsg mustBe ""
    hasMore mustBe false
    total mustBe 5
    peakResult mustBe Seq(
      PeakResult(Seq(Seq("aValue"), Seq("bValue")),Seq("a", "Value", "b", "Value")),
      PeakResult(Seq(Seq("aValue1"), Seq()),Seq("a", "Value1")),
      PeakResult(Seq(Seq(), Seq()), Seq()),
      PeakResult(Seq(Seq(), Seq("bValue2")),Seq("b", "Value2")),
      PeakResult(Seq(Seq(), Seq()), Seq())
    )
  }

  private def transformRule(transformer: Transformer): TransformRuleExecution = {
    val transformation = TransformInput(transformer = transformer,
      inputs = IndexedSeq(PathInput("p", UntypedPath("a")), PathInput("p", UntypedPath("b"))))
    ComplexMapping(operator = transformation).execution(TaskContext.empty)
  }

  it should "collect transformation examples including entities whose transform result is empty" in {
    val rule = transformRule(ConcatTransformer(" "))
    val entities = Iterator(
      entity(Seq("aValue"), Seq("bValue")),
      entity(Seq("aValue1"), Seq()),
      entity(Seq(), Seq()),
      entity(Seq(), Seq("bValue2")),
      entity(Seq("aValue3"), Seq("bValue3")),
      entity(Seq(), Seq())
    )
    val (tries, errors, errorMsg, peakResult, hasMore, total) =
      PeakTransformApi.collectTransformationExamples(rule, entities, limit = 10, computeTotal = true)
    tries mustBe 6
    errors mustBe 0
    errorMsg mustBe ""
    hasMore mustBe false
    total mustBe 6
    peakResult mustBe Seq(
      PeakResult(Seq(Seq("aValue"), Seq("bValue")), Seq("aValue bValue")),
      PeakResult(Seq(Seq("aValue1"), Seq()), Seq()),
      PeakResult(Seq(Seq(), Seq()), Seq()),
      PeakResult(Seq(Seq(), Seq("bValue2")), Seq()),
      PeakResult(Seq(Seq("aValue3"), Seq("bValue3")), Seq("aValue3 bValue3")),
      PeakResult(Seq(Seq(), Seq()), Seq())
    )
  }

  it should "return exception count and message when collecting transformation examples" in {
    val rule = transformRule(DateToTimestampTransformer())
    val entities = Iterator(
      entity(Seq("2015"), Seq("no date")),
      entity(Seq("123"), Seq("also no date"))
    )
    val (tries, errors, errorMsg, peakResult, hasMore, total) =
      PeakTransformApi.collectTransformationExamples(rule, entities, limit = 3, computeTotal = true)
    tries mustBe 2
    errors mustBe 2
    errorMsg must include ("Invalid date format")
    hasMore mustBe false
    // Errored entities produce empty output but are still surfaced in the preview (and counted).
    total mustBe 2
    peakResult mustBe Seq(
      PeakResult(Seq(Seq("2015"), Seq("no date")), Seq()),
      PeakResult(Seq(Seq("123"), Seq("also no date")), Seq())
    )
  }

  it should "drain the iterator after the page is filled when computeTotal is true" in {
    val rule = transformRule(LowerCaseTransformer())
    var counter = 0
    val entities = for(i <- (1 to 1000).view) yield {
      counter += 1
      entity(Seq("UPPER" + i), Seq("UPPER" + i))
    }
    val (tries, errors, _, peakResult, hasMore, total) =
      PeakTransformApi.collectTransformationExamples(rule, entities.iterator, limit = 3, computeTotal = true)
    tries mustBe 1000
    errors mustBe 0
    counter mustBe 1000
    hasMore mustBe true
    total mustBe 1000
    peakResult must have size 3
  }

  it should "collect the examples lazily by default" in {
    val rule = transformRule(LowerCaseTransformer())
    var counter = 0
    val entities = for(i <- (1 to 1000).view) yield {
      counter += 1
      entity(Seq("UPPER" + i), Seq("UPPER" + i))
    }
    val (tries, errors, _, peakResult, hasMore, _) =
      PeakTransformApi.collectTransformationExamples(rule, entities.iterator, limit = 3)
    tries mustBe 3
    errors mustBe 0
    counter mustBe 3
    // `hasMore` reflects "the source iterator still has entities" - it does not require draining.
    hasMore mustBe true
    peakResult must have size 3
  }

  it should "skip the first `offset` successful results when paginating" in {
    val rule = transformRule(LowerCaseTransformer())
    val entities = (1 to 10).map(i => entity(Seq("UPPER" + i), Seq("UPPER" + i)))
    val (_, errors, _, peakResult, hasMore, total) =
      PeakTransformApi.collectTransformationExamples(rule, entities.iterator, limit = 3, offset = 3, computeTotal = true)
    errors mustBe 0
    hasMore mustBe true
    total mustBe 10
    peakResult mustBe Seq(
      PeakResult(Seq(Seq("UPPER4"), Seq("UPPER4")), Seq("upper4", "upper4")),
      PeakResult(Seq(Seq("UPPER5"), Seq("UPPER5")), Seq("upper5", "upper5")),
      PeakResult(Seq(Seq("UPPER6"), Seq("UPPER6")), Seq("upper6", "upper6"))
    )
  }

  it should "report no more results when the page reaches the end of the iterator" in {
    val rule = transformRule(LowerCaseTransformer())
    val entities = (1 to 5).map(i => entity(Seq("UPPER" + i), Seq("UPPER" + i)))
    val (_, _, _, peakResult, hasMore, total) =
      PeakTransformApi.collectTransformationExamples(rule, entities.iterator, limit = 3, offset = 3, computeTotal = true)
    hasMore mustBe false
    total mustBe 5
    peakResult mustBe Seq(
      PeakResult(Seq(Seq("UPPER4"), Seq("UPPER4")), Seq("upper4", "upper4")),
      PeakResult(Seq(Seq("UPPER5"), Seq("UPPER5")), Seq("upper5", "upper5"))
    )
  }

  it should "return an empty page when offset is past the end of the iterator" in {
    val rule = transformRule(LowerCaseTransformer())
    val entities = (1 to 3).map(i => entity(Seq("UPPER" + i), Seq("UPPER" + i)))
    val (_, _, _, peakResult, hasMore, total) =
      PeakTransformApi.collectTransformationExamples(rule, entities.iterator, limit = 3, offset = 10, computeTotal = true)
    hasMore mustBe false
    total mustBe 3
    peakResult mustBe Seq()
  }

  it should "count every entity toward offset/total, even those with empty transform output" in {
    val rule = transformRule(ConcatTransformer(" "))
    val entities = Iterator(
      entity(Seq("a1"), Seq("b1")),       // skipped (counts toward offset)
      entity(Seq("a2"), Seq()),           // skipped (counts toward offset)
      entity(Seq("a3"), Seq("b3")),       // first result of page
      entity(Seq(), Seq()),               // second result of page
      entity(Seq("a4"), Seq("b4")),       // third result of page
      entity(Seq("a5"), Seq("b5"))        // tail
    )
    val (_, _, _, peakResult, hasMore, total) =
      PeakTransformApi.collectTransformationExamples(rule, entities, limit = 3, offset = 2, computeTotal = true)
    hasMore mustBe true
    total mustBe 6
    peakResult mustBe Seq(
      PeakResult(Seq(Seq("a3"), Seq("b3")), Seq("a3 b3")),
      PeakResult(Seq(Seq(), Seq()), Seq()),
      PeakResult(Seq(Seq("a4"), Seq("b4")), Seq("a4 b4"))
    )
  }

  it should "report total = skipped + collected + remaining when there is a non-empty tail" in {
    val rule = transformRule(LowerCaseTransformer())
    val entities = (1 to 8).map(i => entity(Seq("UPPER" + i), Seq("UPPER" + i)))
    val (_, _, _, peakResult, hasMore, total) =
      PeakTransformApi.collectTransformationExamples(rule, entities.iterator, limit = 2, offset = 2, computeTotal = true)
    peakResult must have size 2
    // 2 skipped + 2 collected + 4 in tail = 8
    total mustBe 8
    hasMore mustBe true
  }

  it should "filter results by case-insensitive substring against source values" in {
    val rule = transformRule(ConcatTransformer(" "))
    val entities = Iterator(
      entity(Seq("Alice"),   Seq("Smith")),
      entity(Seq("Bob"),     Seq("Jones")),
      entity(Seq("Charlie"), Seq("aLi")),    // "ali" appears in transformed
      entity(Seq("Dave"),    Seq("Roberts"))
    )
    val (_, _, _, peakResult, hasMore, total) =
      PeakTransformApi.collectTransformationExamples(rule, entities, limit = 10, search = Some("ALI"), computeTotal = true)
    hasMore mustBe false
    total mustBe 2
    peakResult mustBe Seq(
      PeakResult(Seq(Seq("Alice"), Seq("Smith")), Seq("Alice Smith")),
      PeakResult(Seq(Seq("Charlie"), Seq("aLi")), Seq("Charlie aLi"))
    )
  }

  it should "treat blank search input as no filter" in {
    val rule = transformRule(LowerCaseTransformer())
    val entities = (1 to 3).map(i => entity(Seq("UPPER" + i), Seq("UPPER" + i)))
    val (_, _, _, peakResult, _, total) =
      PeakTransformApi.collectTransformationExamples(rule, entities.iterator, limit = 10, search = Some(""), computeTotal = true)
    peakResult must have size 3
    total mustBe 3
  }

  it should "paginate filtered results: search interacts correctly with offset and limit" in {
    val rule = transformRule(ConcatTransformer(" "))
    val entities = Iterator(
      entity(Seq("foo1"), Seq("x")),  // matches
      entity(Seq("bar"),  Seq("x")),  // skipped (no match)
      entity(Seq("foo2"), Seq("x")),  // matches
      entity(Seq("baz"),  Seq("x")),  // skipped (no match)
      entity(Seq("foo3"), Seq("x")),  // matches
      entity(Seq("foo4"), Seq("x")),  // matches
      entity(Seq("foo5"), Seq("x"))   // matches
    )
    val (_, _, _, peakResult, hasMore, total) =
      PeakTransformApi.collectTransformationExamples(rule, entities, limit = 2, offset = 1, search = Some("foo"), computeTotal = true)
    // 5 entities match "foo"; offset=1 skips foo1, page returns foo2 + foo3, tail has foo4 + foo5.
    total mustBe 5
    hasMore mustBe true
    peakResult mustBe Seq(
      PeakResult(Seq(Seq("foo2"), Seq("x")), Seq("foo2 x")),
      PeakResult(Seq(Seq("foo3"), Seq("x")), Seq("foo3 x"))
    )
  }

  it should "return results from the API without pagination metadata by default" in {
    val peakResult = peakChildRuleRequest(PatternUriMapping(pattern = "urn:{Name}/{Events/Birth}"))
    peakResult.status.id mustBe "success"
    peakResult.sourcePaths mustBe Some(
      Seq(
        Seq("/Name"),
        Seq("/Events", "/Birth")
      )
    )
    peakResult.results mustBe Some(Seq(
      PeakResult(Seq(Seq("Max Doe"), Seq("May 1900")), Seq("urn:Max+Doe/May+1900")),
      PeakResult(Seq(Seq("Max Noe"), Seq()), Seq())
    ))
    peakResult.total mustBe None
    peakResult.totalIsExact mustBe None
    peakResult.nextOffset mustBe None
  }

  it should "include pagination metadata when includeTotal is requested" in {
    val peakResult = peakChildRuleRequest(PatternUriMapping(pattern = "urn:{Name}/{Events/Birth}"), includeTotal = true)
    peakResult.status.id mustBe "success"
    peakResult.results mustBe Some(Seq(
      PeakResult(Seq(Seq("Max Doe"), Seq("May 1900")), Seq("urn:Max+Doe/May+1900")),
      PeakResult(Seq(Seq("Max Noe"), Seq()), Seq())
    ))
    peakResult.total mustBe Some(2)
    peakResult.totalIsExact mustBe Some(true)
    peakResult.nextOffset mustBe None
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
    peakResult.total mustBe None
    peakResult.totalIsExact mustBe None
  }

  private def peakChildRuleRequest(transformRule: TransformRule, objectPath: Option[String] = None, includeTotal: Boolean = false): PeakResults = {
    val uriPatternUrl = controllers.transform.routes.PeakTransformApi.peakChildRule(projectId, transformXmlTask, rootRuleId).url
    var request = client.url(s"$baseUrl$uriPatternUrl")
    val params = scala.collection.mutable.Buffer.empty[(String, String)]
    if (objectPath.isDefined) {
      params += "objectPath" -> objectPath.get
    }
    if (includeTotal) {
      params += "includeTotal" -> "true"
    }
    if (params.nonEmpty) {
      request = request.withQueryStringParameters(params.toSeq: _*)
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
