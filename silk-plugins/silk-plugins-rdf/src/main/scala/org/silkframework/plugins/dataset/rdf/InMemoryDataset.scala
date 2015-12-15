package org.silkframework.plugins.dataset.rdf

import java.io.StringReader
import java.util.logging.{Level, Logger}

import com.hp.hpl.jena.rdf.model.ModelFactory
import org.silkframework.dataset._
import org.silkframework.entity.{Entity, EntitySchema, Path}
import org.silkframework.entity.rdf.{SparqlEntitySchema, SparqlRestriction}
import org.silkframework.plugins.dataset.rdf.endpoint.JenaModelEndpoint
import org.silkframework.plugins.dataset.rdf.sparql.{EntityRetriever, SparqlAggregatePathsCollector, SparqlTypesCollector}
import org.silkframework.util.Uri

case class InMemoryDataset() extends DatasetPlugin {



  /**
    * Returns a data source for reading entities from the data set.
    */
  override def source: DataSource = ???

  /**
    * Returns a entity sink for writing entities to the data set.
    */
  override def entitySink: EntitySink = ???

  /**
    * Returns a link sink for writing entity links to the data set.
    */
  override def linkSink: LinkSink = ???
}
