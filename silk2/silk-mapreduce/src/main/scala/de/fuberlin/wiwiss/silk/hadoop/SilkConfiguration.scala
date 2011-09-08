package de.fuberlin.wiwiss.silk.hadoop

import impl.HadoopInstanceCache
import org.apache.hadoop.fs.{FileSystem, Path}
import de.fuberlin.wiwiss.silk.impl.DefaultImplementations
import de.fuberlin.wiwiss.silk.instance.InstanceSpecification
import de.fuberlin.wiwiss.silk.config.SilkConfig

object SilkConfiguration {
  val InputParam = "silk.inputpath"
  val OutputParam = "silk.outputpath"
  val LinkSpecParam = "silk.linkspec"

  @volatile var config : SilkConfiguration = null

  def get(hadoopConfig : org.apache.hadoop.conf.Configuration) = {
    //This method is not synchronized, because multiple instantiations of SilkConfiguration are not a problem
    if(config == null) {
      config = new SilkConfiguration(hadoopConfig)
    }
    config
  }
}

class SilkConfiguration private(hadoopConfig : org.apache.hadoop.conf.Configuration) {
  def instanceCachePath = new Path(hadoopConfig.get(SilkConfiguration.InputParam))

  def outputPath = new Path(hadoopConfig.get(SilkConfiguration.OutputParam))

  private lazy val cacheFS = FileSystem.get(instanceCachePath.toUri, hadoopConfig)

  lazy val config = {
    DefaultImplementations.register()
    SilkConfig.load(cacheFS.open(instanceCachePath.suffix("/config.xml")))
  }

  lazy val linkSpec = {
    val linkSpecId = hadoopConfig.get(SilkConfiguration.LinkSpecParam, config.linkSpecs.head.id)
    config.linkSpec(linkSpecId)
  }

  lazy val instanceSpecs = InstanceSpecification.retrieve(linkSpec)

  lazy val sourceCache = {
    new HadoopInstanceCache(instanceSpecs.source, cacheFS, instanceCachePath.suffix("/source/" + linkSpec.id + "/"), config.runtime)
  }

  lazy val targetCache = {
    new HadoopInstanceCache(instanceSpecs.target, cacheFS, instanceCachePath.suffix("/target/" + linkSpec.id + "/"), config.runtime)
  }
}
