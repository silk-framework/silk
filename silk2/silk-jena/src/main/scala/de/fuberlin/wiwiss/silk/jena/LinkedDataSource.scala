package de.fuberlin.wiwiss.silk.jena

import de.fuberlin.wiwiss.silk.instance.{Instance, InstanceSpecification}
import com.hp.hpl.jena.rdf.model.ModelFactory
import de.fuberlin.wiwiss.silk.datasource.DataSource
import de.fuberlin.wiwiss.silk.util.sparql.InstanceRetriever
import de.fuberlin.wiwiss.silk.util.strategy.StrategyAnnotation

@StrategyAnnotation(id = "linkedData", label = "Linked Data", description = "TODO")
class LinkedDataSource extends DataSource
{
  override def retrieve(instanceSpec : InstanceSpecification, instances : Seq[String]) : Traversable[Instance] =
  {
    require(!instances.isEmpty, "Retrieving all instances not supported")

    val model = ModelFactory.createDefaultModel
    for(uri <- instances)
    {
      model.read(uri)
    }

    val endpoint = new JenaSparqlEndpoint(model)

    val instanceRetriever = InstanceRetriever(endpoint)

    instanceRetriever.retrieve(instanceSpec, instances)
  }
}