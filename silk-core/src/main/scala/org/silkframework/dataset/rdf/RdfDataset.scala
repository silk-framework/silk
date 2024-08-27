package org.silkframework.dataset.rdf

import org.silkframework.dataset.DatasetCharacteristics.{SpecialPathInfo, SuggestedForEnum, SupportedPathExpressions}
import org.silkframework.dataset.{Dataset, DatasetCharacteristics}
import org.silkframework.entity.rdf.SparqlEntitySchema.specialPaths

trait RdfDataset extends Dataset {

  def sparqlEndpoint: SparqlEndpoint

  /**
    * URI of the graph this RDF dataset is referring to, if any.
    */
  def graphOpt: Option[String] = None

  /** Shared characteristics of all RDF Datasets */
  override final val characteristics: DatasetCharacteristics = {
    DatasetCharacteristics(
      SupportedPathExpressions(
        multiHopPaths = true,
        backwardPaths = true,
        propertyFilter = true,
        languageFilter = true,
        specialPaths = Seq(
          SpecialPathInfo(specialPaths.LANG, Some("Returns the language tag of a language-tagged literal."), SuggestedForEnum.ValuePathOnly),
          SpecialPathInfo(specialPaths.TEXT, Some("Returns the lexical value of the resource or literal this is requested from."), SuggestedForEnum.ValuePathOnly)
        )
      ),
      supportsMultipleTables = true,
      supportsMultipleWrites = true,
      typedEntities = true
    )
  }

}
