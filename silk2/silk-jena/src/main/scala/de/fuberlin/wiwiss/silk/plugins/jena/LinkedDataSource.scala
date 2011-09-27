package de.fuberlin.wiwiss.silk.plugins.jena

import de.fuberlin.wiwiss.silk.entity.{Entity, EntityDescription}
import com.hp.hpl.jena.rdf.model.ModelFactory
import de.fuberlin.wiwiss.silk.datasource.DataSource
import de.fuberlin.wiwiss.silk.util.sparql.EntityRetriever
import de.fuberlin.wiwiss.silk.util.plugin.Plugin

@Plugin(id = "linkedData", label = "Linked Data", description = "TODO")
class LinkedDataSource extends DataSource {
  override def retrieve(entityDesc: EntityDescription, entities: Seq[String]): Traversable[Entity] = {
    require(!entities.isEmpty, "Retrieving all entities not supported")

    val model = ModelFactory.createDefaultModel
    for (uri <- entities) {
      model.read(uri)
    }

    val endpoint = new JenaSparqlEndpoint(model)

    val entityRetriever = EntityRetriever(endpoint)

    entityRetriever.retrieve(entityDesc, entities)
  }
}