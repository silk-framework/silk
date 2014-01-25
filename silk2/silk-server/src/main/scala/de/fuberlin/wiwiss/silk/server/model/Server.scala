/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fuberlin.wiwiss.silk.server.model

import de.fuberlin.wiwiss.silk.datasource.{DataSource, Source}
import de.fuberlin.wiwiss.silk.plugins.Plugins
import de.fuberlin.wiwiss.silk.plugins.writer.NTriplesFormatter
import de.fuberlin.wiwiss.silk.plugins.jena.{FileDataSource, RdfDataSource}
import de.fuberlin.wiwiss.silk.config.LinkingConfig
import de.fuberlin.wiwiss.silk.entity.Link
import java.util.logging.{Level, Logger}
import de.fuberlin.wiwiss.silk.util.plugin.FileResourceManager

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
  def process(source : Source) : String = {
    require(server != null, "Server must be initialized")
    server.process(source)
  }

  def processAndReturnLinks(instanceSource : Source) : Traversable[MatchResult] =
  {
    require(server != null, "Server must be initialized")
    server.processAndReturnLinks(instanceSource)
  }
}

/**
 *
 */
private class Server {
  Plugins.register()
  DataSource.register(classOf[RdfDataSource])
  DataSource.register(classOf[FileDataSource])
  DataSource.register(classOf[NopDataSource])


  val serverConfig = ServerConfig.load()

  /**
   * The datasets held by this server.
   * Each link specification in the configuration is represented by one dataset.
   */
  val datasets : Traversable[Dataset] = {
    val resourceLoader = new FileResourceManager(serverConfig.configDir)
    //Iterate through all configuration files and create a dataset for each link spec
    for( file <- serverConfig.configDir.listFiles if file.getName.endsWith("xml");
         config = LinkingConfig.load(resourceLoader)(file);
         linkSpec <- config.linkSpecs) yield {
      new Dataset(name = file.getName.takeWhile(_ != '.'),
                  config = config,
                  linkSpec = linkSpec,
                  writeUnmatchedEntities = serverConfig.writeUnknownEntities,
                  matchOnlyInProvidedGraph = serverConfig.matchOnlyInProvidedGraph)
    }
  }

  private val silkUriPrefix = "http://www4.wiwiss.fu-berlin.de/bizer/silk/"

  private val matchResultPropertyUri = silkUriPrefix + "matchingResult"

  private val unknownEntitiyUri = silkUriPrefix + "UnknownEntity"

  def processAndReturnLinks(source : Source) : Traversable[MatchResult] =
  {
    Logger.getLogger("de.fuberlin.wiwiss.silk").setLevel(Level.WARNING)
    val results = datasets.map(m => m(source))
    Logger.getLogger("de.fuberlin.wiwiss.silk").setLevel(Level.INFO)
    results
 }

  def process(source : Source) : String =
  {
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
          yield formatter.format(new Link(unmatchedEntity, unknownEntitiyUri), matchResultPropertyUri)

      //Return result
      formattedLinks.mkString + formattedUnmatchedEntities.mkString
    }
  }
}
