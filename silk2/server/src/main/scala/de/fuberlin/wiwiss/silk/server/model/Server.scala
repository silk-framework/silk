package de.fuberlin.wiwiss.silk.server.model

import de.fuberlin.wiwiss.silk.datasource.DataSource
import de.fuberlin.wiwiss.silk.impl.DefaultImplementations
import de.fuberlin.wiwiss.silk.output.Link
import de.fuberlin.wiwiss.silk.impl.writer.NTriplesFormatter
import de.fuberlin.wiwiss.silk.config.ConfigLoader
import de.fuberlin.wiwiss.silk.jena.{FileDataSource, RdfDataSource}
import java.util.logging.Logger

/**
 * The Silk Server.
 *
 * @see ServerConfig
 */
object Server
{
    private val logger = Logger.getLogger(Server.getClass.getName)

    @volatile
    private var server : Server = null

    /**
     * Starts the Server.
     */
    def start()
    {
        logger.info("Starting server")

        server = new Server()

        logger.info("Server started")
    }

    /**
     * The datasets held by this server.
     * Each link specification in the configuration is represented by one dataset.
     */
    def datasets : Traversable[Dataset] =
    {
        require(server != null, "Server must be initialized")
        server.datasets
    }

    /**
     * Processes a source of new instances.
     * Returns the matching results as N-Triples.
     */
    def process(instanceSource : DataSource) : String =
    {
        require(server != null, "Server must be initialized")
        server.process(instanceSource)
    }
}

/**
 *
 */
private class Server
{
    DefaultImplementations.register()
    DataSource.register("rdf", classOf[RdfDataSource])
    DataSource.register("file", classOf[FileDataSource])

    val serverConfig = ServerConfig.load()

    /**
     * The datasets held by this server.
     * Each link specification in the configuration is represented by one dataset.
     */
    val datasets : Traversable[Dataset] =
    {
        //Iterate through all configuration files and create a dataset for each link spec
        for( file <- serverConfig.configDir.listFiles if file.getName.endsWith("xml");
             config = ConfigLoader.load(file);
             (id, linkSpec) <- config.linkSpecs ) yield
        {
            new Dataset(name = file.getName.takeWhile(_ != '.'),
                        config = config,
                        linkSpec = linkSpec,
                        writeUnmatchedInstances = serverConfig.writeUnknownInstances)
        }
    }

    private val silkUriPrefix = "http://www4.wiwiss.fu-berlin.de/bizer/silk/"

    private val matchResultPropertyUri = silkUriPrefix + "matchingResult"

    private val unknownInstanceUri = silkUriPrefix + "UnknownInstance"

    def process(instanceSource : DataSource) : String =
    {
        //Logger.getLogger("de.fuberlin.wiwiss.silk").setLevel(Level.WARNING)
        val matchResults = datasets.map(m => m(instanceSource))
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

        if(!serverConfig.returnUnknownInstances)
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
