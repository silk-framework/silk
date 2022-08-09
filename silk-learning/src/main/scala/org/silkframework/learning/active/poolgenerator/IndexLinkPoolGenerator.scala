package org.silkframework.learning.active.poolgenerator

import org.silkframework.cache.{EntityCache, MemoryEntityCache}
import org.silkframework.config.Prefixes
import org.silkframework.dataset.DataSource
import org.silkframework.entity._
import org.silkframework.learning.active.comparisons.ComparisonPair
import org.silkframework.learning.active.{LinkCandidate, MatchingValues}
import org.silkframework.rule.plugins.distance.characterbased.LevenshteinDistance
import org.silkframework.rule.similarity.SimpleDistanceMeasure
import org.silkframework.rule.{LinkSpec, RuntimeLinkingConfig}
import org.silkframework.runtime.activity.{Activity, ActivityContext, UserContext}
import org.silkframework.util.DPair

import scala.collection.mutable
import scala.util.Random

/**
  * Link Pool Generator that generates link candidates based indices.
  * Uses separate entity caches for each path in each data source and matches all combinations of caches between both data sources.
  */
class IndexLinkPoolGenerator() extends LinkPoolGenerator {

  // Load this many entities from each data source
  private val maxEntities = 10000

  // For each pair of paths, generate at most that many link candidates that have matching values for the given paths.
  private val maxLinksPerPathPair = 1000

  // Distance measure that will be used for comparing values
  // At the moment, we are always using the levenshtein distance, but this could be extended to use other measures, such as the numeric distance.
  private val distanceMeasure: SimpleDistanceMeasure = LevenshteinDistance()

  // Link candidates will be generated for values closer than the max distance (according to the distance measure)
  private val maxDistance: Double = 1.0

  // Maximum number of indices for each path value. Additional indices will be cropped.
  private val maxIndices: Int = 5

  override def generator(inputs: DPair[DataSource], linkSpec: LinkSpec, paths: Seq[ComparisonPair], randomSeed: Long)
                        (implicit prefixes: Prefixes): Activity[UnlabeledLinkPool] = {
    new LinkPoolGeneratorActivity(inputs, linkSpec, paths, randomSeed)
  }

