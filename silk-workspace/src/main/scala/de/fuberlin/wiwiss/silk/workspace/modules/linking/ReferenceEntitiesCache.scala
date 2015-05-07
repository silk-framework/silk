package de.fuberlin.wiwiss.silk.workspace.modules.linking

import de.fuberlin.wiwiss.silk.config.LinkSpecification
import de.fuberlin.wiwiss.silk.dataset.{Dataset, DataSource}
import de.fuberlin.wiwiss.silk.entity.{Entity, EntityDescription, Link}
import de.fuberlin.wiwiss.silk.evaluation.ReferenceEntities
import de.fuberlin.wiwiss.silk.runtime.activity.{ActivityContext, Activity}
import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.workspace.Project
import de.fuberlin.wiwiss.silk.workspace.modules.{Task, Cache}

import scala.xml.Node

class ReferenceEntitiesCache(task: Task[LinkSpecification], project: Project) extends Activity[ReferenceEntities] {

  override def initialValue = Some(ReferenceEntities.empty)

  override def run(context: ActivityContext[ReferenceEntities]) = {
    context.status.update("Waiting for paths cache", 0.0)
    val pathsCache = task.activity[PathsCache]
    while(pathsCache.status().isRunning)
      Thread.sleep(1000)
    if(pathsCache.status().failed)
     throw new Exception("Cannot load reference entities cache, because the paths cache could not be loaded.")

    val entityLoader = new EntityLoader(context, pathsCache.value())
    entityLoader.load()
  }

  private class EntityLoader(context: ActivityContext[ReferenceEntities], entityDescs: DPair[EntityDescription]) {

    private val sources = task.data.datasets.map(ds => project.task[Dataset](ds.datasetId).data.source)

    private val linkSpec = task.data

    private var updated = false

    def load() = {
      context.status.update("Loading entities", 0.0)

      val linkCount = linkSpec.referenceLinks.positive.size + linkSpec.referenceLinks.negative.size
      var loadedLinks = 0
      updated = false

      for (link <- linkSpec.referenceLinks.positive) {
        if(Thread.currentThread.isInterrupted) throw new InterruptedException()
        for(l <- loadPositiveLink(link))
          context.value() = context.value().withPositive(l)
        loadedLinks += 1
        if(loadedLinks % 10 == 0)
          context.status.update(0.5 * (loadedLinks.toDouble / linkCount))
      }

      for (link <- linkSpec.referenceLinks.negative) {
        if(Thread.currentThread.isInterrupted) throw new InterruptedException()
        for(l <- loadNegativeLink(link))
          context.value() = context.value().withNegative(l)
        loadedLinks += 1
        if(loadedLinks % 10 == 0)
          context.status.update(0.5 + 0.5 * (loadedLinks.toDouble / linkCount))
      }

      updated
    }

    private def loadPositiveLink(link: Link): Option[DPair[Entity]] = {
      link.entities match {
        case Some(entities) => Some(entities)
        case None => {
          updated = true
          context.value().positive.get(link) match {
            case None => retrieveEntityPair(link)
            case Some(entityPair) => updateEntityPair(entityPair)
          }
        }
      }
    }

    private def loadNegativeLink(link: Link): Option[DPair[Entity]] = {
      link.entities match {
        case Some(entities) => Some(entities)
        case None => {
          updated = true
          context.value().negative.get(link) match {
            case None => retrieveEntityPair(link)
            case Some(entityPair) => updateEntityPair(entityPair)
          }
        }
      }
    }

    private def retrieveEntityPair(uris: DPair[String]): Option[DPair[Entity]]  = {
       for(source <- sources.source.retrieve(entityDescs.source, uris.source :: Nil).headOption;
           target <-  sources.target.retrieve(entityDescs.target, uris.target :: Nil).headOption) yield {
         DPair(source, target)
       }
    }

    private def updateEntityPair(entities: DPair[Entity]): Option[DPair[Entity]] = {
      Some(DPair(
        source = updateEntity(entities.source, entityDescs.source, sources.source),
        target = updateEntity(entities.target, entityDescs.target, sources.target)
      ))
    }

    /**
     * Updates an entity so that it conforms to a new entity description.
     * All property paths values which are not available in the given entity are loaded from the source.
     */
    private def updateEntity(entity: Entity, entityDesc: EntityDescription, source: DataSource) = {
      if (entity.desc.paths == entityDesc.paths) {
        //The given entity already contains all paths in the correct order.
        entity
      } else {
        //Compute the paths which are missing on the given entity
        val existingPaths = entity.desc.paths.toSet
        val missingPaths = entityDesc.paths.filterNot(existingPaths.contains)

        //Retrieve an entity with all missing paths
        val missingEntity =
          source.retrieve(
            entityDesc = entity.desc.copy(paths = missingPaths),
            entities = entity.uri :: Nil
          ).head

        //Collect values from the existing and the new entity
        val completeValues =
          for(path <- entityDesc.paths) yield {
            val pathIndex = entity.desc.paths.indexOf(path)
            if(pathIndex != -1)
              entity.evaluate(pathIndex)
            else
              missingEntity.evaluate(path)
          }

        //Return the updated entity
        new Entity(
          uri = entity.uri,
          values = completeValues,
          desc = entityDesc
        )
      }
    }
  }
}
