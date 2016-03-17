package org.silkframework.workspace.activity.linking

import java.util

import org.silkframework.config.LinkSpecification
import org.silkframework.dataset.DataSource
import org.silkframework.entity.{Entity, EntitySchema, Link}
import org.silkframework.evaluation.ReferenceEntities
import org.silkframework.runtime.activity.{Activity, ActivityContext}
import org.silkframework.util.{DPair, Uri}
import org.silkframework.workspace.Task
import LinkingTaskUtils._
import scala.collection.JavaConverters._


/**
 * For each reference link, the reference entities cache holds all values of the linked entities.
 */
class ReferenceEntitiesCache(task: Task[LinkSpecification]) extends Activity[ReferenceEntities] {

  @volatile
  private var canceled = false

  override def name = s"Entities cache ${task.name}"

  override def initialValue = Some(ReferenceEntities.empty)

  override def cancelExecution(): Unit = {
    canceled = true
  }

  override def run(context: ActivityContext[ReferenceEntities]) = {
    canceled = false
    context.status.update("Waiting for paths cache", 0.0)
    val pathsCache = task.activity[LinkingPathsCache].control
    while (pathsCache.status().isRunning && !canceled)
      Thread.sleep(1000)
    if (pathsCache.status().failed)
      throw new Exception(s"Cannot load reference entities cache for ${task.name}, because the paths cache could not be loaded.")
    if (!Option(pathsCache.value()).exists(ed => ed.source.paths.nonEmpty || ed.target.paths.nonEmpty))
      context.log.info(s"Could not load reference entities cache for ${task.name} as that paths cache does not define paths.")
    else {
      val entityLoader = new EntityLoader(context, pathsCache.value())
      entityLoader.load()
    }
  }

  private class EntityLoader(context: ActivityContext[ReferenceEntities], entityDescs: DPair[EntitySchema]) {

    private val sources = task.dataSources

    private val linkSpec = task.data

    def load() = {
      context.status.update("Loading entities", 0.0)

      import linkSpec.referenceLinks.{negative, positive, unlabeled}
      val links = Seq(negative, positive, unlabeled)
      // Get load the different types of links
      val loadLinkEntitiesFNs: Seq[Link => Option[DPair[Entity]]] = Seq(
        context.value().negativeLinkToEntities,
        context.value().positiveLinkToEntities,
        context.value().unlabeledLinkToEntities
      )

      var sourceEntities = Map[String, Entity]()
      var targetEntities = Map[String, Entity]()

      val sourceEntityUrisNeedingUpdate = new util.HashSet[String]()
      val targetEntityUrisNeedingUpdate = new util.HashSet[String]()
      for ((links, loadLinkFn) <- links.zip(loadLinkEntitiesFNs) if !canceled) {
        for (link <- links if !canceled) {
          if (Thread.currentThread.isInterrupted) throw new InterruptedException()
          link.entities match {
            case Some(entities) =>
              // There are already entities attached to the link that we might reuse
              if (entityMatchesDescription(entities.source, entityDescs.source)) {
                sourceEntities += ((entities.source.uri, entities.source))
              } else {
                sourceEntityUrisNeedingUpdate.add(link.source)
              }
              if (entityMatchesDescription(entities.target, entityDescs.target)) {
                targetEntities += ((entities.target.uri, entities.target))
              } else {
                targetEntityUrisNeedingUpdate.add(link.target)
              }
            case None =>
              sourceEntityUrisNeedingUpdate.add(link.source)
              targetEntityUrisNeedingUpdate.add(link.target)
          }
        }
      }

      sourceEntities ++= getSourceEntities(sourceEntityUrisNeedingUpdate)
      context.status.updateProgress(0.5)
      targetEntities ++= getTargetEntities(targetEntityUrisNeedingUpdate)
      context.status.updateProgress(0.99)

      // Add new entities to reference entities
      context.value() = context.value().update(
        sourceEntities.values,
        targetEntities.values,
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
        val existingPaths = entity.desc.paths.toSet
        val missingPaths = entityDesc.paths.filterNot(existingPaths.contains)

        //Retrieve an entity with all missing paths
        val missingEntity =
          source.retrieveByUri(
            entitySchema = entity.desc.copy(paths = missingPaths),
            entities = entity.uri :: Nil
          ).head

        //Collect values from the existing and the new entity
        val completeValues =
          for (path <- entityDesc.paths) yield {
            val pathIndex = entity.desc.paths.indexOf(path)
            if (pathIndex != -1)
              entity.evaluate(pathIndex)
            else
              missingEntity.evaluate(path)
          }

        //Return the updated entity
        Some(new Entity(
          uri = entity.uri,
          values = completeValues,
          desc = entityDesc
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
      entities map { e => (e.uri, e)} toMap
    }

    private def entityMatchesDescription(entity: Entity, entityDesc: EntitySchema): Boolean = {
      entity.desc.paths == entityDesc.paths
    }
  }
}
