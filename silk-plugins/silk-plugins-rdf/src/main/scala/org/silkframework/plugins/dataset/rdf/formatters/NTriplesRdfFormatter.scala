package org.silkframework.plugins.dataset.rdf.formatters

import com.hp.hpl.jena.rdf.model.{ModelFactory, Model}
import com.hp.hpl.jena.vocabulary.OWL
import org.silkframework.entity.Link

/**
 * Created by andreas on 12/11/15.
 */
class NTriplesRdfFormatter extends RdfFormatter {
  override def format(link: Link, predicate: String): Model = {
    val model = ModelFactory.createDefaultModel()
    val sourceResource = model.getResource(link.source)
    val targetResource = model.getResource(link.target)
    sourceResource.addProperty(OWL.sameAs, targetResource)
    model
  }
}