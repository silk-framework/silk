package de.fuberlin.wiwiss.silk.hadoop

import impl.HadoopInstanceCache
import org.apache.hadoop.fs.{FileSystem, Path}
import de.fuberlin.wiwiss.silk.config.ConfigLoader
import de.fuberlin.wiwiss.silk.impl.DefaultImplementations

object SilkConfiguration
{
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
    def instanceCachePath = new Path(hadoopConfig.get("silk.instancecache.path"))

    //TODO use default hadoop path instead?
    def outputPath = new Path(hadoopConfig.get("silk.output.path"))

    private lazy val cacheFS = FileSystem.get(instanceCachePath.toUri, hadoopConfig)

    lazy val config =
    {
        DefaultImplementations.register()
        ConfigLoader.load(cacheFS.open(instanceCachePath.suffix("/config.xml")))
    }

    lazy val linkSpec =
    {
        val linkSpecId = hadoopConfig.get("silk.linkSpec", config.linkSpecs.keys.head)
        config.linkSpecs(linkSpecId)
    }

    lazy val sourceCache =
    {
        val numBlocks = linkSpec.blocking.map(_.blocks).getOrElse(1)
        new HadoopInstanceCache(cacheFS, instanceCachePath.suffix("/source/" + linkSpec.id + "/"), numBlocks)
    }

    lazy val targetCache =
    {
        val numBlocks = linkSpec.blocking.map(_.blocks).getOrElse(1)
        new HadoopInstanceCache(cacheFS, instanceCachePath.suffix("/target/" + linkSpec.id + "/"), numBlocks)
    }
}
