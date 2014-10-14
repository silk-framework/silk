package de.fuberlin.wiwiss.silk.plugins.dataset.rdf

import com.hp.hpl.jena.rdf.model.ModelFactory
import de.fuberlin.wiwiss.silk.dataset.DataSource
import de.fuberlin.wiwiss.silk.entity.{Entity, EntityDescription}
import de.fuberlin.wiwiss.silk.plugins.dataset.rdf.endpoint.JenaModelEndpoint
import de.fuberlin.wiwiss.silk.plugins.dataset.rdf.sparql.EntityRetriever
import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin

@Plugin(id = "linkedData", label = "Linked Data", description = "TODO")
case class LinkedDataSource() extends DataSource {
  override def retrieve(entityDesc: EntityDescription, entities: Seq[String]): Traversable[Entity] = {
    require(!entities.isEmpty, "Retrieving all entities not supported")

    val model = ModelFactory.createDefaultModel
    for (uri <- entities) {
      model.read(uri)
    }

    val endpoint = new JenaModelEndpoint(model)

    val entityRetriever = EntityRetriever(endpoint)

    entityRetriever.retrieve(entityDesc, entities)
  }
}
