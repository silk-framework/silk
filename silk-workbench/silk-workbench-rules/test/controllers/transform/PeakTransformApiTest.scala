package controllers.transform

import helper.IntegrationTestTrait
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.rule.input.{PathInput, TransformInput, Transformer}
import org.silkframework.rule.plugins.transformer.combine.ConcatTransformer
import org.silkframework.rule.plugins.transformer.date.DateToTimestampTransformer
import org.silkframework.rule.plugins.transformer.normalize.LowerCaseTransformer
import org.silkframework.rule.plugins.transformer.tokenization.CamelCaseTokenizer
import org.silkframework.rule.{ComplexMapping, MappingRules, MappingTarget, ObjectMapping, PatternUriMapping, TaskContext, TransformRule, TransformRuleExecution, TransformSpec}
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
      PeakTransformApi.collectTransformationExamples(rule, entities.iterator, limit = 10, computeTotal = true, perRecord = true)
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
      PeakTransformApi.collectTransformationExamples(rule, entities, limit = 10, computeTotal = true, perRecord = true)
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
      PeakTransformApi.collectTransformationExamples(rule, entities, limit = 3, computeTotal = true, perRecord = true)
    tries mustBe 2
    errors mustBe 2
    errorMsg must include ("Invalid date format")
    hasMore mustBe false
    // Errored entities produce empty output but are still surfaced in the preview (and counted).
    total mustBe 2
    peakResult.map(_.sourceValues) mustBe Seq(
      Seq(Seq("2015"), Seq("no date")),
      Seq(Seq("123"), Seq("also no date"))
    )
    // Each errored row keeps its (empty) output and is individually flagged with its transform error.
    peakResult.foreach { r =>
      r.transformedValues mustBe empty
      r.error mustBe defined
      r.error.get must include ("Invalid date format")
    }
  }

  it should "keep and flag a row whose values violate a single-cardinality target" in {
    val rule = singleCardinalityRule
    val entities = Iterator(
      entity(Seq("v1", "v2", "v3"), Seq()),  // multi-valued -> violates the isAttribute target
      entity(Seq("only"), Seq()),            // single value -> valid
      entity(Seq(), Seq())                   // no value -> valid
    )
    val (tries, errors, errorMsg, peakResult, _, total) =
      PeakTransformApi.collectTransformationExamples(rule, entities, limit = 10, computeTotal = true, perRecord = true)
    tries mustBe 3
    errors mustBe 1
    errorMsg must include ("MultipleValuesException")
    total mustBe 3
    // The violating record stays in the result set, grouped, with its computed values kept and flagged.
    val flagged = peakResult.head
    flagged.sourceValues mustBe Seq(Seq("v1", "v2", "v3"), Seq())
    flagged.transformedValues mustBe Seq("v1", "v2", "v3")
    flagged.error mustBe defined
    flagged.error.get must include ("MultipleValuesException")
    // The valid records are present and carry no error.
    peakResult(1) mustBe PeakResult(Seq(Seq("only"), Seq()), Seq("only"))
    peakResult(2) mustBe PeakResult(Seq(Seq(), Seq()), Seq())
  }

  private def singleCardinalityRule: TransformRuleExecution = {
    ComplexMapping(
      operator = PathInput("p", UntypedPath("a")),
      target = Some(MappingTarget(Uri("https://schema.org/text"), isAttribute = true))
    ).execution(TaskContext.empty)
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
      PeakTransformApi.collectTransformationExamples(rule, entities, limit = 3, offset = 2, computeTotal = true, perRecord = true)
    hasMore mustBe true
    total mustBe 6
    peakResult mustBe Seq(
      PeakResult(Seq(Seq("a3"), Seq("b3")), Seq("a3 b3")),
      PeakResult(Seq(Seq(), Seq()), Seq()),
      PeakResult(Seq(Seq("a4"), Seq("b4")), Seq("a4 b4"))
    )
  }

  it should "skip entities with an empty transform result by default (legacy mapping editor behaviour)" in {
    val rule = transformRule(ConcatTransformer(" "))
    val entities = Iterator(
      entity(Seq("a1"), Seq("b1")),   // "a1 b1"  -> kept
      entity(Seq("a2"), Seq()),       // empty    -> skipped (not a result, not counted)
      entity(Seq("a3"), Seq("b3")),   // "a3 b3"  -> kept
      entity(Seq(), Seq()),           // empty    -> skipped
      entity(Seq("a4"), Seq("b4"))    // "a4 b4"  -> kept
    )
    val (tries, errors, _, peakResult, hasMore, total) =
      PeakTransformApi.collectTransformationExamples(rule, entities, limit = 10, computeTotal = true)
    tries mustBe 5
    errors mustBe 0
    hasMore mustBe false
    // Only the three non-empty results are examples; the two empty records do not count toward the total.
    total mustBe 3
    peakResult mustBe Seq(
      PeakResult(Seq(Seq("a1"), Seq("b1")), Seq("a1 b1")),
      PeakResult(Seq(Seq("a3"), Seq("b3")), Seq("a3 b3")),
      PeakResult(Seq(Seq("a4"), Seq("b4")), Seq("a4 b4"))
    )
  }

  it should "honor offset over non-empty results only in legacy (skip-empty) mode" in {
    val rule = transformRule(ConcatTransformer(" "))
    val entities = Iterator(
      entity(Seq("a1"), Seq("b1")),   // non-empty (skipped by offset)
      entity(Seq(), Seq()),           // empty (ignored entirely)
      entity(Seq("a2"), Seq("b2")),   // non-empty (skipped by offset)
      entity(Seq(), Seq()),           // empty (ignored entirely)
      entity(Seq("a3"), Seq("b3")),   // non-empty -> first page result
      entity(Seq("a4"), Seq("b4"))    // non-empty -> tail
    )
    val (_, _, _, peakResult, hasMore, total) =
      PeakTransformApi.collectTransformationExamples(rule, entities, limit = 1, offset = 2, computeTotal = true)
    // 4 non-empty results in total; the 2 empties never count toward offset/total.
    total mustBe 4
    hasMore mustBe true
    peakResult mustBe Seq(PeakResult(Seq(Seq("a3"), Seq("b3")), Seq("a3 b3")))
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

  it should "skip records with an empty transform result by default at the API (legacy mapping editor)" in {
    // 'Max Noe' has no Events/Birth, so this pattern yields an empty result for that record. In the default
    // (legacy) mode that record is not a preview row at all - only the real 'Max Doe' example is returned.
    val peakResult = peakChildRuleRequest(PatternUriMapping(pattern = "urn:{Name}/{Events/Birth}"), perRecord = false)
    peakResult.status.id mustBe "success"
    peakResult.results mustBe Some(Seq(
      PeakResult(Seq(Seq("Max Doe"), Seq("May 1900")), Seq("urn:Max+Doe/May+1900"))
    ))
  }

  it should "keep the empty record as its own grouped row when perRecord is set (new mapping editor)" in {
    // Same rule, but with per-record grouping the value-less 'Max Noe' record still appears as an empty row.
    val peakResult = peakChildRuleRequest(PatternUriMapping(pattern = "urn:{Name}/{Events/Birth}"), perRecord = true)
    peakResult.results mustBe Some(Seq(
      PeakResult(Seq(Seq("Max Doe"), Seq("May 1900")), Seq("urn:Max+Doe/May+1900")),
      PeakResult(Seq(Seq("Max Noe"), Seq()), Seq())
    ))
  }

  it should "reject a maxTryEntities below 1 with a 400" in {
    val peakUrl = controllers.transform.routes.PeakTransformApi.peak(projectId, transformXmlTask, rootRuleId, maxTryEntities = 0).url
    checkResponseCode(client.url(s"$baseUrl$peakUrl").post(""), responseCode = 400)
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

  it should "keep and flag preview rows whose values violate a single-cardinality target" in {
    // Person 1 holds three values for Properties/Property/Value; mapping them onto a single-value target
    // must not drop the record - it stays, grouped, flagged with the validation error, and the overall
    // status reports "with exceptions".
    val peakResult = peakChildRuleRequest(
      ComplexMapping(
        id = "text",
        operator = PathInput("p", UntypedPath.parse("Properties/Property/Value")),
        target = Some(MappingTarget(Uri("https://schema.org/text"), isAttribute = true))),
      includeTotal = true)
    peakResult.status.id mustBe "with exceptions"
    val results = peakResult.results.getOrElse(Seq.empty)
    val violating = results.find(_.sourceValues == Seq(Seq("V1", "V2", "V3")))
    violating mustBe defined
    violating.get.error mustBe defined
    violating.get.error.get must include ("MultipleValuesException")
    // The record without a multi-value is still present and carries no error.
    val nonViolating = results.find(_.sourceValues == Seq(Seq()))
    nonViolating mustBe defined
    nonViolating.get.error mustBe None
  }

  it should "report no per-row error for a clean single-cardinality mapping" in {
    // Every record has at most one Name, so the single-value target is never violated.
    val peakResult = peakChildRuleRequest(
      ComplexMapping(
        id = "name",
        operator = PathInput("p", UntypedPath.parse("Name")),
        target = Some(MappingTarget(Uri("https://schema.org/name"), isAttribute = true))))
    peakResult.status.id mustBe "success"
    val results = peakResult.results.getOrElse(Seq.empty)
    results must not be empty
    results.map(_.error).foreach(_ mustBe None)
  }

  it should "return the generated URIs when peaking an object mapping rule" in {
    val peakResult = peakRequest(transformXmlTask, "object")
    peakResult.status.id mustBe "success"
    val results = peakResult.results.get
    results must have size 3
    for(result <- results) {
      result.transformedValues must have size 1
      result.transformedValues.head must endWith ("/object")
    }
  }

  it should "return the generated URIs when peaking an object mapping rule with an explicit URI rule" in {
    val objectRule = ObjectMapping(
      id = "objectWithUriRule",
      sourcePath = UntypedPath("Events"),
      rules = MappingRules(uriRule = Some(PatternUriMapping(id = "eventUri", pattern = "urn:event:{Birth}")))
    )
    val transformSpec = project.task[TransformSpec](transformXmlTask).data
    val updatedRootRule = transformSpec.mappingRule.copy(rules =
      transformSpec.mappingRule.rules.copy(propertyRules = transformSpec.mappingRule.rules.propertyRules :+ objectRule))
    project.updateTask("TransformWithObjectUriRule", transformSpec.copy(mappingRule = updatedRootRule))

    val peakResult = peakRequest("TransformWithObjectUriRule", "objectWithUriRule")
    peakResult.status.id mustBe "success"
    peakResult.sourcePaths mustBe Some(Seq(Seq("/Birth")))
    peakResult.results mustBe Some(Seq(
      PeakResult(Seq(Seq("May 1900")), Seq("urn:event:May+1900"))
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
    peakResult.total mustBe None
    peakResult.totalIsExact mustBe None
  }

  it should "preview an object mapping via its URI rule" in {
    // The 'object' rule is an ObjectMapping (a container rule). Peaking it must not fail with the
    // "non-value rule" error; instead it previews the IRI minted by the object's URI rule.
    val peakResult = peakNamedRuleRequest("object")
    peakResult.status.id mustBe "success"
    // The object has no authored URI rule, so its synthesized default pattern ('{}/object') has no
    // source paths - it mints an IRI purely from the object entity's own URI.
    peakResult.sourcePaths mustBe Some(Seq())
    val results = peakResult.results.getOrElse(Seq.empty)
    results must not be empty
    // Every previewed value is the IRI minted by the object's URI rule.
    val transformedValues = results.flatMap(_.transformedValues)
    transformedValues must not be empty
    all (transformedValues) must (fullyMatch regex """urn:instance:Property#.+/object""")
  }

  it should "preview a bare source path with its values grouped per source entity" in {
    // A single record holds multiple values for the path (Person 1: V1/V2/V3); they must stay grouped
    // in one result, while a record without the path (Person 2) yields an empty group - not separate rows.
    val peakResult = peakSourcePathRequest("Properties/Property/Value")
    peakResult.status.id mustBe "success"
    peakResult.sourcePaths mustBe Some(Seq(Seq("/Properties", "/Property", "/Value")))
    peakResult.results mustBe Some(Seq(
      PeakResult(Seq(Seq("V1", "V2", "V3")), Seq("V1", "V2", "V3")),
      PeakResult(Seq(Seq()), Seq())
    ))
  }

  it should "preview a source path within an object path context" in {
    val peakResult = peakSourcePathRequest("Birth", objectPath = Some("Events"))
    peakResult.status.id mustBe "success"
    peakResult.sourcePaths mustBe Some(Seq(Seq("/Birth")))
    peakResult.results mustBe Some(Seq(
      PeakResult(Seq(Seq("May 1900")), Seq("May 1900"))
    ))
  }

  it should "report pagination metadata for a source path preview when includeTotal is requested" in {
    val peakResult = peakSourcePathRequest("Name", includeTotal = true)
    peakResult.results mustBe Some(Seq(
      PeakResult(Seq(Seq("Max Doe")), Seq("Max Doe")),
      PeakResult(Seq(Seq("Max Noe")), Seq("Max Noe"))
    ))
    peakResult.total mustBe Some(2)
    peakResult.totalIsExact mustBe Some(true)
    peakResult.nextOffset mustBe None
  }

  it should "scope grouping at an array-boundary objectPath, matching a bound child rule's rows" in {
    // Properties/Property is an array (Person 1 has 3 entries, Person 2 has none). Scoping the preview at
    // that boundary must yield one row per Property element - the same row count a bound value rule nested
    // under an object mapping over Properties/Property produces - not one lumped root row.
    val sourcePreview = peakSourcePathRequest("Key", objectPath = Some("Properties/Property"))
    sourcePreview.results mustBe Some(Seq(
      PeakResult(Seq(Seq("1")), Seq("1")),
      PeakResult(Seq(Seq("2")), Seq("2")),
      PeakResult(Seq(Seq("3")), Seq("3"))
    ))
    // The bound peek over the same boundary (a child value rule on Key) groups the source side identically:
    // the unmapped preview agrees with what the eventual mapped rule would show.
    val boundPeek = peakChildRuleRequest(
      ComplexMapping(id = "key", operator = PathInput("p", UntypedPath("Key"))),
      objectPath = Some("Properties/Property"))
    boundPeek.results.map(_.map(_.sourceValues)) mustBe sourcePreview.results.map(_.map(_.sourceValues))
  }

  // The API helpers default to `perRecord = true` (the new mapping editor's per-record grouping) because the
  // existing assertions expect value-less records to still appear as (empty) rows. Legacy skip-empty behaviour
  // is exercised explicitly with `perRecord = false`.
  private def peakSourcePathRequest(path: String, objectPath: Option[String] = None, includeTotal: Boolean = false, perRecord: Boolean = true): PeakResults = {
    val peakUrl = controllers.transform.routes.PeakTransformApi.peakSourcePath(
      projectId, transformXmlTask, rootRuleId, path, objectPath, includeTotal = includeTotal, perRecord = perRecord).url
    val request = client.url(s"$baseUrl$peakUrl")
    val jsonResponse = checkResponse(request.post("")).json
    JsonHelpers.fromJsonValidated[PeakResults](jsonResponse)
  }

  private def peakNamedRuleRequest(ruleId: String, perRecord: Boolean = true): PeakResults = {
    val peakUrl = controllers.transform.routes.PeakTransformApi.peak(projectId, transformXmlTask, ruleId, perRecord = perRecord).url
    val request = client.url(s"$baseUrl$peakUrl")
    val jsonResponse = checkResponse(request.post("")).json
    JsonHelpers.fromJsonValidated[PeakResults](jsonResponse)
  }

  private def peakRequest(taskId: String, ruleId: String, perRecord: Boolean = true): PeakResults = {
    val peakUrl = controllers.transform.routes.PeakTransformApi.peak(projectId, taskId, ruleId, perRecord = perRecord).url
    val jsonResponse = checkResponse(client.url(s"$baseUrl$peakUrl").post("")).json
    JsonHelpers.fromJsonValidated[PeakResults](jsonResponse)
  }

  private def peakChildRuleRequest(transformRule: TransformRule, objectPath: Option[String] = None, includeTotal: Boolean = false, perRecord: Boolean = true): PeakResults = {
    val uriPatternUrl = controllers.transform.routes.PeakTransformApi.peakChildRule(projectId, transformXmlTask, rootRuleId).url
    var request = client.url(s"$baseUrl$uriPatternUrl")
    val params = scala.collection.mutable.Buffer.empty[(String, String)]
    if (objectPath.isDefined) {
      params += "objectPath" -> objectPath.get
    }
    if (includeTotal) {
      params += "includeTotal" -> "true"
    }
    if (perRecord) {
      params += "perRecord" -> "true"
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
