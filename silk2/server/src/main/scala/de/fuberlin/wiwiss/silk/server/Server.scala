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

    private val silkUriPrefix = "http://www4.wiwiss.fu-berlin.de/bizer/silk/"

    private val matchResultPropertyUri = silkUriPrefix + "matchingResult"

    private val unknownInstanceUri = silkUriPrefix + "UnknownInstance"

    private var matchers : Traversable[LinkSpecMatcher] = null

    def init()
    {
        matchers =
            for(config <- configs;
                linkSpec <- config.linkSpecs.values)
                yield new LinkSpecMatcher(config, linkSpec)
    }

    def process(instanceSource : DataSource) : String =
    {
        //Logger.getLogger("de.fuberlin.wiwiss.silk").setLevel(Level.WARNING)
        val matchResults = matchers.map(m => m(instanceSource))
        //Logger.getLogger("de.fuberlin.wiwiss.silk").setLevel(Level.INFO)

        val unknownInstances = addUnknownInstances(instanceSource, matchResults)

        formatResults(matchResults, unknownInstances)
    }

    private def formatResults(matchResults : Traversable[MatchResult], unknownInstances : Traversable[String]) =
    {
        val formatter = new NTriplesFormatter()

        //Format matchResults
        val formattedResults =
            for(matchResult <- matchResults;
                link <- matchResult.links)
                yield formatter.format(link, matchResult.linkType)

        //Format unknown instances
        val formattedUnknownInstances =
            for(unknownInstance <- unknownInstances)
                yield formatter.format(new Link(unknownInstance, unknownInstanceUri, 0.0), matchResultPropertyUri)

        //Return result
        formattedResults.mkString + formattedUnknownInstances.mkString
    }

    private def addUnknownInstances(instanceSource : DataSource, matchResults : Traversable[MatchResult]) : Traversable[String] =
    {
        //Create a source yielding all unknown instances
        val unknownInstancesSource = new FilterDataSource(instanceSource, matchResults)

        //Add all unknown instances to the instance caches
        matchers.foreach(m => m.addInstances(unknownInstancesSource))

        //Retrieve all unknown instances (we are only interested in their URI)
        val unknownInstances = unknownInstancesSource.retrieve(InstanceSpecification.empty, Map.empty).map(_.uri)

        unknownInstances
    }

    private class LinkSpecMatcher(config : Configuration, linkSpec : LinkSpecification)
    {
        private val sourceCache = new MemoryInstanceCache()
        private val targetCache = new MemoryInstanceCache()
        new Loader(config, linkSpec).loadCaches(sourceCache, targetCache)

        private val (sourceInstanceSpec, targetInstanceSpec) = InstanceSpecification.retrieve(linkSpec)

        def apply(instanceSource : DataSource) : MatchResult =
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

            MatchResult(writer.links, linkSpec.linkType)
        }

        def addInstances(instanceSource : DataSource)
        {
            //TODO
            sourceCache.write(instanceSource.retrieve(sourceInstanceSpec, config.prefixes))
            targetCache.write(instanceSource.retrieve(targetInstanceSpec, config.prefixes))
        }
    }

    private case class MatchResult(links : Traversable[Link], linkType : String)

    /**
     * A DataSource which only returns unknown instances.
     */
    private class FilterDataSource(dataSource : DataSource, matchResults : Traversable[MatchResult]) extends DataSource
    {
        private val knownUris =
        {
            val links = matchResults.flatMap(_.links)
            links.map(_.sourceUri).toSet ++ links.map(_.targetUri).toSet
        }

        override def retrieve(instanceSpec : InstanceSpecification, prefixes : Map[String, String]) =
        {
             dataSource.retrieve(instanceSpec, prefixes)
                       .view.filter(instance => !knownUris.contains(instance.uri))
        }

        override val params = Map[String, String]()
    }
}
