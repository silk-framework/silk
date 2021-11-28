package org.silkframework.learning.active

import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.config.{PlainTask, Prefixes}
import org.silkframework.dataset.{DatasetSpec, EmptyDataset, EntityDatasource}
import org.silkframework.entity.paths.{TypedPath, UntypedPath}
import org.silkframework.entity.{Entity, EntitySchema, Link, ValueType}
import org.silkframework.learning.active.linkselector.MaximumConfidenceSelector
import org.silkframework.learning.active.poolgenerator.{IndexLinkPoolGenerator, MatchingPathsFinder}
import org.silkframework.rule.LinkSpec
import org.silkframework.rule.evaluation.ReferenceEntities
import org.silkframework.runtime.activity.{Activity, UserContext}
import org.silkframework.util.DPair

import scala.util.Random

/**
  * Tests if:
  *   - link candidates are generated and ranked
  *   - matching path pairs are found and ranked
  */
class LinkCandidatesRankingIntegrationTest extends FlatSpec with Matchers {

  private val typePath = TypedPath(UntypedPath("type"), ValueType.STRING, isAttribute = false)
  private val namePath = TypedPath(UntypedPath("name"), ValueType.STRING, isAttribute = false)
  private val genderPath = TypedPath(UntypedPath("gender"), ValueType.INTEGER, isAttribute = false)
  private val idPath = TypedPath(UntypedPath("id"), ValueType.STRING, isAttribute = false)

  private val sourceSchema = EntitySchema("", IndexedSeq(typePath, namePath, genderPath))
  private val targetSchema = EntitySchema("", IndexedSeq(typePath, namePath, idPath, genderPath))

  val sourceEntity1 = new Entity("s1", IndexedSeq(Seq("Person"), Seq("Mary"), Seq("Female")), sourceSchema)
  val sourceEntity2 = new Entity("s2", IndexedSeq(Seq("Person"), Seq("John"), Seq("Male")), sourceSchema)
  val sourceEntity3 = new Entity("s3", IndexedSeq(Seq("Person"), Seq("Veronica"), Seq("Female")), sourceSchema)

  val targetEntity1 = new Entity("t1", IndexedSeq(Seq("Person"), Seq("Marry"), Seq("1"), Seq("Female")), targetSchema)
  val targetEntity2 = new Entity("t2", IndexedSeq(Seq("Person"), Seq("John"), Seq("2"), Seq("n.a.")), targetSchema)
  val targetEntity3 = new Entity("t3", IndexedSeq(Seq("Person"), Seq("Paul"), Seq("3"), Seq("Male")), targetSchema)
  val targetEntity4 = new Entity("t4", IndexedSeq(Seq("Person"), Seq("Peter"), Seq("4"), Seq("Male")), targetSchema)
  val targetEntity5 = new Entity("t5", IndexedSeq(Seq("Person"), Seq("Paula"), Seq("5"), Seq("Female")), targetSchema)

  val sourceEntities = Seq(sourceEntity1, sourceEntity2, sourceEntity3)
  val targetEntities = Seq(targetEntity1, targetEntity2, targetEntity3, targetEntity4, targetEntity5)

  val linkCandidates = Seq(Link(sourceEntity1, targetEntity1), Link(sourceEntity2, targetEntity2), Link(sourceEntity3, targetEntity3))

  // Expected sorting of link candidates.
  val top2LinkCandidates = Seq(
    // Name and gender match (within levenshtein distance 1)
    Link(sourceEntity1, targetEntity1),
    // Only the name matches (name matches should be scored higher because names are more diverse than the values in other fields)
    Link(sourceEntity2, targetEntity2),
  )

  // Paths with matching values sorted by TF/IDF
  val expectedPathPairs = Seq(
    DPair(namePath, namePath),
    DPair(genderPath, genderPath),
    DPair(typePath, typePath)
  )

  private val dummyTask = PlainTask("dataset", DatasetSpec(EmptyDataset))

  private implicit val prefixes: Prefixes = Prefixes.empty
  private implicit def userContext: UserContext = UserContext.Empty

  "MaximumSimilaritySelector" should "select link candidates which are the most similar according to TF/IDF" in {
    implicit val random: Random = new Random(0)

    // Generate link candidates from entities
    val sources = DPair(EntityDatasource(dummyTask, sourceEntities, sourceSchema), EntityDatasource(dummyTask, targetEntities, targetSchema))
    // We put in the complete cartesian product of paths, because we want the algorithm to figure out which ones match
    val pathPairs = for(sourcePath <- sourceSchema.typedPaths; targetPath <- targetSchema.typedPaths) yield DPair(sourcePath, targetPath)
    val generatorActivity = new IndexLinkPoolGenerator().generator(sources, LinkSpec(), pathPairs, randomSeed = random.nextLong())
    val linkCandidates = Activity(generatorActivity).startBlockingAndGetValue().links

    // Find top link candidates
    val sortedCandidates = MaximumConfidenceSelector()(Seq.empty, linkCandidates, ReferenceEntities.empty)
    // Make sure that it identified the top 2 candidates correctly
    sortedCandidates.take(2) shouldBe top2LinkCandidates
    // Make sure that the following candidate scored worse
    sortedCandidates(1).confidence shouldBe > (sortedCandidates(2).confidence)

    // Find matching paths
    val matchingPaths = MatchingPathsFinder(linkCandidates)
    matchingPaths shouldBe expectedPathPairs
  }
}
