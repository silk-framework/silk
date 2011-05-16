package de.fuberlin.wiwiss.silk.util.convert

import de.fuberlin.wiwiss.silk.instance.{Restriction, SparqlRestriction}
import de.fuberlin.wiwiss.silk.config.Prefixes

/**
 * Converts a SPARQL restriction to a Silk restriction.
 */
object RestrictionConverter
{
  def apply(subjectVar : String, sparqlRestriction : SparqlRestriction)(implicit prefixes : Prefixes) : Restriction =
  {
    //Dummy
    Restriction.empty
  }
}