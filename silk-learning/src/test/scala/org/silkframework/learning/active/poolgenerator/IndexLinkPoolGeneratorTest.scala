package org.silkframework.learning.active.poolgenerator

import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.config.{PlainTask, Prefixes}
import org.silkframework.dataset.{DataSource, DatasetSpec, EmptyDataset, EntityDatasource}
import org.silkframework.entity.paths.{TypedPath, UntypedPath}
import org.silkframework.entity.{Entity, EntitySchema, Link, ValueType}
import org.silkframework.learning.active.comparisons.ComparisonPair
import org.silkframework.rule.LinkSpec
import org.silkframework.rule.plugins.distance.characterbased.LevenshteinDistance
import org.silkframework.runtime.activity.{Activity, UserContext}
import org.silkframework.util.DPair

class IndexLinkPoolGeneratorTest extends FlatSpec with Matchers {

  behavior of "IndexLinkPoolGenerator"

  private val schema =
    EntitySchema(
      typeUri = "",
      typedPaths =
        IndexedSeq(
          TypedPath(UntypedPath("value"), ValueType.STRING, isAttribute = false)
        )
    )

  private val dummyTask = PlainTask("dataset", DatasetSpec(EmptyDataset))

  private implicit val prefixes: Prefixes = Prefixes.empty
  private implicit def userContext: UserContext = UserContext.Empty

  it should "generate link candidates for all entities with similar values" in {
    val sourceValues = Seq(
      "Liam", "Noah", "Oliver", "Elijah", "William", "James", "Olivia", "Emma", "Ava", "Charlotte", "Sophia", "Amelia", "Khloe", "Isabella", "Mia", "Evelyn",
      "alexander", "Harper", "Benjamin", "Lucas", "Henry", "CArTeR", "Alexander")

    val targetValues = Seq(
      "Scarlett", "Sebastian", "Sofia" , "Carter", "Nova" , "Daniel", "Ema", "Aurora" , "W_illiam", "Charlote", "Chloe" , "Alexander", "Riley" , "Ezra", "Nora", "Owen",
      "Hazel", "Michael", "Abigail")

    // Generate link candidates
    val links = generateLinkCandidates(sourceValues, targetValues)

    // It should generate candidates for all values that have an levenshtein distance of at most 1
    val distance = LevenshteinDistance()
    val expectedLinks =
      for {
        sourceValue <- sourceValues
        targetValue <- targetValues
        if distance.evaluate(sourceValue.toLowerCase, targetValue.toLowerCase) <= 1.0
      } yield Link(sourceValue, targetValue)

    // Make sure that all expected links have been generated
    links should contain allElementsOf expectedLinks
    // It shouldn't generate too many excess links
    links.size should be <= expectedLinks.size * 2
  }

  private def generateLinkCandidates(sourceValues: Seq[String], targetValues: Seq[String]): Seq[Link] = {
    val sources = DPair(createSource(sourceValues), createSource(targetValues))
    val generator = new IndexLinkPoolGenerator()
    val generatorActivity = generator.generator(sources, LinkSpec(), schema.typedPaths.map(p => ComparisonPair(p, p)), randomSeed = 0)
    val pool = Activity(generatorActivity).startBlockingAndGetValue()
    pool.linkCandidates
  }

  private def createSource(values: Seq[String]): DataSource = {
    val entities = values.map(v => new Entity(v, IndexedSeq(Seq(v)), schema))
    EntityDatasource(dummyTask, entities, schema)
  }

}
