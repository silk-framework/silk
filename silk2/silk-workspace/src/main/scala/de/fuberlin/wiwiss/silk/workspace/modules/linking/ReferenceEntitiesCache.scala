package de.fuberlin.wiwiss.silk.workspace.modules.linking

import de.fuberlin.wiwiss.silk.dataset.DataSource
import de.fuberlin.wiwiss.silk.entity.{Entity, EntityDescription, Link}
import de.fuberlin.wiwiss.silk.evaluation.ReferenceEntities
import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.workspace.Project
import de.fuberlin.wiwiss.silk.workspace.modules.Cache
import de.fuberlin.wiwiss.silk.workspace.modules.dataset.DatasetTask

import scala.xml.{Node, NodeBuffer, NodeSeq}

class ReferenceEntitiesCache(pathsCache: PathsCache) extends Cache[LinkingTask, ReferenceEntities](ReferenceEntities.empty) {

  /** Alias for value to make code more readable */
  private def entities = value

  /** Alias for value to make code more readable */
  private def entities_=(v: ReferenceEntities) { value = v}

  override def update(project: Project, task: LinkingTask) = {
    updateStatus("Waiting for paths cache", 0.0)
    pathsCache.waitUntilLoaded()
    if(pathsCache.status.failed)
     throw new Exception("Cannot load reference entities cache, because the paths cache could not be loaded.")

//    if(value == null ||
//       currentEntityDescs.source.restrictions != value.source.restrictions &&
//       currentEntityDescs.target.restrictions != value.target.restrictions) {
//      update = true
      //TODO reset reference entities = ReferenceEntities.empty
//    }

    val entityLoader = new EntityLoader(project, task, pathsCache.value)
    entityLoader.load()
  }

  override def toXML: NodeSeq = {
    val nodes = new NodeBuffer()

    nodes.append(
      <PositiveEntities>
        {for (DPair(sourceEntity, targetEntity) <- entities.positive.values) yield {
        <Pair>
          <Source>
            {sourceEntity.toXML}
          </Source>
          <Target>
            {targetEntity.toXML}
          </Target>
        </Pair>
      }}
      </PositiveEntities>)

    nodes.append(
      <NegativeEntities>
        {for (DPair(sourceEntity, targetEntity) <- entities.negative.values) yield {
        <Pair>
          <Source>
            {sourceEntity.toXML}
          </Source>
          <Target>
            {targetEntity.toXML}
          </Target>
        </Pair>
      }}
      </NegativeEntities>)

    NodeSeq.fromSeq(nodes)
  }

  override def loadFromXML(node: Node) {
    val positiveEntities: Traversable[DPair[Entity]] = {
      if ((node \ "PositiveEntities").isEmpty) {
        Traversable.empty
      } else {
        for (pairNode <- (node \ "PositiveEntities" \ "Pair").toList) yield {
          DPair(
            Entity.fromXML(pairNode \ "Source" \ "Entity" head, pathsCache.value.source),
            Entity.fromXML(pairNode \ "Target" \ "Entity" head, pathsCache.value.target))
        }
      }
    }

    val negativeEntities: Traversable[DPair[Entity]] = {
      if ((node \ "NegativeEntities").isEmpty) {
        Traversable.empty
      } else {
        for (pairNode <- (node \ "NegativeEntities" \ "Pair").toList) yield {
          DPair(
            Entity.fromXML(pairNode \ "Source" \ "Entity" head, pathsCache.value.source),
            Entity.fromXML(pairNode \ "Target" \ "Entity" head, pathsCache.value.target))
        }
      }
    }

    entities = ReferenceEntities.fromEntities(positiveEntities, negativeEntities)
  }

  private class EntityLoader(project: Project, task: LinkingTask, entityDescs: DPair[EntityDescription]) {

    private val sources = task.linkSpec.datasets.map(ds => project.task[DatasetTask](ds.datasetId).source)

    private var updated = false

    def load() = {
      updateStatus("Loading entities", 0.0)

      val linkCount = task.referenceLinks.positive.size + task.referenceLinks.negative.size
      var loadedLinks = 0
      updated = false

      for (link <- task.referenceLinks.positive) {
        if(Thread.currentThread.isInterrupted) throw new InterruptedException()
        entities = entities.withPositive(loadPositiveLink(link))
        loadedLinks += 1
        if(loadedLinks % 10 == 0)
          updateStatus(0.5 * (loadedLinks.toDouble / linkCount))
      }

      for (link <- task.referenceLinks.negative) {
        if(Thread.currentThread.isInterrupted) throw new InterruptedException()
        entities = entities.withNegative(loadNegativeLink(link))
        loadedLinks += 1
        if(loadedLinks % 10 == 0)
          updateStatus(0.5 + 0.5 * (loadedLinks.toDouble / linkCount))
      }

      updated
    }

    private def loadPositiveLink(link: Link) = {
      link.entities match {
        case Some(entities) => entities
        case None => {
          updated = true
          entities.positive.get(link) match {
            case None => retrieveEntityPair(link)
            case Some(entityPair) => updateEntityPair(entityPair)
          }
        }
      }
    }

    private def loadNegativeLink(link: Link) = {
      link.entities match {
        case Some(entities) => entities
        case None => {
          updated = true
          entities.negative.get(link) match {
            case None => retrieveEntityPair(link)
            case Some(entityPair) => updateEntityPair(entityPair)
          }
        }
      }
    }

    private def retrieveEntityPair(uris: DPair[String]) = {
      DPair(
        source = sources.source.retrieve(entityDescs.source, uris.source :: Nil).head,
        target = sources.target.retrieve(entityDescs.target, uris.target :: Nil).head
      )
    }

    private def updateEntityPair(entities: DPair[Entity]) = {
      DPair(
        source = updateEntity(entities.source, entityDescs.source, sources.source),
        target = updateEntity(entities.target, entityDescs.target, sources.target)
      )
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
