package org.silkframework.workspace.activity.linking

import java.util

import org.silkframework.dataset.DataSource
import org.silkframework.entity.{Entity, EntitySchema, Link}
import org.silkframework.runtime.activity.{Activity, ActivityContext, UserContext}
import org.silkframework.util.{DPair, Uri}
import org.silkframework.workspace.ProjectTask
import LinkingTaskUtils._
import org.silkframework.rule.LinkSpec
import org.silkframework.rule.evaluation.ReferenceEntities
import org.silkframework.runtime.resource.WritableResource
import org.silkframework.workspace.activity.CachedActivity

import scala.collection.JavaConverters._


/**
 * For each reference link, the reference entities cache holds all values of the linked entities.
 */
class ReferenceEntitiesCache(task: ProjectTask[LinkSpec]) extends CachedActivity[ReferenceEntities] {

  @volatile
  private var canceled = false

  override def name = s"Entities cache ${task.id}"

  override def initialValue: Option[ReferenceEntities] = Some(ReferenceEntities.empty)

  override def cancelExecution(): Unit = {
    canceled = true
  }

  override def reset(): Unit = {
    val pathsCache =  task.activity[LinkingPathsCache].control
    pathsCache.reset()
    pathsCache.start()
  }

  override def run(context: ActivityContext[ReferenceEntities])
                  (implicit userContext: UserContext): Unit = {
    canceled = false
    context.status.update("Waiting for paths cache", 0.0)
    val pathsCache = task.activity[LinkingPathsCache].control
    pathsCache.waitUntilFinished()

    if (pathsCache.status().failed)
      throw new Exception(s"Cannot load reference entities cache for ${task.id}, because the paths cache could not be loaded.")
    if (!Option(pathsCache.value()).exists(ed => ed.source.typedPaths.nonEmpty || ed.target.typedPaths.nonEmpty))
      context.log.info(s"Could not load reference entities cache for ${task.id} as that paths cache does not define paths.")
    else {
      val entityLoader = new EntityLoader(context, pathsCache.value())
      entityLoader.load()
    }
  }

  private class EntityLoader(context: ActivityContext[ReferenceEntities], entityDescs: DPair[EntitySchema])
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
      for ((links, _) <- links.zip(loadLinkEntitiesFNs) if !canceled) {
        for (link <- links if !canceled) {
          if (Thread.currentThread.isInterrupted) throw new InterruptedException()
          // Find existing source entity
          val existingSourceEntity = cache.sourceEntities.get(link.source).orElse(link.entities.map(_.source))
          existingSourceEntity match {
            case Some(entity) if entityMatchesDescription(entity, entityDescs.source) =>
              sourceEntities += ((entity.uri, entity))
            case _ =>
              sourceEntityUrisNeedingUpdate.add(link.source)
          }
          // Find existing target entity
          val existingTargetEntity = cache.targetEntities.get(link.target).orElse(link.entities.map(_.target))
          existingTargetEntity match {
            case Some(entity) if entityMatchesDescription(entity, entityDescs.target) =>
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
        entityDescs.source,
        sources.source)
    }

    private def getTargetEntities(targetEntityUrisNeedingUpdate: util.HashSet[String]): Map[String, Entity] = {
      getEntitiesByUri(
        targetEntityUrisNeedingUpdate.asScala.toSeq,
        entityDescs.target,
        sources.target)
    }

    private def retrieveEntityPair(uris: DPair[String]): Option[DPair[Entity]] = {
      for (source <- sources.source.retrieveByUri(entityDescs.source, uris.source :: Nil).headOption;
           target <- sources.target.retrieveByUri(entityDescs.target, uris.target :: Nil).headOption) yield {
        DPair(source, target)
      }
    }

    private def updateEntityPair(entities: DPair[Entity]): Option[DPair[Entity]] = {
      val source = updateEntity(entities.source, entityDescs.source, sources.source)
      val target = updateEntity(entities.target, entityDescs.target, sources.target)
      // If either source or target has been updated, we need to update the whole pair
      if (source.isDefined || target.isDefined) {
        Some(DPair(
          source = source.getOrElse(entities.source),
          target = target.getOrElse(entities.target)
        ))
      } else {
        None
      }
    }

    /**
     * Updates an entity so that it conforms to a new entity description.
     * All property paths values which are not available in the given entity are loaded from the source.
     */
    private def updateEntity(entity: Entity, entityDesc: EntitySchema, source: DataSource): Option[Entity] = {
      if (entityMatchesDescription(entity, entityDesc)) {
        // No updated needed as the given entity already contains all paths in the correct order.
        None
      } else {
        //Compute the paths which are missing on the given entity
        val existingPaths = entity.schema.typedPaths.toSet
        val missingPaths = entityDesc.typedPaths.filterNot(existingPaths.contains)

        //Retrieve an entity with all missing paths
        val missingEntity =
          source.retrieveByUri(
            entitySchema = entity.schema.copy(typedPaths = missingPaths),
            entities = entity.uri :: Nil
          ).head

        //Collect values from the existing and the new entity
        val completeValues =
          for (path <- entityDesc.typedPaths) yield {
            entity.schema.typedPaths.find(p => p == path) match{
              case Some(fp) => entity.evaluate(fp)
              case None => missingEntity.evaluate(path)
            }
          }

        //Return the updated entity
        Some(Entity(
          uri = entity.uri,
          values = completeValues,
          schema = entityDesc
        ))
      }
    }

    private def getEntitiesByUri(entityUris: Seq[String],
                            entityDesc: EntitySchema,
                            source: DataSource): Map[String, Entity] = {
      val entities = source.retrieveByUri(
        entitySchema = entityDesc,
        entities = entityUris map Uri.apply
      )
      entities.map{ e => (e.uri.toString, e)}.toMap
    }

    private def entityMatchesDescription(entity: Entity, entityDesc: EntitySchema): Boolean = {
      entity.schema.typedPaths == entityDesc.typedPaths
    }
  }

  override def resource: WritableResource = task.project.cacheResources.child("linking").child(task.id).get(s"referenceEntitiesCache.xml")

  override protected val wrappedXmlFormat = WrappedXmlFormat()
}
