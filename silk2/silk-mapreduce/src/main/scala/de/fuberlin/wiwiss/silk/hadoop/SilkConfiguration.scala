package de.fuberlin.wiwiss.silk.hadoop

import impl.HadoopEntityCache
import org.apache.hadoop.fs.{FileSystem, Path}
import de.fuberlin.wiwiss.silk.plugins.Plugins
import de.fuberlin.wiwiss.silk.entity.EntityDescription
import de.fuberlin.wiwiss.silk.config.LinkingConfig

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
  def entityCachePath = new Path(hadoopConfig.get(SilkConfiguration.InputParam))

  def outputPath = new Path(hadoopConfig.get(SilkConfiguration.OutputParam))

  private lazy val cacheFS = FileSystem.get(entityCachePath.toUri, hadoopConfig)

  lazy val config = {
    Plugins.register()
    LinkingConfig.load(cacheFS.open(entityCachePath.suffix("/config.xml")))
  }

  lazy val linkSpec = {
    val linkSpecId = hadoopConfig.get(SilkConfiguration.LinkSpecParam, config.linkSpecs.head.id)
    config.linkSpec(linkSpecId)
  }

  lazy val entityDescs = EntityDescription.retrieve(linkSpec)

  lazy val sourceCache = {
    new HadoopEntityCache(entityDescs.source, cacheFS, entityCachePath.suffix("/source/" + linkSpec.id + "/"), config.runtime)
  }

  lazy val targetCache = {
    new HadoopEntityCache(entityDescs.target, cacheFS, entityCachePath.suffix("/target/" + linkSpec.id + "/"), config.runtime)
  }
}
