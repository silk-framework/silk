package de.fuberlin.wiwiss.silk.jena

import de.fuberlin.wiwiss.silk.datasource.DataSource


object JenaImplementations
{
  def register()
  {
    DataSource.register(classOf[RdfDataSource])
    DataSource.register(classOf[FileDataSource])
    DataSource.register(classOf[LinkedDataSource])
  }
}