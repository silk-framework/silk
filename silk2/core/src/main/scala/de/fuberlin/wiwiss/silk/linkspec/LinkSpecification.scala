package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.output.Output

case class LinkSpecification(val linkType : String, val sourceDatasetSpecification : DatasetSpecification,
                             val targetDatasetSpecification : DatasetSpecification, val blocking : Option[Blocking], val condition : LinkCondition,
                             val filter : LinkFilter, val outputs : Traversable[Output])