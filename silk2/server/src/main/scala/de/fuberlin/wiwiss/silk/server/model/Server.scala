package de.fuberlin.wiwiss.silk.server.model

import de.fuberlin.wiwiss.silk.datasource.DataSource
import de.fuberlin.wiwiss.silk.impl.DefaultImplementations
import de.fuberlin.wiwiss.silk.instance.InstanceSpecification
import de.fuberlin.wiwiss.silk.output.Link
import de.fuberlin.wiwiss.silk.impl.writer.NTriplesFormatter
import de.fuberlin.wiwiss.silk.config.{Configuration, ConfigLoader}
import java.io.{FileNotFoundException, File}

object Server
{
    DefaultImplementations.register()

    private val silkUriPrefix = "http://www4.wiwiss.fu-berlin.de/bizer/silk/"

    private val matchResultPropertyUri = silkUriPrefix + "matchingResult"

    private val unknownInstanceUri = silkUriPrefix + "UnknownInstance"

    private var _datasets = Traversable[Dataset]()

    private val writeUnknownInstances = true

    def datasets = _datasets

    def init()
    {
        //Load configurations
        val configDir = new File("./config")
        if(!configDir.exists) throw new FileNotFoundException("Config directory " + configDir + " not found")
        for(file <- configDir.listFiles if file.getName.endsWith("xml"))
        {
            addConfig(ConfigLoader.load(file), file.getName.takeWhile(_ != '.'))
        }
    }

    def addConfig(config : Configuration, name : String)
    {
        _datasets ++= (for((id, linkSpec) <- config.linkSpecs) yield new Dataset(name, config, linkSpec))
    }

    def process(instanceSource : DataSource) : String =
    {
        //Logger.getLogger("de.fuberlin.wiwiss.silk").setLevel(Level.WARNING)
        val matchResults = _datasets.map(m => m(instanceSource))
        //Logger.getLogger("de.fuberlin.wiwiss.silk").setLevel(Level.INFO)

        val unknownInstances = handleUnknownInstances(instanceSource, matchResults)

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

    private def handleUnknownInstances(instanceSource : DataSource, matchResults : Traversable[MatchResult]) : Traversable[String] =
    {
        //Create a source yielding all unknown instances
        val unknownInstancesSource = new FilterDataSource(instanceSource, matchResults)

        //Add all unknown instances to the instance caches
        if(writeUnknownInstances)
        {
            datasets.foreach(m => m.addInstances(unknownInstancesSource))
        }

        //Retrieve all unknown instances (we are only interested in their URI)
        val unknownInstances = unknownInstancesSource.retrieve(InstanceSpecification.empty, Map.empty).map(_.uri)

        unknownInstances
    }

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
