package de.fuberlin.wiwiss.silk.linkspec

import condition.Blocking
import de.fuberlin.wiwiss.silk.output.Output
import de.fuberlin.wiwiss.silk.util.SourceTargetPair

/**
 * @param id The id which identifies this link specification. May only contain alphanumeric characters (a - z, 0 - 9).
 */
//TODO make LinkSpecification self contained by including a link to the prefixes?
//TODO move blocking to configuration
case class LinkSpecification(val id : String, val linkType : String, val datasets : SourceTargetPair[DatasetSpecification],
                             val blocking : Option[Blocking], val condition : LinkCondition,
                             val filter : LinkFilter, val outputs : Traversable[Output])
{
  require(id.forall(c => (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')),
          "A link specification ID may only contain alphanumeric characters (a - z, 0 - 9). The following id is not valid: '" + id + "'")
}
