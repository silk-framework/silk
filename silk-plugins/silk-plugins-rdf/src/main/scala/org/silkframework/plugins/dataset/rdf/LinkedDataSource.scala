package org.silkframework.plugins.dataset.rdf

import java.util.logging.{Logger, Level}

import com.hp.hpl.jena.rdf.model.ModelFactory
import org.silkframework.dataset.DataSource
import org.silkframework.entity.Entity
import org.silkframework.entity.rdf.SparqlEntitySchema
import org.silkframework.plugins.dataset.rdf.endpoint.JenaModelEndpoint
import org.silkframework.plugins.dataset.rdf.sparql.EntityRetriever
import org.silkframework.runtime.plugin.Plugin
import org.silkframework.util.Uri

@Plugin(id = "linkedData", label = "Linked Data", description = "TODO")
case class LinkedDataSource() extends DataSource {

  private val logger = Logger.getLogger(getClass.getName)

  override def retrieveSparqlEntities(entityDesc: SparqlEntitySchema, entities: Seq[String]): Traversable[Entity] = {
    require(!entities.isEmpty, "Retrieving all entities not supported")

    logger.log(Level.FINE, "Retrieving data from Linked Data.")

    val model = ModelFactory.createDefaultModel
    for (uri <- entities) {
      model.read(uri)
    }

    val endpoint = new JenaModelEndpoint(model)

    val entityRetriever = EntityRetriever(endpoint)

    entityRetriever.retrieve(entityDesc, entities.map(Uri(_)), None)
  }
}
