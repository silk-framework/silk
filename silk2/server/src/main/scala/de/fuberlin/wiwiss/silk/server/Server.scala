package de.fuberlin.wiwiss.silk.server

import java.io.File
import de.fuberlin.wiwiss.silk.datasource.DataSource
import de.fuberlin.wiwiss.silk.impl.DefaultImplementations
import de.fuberlin.wiwiss.silk.config.{Configuration, ConfigLoader}
import de.fuberlin.wiwiss.silk.instance.{MemoryInstanceCache, InstanceSpecification}
import de.fuberlin.wiwiss.silk.{Matcher, Loader}
import de.fuberlin.wiwiss.silk.linkspec.LinkSpecification
import java.util.logging.{Level, Logger}
import de.fuberlin.wiwiss.silk.output.{Link, Output}
import de.fuberlin.wiwiss.silk.impl.writer.{NTriplesFormatter, MemoryWriter}

object Server
{
    DefaultImplementations.register()

    val configFile = new File("./src/main/resources/de/fuberlin/wiwiss/silk/server/config/sider_drugbank_drugs.xml")

    val configs = ConfigLoader.load(configFile) :: Nil

    private val unknownInstanceUri = "UnknownInstance"

    private var matchers : Traversable[InstanceMatcher] = null

    def init()
    {
        matchers =
            for(config <- configs;
                linkSpec <- config.linkSpecs.values)
                yield new InstanceMatcher(config, linkSpec)
    }

    def process(instanceSource : DataSource) : String =
    {
        //Logger.getLogger("de.fuberlin.wiwiss.silk").setLevel(Level.WARNING)
        val links = matchers.flatMap(m => m(instanceSource))
        //Logger.getLogger("de.fuberlin.wiwiss.silk").setLevel(Level.INFO)

        val unknownInstances = addUnknownInstances(instanceSource, links)

        val unknownLinks = unknownInstances.map(uri => new Link(uri, unknownInstanceUri, 0.0))

        val formatter = new NTriplesFormatter()

        (links ++ unknownLinks).map(link => formatter.format(link, "owl:sameAs")).mkString
    }

    private def addUnknownInstances(instanceSource : DataSource, links : Traversable[Link]) : Traversable[String] =
    {
        //Create a source yielding all unknown instances
        val unknownInstancesSource = new FilterDataSource(instanceSource, links)

        //Add all unknown instances to the instance caches
        matchers.foreach(m => m.addInstances(unknownInstancesSource))

        //Retrieve all unknown instances (we are only interested in their URI)
        val unknownInstances = unknownInstancesSource.retrieve(InstanceSpecification.empty, Map.empty).map(_.uri)

        unknownInstances
    }

    private class InstanceMatcher(config : Configuration, linkSpec : LinkSpecification)
    {
        private val (sourceCache, targetCache) = new Loader(config, linkSpec).loadCaches

        private val (sourceInstanceSpec, targetInstanceSpec) = InstanceSpecification.retrieve(linkSpec)

        def apply(instanceSource : DataSource) : Traversable[Link] =
        {
            val instanceCache = new MemoryInstanceCache()
            val writer = new MemoryWriter()
            val matcher = new Matcher(config.copy(outputs = new Output(writer) :: Nil), linkSpec)

            instanceCache.write(instanceSource.retrieve(sourceInstanceSpec, config.prefixes))
            if(instanceCache.instanceCount > 0)
            {
                matcher.execute(instanceCache, targetCache)
            }

            instanceCache.write(instanceSource.retrieve(targetInstanceSpec, config.prefixes))
            if(instanceCache.instanceCount > 0)
            {
                matcher.execute(sourceCache, instanceCache)
            }

            writer.links
        }

        def addInstances(instanceSource : DataSource)
        {
            sourceCache.write(instanceSource.retrieve(sourceInstanceSpec, config.prefixes))
            targetCache.write(instanceSource.retrieve(targetInstanceSpec, config.prefixes))
        }
    }

    /**
     * A DataSource which only returns unknown instances.
     */
    private class FilterDataSource(dataSource : DataSource, links : Traversable[Link]) extends DataSource
    {
        private val knownUris = links.map(_.sourceUri).toSet ++ links.map(_.targetUri).toSet

        override def retrieve(instanceSpec : InstanceSpecification, prefixes : Map[String, String]) =
        {
             dataSource.retrieve(instanceSpec, prefixes)
                       .view.filter(instance => !knownUris.contains(instance.uri))
        }

        override val params = Map[String, String]()
    }
}
