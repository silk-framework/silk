package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.datasource.Source

case class DatasetSpecification(val source : Source, val variable : String, val restriction : String)