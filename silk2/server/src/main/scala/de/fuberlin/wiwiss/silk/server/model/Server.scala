package de.fuberlin.wiwiss.silk.server.model

import de.fuberlin.wiwiss.silk.datasource.DataSource
import de.fuberlin.wiwiss.silk.impl.DefaultImplementations
import de.fuberlin.wiwiss.silk.output.Link
import de.fuberlin.wiwiss.silk.impl.writer.NTriplesFormatter
import de.fuberlin.wiwiss.silk.config.{Configuration, ConfigLoader}
import java.io.{FileNotFoundException, File}
import de.fuberlin.wiwiss.silk.jena.{FileDataSource, RdfDataSource}
import java.util.logging.{Level, Logger}

object Server
{
    DefaultImplementations.register()
    DataSource.register("rdf", classOf[RdfDataSource])
    DataSource.register("file", classOf[FileDataSource])

    private val serverConfig = new ServerConfig()

    private val silkUriPrefix = "http://www4.wiwiss.fu-berlin.de/bizer/silk/"

    private val matchResultPropertyUri = silkUriPrefix + "matchingResult"

    private val unknownInstanceUri = silkUriPrefix + "UnknownInstance"

    private var _datasets = Traversable[Dataset]()

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
        _datasets ++= (for((id, linkSpec) <- config.linkSpecs) yield new Dataset(name, config, linkSpec, serverConfig.writeUnmatchedInstances))
    }

    def process(instanceSource : DataSource) : String =
    {
        //Logger.getLogger("de.fuberlin.wiwiss.silk").setLevel(Level.WARNING)
        val matchResults = _datasets.map(m => m(instanceSource))
        //Logger.getLogger("de.fuberlin.wiwiss.silk").setLevel(Level.INFO)

        formatResults(matchResults)
    }

    private def formatResults(matchResults : Traversable[MatchResult]) =
    {
        val formatter = new NTriplesFormatter()

        //Format matchResults
        val formattedLinks =
            for(matchResult <- matchResults;
                link <- matchResult.links)
                yield formatter.format(link, matchResult.linkType)

        if(!serverConfig.returnUnmatchedInstances)
        {
            formattedLinks.mkString
        }
        else
        {
            //Get the set of all instances which have not been matched by any link specification
            val allUnmatchedInstances = matchResults.flatMap(_.unmatchedInstances).toSet
            val matchedInstances = matchResults.flatMap(_.links).flatMap(link => link.sourceUri :: link.targetUri :: Nil)
            val unmatchedInstances = allUnmatchedInstances -- matchedInstances

            //Format unmatched instances
            val formattedUnmatchedInstances =
                for(unmatchedInstance <- unmatchedInstances)
                    yield formatter.format(new Link(unmatchedInstance, unknownInstanceUri, 0.0), matchResultPropertyUri)

            //Return result
            formattedLinks.mkString + formattedUnmatchedInstances.mkString
        }
    }
}
