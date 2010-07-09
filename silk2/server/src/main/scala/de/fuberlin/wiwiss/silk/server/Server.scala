package de.fuberlin.wiwiss.silk.server

import de.fuberlin.wiwiss.silk.Silk
import java.io.File
import de.fuberlin.wiwiss.silk.config.ConfigLoader
import de.fuberlin.wiwiss.silk.datasource.DataSource
import de.fuberlin.wiwiss.silk.instance.InstanceSpecification

object Server
{
    val configFile = new File("./src/main/resources/de/fuberlin/wiwiss/silk/server/config/sider_drugbank_drugs.xml")

    def load()
    {
        val config = ConfigLoader.load(configFile)

        for(linkSpec <- config.linkSpecs.values)
        {
            val silk = new Silk(config, linkSpec)
            silk.load()
        }
    }

    def process(source : DataSource) : String =
    {
        val config = ConfigLoader.load(configFile)

        val linkSpec = config.linkSpecs.values.head

        val (sourceInstanceSpec, targetInstanceSpec) = InstanceSpecification.retrieve(linkSpec)

        val instances = source.retrieve(sourceInstanceSpec, config.prefixes)

        instances.head.toString
    }
}
