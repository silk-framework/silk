package de.fuberlin.wiwiss.silk.workbench.workspace.modules.linking

import de.fuberlin.wiwiss.silk.instance.{InstanceSpecification, Instance}
import xml.{NodeBuffer, Node}
import de.fuberlin.wiwiss.silk.config.Prefixes
import de.fuberlin.wiwiss.silk.workbench.workspace.Project
import collection.immutable.List._
import de.fuberlin.wiwiss.silk.datasource.DataSource
import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import de.fuberlin.wiwiss.silk.output.Link
import de.fuberlin.wiwiss.silk.util.task._
import de.fuberlin.wiwiss.silk.linkspec.LinkSpecification
import de.fuberlin.wiwiss.silk.evaluation.{Alignment, ReferenceInstances}
import java.lang.InterruptedException
import java.util.logging.Level

//TODO use options?
//TODO store path frequencies
class Cache(existingInstanceSpecs: SourceTargetPair[InstanceSpecification] = null,
            existingInstances: ReferenceInstances = ReferenceInstances.empty) extends HasStatus {

  /**The cached instance specifications containing the most frequent paths */
  @volatile private var cachedInstanceSpecs: SourceTargetPair[InstanceSpecification] = null

  /**The cached instances */
  @volatile private var cachedInstances: ReferenceInstances = ReferenceInstances.empty

  @volatile private var loadingThread: CacheLoader = null

  logLevel = Level.FINE

  /**The cached instance specifications containing the most frequent paths */
  def instanceSpecs = cachedInstanceSpecs

  /**The cached instances */
  def instances = cachedInstances

  /**
   * Update this cache.
   */
  def update(project : Project, linkSpec: LinkSpecification, alignment: Alignment) = {
    stopLoading()
    val updatedCache = new Cache(instanceSpecs, instances)
    updatedCache.load(project, linkSpec, alignment)
    updatedCache
  }

  /**
   * Reloads the cache.
   */
  def reload(project : Project, linkSpec: LinkSpecification, alignment: Alignment) {
    cachedInstanceSpecs = null
    cachedInstances = ReferenceInstances.empty

    load(project, linkSpec, alignment)
  }

  /**
   * Load the cache.
   */
  private def load(project : Project, linkSpec: LinkSpecification, alignment: Alignment) {
    loadingThread = new CacheLoader(project, linkSpec, alignment)
    loadingThread.start()
  }

  private def stopLoading() {
    loadingThread.interrupt()
  }

  /**
   * Serializes the cache to XML.
   */
  def toXML(implicit prefixes: Prefixes): Node = {
    val nodes = new NodeBuffer()

    if (instanceSpecs != null) {
      nodes.append(
        <InstanceSpecifications>
          <Source>
            {instanceSpecs.source.toXML}
          </Source>
          <Target>
            {instanceSpecs.target.toXML}
          </Target>
        </InstanceSpecifications>)
    }

    nodes.append(
      <PositiveInstances>
        {for (SourceTargetPair(sourceInstance, targetInstance) <- instances.positive.values) yield {
        <Pair>
          <Source>
            {sourceInstance.toXML}
          </Source>
          <Target>
            {targetInstance.toXML}
          </Target>
        </Pair>
      }}
      </PositiveInstances>)

    nodes.append(
      <NegativeInstances>
        {for (SourceTargetPair(sourceInstance, targetInstance) <- instances.negative.values) yield {
        <Pair>
          <Source>
            {sourceInstance.toXML}
          </Source>
          <Target>
            {targetInstance.toXML}
          </Target>
        </Pair>
      }}
      </NegativeInstances>)

    <Cache>
      {nodes}
    </Cache>
  }

  private class CacheLoader(project: Project, linkSpec: LinkSpecification, alignment: Alignment) extends Thread {

    private val sources = linkSpec.datasets.map(ds => project.sourceModule.task(ds.sourceId).source.dataSource)

    override def run() {
      updateStatus(Started("Loading cache"))
      try {
        loadPaths()
        loadInstances()
        updateStatus(Finished("Loading cache", true, None))
      } catch {
        case ex: Exception => updateStatus(Finished("Loading cache", false, Some(ex)))
      }
    }

    /**
     * Loads the most frequent property paths.
     */
    private def loadPaths() {
      updateStatus("Retrieving frequent property paths", 0.0)

      //Create an instance spec from the link specification
      val currentInstanceSpecs = InstanceSpecification.retrieve(linkSpec)

      if (existingInstanceSpecs == null) {
        //Retrieve most frequent paths
        val paths = for ((source, dataset) <- sources zip linkSpec.datasets) yield source.retrievePaths(dataset.restriction, 1, Some(50))

        //Add the frequent paths to the instance specification
        cachedInstanceSpecs = for ((instanceSpec, paths) <- currentInstanceSpecs zip paths) yield instanceSpec.copy(paths = (instanceSpec.paths ++ paths.map(_._1)).distinct)
      }
      else {
        //Add the existing paths to the instance specification
        cachedInstanceSpecs = for ((spec1, spec2) <- currentInstanceSpecs zip existingInstanceSpecs) yield spec1.copy(paths = (spec1.paths ++ spec2.paths).distinct)
      }
    }

    /**
     * Loads the instances.
     */
    private def loadInstances() {
      updateStatus("Loading instances", 0.2)

      val linkCount = alignment.positive.size + alignment.negative.size
      var loadedLinks = 0

      for (link <- alignment.positive) {
        if(isInterrupted) throw new InterruptedException()
        cachedInstances = instances.withPositive(loadPositiveLink(link))
        loadedLinks += 1
        updateStatus(0.2 + 0.8 * (loadedLinks.toDouble / linkCount))
      }

      for (link <- alignment.negative) {
        if(isInterrupted) throw new InterruptedException()
        cachedInstances = instances.withNegative(loadNegativeLink(link))
        loadedLinks += 1
        updateStatus(0.2 + 0.8 * (loadedLinks.toDouble / linkCount))
      }
    }

    private def loadPositiveLink(link: Link) = {
      existingInstances.positive.get(link) match {
        case None => retrieveInstancePair(link)
        case Some(instancePair) => updateInstancePair(instancePair)
      }
    }

    private def loadNegativeLink(link: Link) = {
      existingInstances.negative.get(link) match {
        case None => retrieveInstancePair(link)
        case Some(instancePair) => updateInstancePair(instancePair)
      }
    }

    private def retrieveInstancePair(uris: SourceTargetPair[String]) = {
      SourceTargetPair(
        source = sources.source.retrieve(instanceSpecs.source, uris.source :: Nil).head,
        target = sources.target.retrieve(instanceSpecs.target, uris.target :: Nil).head
      )
    }

    private def updateInstancePair(instances: SourceTargetPair[Instance]) = {
      SourceTargetPair(
        source = updateInstance(instances.source, instanceSpecs.source, sources.source),
        target = updateInstance(instances.target, instanceSpecs.target, sources.target)
      )
    }

    private def updateInstance(instance: Instance, instanceSpec: InstanceSpecification, source: DataSource) = {
      //Compute the paths which are missing on the given instance
      val existingPaths = instance.spec.paths.toSet
      val missingPaths = instanceSpec.paths.filterNot(existingPaths.contains)

      if (missingPaths.isEmpty) {
        instance
      }
      else {
        //Retrieve an instance with all missing paths
        val missingInstance =
          source.retrieve(
            instanceSpec = instance.spec.copy(paths = missingPaths),
            instances = instance.uri :: Nil
          ).head

        //Return the updated instance
        new Instance(
          uri = instance.uri,
          values = instance.values ++ missingInstance.values,
          spec = instance.spec.copy(paths = instance.spec.paths ++ missingPaths)
        )
      }
    }
  }

}

