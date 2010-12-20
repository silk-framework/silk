package de.fuberlin.wiwiss.silk.workbench.project

import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import de.fuberlin.wiwiss.silk.impl.datasource.SparqlDataSource
import de.fuberlin.wiwiss.silk.config.Configuration
import java.io.File
import de.fuberlin.wiwiss.silk.linkspec._
import de.fuberlin.wiwiss.silk.datasource.Source
import de.fuberlin.wiwiss.silk.output.Link
import de.fuberlin.wiwiss.silk.workbench._
import de.fuberlin.wiwiss.silk.evaluation.Alignment

private object ProjectCreator
{
  def create(description : SourceTargetPair[Description]) =
  {
    //Generate initial configuration
    val config : Configuration =
    {
      val sourceDataSource = new Source(Constants.SourceId, new SparqlDataSource(Map("endpointURI" -> description.source.endpoint.uri.toString)))
      val targetDataSource = new Source(Constants.TargetId, new SparqlDataSource(Map("endpointURI" -> description.target.endpoint.uri.toString)))

      val linkSpec =
        new LinkSpecification(
          id = "id",
          linkType = "http://www.w3.org/2002/07/owl#sameAs",
          datasets = new SourceTargetPair(new DatasetSpecification(sourceDataSource, Constants.SourceVariable, description.source.restriction),
                                          new DatasetSpecification(targetDataSource, Constants.TargetVariable, description.target.restriction)),
          blocking = None,
          condition = new LinkCondition(Aggregation(false, 1, Traversable.empty, Aggregator("max"))),
          filter = new LinkFilter(0.95, None),
          outputs = Nil
        )

      new Configuration(Map.empty, Traversable(sourceDataSource, targetDataSource), Traversable(linkSpec))
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