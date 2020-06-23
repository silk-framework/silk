package org.silkframework.dataset

import org.silkframework.entity.paths.TypedPath
import org.silkframework.entity.rdf.SparqlRestriction
import org.silkframework.runtime.activity.UserContext

/**
  * Adds a more generic retrieve paths method than the one defined in [[DataSource]].
  */
trait SparqlRestrictionDataSource { this: DataSource =>
  /** Returns the direct paths, i.e. of length 1, that comply with the SPARQL restriction */
  def retrievePathsSparqlRestriction(sparqlRestriction: SparqlRestriction,
                                     limit: Option[Int] = None)
                                    (implicit userContext: UserContext): IndexedSeq[TypedPath]
}
