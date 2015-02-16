package de.fuberlin.wiwiss.silk.dataset.rdf

import scala.collection.immutable.SortedMap

case class ResultSet(bindings: Traversable[SortedMap[String, RdfNode]])
