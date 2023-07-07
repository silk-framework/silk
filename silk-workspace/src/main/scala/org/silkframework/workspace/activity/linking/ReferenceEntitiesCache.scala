package org.silkframework.workspace.activity.linking

import org.silkframework.config.Prefixes
import org.silkframework.dataset.DataSource
import org.silkframework.entity.paths.TypedPath
import org.silkframework.entity.{Entity, EntitySchema, Link}
import org.silkframework.rule.LinkSpec
import org.silkframework.rule.evaluation.ReferenceEntities
import org.silkframework.runtime.activity.{ActivityContext, UserContext}
import org.silkframework.runtime.resource.WritableResource
import org.silkframework.util.{DPair, Uri}
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.CachedActivityStreaming
import org.silkframework.workspace.activity.linking.LinkingTaskUtils._

import java.util
import scala.collection.mutable
import scala.jdk.CollectionConverters.SetHasAsScala


/**
 * For each reference link, the reference entities cache holds all values of the linked entities.
 */
class ReferenceEntitiesCache(task: ProjectTask[LinkSpec]) extends CachedActivityStreaming[ReferenceEntities] {

  override def name: String = s"Entities cache ${task.id}"

  override protected val wrappedStreamXmlFormat = WrappedStreamXmlFormat()

  override def initialValue: Option[ReferenceEntities] = Some(ReferenceEntities.empty)

  /** Additional paths that should be loaded into the cache. These will be deleted on a reset. */
  private val additionalSourcePaths = new mutable.LinkedHashSet[TypedPath]()
  private val additionalTargetPaths = new mutable.LinkedHashSet[TypedPath]()

  override def reset()(implicit userContext: UserContext): Unit = {
    additionalSourcePaths.clear()
    additionalTargetPaths.clear()
    val pathsCache =  task.activity[LinkingPathsCache].control
    pathsCache.reset()
    pathsCache.start()
  }

  override def loadCache(context: ActivityContext[ReferenceEntities], fullReload: Boolean)
                        (implicit userContext: UserContext): Unit = {
    cancelled = false
    context.status.update("Waiting for paths cache", 0.0)
    val pathsCache = task.activity[LinkingPathsCache].control
    pathsCache.waitUntilFinished()

    if (pathsCache.status().failed) {
      throw new Exception(s"Cannot load reference entities cache for ${task.id}, because the paths cache could not be loaded.")
    }
    if (!Option(pathsCache.value()).exists(ed => ed.source.typedPaths.nonEmpty || ed.target.typedPaths.nonEmpty)) {
      context.log.info(s"Could not load reference entities cache for ${task.id} as that paths cache does not define paths.")
    } else {
      val pathCacheValue = pathsCache.value()
      val updatedPaths = addAdditionalPaths(pathCacheValue)
      val entityLoader = new ReferenceLinksEntityLoader(task, context, updatedPaths, cancelled)
      entityLoader.load()
    }
  }

  def addSourcePathToCache(sourcePath: TypedPath) = synchronized {
    additionalSourcePaths.add(sourcePath)
  }

  def addTargetPathToCache(targetPath: TypedPath) = synchronized {
    additionalTargetPaths.add(targetPath)
  }

  private def addAdditionalPaths(pathCacheValue: DPair[EntitySchema]): DPair[EntitySchema] = {
    // Remove duplicate paths
    DPair(
      addAdditionalPathsToSchema(pathCacheValue.source, additionalSourcePaths),
      addAdditionalPathsToSchema(pathCacheValue.target, additionalTargetPaths)
    )
  }

  private def addAdditionalPathsToSchema(schema: EntitySchema, additionalPaths: mutable.LinkedHashSet[TypedPath]): EntitySchema = synchronized {
    // Remove duplicate paths from additional paths first
    schema.typedPaths.foreach(p => additionalPaths.remove(p))
    schema.copy(typedPaths = schema.typedPaths ++ additionalPaths)
  }

