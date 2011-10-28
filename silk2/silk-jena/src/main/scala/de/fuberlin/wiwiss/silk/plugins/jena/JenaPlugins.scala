package de.fuberlin.wiwiss.silk.plugins.jena

import de.fuberlin.wiwiss.silk.datasource.DataSource

object JenaPlugins {
  def register() {
    DataSource.register(classOf[FileDataSource])
    //DataSource.register(classOf[RdfDataSource])
    //DataSource.register(classOf[LinkedDataSource])
  }
}