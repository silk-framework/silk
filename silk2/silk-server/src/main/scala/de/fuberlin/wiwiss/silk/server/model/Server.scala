package de.fuberlin.wiwiss.silk.server.model

import de.fuberlin.wiwiss.silk.datasource.DataSource
import de.fuberlin.wiwiss.silk.plugins.Plugins
import de.fuberlin.wiwiss.silk.output.Link
import de.fuberlin.wiwiss.silk.plugins.writer.NTriplesFormatter
import de.fuberlin.wiwiss.silk.plugins.jena.{FileDataSource, RdfDataSource}
import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.config.LinkingConfig

/**
 * The Silk Server.
 *
 * @see ServerConfig
 */
object Server {
  private val logger = Logger.getLogger(Server.getClass.getName)

  @volatile
  private var server : Server = null

  /**
   * Starts the Server.
   */
  def start() {
    logger.info("Starting server")

    server = new Server()

    logger.info("Server started")
  }

  /**
   * The datasets held by this server.
   * Each link specification in the configuration is represented by one dataset.
   */
  def datasets : Traversable[Dataset] = {
    require(server != null, "Server must be initialized")
    server.datasets
  }

  /**
   * Processes a source of new entities.
   * Returns the matching results as N-Triples.
   */
  def process(source : DataSource) : String = {
    require(server != null, "Server must be initialized")
    server.process(source)
  }
}

/**
 *
 */
private class Server {
  Plugins.register()
  DataSource.register(classOf[RdfDataSource])
  DataSource.register(classOf[FileDataSource])

  val serverConfig = ServerConfig.load()

  /**
   * The datasets held by this server.
   * Each link specification in the configuration is represented by one dataset.
   */
  val datasets : Traversable[Dataset] = {
    //Iterate through all configuration files and create a dataset for each link spec
    for( file <- serverConfig.configDir.listFiles if file.getName.endsWith("xml");
         config = LinkingConfig.load(file);
         linkSpec <- config.linkSpecs) yield {
      new Dataset(name = file.getName.takeWhile(_ != '.'),
                  config = config,
                  linkSpec = linkSpec,
                  writeUnmatchedEntities = serverConfig.writeUnknownEntities)
    }
  }

  private val silkUriPrefix = "http://www4.wiwiss.fu-berlin.de/bizer/silk/"

  private val matchResultPropertyUri = silkUriPrefix + "matchingResult"

  private val unknownEntitiyUri = silkUriPrefix + "UnknownEntity"

  def process(source : DataSource) : String = {
    //Logger.getLogger("de.fuberlin.wiwiss.silk").setLevel(Level.WARNING)
    val matchResults = datasets.map(m => m(source))
    //Logger.getLogger("de.fuberlin.wiwiss.silk").setLevel(Level.INFO)

    formatResults(matchResults)
  }

  private def formatResults(matchResults : Traversable[MatchResult]) = {
    val formatter = new NTriplesFormatter()

    //Format matchResults
    val formattedLinks =
      for(matchResult <- matchResults;
          link <- matchResult.links)
          yield formatter.format(link, matchResult.linkType.toString)

    if(!serverConfig.returnUnknownEntities) {
      formattedLinks.mkString
    }
    else {
      //Get the set of all entities which have not been matched by any link specification
      val allUnmatchedEntities = matchResults.flatMap(_.unmatchedEntities).toSet
      val matchedEntities = matchResults.flatMap(_.links).flatMap(link => link.source :: link.target :: Nil)
      val unmatchedEntities = allUnmatchedEntities -- matchedEntities

      //Format unmatched entities
      val formattedUnmatchedEntities =
        for(unmatchedEntity <- unmatchedEntities)
          yield formatter.format(new Link(unmatchedEntity, unknownEntitiyUri, 0.0), matchResultPropertyUri)

      //Return result
      formattedLinks.mkString + formattedUnmatchedEntities.mkString
    }
  }
}
