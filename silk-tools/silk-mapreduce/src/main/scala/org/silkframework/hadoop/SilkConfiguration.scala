/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.silkframework.hadoop

import org.apache.hadoop.fs.{FileSystem, Path}
import org.silkframework.config.LinkingConfig
import org.silkframework.runtime.resource.EmptyResourceManager
import org.silkframework.hadoop.impl.HadoopEntityCache
import org.silkframework.runtime.serialization.ReadContext
import scala.xml.XML

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
    // FIXME:
    // Plugins.register()
    // JenaPlugins.register()
    LinkingConfig.LinkingConfigFormat.read(XML.load(cacheFS.open(entityCachePath.suffix("/config.xml"))))(new ReadContext(EmptyResourceManager))
  }

  lazy val linkSpec = {
    val linkSpecId = hadoopConfig.get(SilkConfiguration.LinkSpecParam, config.linkSpecs.head.id)
    config.linkSpec(linkSpecId)
  }

  lazy val entityDescs = linkSpec.entityDescriptions

  lazy val sourceCache = {
    new HadoopEntityCache(entityDescs.source, linkSpec.rule.index(_, true), cacheFS, entityCachePath.suffix("/source/" + linkSpec.id + "/"), config.runtime)
  }

  lazy val targetCache = {
    new HadoopEntityCache(entityDescs.target, linkSpec.rule.index(_, false), cacheFS, entityCachePath.suffix("/target/" + linkSpec.id + "/"), config.runtime)
  }
}
