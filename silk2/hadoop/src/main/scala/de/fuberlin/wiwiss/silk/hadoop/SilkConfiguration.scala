package de.fuberlin.wiwiss.silk.hadoop

import impl.HadoopInstanceCache
import org.apache.hadoop.fs.{FileSystem, Path}
import de.fuberlin.wiwiss.silk.config.ConfigReader
import de.fuberlin.wiwiss.silk.impl.DefaultImplementations
import de.fuberlin.wiwiss.silk.instance.InstanceSpecification

object SilkConfiguration
{
    val InputParam = "silk.inputpath"
    val OutputParam = "silk.outputpath"
    val LinkSpecParam = "silk.linkspec"

    @volatile var config : SilkConfiguration = null

    def get(hadoopConfig : org.apache.hadoop.conf.Configuration) =
    {
        //This method is not synchronized, because multiple instantiations of SilkConfiguration are not be a problem
        if(config == null)
        {
            config = new SilkConfiguration(hadoopConfig)
        }
        config
    }
}

class SilkConfiguration private(hadoopConfig : org.apache.hadoop.conf.Configuration)
{
    def instanceCachePath = new Path(hadoopConfig.get(SilkConfiguration.InputParam))

    def outputPath = new Path(hadoopConfig.get(SilkConfiguration.OutputParam))

    private lazy val cacheFS = FileSystem.get(instanceCachePath.toUri, hadoopConfig)

    lazy val config =
    {
        DefaultImplementations.register()
        ConfigReader.read(cacheFS.open(instanceCachePath.suffix("/config.xml")))
    }

    lazy val linkSpec =
    {
        val linkSpecId = hadoopConfig.get(SilkConfiguration.LinkSpecParam, config.linkSpecs.head.id)
        config.linkSpec(linkSpecId).get
    }

    lazy val instanceSpecs = InstanceSpecification.retrieve(config, linkSpec)

    lazy val sourceCache =
    {
        val numBlocks = linkSpec.blocking.map(_.blocks).getOrElse(1)
        new HadoopInstanceCache(instanceSpecs.source, cacheFS, instanceCachePath.suffix("/source/" + linkSpec.id + "/"), numBlocks)
    }

    lazy val targetCache =
    {
        val numBlocks = linkSpec.blocking.map(_.blocks).getOrElse(1)
        new HadoopInstanceCache(instanceSpecs.target, cacheFS, instanceCachePath.suffix("/target/" + linkSpec.id + "/"), numBlocks)
    }
}