  private class LinkPoolGeneratorActivity(inputs: DPair[DataSource],
                                          linkSpec: LinkSpec,
                                          paths: Seq[ComparisonPair],
                                          randomSeed: Long)
                                         (implicit prefixes: Prefixes) extends Activity[UnlabeledLinkPool] {

    private val runtimeConfig = RuntimeLinkingConfig(partitionSize = 100, useFileCache = false, generateLinksWithEntities = true)

    private val fullEntitySchema = LinkPoolGeneratorUtils.entitySchema(linkSpec, paths)

    override def run(context: ActivityContext[UnlabeledLinkPool])(implicit userContext: UserContext): Unit = {
      val linkCandidates = findLinkCandidates(context)
      context.status.update("Weighting link candidates", 0.9)
      val weightedCandidates = weightLinkCandidates(linkCandidates).toSeq
      // If we found fewer than 30 candidates, we use shuffling to generate some additional ones
      val allCandidates = if(weightedCandidates.size > 30) weightedCandidates else LinkPoolGeneratorUtils.shuffleLinks(weightedCandidates)
      context.value() = UnlabeledLinkPool(fullEntitySchema, allCandidates)
    }

    private def findLinkCandidates(context: ActivityContext[UnlabeledLinkPool])
                                  (implicit userContext: UserContext): Iterable[LinkCandidate] = {
      implicit val random: Random = new Random(randomSeed)

      context.status.update("Loading source dataset", 0.1)
      val sourceCaches = loadCaches(inputs.source, fullEntitySchema.source, sourceOrTarget = true)

      context.status.update("Loading target dataset", 0.2)
      val targetCaches = loadCaches(inputs.target, fullEntitySchema.target, sourceOrTarget = false)

      context.status.update("Finding link candidates", 0.3)
      val links = new mutable.HashMap[DPair[String], LinkCandidate]()
      for {
        (sourceCache, sourceIndex) <- sourceCaches.zipWithIndex
        (targetCache, targetIndex) <- targetCaches.zipWithIndex
      } {
        context.status.updateProgress(0.3 + 0.6 * (sourceIndex * targetCaches.size + targetIndex).toDouble / (sourceCaches.size * targetCaches.size).toDouble, logStatus = false)
        findLinks(sourceCache, sourceIndex, targetCache, targetIndex, links)
      }
      links.values
    }

    /**
      * Given an entity schema, loads a separate cache for each of its paths
      */
    private def loadCaches(input: DataSource, entitySchema: EntitySchema, sourceOrTarget: Boolean)(implicit userContext: UserContext, random: Random): Seq[EntityCache] = {
      val caches =
        for(pathIndex <- entitySchema.typedPaths.indices) yield {
          def indexFunction(e: Entity): Index = distanceMeasure.index(normalize(e.evaluate(pathIndex)), limit = maxDistance, sourceOrTarget = false).crop(maxIndices)
          val cache = new MemoryEntityCache(entitySchema, indexFunction, runtimeConfig)
          cache
        }

      val entities = input.sampleEntities(entitySchema, maxEntities)
      for(entity <- entities) {
        for(cache <- caches) {
          cache.write(entity)
        }
      }
      caches.foreach(_.close())

      caches
    }

    /**
      * Finds links between two sequences of entity caches.
      * Each cache contains entities indexed by a single path.
      */
    private def findLinks(sourceCache: EntityCache, sourcePathIndex: Int, targetCache: EntityCache, targetPathIndex: Int,
                          links: mutable.Map[DPair[String], LinkCandidate]): Int = {
      var count = 0
      for {
        sourceBlock <- 0 until sourceCache.blockCount
        targetBlock <- 0 until targetCache.blockCount
        sourcePartitionIndex <- 0 until sourceCache.partitionCount(sourceBlock)
        targetPartitionIndex <- 0 until targetCache.partitionCount(targetBlock)
      } {
        val sourcePartition = sourceCache.read(sourceBlock, sourcePartitionIndex)
        val targetPartition = targetCache.read(targetBlock, targetPartitionIndex)

        val entityPairs = runtimeConfig.executionMethod.comparisonPairs(sourcePartition, targetPartition, full = true)
        for (entityPair <- entityPairs) {
          val entityUris = DPair(entityPair.source.uri.uri, entityPair.target.uri.uri)
          val sourceValues = normalize(entityPair.source.evaluate(sourcePathIndex))
          val targetValues = normalize(entityPair.target.evaluate(targetPathIndex))
          for {
            sourceValue <- sourceValues
            targetValue <- targetValues
          } {
            if (distanceMeasure.evaluate(sourceValue, targetValue) <= maxDistance) {
              val matchingPair = MatchingValues(sourcePathIndex, targetPathIndex, sourceValue, targetValue)
              // Either update an existing link candidate for the same entities or create a new one
              val linkCandidate =
                links.get(entityUris) match {
                  case Some(link) =>
                    link.withMatch(matchingPair)
                  case None =>
                    LinkCandidate(entityPair.source, entityPair.target, Seq(matchingPair))
                }
              links.put(entityUris, linkCandidate)
              count += 1
              if(count >= maxLinksPerPathPair) {
                return count
              }
            }
          }
        }
      }
      count
    }

    /**
      * Normalizes values before they are indexed and matched.
      */
    @inline
    private def normalize(values: Seq[String]): Seq[String] = {
      values.map(_.toLowerCase)
    }

    private def weightLinkCandidates(linkCandidates: Traversable[LinkCandidate]): Traversable[LinkCandidate] = {
      // Compute value frequencies
      val sourceFrequencies = mutable.Map[String, Int]()
      val targetFrequencies = mutable.Map[String, Int]()
      for {
        linkCandidate <- linkCandidates
        matchingValues <- linkCandidate.matchingValues
      } {
        val sourceValue = matchingValues.normalizedSourceValue
        val targetValue = matchingValues.normalizedTargetValue
        sourceFrequencies.put(sourceValue, sourceFrequencies.getOrElse(sourceValue, 0) + 1)
        targetFrequencies.put(targetValue, targetFrequencies.getOrElse(targetValue, 0) + 1)
      }

      // Update scores
      for(linkCandidate <- linkCandidates) yield {
        val scoredMatchingValues =
          for(matchingValues <- linkCandidate.matchingValues) yield {
            val score =  1.0 / (math.pow(sourceFrequencies(matchingValues.normalizedSourceValue), 2.0) + math.pow(targetFrequencies(matchingValues.normalizedTargetValue), 2.0))
            matchingValues.copy(score = score)
          }
        linkCandidate.copy(matchingValues = scoredMatchingValues, confidence = Some(scoredMatchingValues.map(_.score).sum))
      }
    }
  }
}