object Cache {
  def fromXML(node: Node, project: Project, linkSpec: LinkSpecification, alignment: Alignment): Cache = {
    val instanceSpecs = {
      if (node \ "InstanceSpecifications" isEmpty) {
        null
      }
      else {
        val sourceSpec = InstanceSpecification.fromXML(node \ "InstanceSpecifications" \ "Source" \ "_" head)
        val targetSpec = InstanceSpecification.fromXML(node \ "InstanceSpecifications" \ "Target" \ "_" head)
        new SourceTargetPair(sourceSpec, targetSpec)
      }
    }

    val positiveInstances: Traversable[SourceTargetPair[Instance]] = {
      if (node \ "PositiveInstances" isEmpty) {
        Traversable.empty
      }
      else {
        for (pairNode <- node \ "PositiveInstances" \ "Pair" toList) yield {
          SourceTargetPair(
            Instance.fromXML(pairNode \ "Source" \ "Instance" head, instanceSpecs.source),
            Instance.fromXML(pairNode \ "Target" \ "Instance" head, instanceSpecs.target))
        }
      }
    }

    val negativeInstances: Traversable[SourceTargetPair[Instance]] = {
      if (node \ "NegativeInstances" isEmpty) {
        Traversable.empty
      }
      else {
        for (pairNode <- node \ "NegativeInstances" \ "Pair" toList) yield {
          SourceTargetPair(
            Instance.fromXML(pairNode \ "Source" \ "Instance" head, instanceSpecs.source),
            Instance.fromXML(pairNode \ "Target" \ "Instance" head, instanceSpecs.target))
        }
      }
    }

    val cache = new Cache(instanceSpecs, ReferenceInstances.fromInstances(positiveInstances, negativeInstances))
    cache.load(project, linkSpec, alignment)
    cache
  }
}
