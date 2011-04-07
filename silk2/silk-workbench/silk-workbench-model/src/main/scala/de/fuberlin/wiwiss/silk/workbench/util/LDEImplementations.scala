package de.fuberlin.wiwiss.silk.workbench.util

import de.fuberlin.wiwiss.silk.datasource.DataSource

/**
 * Registers all LDE implementations.
 */
object LDEImplementations
{
    def register()
    {
        DataSource.register(classOf[LDEDataSource])
    }
}