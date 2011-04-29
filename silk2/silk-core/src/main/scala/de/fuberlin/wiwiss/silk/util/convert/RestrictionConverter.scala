package de.fuberlin.wiwiss.silk.util.convert

import de.fuberlin.wiwiss.silk.instance.{Restriction, SparqlRestriction}

/**
 * Converts a SPARQL restriction to a Silk restriction.
 */
object RestrictionConverter
{
  def apply(subjectVar : String, sparqlRestriction : SparqlRestriction) : Restriction =
  {
    //Dummy
    Restriction.empty
  }
}