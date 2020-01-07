package org.silkframework.learning.active.poolgenerator
import org.silkframework.cache.{EntityCache, MemoryEntityCache}
import org.silkframework.dataset.DataSource
import org.silkframework.entity.paths.TypedPath
import org.silkframework.entity._
import org.silkframework.learning.active.UnlabeledLinkPool
import org.silkframework.rule.{LinkSpec, RuntimeLinkingConfig}
import org.silkframework.runtime.activity.{Activity, ActivityContext, UserContext}
import org.silkframework.util.DPair

import scala.collection.mutable.ArrayBuffer

class IndexLinkPoolGenerator extends LinkPoolGenerator {

  private val maxEntities = 10000

  override def generator(inputs: DPair[DataSource], linkSpec: LinkSpec, paths: Seq[DPair[TypedPath]]): Activity[UnlabeledLinkPool] = {
    new LinkPoolGenerator(inputs, linkSpec, paths)
  }

  private class LinkPoolGenerator(inputs: DPair[DataSource],
                                  linkSpec: LinkSpec,
                                  paths: Seq[DPair[TypedPath]]) extends Activity[UnlabeledLinkPool] {

    private val runtimeConfig = RuntimeLinkingConfig(partitionSize = 100, useFileCache = false, generateLinksWithEntities = true)

    override def run(context: ActivityContext[UnlabeledLinkPool])(implicit userContext: UserContext): Unit = {
      val fullEntityDescription = DPair(
        source = linkSpec.entityDescriptions.source.copy(typedPaths = paths.map(_.source).distinct.toIndexedSeq),
        target = linkSpec.entityDescriptions.target.copy(typedPaths = paths.map(_.target).distinct.toIndexedSeq)
      )

      context.status.update("Loading source dataset", 0.1)
      val sourceCaches = loadCaches(inputs.source, fullEntityDescription.source, "source")

      context.status.update("Loading target dataset", 0.2)
      val targetCaches = loadCaches(inputs.target, fullEntityDescription.target, "target")

      val links = ArrayBuffer[Link]()
      for {
        (sourceCache, sourceIndex) <- sourceCaches.zipWithIndex
        (targetCache, targetIndex) <- targetCaches.zipWithIndex} {
        context.status.update("Finding candidates", 0.2 + 0.8 * (sourceIndex * targetCaches.size + targetIndex).toDouble / (sourceCaches.size * targetCaches.size).toDouble)
        for {
          sourceBlock <- 0 until sourceCache.blockCount
          targetBlock <- 0 until targetCache.blockCount
          sourcePartitionIndex <- 0 until sourceCache.partitionCount(sourceBlock)
          targetPartitionIndex <- 0 until targetCache.partitionCount(targetBlock)
        } {
          val sourcePartition = sourceCache.read(sourceBlock, sourcePartitionIndex)
          val targetPartition = targetCache.read(targetBlock, targetPartitionIndex)

          val entityPairs = runtimeConfig.executionMethod.comparisonPairs(sourcePartition, targetPartition, full = false)
          for (entityPair <- entityPairs) {
            links.append(new LinkWithEntities(entityPair.source.uri, entityPair.target.uri, entityPair))
          }
        }
      }

      val shuffledLinks = for ((s, t) <- links zip (links.tail :+ links.head)) yield new LinkWithEntities(s.source, t.target, DPair(s.entities.get.source, t.entities.get.target))
      context.value() = UnlabeledLinkPool(fullEntityDescription, links ++ shuffledLinks)
    }

    def loadCaches(input: DataSource, entitySchema: EntitySchema, name: String)(implicit userContext: UserContext): Seq[EntityCache] = {
      val caches =
        for(path <- entitySchema.typedPaths) yield {
          val pathIndex = entitySchema.indexOfTypedPath(path)
          def indexFunction(e: Entity) = Index.oneDim(e.evaluate(pathIndex).map(_.toLowerCase.hashCode).toSet)
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
  }
}
