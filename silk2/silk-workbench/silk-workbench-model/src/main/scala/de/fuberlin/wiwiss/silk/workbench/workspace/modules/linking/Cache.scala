package de.fuberlin.wiwiss.silk.workbench.workspace.modules.linking

import xml.{NodeBuffer, Node}
import de.fuberlin.wiwiss.silk.config.Prefixes
import de.fuberlin.wiwiss.silk.workbench.workspace.Project
import collection.immutable.List._
import de.fuberlin.wiwiss.silk.datasource.DataSource
import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.util.task._
import de.fuberlin.wiwiss.silk.evaluation.ReferenceEntities
import java.lang.InterruptedException
import java.util.logging.Level
import de.fuberlin.wiwiss.silk.entity.{Link, EntityDescription, Entity}

//TODO use options?
//TODO store path frequencies
class Cache(private var existingEntityDescs: DPair[EntityDescription] = null,
            private var existingEntities: ReferenceEntities = ReferenceEntities.empty) extends HasStatus {

  /**The cached entity descriptions containing the most frequent paths */
  @volatile private var cachedEntityDescs: DPair[EntityDescription] = null

  /**The cached entities */
  @volatile private var cachedEntities: ReferenceEntities = ReferenceEntities.empty

  @volatile private var loadingThread: CacheLoader = null

  progressLogLevel = Level.FINE

  /**The cached entity descriptions containing the most frequent paths */
  def entityDescs = cachedEntityDescs

  /**The cached entities */
  def entities = cachedEntities

  /**
   * Update this cache.
   */
  def update() = {
    removeSubscriptions()
    new Cache(entityDescs, entities)
  }

  /**
   * Reloads the cache.
   */
  def reload(project : Project, task: LinkingTask) {
    existingEntityDescs = null
    cachedEntityDescs = null
    existingEntities = ReferenceEntities.empty
    cachedEntities = ReferenceEntities.empty

    load(project, task)
  }

  /**
   * Load the cache.
   */
  def load(project : Project, task: LinkingTask) {
    if(loadingThread != null) {
      loadingThread.interrupt()
      loadingThread.join()
    }
    loadingThread = new CacheLoader(project, task)
    loadingThread.start()
  }

  def waitUntilLoaded() {
    Option(loadingThread).map(_.join())
  }

  /**
   * Serializes the cache to XML.
   */
  def toXML(implicit prefixes: Prefixes): Node = {
    val nodes = new NodeBuffer()

    if (entityDescs != null) {
      nodes.append(
        <EntityDescriptions>
          <Source>
            {entityDescs.source.toXML}
          </Source>
          <Target>
            {entityDescs.target.toXML}
          </Target>
        </EntityDescriptions>)
    }

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

    <Cache>
      {nodes}
    </Cache>
  }

  private class CacheLoader(project: Project, task: LinkingTask) extends Thread {
    private val sources = task.linkSpec.datasets.map(ds => project.sourceModule.task(ds.sourceId).source.dataSource)

    override def run() {
      val startTime = System.currentTimeMillis
      updateStatus(TaskStarted("Loading cache"))
      try {
        loadPaths()
        loadEntities()
        updateStatus(TaskFinished("Loading cache", true, System.currentTimeMillis - startTime, None))
        if(!isInterrupted) {
          project.linkingModule.update(task)
        }
      } catch {
        case ex: InterruptedException => {
          logger.log(Level.WARNING, "Loading cache stopped")
          updateStatus(TaskFinished("Loading stopped", false, System.currentTimeMillis - startTime, None))
        }
        case ex: Exception => {
          logger.log(Level.WARNING, "Loading cache failed", ex)
          updateStatus(TaskFinished("Loading cache", false, System.currentTimeMillis - startTime, Some(ex)))
        }
      }

      loadingThread = null
    }

    /**
     * Loads the most frequent property paths.
     */
    private def loadPaths() {
      updateStatus("Retrieving frequent property paths", 0.0)

      //Create an entity description from the link specification
      val currentEntityDescs = task.linkSpec.entityDescriptions

      //Check if the restriction has been changed
      if(existingEntityDescs != null &&
         currentEntityDescs.source.restrictions == existingEntityDescs.source.restrictions &&
         currentEntityDescs.target.restrictions == existingEntityDescs.target.restrictions) {
        cachedEntityDescs = existingEntityDescs
      } else {
        cachedEntityDescs = null
        cachedEntities = ReferenceEntities.empty
      }

      if (cachedEntityDescs == null) {
        //Retrieve most frequent paths
        val paths = for ((source, dataset) <- sources zip task.linkSpec.datasets) yield source.retrievePaths(dataset.restriction, 1, Some(50))

        //Add the frequent paths to the entity description
        cachedEntityDescs = for ((entityDesc, paths) <- currentEntityDescs zip paths) yield entityDesc.copy(paths = (entityDesc.paths ++ paths.map(_._1)).distinct)
      } else {
        //Add the existing paths to the entity description
        cachedEntityDescs = for ((spec1, spec2) <- currentEntityDescs zip existingEntityDescs) yield spec1.copy(paths = (spec1.paths ++ spec2.paths).distinct)
      }
    }

    /**
     * Loads the entities.
     */
    private def loadEntities() {
      updateStatus("Loading entities", 0.2)

      val linkCount = task.referenceLinks.positive.size + task.referenceLinks.negative.size
      var loadedLinks = 0

      for (link <- task.referenceLinks.positive) {
        if(isInterrupted) throw new InterruptedException()
        cachedEntities = entities.withPositive(loadPositiveLink(link))
        loadedLinks += 1
        updateStatus(0.2 + 0.8 * (loadedLinks.toDouble / linkCount))
      }

      for (link <- task.referenceLinks.negative) {
        if(isInterrupted) throw new InterruptedException()
        cachedEntities = entities.withNegative(loadNegativeLink(link))
        loadedLinks += 1
        updateStatus(0.2 + 0.8 * (loadedLinks.toDouble / linkCount))
      }
    }

    private def loadPositiveLink(link: Link) = {
      link.entities match {
        case Some(entities) => entities
        case None => {
          existingEntities.positive.get(link) match {
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
          existingEntities.negative.get(link) match {
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
        val values =
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
          values = values,
          desc = entityDesc
        )
      }
    }
  }

}

object Cache {
  def fromXML(node: Node) = {
    val existingEntityDescs = {
      if (node \ "EntityDescriptions" isEmpty) {
        null
      } else {
        val sourceSpec = EntityDescription.fromXML(node \ "EntityDescriptions" \ "Source" \ "_" head)
        val targetSpec = EntityDescription.fromXML(node \ "EntityDescriptions" \ "Target" \ "_" head)
        new DPair(sourceSpec, targetSpec)
      }
    }

    val positiveEntities: Traversable[DPair[Entity]] = {
      if (node \ "PositiveEntities" isEmpty) {
        Traversable.empty
      } else {
        for (pairNode <- node \ "PositiveEntities" \ "Pair" toList) yield {
          DPair(
            Entity.fromXML(pairNode \ "Source" \ "Entity" head, existingEntityDescs.source),
            Entity.fromXML(pairNode \ "Target" \ "Entity" head, existingEntityDescs.target))
        }
      }
    }

    val negativeEntities: Traversable[DPair[Entity]] = {
      if (node \ "NegativeEntities" isEmpty) {
        Traversable.empty
      } else {
        for (pairNode <- node \ "NegativeEntities" \ "Pair" toList) yield {
          DPair(
            Entity.fromXML(pairNode \ "Source" \ "Entity" head, existingEntityDescs.source),
            Entity.fromXML(pairNode \ "Target" \ "Entity" head, existingEntityDescs.target))
        }
      }
    }

    val existingEntities = ReferenceEntities.fromEntities(positiveEntities, negativeEntities)

    new Cache(existingEntityDescs, existingEntities)
  }
}

