package de.fuberlin.wiwiss.silk.workbench.project

import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import de.fuberlin.wiwiss.silk.impl.datasource.SparqlDataSource
import de.fuberlin.wiwiss.silk.config.Configuration
import java.io.File
import de.fuberlin.wiwiss.silk.linkspec._
import de.fuberlin.wiwiss.silk.output.Link
import de.fuberlin.wiwiss.silk.workbench._
import de.fuberlin.wiwiss.silk.evaluation.Alignment
import de.fuberlin.wiwiss.silk.datasource.{DataSource, Source}

private object ProjectCreator
{
  def create(description : SourceTargetPair[Description], prefixes : Map[String, String]) =
  {
    //Generate initial configuration
    val config : Configuration =
    {
      val sourceDataSource = new Source(Constants.SourceId, DataSource("sparqlEndpoint", Map("endpointURI" -> description.source.endpointUri.toString)))
      val targetDataSource = new Source(Constants.TargetId, DataSource("sparqlEndpoint", Map("endpointURI" -> description.target.endpointUri.toString)))

      val linkSpec =
        new LinkSpecification(
          id = "id",
          linkType = "http://www.w3.org/2002/07/owl#sameAs",
          datasets = new SourceTargetPair(new DatasetSpecification(Constants.SourceId, Constants.SourceVariable, description.source.restriction),
                                          new DatasetSpecification(Constants.TargetId, Constants.TargetVariable, description.target.restriction)),
          condition = new LinkCondition(None),
          filter = new LinkFilter(0.95, None),
          outputs = Nil
        )

      new Configuration(prefixes, Traversable(sourceDataSource, targetDataSource), None, Traversable(linkSpec))
    }

    //Generate project
    new Project(
      desc = description,
      config = config,
      linkSpec = config.linkSpecs.head,
      alignment = new Alignment(Set.empty, Set.empty),
      cache = new Cache())
  }
}