  override def resource: WritableResource = task.project.cacheResources.child("linking").child(task.id).get(s"referenceEntitiesCache.xml")

}

/**
  * Given a set of reference links, loads the referenced entities.
  */
class ReferenceLinksEntityLoader(task: ProjectTask[LinkSpec],
                                 context: ActivityContext[ReferenceEntities],
                                 entitySchemata: DPair[EntitySchema],
                                 cancelled: => Boolean)
                                (implicit userContext: UserContext) {

  private val sources = task.dataSources

  private val linkSpec = task.data

  //noinspection ScalaStyle
  def load(): Unit = {
    context.status.update("Loading entities", 0.0)

    import linkSpec.referenceLinks.{negative, positive, unlabeled}
    val links = Seq(negative, positive, unlabeled)
    // Get load the different types of links
    val loadLinkEntitiesFNs: Seq[Link => Option[DPair[Entity]]] = Seq(
      context.value().negativeLinkToEntities,
      context.value().positiveLinkToEntities,
      context.value().unlabeledLinkToEntities
    )

    val cache = context.value()
    var sourceEntities = Map[String, Entity]()
    var targetEntities = Map[String, Entity]()

    val sourceEntityUrisNeedingUpdate = new util.HashSet[String]()
    val targetEntityUrisNeedingUpdate = new util.HashSet[String]()
    for ((links, _) <- links.zip(loadLinkEntitiesFNs) if !cancelled) {
      for (link <- links if !cancelled) {
        if (Thread.currentThread.isInterrupted) throw new InterruptedException()
        // Find existing source entity
        val existingSourceEntity = cache.sourceEntities.get(link.source).orElse(link.entities.map(_.source))
        existingSourceEntity match {
          case Some(entity) if entityMatchesDescription(entity, entitySchemata.source) =>
            sourceEntities += ((entity.uri, entity))
          case _ =>
            sourceEntityUrisNeedingUpdate.add(link.source)
        }
        // Find existing target entity
        val existingTargetEntity = cache.targetEntities.get(link.target).orElse(link.entities.map(_.target))
        existingTargetEntity match {
          case Some(entity) if entityMatchesDescription(entity, entitySchemata.target) =>
            targetEntities += ((entity.uri, entity))
          case _ =>
            targetEntityUrisNeedingUpdate.add(link.target)
        }
      }
    }

    sourceEntities ++= getSourceEntities(sourceEntityUrisNeedingUpdate)
    context.status.updateProgress(0.5)
    targetEntities ++= getTargetEntities(targetEntityUrisNeedingUpdate)
    context.status.updateProgress(0.99)

    // Update reference entities
    context.value() =
      ReferenceEntities(
        sourceEntities,
        targetEntities,
        positive,
        negative,
        unlabeled
      )
  }

  private def getSourceEntities(sourceEntityUrisNeedingUpdate: util.HashSet[String]): Map[String, Entity] = {
    getEntitiesByUri(
      sourceEntityUrisNeedingUpdate.asScala.toSeq,
      entitySchemata.source,
      sources.source)
  }

  private def getTargetEntities(targetEntityUrisNeedingUpdate: util.HashSet[String]): Map[String, Entity] = {
    getEntitiesByUri(
      targetEntityUrisNeedingUpdate.asScala.toSeq,
      entitySchemata.target,
      sources.target)
  }

  private def getEntitiesByUri(entityUris: Seq[String],
                               entityDesc: EntitySchema,
                               source: DataSource): Map[String, Entity] = {
    if (entityUris.isEmpty) {
      Map.empty
    } else {
      implicit val prefixes: Prefixes = task.project.config.prefixes
      source.retrieveByUri(
        entitySchema = entityDesc,
        entities = entityUris map Uri.apply
      ).use(_.map { e => (e.uri.toString, e) }.toMap)
    }
  }

  private def entityMatchesDescription(entity: Entity, entityDesc: EntitySchema): Boolean = {
    entity.schema.typedPaths == entityDesc.typedPaths
  }
}
