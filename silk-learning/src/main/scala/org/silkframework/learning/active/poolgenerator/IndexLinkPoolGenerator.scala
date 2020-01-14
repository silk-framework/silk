package org.silkframework.learning.active.poolgenerator

import org.silkframework.cache.{EntityCache, MemoryEntityCache}
import org.silkframework.dataset.DataSource
import org.silkframework.entity.paths.TypedPath
import org.silkframework.entity._
import org.silkframework.learning.active.UnlabeledLinkPool
import org.silkframework.rule.{LinkSpec, RuntimeLinkingConfig}
import org.silkframework.runtime.activity.{Activity, ActivityContext, UserContext}
import org.silkframework.util.DPair
import LinkPoolGeneratorUtils._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.Random

/**
  * Link Pool Generator that generates link candidates based indices.
  * Uses separate entity caches for each path in each data source and matches all combinations of caches between both data sources.
  */
class IndexLinkPoolGenerator extends LinkPoolGenerator {

  // Load this many entities from each data source
  private val maxEntities = 10000

  // For each pair of paths, generate at most that many links that have matching values for the given paths.
  private val maxLinksPerPathPair = 1000

  override def generator(inputs: DPair[DataSource], linkSpec: LinkSpec, paths: Seq[DPair[TypedPath]], randomSeed: Long): Activity[UnlabeledLinkPool] = {
    new LinkPoolGeneratorActivity(inputs, linkSpec, paths, randomSeed)
  }

  private class LinkPoolGeneratorActivity(inputs: DPair[DataSource],
                                          linkSpec: LinkSpec,
                                          paths: Seq[DPair[TypedPath]],
                                          randomSeed: Long) extends Activity[UnlabeledLinkPool] {

    private val runtimeConfig = RuntimeLinkingConfig(partitionSize = 100, useFileCache = false, generateLinksWithEntities = true)

    private val fullEntitySchema = entitySchema(linkSpec, paths)

    override def run(context: ActivityContext[UnlabeledLinkPool])(implicit userContext: UserContext): Unit = {
      implicit val random: Random = new Random(randomSeed)

      context.status.update("Loading source dataset", 0.1)
      val sourceCaches = loadCaches(inputs.source, fullEntitySchema.source, "source")

      context.status.update("Loading target dataset", 0.2)
      val targetCaches = loadCaches(inputs.target, fullEntitySchema.target, "target")

      val links = ArrayBuffer[Link]()
      for {
        (sourceCache, sourceIndex) <- sourceCaches.zipWithIndex
        (targetCache, targetIndex) <- targetCaches.zipWithIndex
      } {
        context.status.update("Finding candidates", 0.2 + 0.8 * (sourceIndex * targetCaches.size + targetIndex).toDouble / (sourceCaches.size * targetCaches.size).toDouble)
        findLinks(sourceCache, sourceIndex, targetCache, targetIndex, links)
      }

      context.value() = UnlabeledLinkPool(fullEntitySchema, shuffleLinks(links))
    }

    /**
      * Given an entity schema, loads a separate cache for each of its paths
      */
    def loadCaches(input: DataSource, entitySchema: EntitySchema, name: String)(implicit userContext: UserContext, random: Random): Seq[EntityCache] = {
      val caches =
        for(pathIndex <- entitySchema.typedPaths.indices) yield {
          def indexFunction(e: Entity) = Index.oneDim(normalize(e.evaluate(pathIndex)).map(_.hashCode).toSet)
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
    def findLinks(sourceCache: EntityCache, sourceIndex: Int, targetCache: EntityCache, targetIndex: Int, linkBuffer: mutable.Buffer[Link]): Unit = {
      var count = 0
      for {
        sourceBlock <- 0 until sourceCache.blockCount
        targetBlock <- 0 until targetCache.blockCount
        sourcePartitionIndex <- 0 until sourceCache.partitionCount(sourceBlock)
        targetPartitionIndex <- 0 until targetCache.partitionCount(targetBlock)
        if count < maxLinksPerPathPair
      } {
        val sourcePartition = sourceCache.read(sourceBlock, sourcePartitionIndex)
        val targetPartition = targetCache.read(targetBlock, targetPartitionIndex)

        val entityPairs = runtimeConfig.executionMethod.comparisonPairs(sourcePartition, targetPartition, full = false)
        for (entityPair <- entityPairs if count < maxLinksPerPathPair) {
          val sourceValues = normalize(entityPair.source.evaluate(sourceIndex)).toSet
          val targetValues = normalize(entityPair.target.evaluate(targetIndex)).toSet
          if(sourceValues.exists(targetValues.contains)) {
            linkBuffer.append(new LinkWithEntities(entityPair.source.uri, entityPair.target.uri, entityPair))
            count += 1
          }
        }
      }
    }

    /**
      * Normalizes values before they are indexed and matched.
      */
    @inline
    def normalize(values: Seq[String]): Seq[String] = {
      values.map(_.toLowerCase)
    }
  }
}
