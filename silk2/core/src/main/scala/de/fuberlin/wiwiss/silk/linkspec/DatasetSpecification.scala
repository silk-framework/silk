package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.datasource.DataSource

case class DatasetSpecification(val dataSource : DataSource, val variable : String, val restriction : String)